/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut15.manyImages;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import com.jogamp.opengl.util.texture.spi.DDSImage.ImageInfo;
import glutil.MatrixStack;
import glutil.Timer;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec3;
import mesh.Mesh;
import tut15.manyImages.program.ProgramData;

/**
 *
 * @author gbarbieri
 */
public class ManyImages implements GLEventListener, KeyListener {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final ManyImages manyImages = new ManyImages();
        manyImages.initGL();

        Frame frame = new Frame("Tutorial 15 - Many Images");

        frame.add(manyImages.newtCanvasAWT);

        frame.setSize(manyImages.glWindow.getWidth(), manyImages.glWindow.getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(manyImages.glWindow, 30);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                fPSAnimator.stop();
                manyImages.glWindow.destroy();
                System.exit(0);
            }
        });

        fPSAnimator.start();
        frame.setVisible(true);
    }

    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvasAWT;
    private ProgramData program;
    private Mesh plane;
    private Mesh corridor;
    private int[] uniformBlockBuffers = new int[UniformBlockBinding.size.ordinal()];
    private int[] textures = new int[Textures.size.ordinal()];
    private int[] samplers;
    private Timer camTimer = new Timer(Timer.Type.Loop, 5f);
    private boolean useMipmapTexture = false;
    private int currSampler = 0;
    private boolean drawCorridor = false;

    public ManyImages() {

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

        initializeProgram(gl3);

        String dataPath = "/tut15/manyImages/data/";

        plane = new Mesh(dataPath + "BigPlane.xml", gl3);
        corridor = new Mesh(dataPath + "Corridor.xml", gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0f, 1f);
        gl3.glEnable(GL3.GL_DEPTH_CLAMP);

        //Setup our Uniform Buffers
        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBinding.projection.ordinal());
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, 16 * 4, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.projection.ordinal(),
                    uniformBlockBuffers[UniformBlockBinding.projection.ordinal()], 0, 16 * 4);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        loadCheckerTexture(gl3, dataPath);

        loadMipmapTexture(gl3);

        createSamplers(gl3);
    }

    private void initializeProgram(GL3 gl3) {

        String shadersPath = "/tut15/manyImages/shaders/";

        program = new ProgramData(gl3, shadersPath, "PT_VS.glsl", "Tex_FS.glsl");
    }

    private void loadCheckerTexture(GL3 gl3, String dataPath) {

        URL url = getClass().getResource(dataPath + "checker.dds");
        File file = new File(url.getPath());

        DDSImage ddsImage = null;

        try {
            ddsImage = DDSImage.read(file);
        } catch (IOException ex) {
            Logger.getLogger(ManyImages.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl3.glGenTextures(1, textures, Textures.checker.ordinal());
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Textures.checker.ordinal()]);
        {
            for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {

                ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);

                gl3.glTexImage2D(GL3.GL_TEXTURE_2D, mipmapLevel, GL3.GL_RGB8, mipmap.getWidth(), mipmap.getHeight(),
                        0, GL3.GL_BGRA, GL3.GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
            }

            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
    }

    private void loadMipmapTexture(GL3 gl3) {

        byte[] mipmapColors = new byte[]{
            (byte) 0xFF, (byte) 0xFF, (byte) 0x00,
            (byte) 0xFF, (byte) 0x00, (byte) 0xFF,
            (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0xFF, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0xFF,
            (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        gl3.glGenTextures(1, textures, Textures.mipmapTest.ordinal());

        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Textures.mipmapTest.ordinal()]);
        {
            int[] oldAlign = new int[]{0};

            gl3.glGetIntegerv(GL3.GL_UNPACK_ALIGNMENT, oldAlign, 0);
            gl3.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1);
            {
                for (int mipmapLevel = 0; mipmapLevel < 8; mipmapLevel++) {

                    int width = 128 >> mipmapLevel;
                    int height = 128 >> mipmapLevel;

                    byte[] buffer = fillWithColors(mipmapColors[mipmapLevel * 3 + 0],
                            mipmapColors[mipmapLevel * 3 + 1], mipmapColors[mipmapLevel * 3 + 2], width, height);

                    ByteBuffer byteBuffer = GLBuffers.newDirectByteBuffer(buffer);

                    gl3.glTexImage2D(GL3.GL_TEXTURE_2D, mipmapLevel, GL3.GL_RGB8, width, height,
                            0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, byteBuffer);
                }
            }
            gl3.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, oldAlign[0]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 7);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
    }

    private void createSamplers(GL3 gl3) {

        samplers = new int[6];

        gl3.glGenSamplers(6, samplers, 0);

        for (int samplerIx = 0; samplerIx < 6; samplerIx++) {

            gl3.glSamplerParameteri(samplers[samplerIx], GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
            gl3.glSamplerParameteri(samplers[samplerIx], GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
        }
        //Nearest
        gl3.glSamplerParameteri(samplers[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(samplers[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);

        //Linear
        gl3.glSamplerParameteri(samplers[1], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl3.glSamplerParameteri(samplers[1], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);

        //Linear mipmap Nearest
        gl3.glSamplerParameteri(samplers[2], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl3.glSamplerParameteri(samplers[2], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_NEAREST);

        //Linear mipmap linear
        gl3.glSamplerParameteri(samplers[3], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl3.glSamplerParameteri(samplers[3], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_LINEAR);

        //Low anisotropic
        gl3.glSamplerParameteri(samplers[4], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl3.glSamplerParameteri(samplers[4], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_LINEAR);
        gl3.glSamplerParameterf(samplers[4], GL3.GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f);

        //Max anisotropic
        float[] maxAniso = new float[]{0};

        gl3.glGetFloatv(GL3.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso, 0);

        System.out.println("Maximum anisotropy: " + maxAniso[0]);

        gl3.glSamplerParameteri(samplers[5], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
        gl3.glSamplerParameteri(samplers[5], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR_MIPMAP_LINEAR);
        gl3.glSamplerParameteri(samplers[5], GL3.GL_TEXTURE_MAX_ANISOTROPY_EXT, (int) maxAniso[0]);
    }

    private byte[] fillWithColors(byte red, byte green, byte blue, int width, int height) {

        int numTexels = width * height;
        byte[] buffer = new byte[numTexels * 3];

        for (int i = 0; i < numTexels; i++) {

            buffer[i * 3 + 0] = red;
            buffer[i * 3 + 1] = green;
            buffer[i * 3 + 2] = blue;
        }
        return buffer;
    }

    @Override
    public void dispose(GLAutoDrawable glad) {

    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(.75f, .75f, 1f, 1f);
        gl3.glClearDepth(1f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        if (plane != null && corridor != null) {

            camTimer.update();

            float cyclicAngle = camTimer.getAlpha() * 6.28f;
            float hOffset = (float) (Math.cos(cyclicAngle) * .25f);
            float vOffset = (float) (Math.sin(cyclicAngle) * .25f);

            MatrixStack modelMatrix = new MatrixStack();

            Mat4 worldToCamMat = Mat4.CalcLookAtMatrix(new Vec3(hOffset, 1f, -64f),
                    new Vec3(hOffset, -5f + vOffset, -44f), new Vec3(0f, 1f, 0f));

            modelMatrix.setTop(worldToCamMat);

            modelMatrix.push();
            {
                program.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(program.getModelToCameraMatrixUL(), 1, false,
                            modelMatrix.top().toFloatArray(), 0);

                    gl3.glActiveTexture(GL3.GL_TEXTURE0);
                    gl3.glBindTexture(GL3.GL_TEXTURE_2D,
                            textures[useMipmapTexture ? Textures.mipmapTest.ordinal() : Textures.checker.ordinal()]);
                    {
                        gl3.glBindSampler(TexUnit.color.ordinal(), samplers[currSampler]);
                        {
                            if (drawCorridor) {

                                corridor.render(gl3, "tex");

                            } else {

                                plane.render(gl3, "tex");
                            }
                        }
                        gl3.glBindSampler(TexUnit.color.ordinal(), 0);
                    }
                    gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
                }
                program.unbind(gl3);
            }
            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {

        GL3 gl3 = glad.getGL().getGL3();

        Mat4 persMatrix = Jglm.perspective(90f, w / (float) h, 1f, 1000f);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            Buffer buffer = GLBuffers.newDirectFloatBuffer(persMatrix.toFloatArray());

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, buffer);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_SPACE:
                useMipmapTexture = !useMipmapTexture;
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

            case KeyEvent.VK_3:
                currSampler = 2;
                break;

            case KeyEvent.VK_4:
                currSampler = 5;
                break;

            case KeyEvent.VK_5:
                currSampler = 4;
                break;

            case KeyEvent.VK_6:
                currSampler = 5;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {

    }

    public enum UniformBlockBinding {

        projection,
        size
    }

    public enum TexUnit {

        color,
        size
    }

    public enum Textures {

        checker,
        mipmapTest,
        size
    }
}
