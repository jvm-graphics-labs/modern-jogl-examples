/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.materialTexture;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import glutil.MatrixStack;
import glutil.ObjectData;
import glutil.ObjectPole;
import glutil.Timer;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import jglm.Jglm;
import jglm.Mat3;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec3;
import jglm.Vec4;
import mesh.Mesh;
import tut14.materialTexture.programs.ProgramData;
import tut14.materialTexture.programs.UnlitProgData;

/**
 *
 * @author gbarbieri
 */
public class MaterialTexture implements GLEventListener, KeyListener, MouseListener {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final MaterialTexture basicTexture = new MaterialTexture();
        basicTexture.initGL();

        Frame frame = new Frame("Tutorial 14 - Material Texture");

        frame.add(basicTexture.newtCanvasAWT);

        frame.setSize(basicTexture.glWindow.getWidth(), basicTexture.glWindow.getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(basicTexture.glWindow, 30);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                fPSAnimator.stop();
                basicTexture.glWindow.destroy();
                System.exit(0);
            }
        });

        fPSAnimator.start();
        frame.setVisible(true);
    }

    GLWindow glWindow;
    NewtCanvasAWT newtCanvasAWT;
    ProgramData[] programs = new ProgramData[ShaderMode.NUM_SHADER_MODES.ordinal()];
    UnlitProgData unlit;
    int[] textUnits = new int[TexUnit.size.ordinal()];
    Mesh objectMesh;
    Mesh cubeMesh;
    Mesh planeMesh;
    int NUM_MATERIALS = 2;
    int[] uniformBlockBuffers = new int[UniformBlockBinding.size.ordinal()];
    int NUM_GAUSSIAN_TEXTURES = 4;
    int[] gaussianTextures = new int[NUM_GAUSSIAN_TEXTURES];
    int[] textureSampler;
    Timer lightTimer;
    ViewPole viewPole;
    ObjectPole objectPole;
    int currentTexture = NUM_GAUSSIAN_TEXTURES - 1;
    int currentMaterial = 0;
    int materialOffset;
    boolean drawLights = true;
    boolean drawCameraPos = false;
    boolean useInfinity = true;
    ShaderMode eMode = ShaderMode.MODE_FIXED;
    int[] shineTexture = new int[1];
    String[] shaderModeNames = new String[]{"Fixed Shininess with Gaussian Texture",
        "Texture Shininess with Gaussian Texture", "Texture Shininess with computed Gaussian"};

    public MaterialTexture() {

    }

    public void initGL() {

        GLProfile gLProfile = GLProfile.get(GLProfile.GL3);

        GLCapabilities gLCapabilities = new GLCapabilities(gLProfile);

        glWindow = GLWindow.create(gLCapabilities);

        glWindow.setSize(1024, 768);

        glWindow.addGLEventListener(this);
        glWindow.addKeyListener(this);
        glWindow.addMouseListener(this);
        /*
         *  We combine NEWT GLWindow inside existing AWT application (the main JFrame) 
         *  by encapsulating the glWindow inside a NewtCanvasAWT canvas.
         */
        newtCanvasAWT = new NewtCanvasAWT(glWindow);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        GL3 gl3 = glad.getGL().getGL3();

        buildShaders(gl3);

        String dataPath = "/tut14/data/";

        objectMesh = new Mesh(dataPath + "Infinity.xml", gl3);
        cubeMesh = new Mesh(dataPath + "UnitCube.xml", gl3);
        planeMesh = new Mesh(dataPath + "UnitPlane.xml", gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0f, 1f);
        gl3.glEnable(GL3.GL_DEPTH_CLAMP);
        /**
         * Setup our uniform buffers.
         */
        setupMaterials(gl3);

        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBinding.light.ordinal());
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.light.ordinal()]);
        {
            int size = 4 + 4 + 2 * (4 + 4);
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 4, null, GL3.GL_DYNAMIC_DRAW);
        }

        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBinding.projection.ordinal());
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            int size = 16;
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 4, null, GL3.GL_DYNAMIC_DRAW);
        }
        /**
         * Bind the static buffers.
         */
        int size = 4 + 4 + 2 * (4 + 4);
        int offset = 0;
        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.light.ordinal(),
                uniformBlockBuffers[UniformBlockBinding.light.ordinal()], offset, size * 4);

        size = 16;
        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.projection.ordinal(),
                uniformBlockBuffers[UniformBlockBinding.projection.ordinal()], offset, size * 4);

        int materialBlockSize = 12;
        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.material.ordinal(),
                uniformBlockBuffers[UniformBlockBinding.material.ordinal()], offset, materialBlockSize * 4);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        createGaussianTextures(gl3);
        createShininessTexture(gl3);

        lightTimer = new Timer(Timer.Type.Loop, 6f);

        ObjectData initialObjectData = new ObjectData(new Vec3(0f, 0.5f, 0f), new Quat(0f, 0f, 0f, 1f));

        ViewData initialViewData = new ViewData(initialObjectData.getPosition(),
                new Quat(0.3826834f, 0f, 0f, 0.92387953f), 10f, 0f);

        ViewScale viewScale = new ViewScale(1.5f, 70f, 1.5f, 0.5f, 0f, 0f, 90f / 250f);

        viewPole = new ViewPole(initialViewData, viewScale);

        objectPole = new ObjectPole(initialObjectData, 90f / 250f, viewPole);
    }

    private void setupMaterials(GL3 gl3) {

        MaterialBlock[] mtls = new MaterialBlock[]{
            new MaterialBlock(new Vec4(1f, .673f, .043f, 1f), (new Vec4(1f, .673f, .043f, 1f)).mult(.4f), .125f),
            new MaterialBlock(new Vec4(.01f, .01f, .01f, 1f), new Vec4(.99f, .99f, .99f, 1f), .125f)};

        int[] uniformBufferAlignSize = new int[1];
        gl3.glGetIntegerv(GL3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferAlignSize, 0);
//        System.out.println("uniformBufferAlignSize " + uniformBufferAlignSize[0]);
        materialOffset = MaterialBlock.size;
        materialOffset += uniformBufferAlignSize[0] / 4 - (materialOffset % (uniformBufferAlignSize[0] / 4));
//        System.out.println("blockOffset " + blockOffset);

        gl3.glGenBuffers(1, uniformBlockBuffers, UniformBlockBinding.material.ordinal());

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.material.ordinal()]);
        {
            float[] floatArray = new float[NUM_MATERIALS * materialOffset];

            System.arraycopy(mtls[0].toFloatArray(), 0, floatArray, 0, mtls[0].toFloatArray().length);
            System.arraycopy(mtls[1].toFloatArray(), 0, floatArray, materialOffset, mtls[1].toFloatArray().length);

            FloatBuffer floatBuffer = GLBuffers.newDirectFloatBuffer(floatArray);

            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, NUM_MATERIALS * materialOffset * 4, floatBuffer, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    private void buildShaders(GL3 gl3) {

        String shadersPath = "/tut14/materialTexture/shaders/";

        ShaderPairs[] shaderPairs = new ShaderPairs[]{
            new ShaderPairs("PN_VS.glsl", "FixedShininess_FS.glsl"),
            new ShaderPairs("PNT_VS.glsl", "TextureShininess_FS.glsl"),
            new ShaderPairs("PNT_VS.glsl", "TextureCompute_FS.glsl"),};

        for (int prog = 0; prog < ShaderMode.NUM_SHADER_MODES.ordinal(); prog++) {

            programs[prog] = new ProgramData(gl3, shadersPath, shaderPairs[prog].vertShader,
                    shaderPairs[prog].fragShader, textUnits);
        }

        unlit = new UnlitProgData(gl3, shadersPath, "Unlit_VS.glsl", "Unlit_FS.glsl",
                UniformBlockBinding.projection.ordinal());
    }

    private void createGaussianTextures(GL3 gl3) {

        for (int loop = 0; loop < NUM_GAUSSIAN_TEXTURES; loop++) {
//            System.out.println("loop " + loop);
            int cosAngleResolution = calcCosAngleResolution(loop);
//            System.out.println("cosAngleResolution "+cosAngleResolution);
            gaussianTextures[loop] = createGaussianTexture(gl3, cosAngleResolution, 128);
//            System.out.println("gaussianTextures[loop] "+gaussianTextures[loop]);
        }
        textureSampler = new int[1];
        gl3.glGenSamplers(1, textureSampler, 0);
        gl3.glSamplerParameteri(textureSampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(textureSampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(textureSampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(textureSampler[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
    }

    private int calcCosAngleResolution(int level) {

        int cosAngleStart = 1;

        return cosAngleStart * ((int) Math.pow(2f, level));
    }

    private int createGaussianTexture(GL3 gl3, int cosAngleResolution, int shininessResolution) {

        byte[] textureData = buildGaussianData(cosAngleResolution, shininessResolution);
        System.out.println("textureData.length " + textureData.length);
//        for (int i = 0; i < textureData.length; i++) {
//
//            System.out.println("textureData[" + i + "] " + String.format("%02x", textureData[i]));
//        }
        int[] gaussTexture = new int[1];
        gl3.glGenTextures(1, gaussTexture, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_2D, gaussTexture[0]);
        {
            ByteBuffer byteBuffer = GLBuffers.newDirectByteBuffer(textureData);
            System.out.println("byteBuffer " + byteBuffer.toString());
            System.out.println("cosAngleResolution " + cosAngleResolution + " shininessResolution " + shininessResolution);
            gl3.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_R8, cosAngleResolution, shininessResolution,
                    0, GL3.GL_RED, GL3.GL_UNSIGNED_BYTE, byteBuffer);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);

        return gaussTexture[0];
    }

    private byte[] buildGaussianData(int cosAngleResolution, int shininessResolution) {

        byte[] textureData = new byte[cosAngleResolution * shininessResolution];

        int index = 0;

        for (int iShin = 1; iShin <= shininessResolution; iShin++) {

            float shininess = iShin / (float) shininessResolution;

            for (int iCosAng = 0; iCosAng < cosAngleResolution; iCosAng++) {

                float cosAng = iCosAng / (float) (cosAngleResolution - 1);
                float angle = (float) Math.acos(cosAng);
                float exponent = angle / shininess;
                exponent = -(exponent * exponent);
                float gaussianTerm = (float) Math.exp(exponent);

                textureData[index] = (byte) (gaussianTerm * 255f);
//                System.out.println("textureData[" + index + "] " + String.format("%02x", textureData[index]));
                index++;
            }
        }

        return textureData;
    }

    private void createShininessTexture(GL3 gl3) {

        String filePath = "/tut14/materialTexture/data/main.dds";

        URL url = getClass().getResource(filePath);
        File file = new File(url.getPath());

        DDSImage ddsImage = null;

        try {
            ddsImage = DDSImage.read(file);
        } catch (IOException ex) {
            Logger.getLogger(MaterialTexture.class.getName()).log(Level.SEVERE, null, ex);
        }
        gl3.glGenTextures(1, shineTexture, 0);
//
//        TextureData textureData = null;
//        try {
//            textureData = TextureIO.newTextureData(gl3.getGLProfile(), file, false, TextureIO.DDS);
//        } catch (IOException ex) {
//            Logger.getLogger(MaterialTexture.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Buffer buffer = textureData.getBuffer();
//        buffer.rewind();

//        while(buffer.hasRemaining()){
//            System.out.println("["+buffer.position()+"] = "+buffer.get());
//        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, shineTexture[0]);
        {
//            System.out.println("ddsImage.getWidth() " + ddsImage.getWidth()
//                    + " ddsImage.getHeight() " + ddsImage.getHeight() + " ");
//            ddsImage.debugPrint();
            gl3.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_R8, ddsImage.getWidth(), ddsImage.getHeight(),
                    0, GL3.GL_RED, GL3.GL_UNSIGNED_BYTE, ddsImage.getMipMap(0).getData());

            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        lightTimer.update();

        gl3.glClearColor(0.75f, 0.75f, 1f, 1f);
        gl3.glClearDepthf(1f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        if (objectMesh != null && cubeMesh != null) {

            MatrixStack modelMatrix = new MatrixStack();
            modelMatrix.setTop(viewPole.calcMatrix());
            Mat4 worldToCamMat = new Mat4(modelMatrix.top().toFloatArray());

            Vec3 globalLightDirection = new Vec3(0.707f, 0.707f, 0f);

            PerLight[] lights = new PerLight[]{
                new PerLight(worldToCamMat.mult(new Vec4(globalLightDirection, 0f)), new Vec4(0.6f, 0.6f, .6f, 1f)),
                new PerLight(worldToCamMat.mult(calcLightPosition()), new Vec4(.4f, .4f, .4f, 1f))
            };
            float halfLightDistance = 25f;
            float lightAttenuation = 1f / (halfLightDistance * halfLightDistance);
            LightBlock lightData = new LightBlock(new Vec4(.2f, .2f, .2f, 1f), lightAttenuation, lights);

            gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.light.ordinal()]);
            {
                FloatBuffer lightFB = GLBuffers.newDirectFloatBuffer(lightData.toFloatArray());
                gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, lightData.toFloatArray().length * 4, lightFB);
            }
            gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
            {
                Mesh mesh = useInfinity ? objectMesh : planeMesh;

                int materialBlockSize = 12;
//                int offset = currentMaterial * materialBlockSize;
                int offset = currentMaterial * materialOffset;
                gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, UniformBlockBinding.material.ordinal(),
                        uniformBlockBuffers[UniformBlockBinding.material.ordinal()], offset * 4, materialBlockSize * 4);

                modelMatrix.push();
                {
                    modelMatrix.applyMat(objectPole.calcMatrix());
                    modelMatrix.scale(useInfinity ? new Vec3(2f, 2f, 2f) : new Vec3(4f, 4f, 4f));
//                    modelMatrix.top().print("modelmatrix");
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    normalMatrix = normalMatrix.inverse().transpose();

                    ProgramData prog = programs[eMode.ordinal()];

                    prog.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(prog.getModelToCameraMatrixUL(), 1, false,
                                modelMatrix.top().toFloatArray(), 0);

                        gl3.glUniformMatrix3fv(prog.getNormalModelToCameraMatrixUL(), 1, false,
                                normalMatrix.toFloatArray(), 0);

                        gl3.glActiveTexture(GL3.GL_TEXTURE0 + textUnits[TexUnit.gaussian.ordinal()]);
                        gl3.glBindTexture(GL3.GL_TEXTURE_2D, gaussianTextures[currentTexture]);
                        {
                            gl3.glBindSampler(textUnits[TexUnit.gaussian.ordinal()], textureSampler[0]);
                            {
                                gl3.glActiveTexture(GL3.GL_TEXTURE0 + textUnits[TexUnit.shininess.ordinal()]);
                                gl3.glBindTexture(GL3.GL_TEXTURE_2D, shineTexture[0]);
                                gl3.glBindSampler(textUnits[TexUnit.shininess.ordinal()], textureSampler[0]);
                                {
                                    if (eMode != ShaderMode.MODE_FIXED) {

                                        mesh.render(gl3, "lit-tex");

                                    } else {

                                        mesh.render(gl3, "lit");
                                    }
                                }
                            }
                            gl3.glBindSampler(textUnits[TexUnit.gaussian.ordinal()], 0);
                        }
                        gl3.glBindTexture(GL3.GL_TEXTURE_2D, 0);
                    }
                    prog.unbind(gl3);

                    gl3.glBindBufferBase(GL3.GL_UNIFORM_BUFFER,
                            uniformBlockBuffers[UniformBlockBinding.material.ordinal()], 0);
                }
                modelMatrix.pop();
            }
            if (drawLights) {

                unlit.bind(gl3);
                {
                    modelMatrix.push();
                    {
//                        modelMatrix.top().print("modelmatrix");
//                        calcLightPosition().print("calcLightPosition()");
                        modelMatrix.translate(new Vec3(calcLightPosition()));
                        modelMatrix.scale(new Vec3(0.25f, 0.25f, 0.25f));

                        gl3.glUniformMatrix4fv(unlit.getModelToCameraMatrixUL(),
                                1, false, modelMatrix.top().toFloatArray(), 0);

                        gl3.glUniform4f(unlit.getObjectColorUL(), 1f, 1f, 1f, 1f);

                        cubeMesh.render(gl3, "flat");
                    }
                    modelMatrix.pop();
                    modelMatrix.push();
                    {
                        modelMatrix.translate(globalLightDirection.times(100f));
                        modelMatrix.scale(new Vec3(5.0f, 5.0f, 5.0f));

                        gl3.glUniformMatrix4fv(unlit.getModelToCameraMatrixUL(),
                                1, false, modelMatrix.top().toFloatArray(), 0);

                        cubeMesh.render(gl3, "flat");
                    }
                    modelMatrix.pop();
                }
                unlit.unbind(gl3);
            }
            if (drawCameraPos) {

                modelMatrix.setTop(new Mat4(1f));
                modelMatrix.translate(new Vec3(0f, 0f, -viewPole.getCurrView().getRadius()));
                modelMatrix.scale(new Vec3(.25f, .25f, .25f));

                gl3.glDisable(GL3.GL_DEPTH_TEST);
                gl3.glDepthMask(false);

                unlit.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(unlit.getModelToCameraMatrixUL(), 1, false,
                            modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(unlit.getObjectColorUL(), .25f, .25f, .25f, 1f);

                    cubeMesh.render(gl3, "flat");

                    gl3.glDepthMask(true);
                    gl3.glEnable(GL3.GL_DEPTH_TEST);
                    gl3.glUniform4f(unlit.getObjectColorUL(), 1f, 1f, 1f, 1f);

                    cubeMesh.render(gl3, "flat");
                }
                unlit.unbind(gl3);
            }
        }
    }

    private Vec4 calcLightPosition() {

        float lightHeight = 1f;
        float lightRadius = 3f;

        float scale = (float) (Math.PI * 2f);

        float timeThroughLoop = lightTimer.getAlpha();

        Vec4 ret = new Vec4(0f, lightHeight, 0f, 1f);

        ret.x = (float) (Math.cos(timeThroughLoop * scale) * lightRadius);
        ret.z = (float) (Math.sin(timeThroughLoop * scale) * lightRadius);

        return ret;
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape");
        GL3 gl3 = glad.getGL().getGL3();

        float zNear = 1.0f;
        float zFar = 1000.0f;

        Mat4 projection = Jglm.perspective(45.0f, w / (float) h, zNear, zFar);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, uniformBlockBuffers[UniformBlockBinding.projection.ordinal()]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(projection.toFloatArray());
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, projection.toFloatArray().length * 4, buffer);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
        gl3.glViewport(x, y, w, h);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_SPACE:
                int currentEmode = eMode.ordinal() + 1;
                currentEmode = currentEmode % ShaderMode.NUM_SHADER_MODES.ordinal();
                eMode = ShaderMode.values()[currentEmode];
                System.out.println("" + shaderModeNames[eMode.ordinal()]);
                break;

            case KeyEvent.VK_P:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_MINUS:
                lightTimer.rewind(.5f);
                break;

            case KeyEvent.VK_EQUALS:
                lightTimer.fastForward(.5f);
                break;

            case KeyEvent.VK_T:
                drawCameraPos = !drawCameraPos;
                break;

            case KeyEvent.VK_G:
                drawLights = !drawLights;
                break;

            case KeyEvent.VK_Y:
                useInfinity = !useInfinity;
                break;

            case KeyEvent.VK_1:
                currentTexture = 0;
                System.out.println("Angle resolution: " + calcCosAngleResolution(currentTexture));
//                System.out.println("currentTexture " + currentTexture);
                break;

            case KeyEvent.VK_2:
                currentTexture = 1;
                System.out.println("Angle resolution: " + calcCosAngleResolution(currentTexture));
//                System.out.println("currentTexture " + currentTexture);
                break;

            case KeyEvent.VK_3:
                currentTexture = 2;
                System.out.println("Angle resolution: " + calcCosAngleResolution(currentTexture));
//                System.out.println("currentTexture " + currentTexture);
                break;

            case KeyEvent.VK_4:
                currentTexture = 3;
                System.out.println("Angle resolution: " + calcCosAngleResolution(currentTexture));
//                System.out.println("currentTexture " + currentTexture);
                break;

            case KeyEvent.VK_8:
                currentMaterial = 0;
                break;

            case KeyEvent.VK_9:
                currentMaterial = 1;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        viewPole.mousePressed(e);
        objectPole.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        viewPole.mouseReleased(e);
        objectPole.mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        viewPole.mouseMove(e);
        objectPole.mouseMove(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        viewPole.mouseWheel(e);
    }

    static class MaterialBlock {

        Vec4 diffuseColor;
        Vec4 specularColor;
        float specularShininess;
        Vec3 padding;
        static final int size = 12;

        public MaterialBlock(Vec4 diffuseColor, Vec4 specularColor, float specularShininess) {

            this.diffuseColor = diffuseColor;
            this.specularColor = specularColor;
            this.specularShininess = specularShininess;
            padding = new Vec3();
        }

        public float[] toFloatArray() {

            return new float[]{diffuseColor.x, diffuseColor.y, diffuseColor.z, diffuseColor.w,
                specularColor.x, specularColor.y, specularColor.z, specularColor.w,
                specularShininess, padding.x, padding.y, padding.z};
        }
    }

    static class LightBlock {

        Vec4 ambientIntensity;
        float lightAttenuation;
        Vec3 padding;
        PerLight[] lights;

        public LightBlock(Vec4 ambientIntensity, float lightAttenuation, PerLight[] lights) {

            this.ambientIntensity = ambientIntensity;
            this.lightAttenuation = lightAttenuation;
            padding = new Vec3();
            this.lights = lights;
        }

        public float[] toFloatArray() {

            return new float[]{ambientIntensity.x, ambientIntensity.y, ambientIntensity.z, ambientIntensity.w,
                lightAttenuation, padding.x, padding.y, padding.z,
                lights[0].cameraSpaceLightPos.x, lights[0].cameraSpaceLightPos.y,
                lights[0].cameraSpaceLightPos.z, lights[0].cameraSpaceLightPos.w,
                lights[0].lightIntensity.x, lights[0].lightIntensity.y,
                lights[0].lightIntensity.z, lights[0].lightIntensity.w,
                lights[1].cameraSpaceLightPos.x, lights[1].cameraSpaceLightPos.y,
                lights[1].cameraSpaceLightPos.z, lights[1].cameraSpaceLightPos.w,
                lights[1].lightIntensity.x, lights[1].lightIntensity.y,
                lights[1].lightIntensity.z, lights[1].lightIntensity.w};
        }
    }

    class PerLight {

        Vec4 cameraSpaceLightPos;
        Vec4 lightIntensity;

        public PerLight(Vec4 cameraSpaceLightPos, Vec4 lightIntensity) {

            this.cameraSpaceLightPos = cameraSpaceLightPos;
            this.lightIntensity = lightIntensity;
        }
    }

    class ShaderPairs {

        public String vertShader;
        public String fragShader;

        public ShaderPairs(String vertShader, String fragShader) {

            this.vertShader = vertShader;
            this.fragShader = fragShader;
        }
    }

    enum ShaderMode {

        MODE_FIXED,
        MODE_TEXTURED,
        MODE_TEXTURED_COMPUTE,
        NUM_SHADER_MODES
    }

    public enum UniformBlockBinding {

        material,
        light,
        projection,
        size
    }

    public enum TexUnit {

        gaussian,
        shininess,
        size
    }
}
