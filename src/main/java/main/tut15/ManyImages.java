
package main.tut15;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import glm.mat.Mat4;
import glm.vec._3.Vec3;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import main.tut14.PerspectiveInterpolation;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.time.Timer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINEAR_MIPMAP_LINEAR;
import static com.jogamp.opengl.GL.GL_LINEAR_MIPMAP_NEAREST;
import static com.jogamp.opengl.GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_REPEAT;
import static com.jogamp.opengl.GL.GL_RGB;
import static com.jogamp.opengl.GL.GL_RGB8;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_UNPACK_ALIGNMENT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;


/**
 * @author gbarbieri
 */

public class ManyImages extends Framework {

    public static void main(String[] args) {
        new ManyImages().setup("Tutorial 15 - Many Images");
    }

    private ProgramData program;

    private Mesh plane, corridor;

    private IntBuffer projBufferName = GLBuffers.newDirectIntBuffer(1);

    interface Texture {
        int Checker = 0;
        int MipmapTest = 1;
        int MAX = 2;
    }

    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(Sampler.MAX);

    private Timer camTimer = new Timer(Timer.Type.Loop, 5f);
    private boolean useMipmapTexture = false;
    private int currSampler = 0;
    private boolean drawCorridor = false;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            plane = new Mesh(gl, getClass(), "tut15/BigPlane.xml");
            corridor = new Mesh(gl, getClass(), "tut15/Corridor.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(PerspectiveInterpolation.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        float depthZNear = 0f;
        float depthZFar = 1f;

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(depthZNear, depthZFar);
        gl.glEnable(GL_DEPTH_CLAMP);

        //Setup our Uniform Buffers
        gl.glGenBuffers(1, projBufferName);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, projBufferName.get(0));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projBufferName.get(0), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        // Generate all the texture names
        gl.glGenTextures(Texture.MAX, textureName);

        loadCheckerTexture(gl);
        loadMipmapTexture(gl);
        createSamplers(gl);
    }

    private void initializeProgram(GL3 gl) {
        program = new ProgramData(gl, "pt.vert", "tex.frag");
    }

    private void loadCheckerTexture(GL3 gl) {

        try {
            File file = new File(getClass().getResource("/tut15/checker.dds").toURI());

            DDSImage ddsImage = DDSImage.read(file);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.Checker));

            for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {

                DDSImage.ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);

                gl.glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_RGB8, mipmap.getWidth(), mipmap.getHeight(), 0,
                        GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
            }

            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);
            gl.glBindTexture(GL_TEXTURE_2D, 0);

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(ManyImages.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadMipmapTexture(GL3 gl) {

        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.MipmapTest));

        IntBuffer oldAlign = GLBuffers.newDirectIntBuffer(1);

        gl.glGetIntegerv(GL_UNPACK_ALIGNMENT, oldAlign);
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        for (int mipmapLevel = 0; mipmapLevel < 8; mipmapLevel++) {

            int width = 128 >> mipmapLevel;
            int height = 128 >> mipmapLevel;

            byte[] currColor = mipmapColors[mipmapLevel];
            ByteBuffer buffer = fillWithColors(currColor, width, height);

            gl.glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer);

            destroyBuffer(buffer);
        }

        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, oldAlign.get(0));

        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 7);

        gl.glBindTexture(GL_TEXTURE_2D, 0);

        destroyBuffer(oldAlign);
    }

    private byte[][] mipmapColors = {
            {(byte) 0xFF, (byte) 0xFF, (byte) 0x00},
            {(byte) 0xFF, (byte) 0x00, (byte) 0xFF},
            {(byte) 0x00, (byte) 0xFF, (byte) 0xFF},
            {(byte) 0xFF, (byte) 0x00, (byte) 0x00},
            {(byte) 0x00, (byte) 0xFF, (byte) 0x00},
            {(byte) 0x00, (byte) 0x00, (byte) 0xFF},
            {(byte) 0x00, (byte) 0x00, (byte) 0x00},
            {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}};

    private ByteBuffer fillWithColors(byte[] color, int width, int height) {

        int numTexels = width * height;
        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(numTexels * 3);

        byte red = color[0];
        byte green = color[1];
        byte blue = color[2];

        while(buffer.hasRemaining())
            buffer
                    .put(red)
                    .put(green)
                    .put(blue);

        buffer.position(0);
        return buffer;
    }

    private void createSamplers(GL3 gl) {

        gl.glGenSamplers(Sampler.MAX, samplerName);

        for (int samplerIx = 0; samplerIx < Sampler.MAX; samplerIx++) {

            gl.glSamplerParameteri(samplerName.get(samplerIx), GL_TEXTURE_WRAP_S, GL_REPEAT);
            gl.glSamplerParameteri(samplerName.get(samplerIx), GL_TEXTURE_WRAP_T, GL_REPEAT);
        }

        gl.glSamplerParameteri(samplerName.get(Sampler.Nearest), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(samplerName.get(Sampler.Nearest), GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        gl.glSamplerParameteri(samplerName.get(Sampler.Linear), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.Linear), GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        gl.glSamplerParameteri(samplerName.get(Sampler.Linear_MipMap_Nearest), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.Linear_MipMap_Nearest), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);

        gl.glSamplerParameteri(samplerName.get(Sampler.Linear_MipMap_Linear), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.Linear_MipMap_Linear), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

        gl.glSamplerParameteri(samplerName.get(Sampler.LowAnysotropic), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.LowAnysotropic), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glSamplerParameterf(samplerName.get(Sampler.LowAnysotropic), GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f);


        int maxAniso = caps.limits.MAX_TEXTURE_MAX_ANISOTROPY_EXT;

        System.out.println("Maximum anisotropy: " + maxAniso);

        gl.glSamplerParameteri(samplerName.get(Sampler.MaxAnysotropic), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.MaxAnysotropic), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glSamplerParameteri(samplerName.get(Sampler.MaxAnysotropic), GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
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

        modelMatrix
                .applyMatrix(worldToCamMat)
                .push();
        {
            gl.glUseProgram(program.theProgram);

            gl.glUniformMatrix4fv(program.modelToCameraMatrixUL, 1, false, modelMatrix.top().to(matBuffer));

            gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(useMipmapTexture ? Texture.MipmapTest : Texture.Checker));
            gl.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(currSampler));

            if (drawCorridor)
                corridor.render(gl, "tex");
            else
                plane.render(gl, "tex");

            gl.glBindSampler(Semantic.Sampler.DIFFUSE, 0);
            gl.glBindTexture(GL_TEXTURE_2D, 0);

            gl.glUseProgram(0);
        }
        modelMatrix.pop();
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        MatrixStack persMatrix = new MatrixStack();
        persMatrix.perspective(90f, w / (float) h, 1f, 1000f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projBufferName.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, persMatrix.top().to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        plane.dispose(gl);
        corridor.dispose(gl);

        gl.glDeleteProgram(program.theProgram);

        gl.glDeleteBuffers(1, projBufferName);
        gl.glDeleteTextures(Texture.MAX, textureName);
        gl.glDeleteSamplers(Sampler.MAX, samplerName);

        destroyBuffers(projBufferName, textureName, samplerName);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_SPACE:
                useMipmapTexture = !useMipmapTexture;
                break;

            case KeyEvent.VK_Y:
                drawCorridor = !drawCorridor;
                break;

            case KeyEvent.VK_P:
                camTimer.togglePause();
                break;
        }

        if(KeyEvent.VK_1 <= ke.getKeyCode() && ke.getKeyCode() <= KeyEvent.VK_9) {
            int number = ke.getKeyCode() - KeyEvent.VK_1;
            if(number < Sampler.MAX) {
                System.out.println("Sampler: "+samplerNames[number]);
                currSampler = number;
            }
        }
    }

    interface Sampler {
        int Nearest = 0;
        int Linear = 1;
        int Linear_MipMap_Nearest = 2;
        int Linear_MipMap_Linear = 3;
        int LowAnysotropic = 4;
        int MaxAnysotropic = 5;
        int MAX = 6;
    }

    private String[] samplerNames = {"Nearest", "Linear", "Linear with nearest mipmaps", "Linear with linear mipmaps",
            "Low anisotropic", "Max anisotropic"};

    private class ProgramData {

        public int theProgram;
        public int modelToCameraMatrixUL;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut15", vertex, fragment);

            modelToCameraMatrixUL = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

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