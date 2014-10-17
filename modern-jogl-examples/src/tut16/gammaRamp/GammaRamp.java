/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut16.gammaRamp;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import glsl.GLSLProgramObject;
import glutil.MatrixStack;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import jglm.Mat4;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class GammaRamp implements GLEventListener, KeyListener {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final GammaRamp gammaRamp = new GammaRamp();
        gammaRamp.initGL();

        Frame frame = new Frame("Tutorial 14 - Material Texture");

        frame.add(gammaRamp.newtCanvasAWT);

        frame.setSize(gammaRamp.glWindow.getWidth(), gammaRamp.glWindow.getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(gammaRamp.glWindow, 30);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                fPSAnimator.stop();
                gammaRamp.glWindow.destroy();
                System.exit(0);
            }
        });

        fPSAnimator.start();
        frame.setVisible(true);
    }

    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvasAWT;
    private int[] programs;
    private int[] uniformBlockBuffers;
    private int[] objects;
    private int[] textures;
    private int[] samplerObj;
    private boolean[] useGammaCorrect = new boolean[]{false, false};

    public GammaRamp() {

    }

    public void initGL() {

        GLProfile gLProfile = GLProfile.get(GLProfile.GL3);

        GLCapabilities gLCapabilities = new GLCapabilities(gLProfile);

        glWindow = GLWindow.create(gLCapabilities);

        glWindow.setSize(1024, 768);

        glWindow.addGLEventListener(this);
        glWindow.addKeyListener(this);
//        glWindow.addMouseListener(this);
        /*
         *  We combine NEWT GLWindow inside existing AWT application (the main JFrame) 
         *  by encapsulating the glWindow inside a NewtCanvasAWT canvas.
         */
        newtCanvasAWT = new NewtCanvasAWT(glWindow);
    }

    @Override
    public void init(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        initializePrograms(gl3);

        initializeVertexData(gl3);

        loadTextures(gl3);

        //Setup our Uniform Buffers
        uniformBlockBuffers = new int[UniformBlockBinding.size.ordinal()];

        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBinding.projection.ordinal());

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, 16 * 4, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.projection.ordinal(),
                    uniformBlockBuffers[UniformBlockBinding.projection.ordinal()], 0, 16 * 4);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    private void initializePrograms(GL3 gl3) {

        programs = new int[Programs.size.ordinal()];

        String filepath = "/tut16/gammaRamp/shaders/";

        GLSLProgramObject noGammaProgram = new GLSLProgramObject(gl3, filepath, "screenCoords_VS.glsl",
                "textureNoGamma_FS.glsl");
        programs[Programs.noGamma.ordinal()] = noGammaProgram.getProgramId();

        GLSLProgramObject gammaProgram = new GLSLProgramObject(gl3, filepath, "screenCoords_VS.glsl",
                "textureGamma_FS.glsl");
        programs[Programs.gamma.ordinal()] = gammaProgram.getProgramId();

        int projectionUBI = gl3.glGetUniformBlockIndex(noGammaProgram.getProgramId(), "Projection");
        gl3.glUniformBlockBinding(noGammaProgram.getProgramId(), projectionUBI, UniformBlockBinding.projection.ordinal());

        int colorTextureUL = gl3.glGetUniformLocation(noGammaProgram.getProgramId(), "colorTexture");

        gl3.glUseProgram(noGammaProgram.getProgramId());
        {
            gl3.glUniform1i(colorTextureUL, TexUnit.gammaRamp.ordinal());
        }
        gl3.glUseProgram(0);

        projectionUBI = gl3.glGetUniformBlockIndex(gammaProgram.getProgramId(), "Projection");
        gl3.glUniformBlockBinding(gammaProgram.getProgramId(), projectionUBI, UniformBlockBinding.projection.ordinal());

        colorTextureUL = gl3.glGetUniformLocation(gammaProgram.getProgramId(), "colorTexture");

        gl3.glUseProgram(gammaProgram.getProgramId());
        {
            gl3.glUniform1i(colorTextureUL, TexUnit.gammaRamp.ordinal());
        }
        gl3.glUseProgram(0);
    }

    private void initializeVertexData(GL3 gl3) {

        int[] vertexData = new int[]{
            90, 80, 0, 0,
            90, 16, 0, 1,
            410, 80, 1, 0,
            410, 16, 1, 1,
            90, 176, 0, 0,
            90, 112, 0, 1,
            410, 176, 1, 0,
            410, 112, 1, 1};

        objects = new int[Objects.size.ordinal()];

        gl3.glGenBuffers(1, objects, Objects.dataBuffer.ordinal());

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, objects[Objects.dataBuffer.ordinal()]);
        {
            IntBuffer intBuffer = GLBuffers.newDirectIntBuffer(vertexData);
            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * 4, intBuffer, GL3.GL_STATIC_DRAW);

            gl3.glGenVertexArrays(1, objects, Objects.vao.ordinal());

            gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
            {
                int stride = 4 * 4;
                int offset = 0;
                gl3.glEnableVertexAttribArray(0);
                {
                    gl3.glVertexAttribPointer(0, 2, GL3.GL_UNSIGNED_INT, false, stride, offset);
                }
                offset = 2 * 4;
                gl3.glEnableVertexAttribArray(5);
                {
                    gl3.glVertexAttribPointer(5, 2, GL3.GL_UNSIGNED_INT, false, stride, offset);
                }
            }
            gl3.glBindVertexArray(0);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void loadTextures(GL3 gl3) {

        textures = new int[2];

        gl3.glGenTextures(2, textures, 0);

        String filePath = "/tut16/gammaRamp/data/gamma_ramp.png";
        URL url = getClass().getResource(filePath);
        File file = new File(url.getPath());

        TextureData textureData = null;
        try {
            textureData = TextureIO.newTextureData(gl3.getGLProfile(), file, false, TextureIO.PNG);
        } catch (IOException ex) {
            Logger.getLogger(GammaRamp.class.getName()).log(Level.SEVERE, null, ex);
        }
        Buffer buffer = textureData.getBuffer();
        buffer.rewind();

        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Programs.noGamma.ordinal()]);
        {
            gl3.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGB8, textureData.getWidth(), textureData.getHeight(),
                    0, textureData.getPixelFormat(), textureData.getPixelType(), buffer);

            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Programs.gamma.ordinal()]);
        {
            gl3.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_SRGB8, textureData.getWidth(), textureData.getHeight(),
                    0, textureData.getPixelFormat(), textureData.getPixelType(), buffer);

            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);

        samplerObj = new int[1];

        gl3.glGenSamplers(1, samplerObj, 0);
        gl3.glSamplerParameteri(samplerObj[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerObj[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerObj[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(samplerObj[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {

    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(0f, .5f, .3f, 0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);

        gl3.glActiveTexture(GL3.GL_TEXTURE0 + TexUnit.gammaRamp.ordinal());
        int texUnit = useGammaCorrect[0] ? Programs.gamma.ordinal() : Programs.noGamma.ordinal();
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[texUnit]);
        {
            gl3.glBindSampler(textures[TexUnit.gammaRamp.ordinal()], samplerObj[0]);
            {
                gl3.glBindVertexArray(objects[Objects.vao.ordinal()]);
                {
                    gl3.glUseProgram(programs[Programs.noGamma.ordinal()]);
                    {
                        gl3.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
                    }
                }
            }
        }
        texUnit = useGammaCorrect[1] ? Programs.gamma.ordinal() : Programs.noGamma.ordinal();
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[texUnit]);
        {
            gl3.glUseProgram(programs[Programs.gamma.ordinal()]);
            {
                gl3.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 4, 4);
            }
            gl3.glBindVertexArray(0);
            gl3.glUseProgram(0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
        gl3.glBindSampler(TexUnit.gammaRamp.ordinal(), 0);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {

        GL3 gl3 = glad.getGL().getGL3();

        MatrixStack persMatrix = new MatrixStack();
        persMatrix.translate(new Vec3(-1f, 1f, 0f));
        persMatrix.scale(new Vec3(2f / w, -2f / h, 1f));

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            FloatBuffer floatBuffer = GLBuffers.newDirectFloatBuffer(persMatrix.top().toFloatArray());

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, floatBuffer);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_1:
                useGammaCorrect[0] = !useGammaCorrect[0];
                if(useGammaCorrect[0])
                    System.out.println("Top sRGB texture.");
                else
                    System.out.println("Top linear texture.");
                break;

            case KeyEvent.VK_2:
                useGammaCorrect[1] = !useGammaCorrect[1];
                if(useGammaCorrect[1])
                    System.out.println("Bottom sRGB texture.");
                else
                    System.out.println("Bottom linear texture.");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {

    }

    public enum Objects {

        dataBuffer,
        vao,
        size
    }

    public enum UniformBlockBinding {

        projection,
        size
    }

    public enum TexUnit {

        gammaRamp,
        size
    }

    private enum Programs {

        noGamma,
        gamma,
        size
    }
}
