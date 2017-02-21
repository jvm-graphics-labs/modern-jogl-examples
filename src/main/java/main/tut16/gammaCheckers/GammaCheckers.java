///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package tut16.gammaCheckers;
//
//import com.jogamp.newt.awt.NewtCanvasAWT;
//import com.jogamp.newt.event.KeyEvent;
//import com.jogamp.newt.event.KeyListener;
//import com.jogamp.newt.opengl.GLWindow;
//import com.jogamp.opengl.GL3;
//import com.jogamp.opengl.GLAutoDrawable;
//import com.jogamp.opengl.GLCapabilities;
//import com.jogamp.opengl.GLEventListener;
//import com.jogamp.opengl.GLProfile;
//import com.jogamp.opengl.util.FPSAnimator;
//import com.jogamp.opengl.util.GLBuffers;
//import com.jogamp.opengl.util.texture.spi.DDSImage;
//import com.jogamp.opengl.util.texture.spi.DDSImage.ImageInfo;
//import framework.glutil.MatrixStack;
//import framework.glutil.Timer;
//import java.awt.Frame;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.nio.FloatBuffer;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import framework.jglm.Jglm;
//import framework.jglm.Mat4;
//import framework.jglm.Vec3;
//import framework.component.Mesh;
//import tut16.gammaCheckers.program.ProgramData;
//
///**
// *
// * @author gbarbieri
// */
//public class GammaCheckers implements GLEventListener, KeyListener {
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//
//        final GammaCheckers gammaCheckers = new GammaCheckers();
//        gammaCheckers.initGL();
//
//        Frame frame = new Frame("Tutorial 16 - Gamma Checkers");
//
//        frame.add(gammaCheckers.newtCanvasAWT);
//
//        frame.setSize(gammaCheckers.window.getWidth(), gammaCheckers.window.getHeight());
//
//        final FPSAnimator fPSAnimator = new FPSAnimator(gammaCheckers.glWindow, 30);
//
//        frame.addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent windowEvent) {
//                fPSAnimator.stop();
//                gammaCheckers.glWindow.destroy();
//                System.exit(0);
//            }
//        });
//
//        fPSAnimator.start();
//        frame.setVisible(true);
//    }
//
//    private GLWindow glWindow;
//    private NewtCanvasAWT newtCanvasAWT;
//    private int[] uniformBlockBuffers;
//    private ProgramData[] programs;
//    private Mesh plane;
//    private Mesh corridor;
//    private String dataPath = "/tut16/gammaCheckers/data/";
//    private int[] textures;
//    private int[] samplers;
//    private Timer camTimer = new Timer(Timer.Type.Loop, 5f);
//    private boolean drawGammaProgram = false;
//    private boolean drawGammaTexture = false;
//    private int currSampler = 0;
//    private boolean drawCorridor = false;
//
//    public GammaCheckers() {
//
//    }
//
//    public void initGL() {
//
//        GLProfile gLProfile = GLProfile.get(GLProfile.GL3);
//
//        GLCapabilities gLCapabilities = new GLCapabilities(gLProfile);
//
//        glWindow = GLWindow.create(gLCapabilities);
//
//        glWindow.setSize(1024, 768);
//
//        glWindow.addGLEventListener(this);
//        glWindow.addKeyListener(this);
////        glWindow.addMouseListener(this);
//        /*
//         *  We combine NEWT GLWindow inside existing AWT application (the main JFrame)
//         *  by encapsulating the glWindow inside a NewtCanvasAWT canvas.
//         */
//        newtCanvasAWT = new NewtCanvasAWT(glWindow);
//    }
//
//    @Override
//    public void init(GLAutoDrawable glad) {
//
//        GL3 gl3 = glad.getGL().getGL3();
//
//        initializePrograms(gl3);
//
////        corridor = new Mesh(dataPath + "Corridor.xml", gl3);
////        plane = new Mesh(dataPath + "BigPlane.xml", gl3);
//
//        gl3.glEnable(GL3.GL_CULL_FACE);
//        gl3.glCullFace(GL3.GL_BACK);
//        gl3.glFrontFace(GL3.GL_CW);
//
//        gl3.glEnable(GL3.GL_DEPTH_TEST);
//        gl3.glDepthMask(true);
//        gl3.glDepthFunc(GL3.GL_LEQUAL);
//        gl3.glDepthRange(0f, 1f);
//        gl3.glEnable(GL3.GL_DEPTH_CLAMP);
//
//        //Setup our Uniform Buffers
//        uniformBlockBuffers = new int[UniformBlockBuffers.size.ordinal()];
//        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBuffers.projection.ordinal());
//        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBuffers.projection.ordinal()]);
//        {
//            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, 16 * 4, null, GL3.GL_DYNAMIC_DRAW);
//
//            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBuffers.projection.ordinal(),
//                    uniformBlockBuffers[UniformBlockBuffers.projection.ordinal()], 0, 16 * 4);
//        }
//        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
//
//        loadCheckerTextures(gl3);
//        createSamplers(gl3);
//
////        camTimer.togglePause();
//    }
//
//    private void initializePrograms(GL3 gl3) {
//
//        programs = new ProgramData[Programs.size.ordinal()];
//
//        String shadersFilepath = "/tut16/gammaCheckers/shaders/";
//
//        programs[Programs.noGamma.ordinal()] = new ProgramData(gl3, shadersFilepath, "PT_VS.glsl", "textureNoGamma_FS.glsl");
//        programs[Programs.gamma.ordinal()] = new ProgramData(gl3, shadersFilepath, "PT_VS.glsl", "textureGamma_FS.glsl");
//    }
//
//    private void loadCheckerTextures(GL3 gl3) {
//
//        textures = new int[Textures.size.ordinal()];
//        gl3.glGenTextures(Textures.size.ordinal(), textures, 0);
//
//        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Textures.linear.ordinal()]);
//        {
//            URL url = getClass().getResource(dataPath + "checker_linear.dds");
//            File file = new File(url.getPath());
//
//            try {
//                DDSImage ddsImage = DDSImage.read(file);
//
//                for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {
//
//                    ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);
//
//                    gl3.glTexImage2D(GL3.GL_TEXTURE_2D, mipmapLevel, GL3.GL_SRGB8, mipmap.getWidth(),
//                            mipmap.getHeight(), 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
//                }
//                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
//                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);
//
//            } catch (IOException ex) {
//                Logger.getLogger(GammaCheckers.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        gl3.glBindTexture(GL3.GL_TEXTURE_2D, textures[Textures.gamma.ordinal()]);
//        {
//            URL url = getClass().getResource(dataPath + "checker_gamma.dds");
//            File file = new File(url.getPath());
//
//            try {
//                DDSImage ddsImage = DDSImage.read(file);
//
//                for (int mipmapLevel = 0; mipmapLevel < ddsImage.getNumMipMaps(); mipmapLevel++) {
//
//                    ImageInfo mipmap = ddsImage.getMipMap(mipmapLevel);
//
//                    gl3.glTexImage2D(GL3.GL_TEXTURE_2D, mipmapLevel, GL3.GL_SRGB8, mipmap.getWidth(),
//                            mipmap.getHeight(), 0, GL3.GL_BGRA, GL3.GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.getData());
//                }
//                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
//                gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, ddsImage.getNumMipMaps() - 1);
//
//            } catch (IOException ex) {
//                Logger.getLogger(GammaCheckers.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
//    }
//
//    private void createSamplers(GL3 gl3) {
//
//        samplers = new int[Samplers.size.ordinal()];
//        gl3.glGenSamplers(Samplers.size.ordinal(), samplers, 0);
//
//        for (int samplerIx = 0; samplerIx < Samplers.size.ordinal(); samplerIx++) {
//
//            gl3.glSamplerParameteri(samplers[samplerIx], GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
//            gl3.glSamplerParameteri(samplers[samplerIx], GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
//        }
//        //Linear mipmap linear
//        gl3.glSamplerParameteri(samplers[Samplers.linearMipmapLinear.ordinal()], GL3.GL_TEXTURE_MAG_FILTER,
//                GL3.GL_LINEAR);
//        gl3.glSamplerParameteri(samplers[Samplers.linearMipmapLinear.ordinal()], GL3.GL_TEXTURE_MIN_FILTER,
//                GL3.GL_LINEAR_MIPMAP_LINEAR);
//        //Max anisotropic
//        float[] maxAniso = new float[0];
//        gl3.glGetFloatv(GL3.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso, 0);
//
//        gl3.glSamplerParameteri(samplers[Samplers.maxAnisotropic.ordinal()], GL3.GL_TEXTURE_MAG_FILTER,
//                GL3.GL_LINEAR);
//        gl3.glSamplerParameteri(samplers[Samplers.maxAnisotropic.ordinal()], GL3.GL_TEXTURE_MIN_FILTER,
//                GL3.GL_LINEAR_MIPMAP_LINEAR);
//        gl3.glSamplerParameteri(samplers[Samplers.maxAnisotropic.ordinal()], GL3.GL_TEXTURE_MAX_ANISOTROPY_EXT,
//                (int) maxAniso[0]);
//    }
//
//    @Override
//    public void dispose(GLAutoDrawable glad) {
//
//    }
//
//    @Override
//    public void display(GLAutoDrawable glad) {
//
//        GL3 gl3 = glad.getGL().getGL3();
//
//        gl3.glClearColor(.75f, .75f, 1f, 1f);
//        gl3.glClearDepthf(1f);
//        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
//
//        if (plane != null && corridor != null) {
//
//            camTimer.update();
//
//            float cyclicAngle = camTimer.getAlpha() * 6.28f;
//            float hOffset = (float) (Math.cos(cyclicAngle) * .25f);
//            float vOffset = (float) (Math.sin(cyclicAngle) * .25f);
//
//            MatrixStack modelMatrix = new MatrixStack();
//            Mat4 worldToCamMat = Mat4.CalcLookAtMatrix(new Vec3(hOffset, 1f, -64f),
//                    new Vec3(hOffset, -5f + vOffset, -44f), new Vec3(0f, 1f, 0f));
//
//            modelMatrix.setTop(worldToCamMat);
////            modelMatrix.top().print("modelMatrix");
//
//            ProgramData prog = programs[drawGammaProgram ? Programs.gamma.ordinal() : Programs.noGamma.ordinal()];
//
//            prog.bind(gl3);
//            {
//                gl3.glUniformMatrix4fv(prog.getModelToCameraUL(), 1, false, modelMatrix.top().toFloatArray(), 0);
//
//                gl3.glActiveTexture(GL3.GL_TEXTURE0 + TexUnit.color.ordinal());
//                gl3.glBindTexture(GL3.GL_TEXTURE_2D,
//                        textures[drawGammaTexture ? Textures.gamma.ordinal() : Textures.linear.ordinal()]);
//                {
//                    gl3.glBindSampler(TexUnit.color.ordinal(), samplers[currSampler]);
//                    {
//                        if (drawCorridor) {
//
//                            corridor.render(gl3, "tex");
////                            corridor.render(gl3, "flat");
//
//                        } else {
//
//                            plane.render(gl3, "tex");
////                            plane.render(gl3, "flat");
//                        }
//                    }
//                    gl3.glBindSampler(TexUnit.color.ordinal(), 0);
//                }
//                gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
//            }
//            prog.unbind(gl3);
//        }
//    }
//
//    @Override
//    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
//
//        GL3 gl3 = glad.getGL().getGL3();
//
//        Mat4 cameraToClipMatrix = Jglm.perspective(90f, (w / (float) h), 1, 1000);
////        System.out.println("aspect "+((w / (float) h)));
////        cameraToClipMatrix.print("cameraToClipMatrix");
//
//        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBuffers.projection.ordinal()]);
//        {
//            FloatBuffer floatBuffer = GLBuffers.newDirectFloatBuffer(cameraToClipMatrix.toFloatArray());
//
//            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, floatBuffer);
//        }
//        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
//
//        gl3.glViewport(x, y, w, h);
//    }
//
//    @Override
//    public void keyPressed(KeyEvent ke) {
//
//        switch (ke.getKeyCode()) {
//
//            case KeyEvent.VK_A:
//                drawGammaProgram = !drawGammaProgram;
//                break;
//
//            case KeyEvent.VK_G:
//                drawGammaTexture = !drawGammaTexture;
//                break;
//
//            case KeyEvent.VK_SPACE:
//                drawGammaProgram = !drawGammaProgram;
//                drawGammaTexture = !drawGammaTexture;
//                break;
//
//            case KeyEvent.VK_Y:
//                drawCorridor = !drawCorridor;
//                break;
//
//            case KeyEvent.VK_P:
//                camTimer.togglePause();
//                break;
//
//            case KeyEvent.VK_1:
//                currSampler = 0;
//                break;
//
//            case KeyEvent.VK_2:
//                currSampler = 1;
//                break;
//        }
//        System.out.println("----");
//        System.out.println("Rendering: " + (drawGammaProgram ? "Gamma" : "Linear"));
//        System.out.println("Mipmap generation: " + (drawGammaTexture ? "Gamma" : "Linear"));
//    }
//
//    @Override
//    public void keyReleased(KeyEvent ke) {
//
//    }
//
//    private enum Samplers {
//
//        linearMipmapLinear,
//        maxAnisotropic,
//        size
//    }
//
//    private enum Programs {
//
//        noGamma,
//        gamma,
//        size
//    }
//
//    public enum UniformBlockBuffers {
//
//        projection,
//        size
//    }
//
//    private enum Textures {
//
//        linear,
//        gamma,
//        size
//    }
//
//    public enum TexUnit {
//
//        color,
//        size
//    }
//}
