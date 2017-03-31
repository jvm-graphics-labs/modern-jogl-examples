
package main.tut16;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import com.jogamp.opengl.util.texture.spi.DDSImage.ImageInfo;
import glm.mat.Mat4;
import glm.vec._3.Vec3;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.time.Timer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static glm.GlmKt.glm;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class GammaCheckers extends Framework {

    public static void main(String[] args) {
        new GammaCheckers().setup("Tutorial 16 - Gamma Checkers");
    }

    private ProgramData progNoGamma, progGamma;

    private Mesh plane, corridor;

    private IntBuffer projBufferName = GLBuffers.newDirectIntBuffer(1);

    private interface Texture {
        int Linear = 0;
        int Gamma = 1;
        int MAX = 2;
    }

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);

    private interface Samplers {

        int LinearMipmapLinear = 0;
        int MaxAnisotropic = 1;
        int MAX = 2;
    }

    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(Samplers.MAX);

    private Timer camTimer = new Timer(Timer.Type.Loop, 5f);

    private boolean drawGammaProgram = false;
    private boolean drawGammaTexture = false;
    private int currSampler = 0;
    private boolean drawCorridor = false;

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            corridor = new Mesh(gl, getClass(), "tut16/Corridor.xml");
            plane = new Mesh(gl, getClass(), "tut16/BigPlane.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        float depthZNear = 0f;
        float depthZFar = 1f;

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRange(depthZNear, depthZFar);
        gl.glEnable(GL_DEPTH_CLAMP);

        //Setup our Uniform Buffers
        gl.glGenBuffers(1, projBufferName);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, projBufferName.get(0));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projBufferName.get(0), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        loadCheckerTextures(gl);
        createSamplers(gl);
    }

    private void initializePrograms(GL3 gl) {

        progNoGamma = new ProgramData(gl, "pt.vert", "texture-no-gamma.frag");
        progGamma = new ProgramData(gl, "pt.vert", "texture-gamma.frag");
    }

    private void loadCheckerTextures(GL3 gl) {

        gl.glGenTextures(Texture.MAX, textureName);

        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.Linear));


        try {
            File file = new File(getClass().getResource("/tut16/checker_linear.dds").toURI());
            DDSImage ddsImage = DDSImage.read(file);

            for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {

                ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);

                gl.glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_SRGB8, mipmap.getWidth(), mipmap.getHeight(), 0,
                        GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
            }
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(GammaCheckers.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.Gamma));

        try {
            File file = new File(getClass().getResource("/tut16/checker_gamma.dds").toURI());
            DDSImage ddsImage = DDSImage.read(file);

            for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {

                ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);

                gl.glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_SRGB8, mipmap.getWidth(), mipmap.getHeight(), 0,
                        GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
            }
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(GammaCheckers.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void createSamplers(GL3 gl) {

        gl.glGenSamplers(Samplers.MAX, samplerName);

        for (int samplerIx = 0; samplerIx < Samplers.MAX; samplerIx++) {

            gl.glSamplerParameteri(samplerName.get(samplerIx), GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glSamplerParameteri(samplerName.get(samplerIx), GL_TEXTURE_WRAP_T, GL_REPEAT);
        }

        gl.glSamplerParameteri(samplerName.get(Samplers.LinearMipmapLinear), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Samplers.LinearMipmapLinear), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

        int maxAniso = caps.limits.MAX_TEXTURE_MAX_ANISOTROPY_EXT;

        gl.glSamplerParameteri(samplerName.get(Samplers.MaxAnisotropic), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Samplers.MaxAnisotropic), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Samplers.MaxAnisotropic), GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, .75f).put(1, .75f).put(2, 1f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        camTimer.update();

        float cyclicAngle = camTimer.getAlpha() * 6.28f;
        float hOffset = glm.cos(cyclicAngle) * .25f;
        float vOffset = glm.sin(cyclicAngle) * .25f;

        MatrixStack modelMatrix = new MatrixStack();
        Mat4 worldToCamMat = glm.lookAt(
                new Vec3(hOffset, 1f, -64f),
                new Vec3(hOffset, -5f + vOffset, -44f),
                new Vec3(0f, 1f, 0f));

        modelMatrix.applyMatrix(worldToCamMat);

        ProgramData prog = drawGammaProgram ? progGamma : progNoGamma;

        gl.glUseProgram(prog.theProgram);
        gl.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

        gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(drawGammaTexture ? Texture.Gamma : Texture.Linear));
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(currSampler));

        if (drawCorridor)
            corridor.render(gl, "tex");
        else
            plane.render(gl, "tex");

        gl.glBindSampler(Semantic.Sampler.DIFFUSE, 0);
        gl.glBindTexture(GL_TEXTURE_2D, 0);

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        Mat4 cameraToClipMatrix = glm.perspective(glm.toRad(90f), (w / (float) h), 1, 1000);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projBufferName.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, cameraToClipMatrix.to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        plane.dispose(gl);
        corridor.dispose(gl);

        gl.glDeleteBuffers(1, projBufferName);
        gl.glDeleteTextures(Texture.MAX, textureName);
        gl.glDeleteSamplers(Samplers.MAX, samplerName);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_A:
                drawGammaProgram = !drawGammaProgram;
                break;

            case KeyEvent.VK_G:
                drawGammaTexture = !drawGammaTexture;
                break;

            case KeyEvent.VK_SPACE:
                drawGammaProgram = !drawGammaProgram;
                drawGammaTexture = !drawGammaTexture;
                break;

            case KeyEvent.VK_Y:
                drawCorridor = !drawCorridor;
                break;

            case KeyEvent.VK_P:
                camTimer.togglePause();
                break;

            case KeyEvent.VK_1:
                currSampler = 0;
                break;

            case KeyEvent.VK_2:
                currSampler = 1;
                break;
        }
        System.out.println("----");
        System.out.println("Rendering:\t\t\t" + (drawGammaProgram ? "Gamma" : "Linear"));
        System.out.println("Mipmap Generation:\t" + (drawGammaTexture ? "Gamma" : "Linear"));
    }


    private class ProgramData {

        public int theProgram;
        public int modelToCameraMatrixUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut16", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);

            gl.glUseProgram(theProgram);
            gl.glUniform1i(
                    gl.glGetUniformLocation(theProgram, "colorTexture"),
                    Semantic.Sampler.DIFFUSE);
            gl.glUseProgram(0);
        }
    }
}
