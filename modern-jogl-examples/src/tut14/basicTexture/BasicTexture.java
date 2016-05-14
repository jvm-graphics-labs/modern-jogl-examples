/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.basicTexture;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import framework.glutil.MatrixStack;
import framework.glutil.ObjectData;
import framework.glutil.ObjectPole;
import framework.glutil.Timer;
import framework.glutil.ViewData;
import framework.glutil.ViewPole;
import framework.glutil.ViewScale;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;
import framework.jglm.Jglm;
import framework.jglm.Mat3;
import framework.jglm.Mat4;
import framework.jglm.Quat;
import framework.jglm.Vec3;
import framework.jglm.Vec4;
import framework.component.Mesh;
import tut14.basicTexture.programs.ProgramStandard;
import tut14.basicTexture.programs.ProgramUnlit;

/**
 *
 * @author gbarbieri
 */
public class BasicTexture implements GLEventListener, KeyListener, MouseListener {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final BasicTexture basicTexture = new BasicTexture();
        basicTexture.initGL();

        Frame frame = new Frame("Tutorial 14 - Basic Texture");

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
    String shadersPath = "/tut14/basicTexture/shaders/";
    ProgramStandard litShaderProgram;
    ProgramStandard litTextureProgram;
    ProgramUnlit unlitProgram;
    int materialUBB = 0;
    int lightUBB = 1;
    int projectionUBB = 2;
    int gaussianTextureUnit = 0;
    Mesh objectMesh;
    Mesh cubeMesh;
    float specularShininess = 0.2f;
    int[] materialUBO;
    int[] lightUBO;
    int[] projectionUBO;
    int NUM_GAUSSIAN_TEXTURES = 4;
    int[] gaussianTextures = new int[NUM_GAUSSIAN_TEXTURES];
    byte[] textureData;
    int[] gaussianSampler;
    Timer lightTimer;
    ViewPole viewPole;
    ObjectPole objectPole;
    float lightHeight = 1f;
    float lightRadius = 3f;
    float halfLightDistance = 25f;
    float lightAttenuation = 1f / (halfLightDistance * halfLightDistance);
    boolean useTexture = true;
    int currentTexture = 0;
    boolean drawLights = true;
    boolean drawCameraPos = false;

    public BasicTexture() {

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

//        objectMesh = new Mesh(dataPath + "Infinity.xml", gl3);
//
//        cubeMesh = new Mesh(dataPath + "UnitCube.xml", gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        float depthZnear = 0f;
        float depthZfar = 1f;

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(depthZnear, depthZfar);
        gl3.glEnable(GL3.GL_DEPTH_CLAMP);
        /**
         * Setup our uniform buffers.
         */
        Vec4 diffuseColor = new Vec4(1f, 0.673f, 0.043f, 1f);
        MaterialBlock mtl = new MaterialBlock(diffuseColor, diffuseColor.mult(0.4f), specularShininess);

        materialUBO = new int[1];
        gl3.glGenBuffers(1, materialUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, materialUBO[0]);
        {
            FloatBuffer materialFB = GLBuffers.newDirectFloatBuffer(mtl.toFloatArray());

            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, mtl.toFloatArray().length * 4, materialFB, GL3.GL_STATIC_DRAW);
        }

        lightUBO = new int[1];
        gl3.glGenBuffers(1, lightUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, lightUBO[0]);
        {
            int size = 4 + 4 + 2 * (4 + 4);
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 4, null, GL3.GL_DYNAMIC_DRAW);
        }

        projectionUBO = new int[1];
        gl3.glGenBuffers(1, projectionUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            int size = 16;
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 4, null, GL3.GL_DYNAMIC_DRAW);
        }
        /**
         * Bind the static buffers.
         */
        int size = 4 + 4 + 2 * (4 + 4);
        int offset = 0;
        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, lightUBB, lightUBO[0], offset, size * 4);

        size = 16;
        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionUBB, projectionUBO[0], offset, size * 4);

        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, materialUBB, materialUBO[0], offset, mtl.toFloatArray().length * 4);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        createGaussianTextures(gl3);

        lightTimer = new Timer(Timer.Type.Loop, 6f);

        ObjectData initialObjectData = new ObjectData(new Vec3(0f, 0.5f, 0f), new Quat(0f, 0f, 0f, 1f));

        ViewData initialViewData = new ViewData(initialObjectData.getPosition(),
                new Quat(0.3826834f, 0f, 0f, 0.92387953f), 10f, 0f);

        ViewScale viewScale = new ViewScale(1.5f, 70f, 1.5f, 0.5f, 0f, 0f, 90f / 250f);

        viewPole = new ViewPole(initialViewData, viewScale);

        objectPole = new ObjectPole(initialObjectData, 90f / 250f, viewPole);
    }

    private void buildShaders(GL3 gl3) {

        litShaderProgram = new ProgramStandard(gl3, shadersPath, "PN_VS.glsl", "ShaderGaussian_FS.glsl",
                materialUBB, lightUBB, projectionUBB, gaussianTextureUnit);

        litTextureProgram = new ProgramStandard(gl3, shadersPath, "PN_VS.glsl", "TextureGaussian_FS.glsl",
                materialUBB, lightUBB, projectionUBB, gaussianTextureUnit);

        unlitProgram = new ProgramUnlit(gl3, shadersPath, "Unlit_VS.glsl", "Unlit_FS.glsl", projectionUBB);
    }

    private void createGaussianTextures(GL3 gl3) {

        for (int loop = 0; loop < NUM_GAUSSIAN_TEXTURES; loop++) {
//            System.out.println("loop " + loop);
            int cosAngleResolution = calcCosAngleResolution(loop);

            gaussianTextures[loop] = createGaussianTexture(gl3, cosAngleResolution);
        }
        gaussianSampler = new int[1];
        gl3.glGenSamplers(1, gaussianSampler, 0);
        gl3.glSamplerParameteri(gaussianSampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(gaussianSampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(gaussianSampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
    }

    private int calcCosAngleResolution(int level) {

        int cosAngleStart = 64;

        return cosAngleStart * ((int) Math.pow(2f, level));
    }

    private int createGaussianTexture(GL3 gl3, int cosAngleResolution) {

        buildGaussianData(cosAngleResolution);

//        for (int i = 0; i < textureData.length; i++) {
//
//            System.out.println("textureData[" + i + "] " + String.format("%02x", textureData[i]));
//        }
        int[] gaussianTexture = new int[1];

        gl3.glGenTextures(1, gaussianTexture, 0);
        gl3.glBindTexture(GL3.GL_TEXTURE_1D, gaussianTexture[0]);
        {
            gl3.glTexImage1D(GL3.GL_TEXTURE_1D, 0, GL3.GL_R8, cosAngleResolution, 0,
                    GL3.GL_RED, GL3.GL_UNSIGNED_BYTE, GLBuffers.newDirectByteBuffer(textureData));
            gl3.glTexParameteri(GL3.GL_TEXTURE_1D, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_1D, GL3.GL_TEXTURE_MAX_LEVEL, 0);
        }
        gl3.glBindTexture(GL3.GL_TEXTURE_1D, 0);

        return gaussianTexture[0];
    }

    private void buildGaussianData(int cosAngleResolution) {

        textureData = new byte[cosAngleResolution];

        for (int iCosAng = 0; iCosAng < cosAngleResolution; iCosAng++) {

            float cosAng = iCosAng / (float) (cosAngleResolution - 1);
            float angle = (float) Math.acos(cosAng);
            float exponent = angle / specularShininess;
            exponent = -(exponent * exponent);
            float gaussianTerm = (float) Math.exp(exponent);
//            System.out.println("gaussianTerm "+gaussianTerm+" (gaussianTerm * 255f) "+(gaussianTerm * 255f));
            textureData[iCosAng] = (byte) (gaussianTerm * 255f);
        }
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
            LightBlock lightData = new LightBlock(new Vec4(.2f, .2f, .2f, 1f), lightAttenuation, lights);

            gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, lightUBO[0]);
            {
                FloatBuffer lightFB = GLBuffers.newDirectFloatBuffer(lightData.toFloatArray());
                gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, lightData.toFloatArray().length * 4, lightFB);
            }
            gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
            {
                gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, materialUBB, materialUBO[0], 0, 12 * 4);

                modelMatrix.push();
                {
                    modelMatrix.applyMat(objectPole.calcMatrix());
                    modelMatrix.scale(new Vec3(2f, 2f, 2f));
//                    modelMatrix.top().print("modelmatrix");
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    normalMatrix = normalMatrix.inverse().transpose();

                    ProgramStandard programStandard = useTexture ? litTextureProgram : litShaderProgram;

                    programStandard.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(programStandard.getModelToCameraMatrixUL(), 1, false,
                                modelMatrix.top().toFloatArray(), 0);

                        gl3.glUniformMatrix3fv(programStandard.getNormalModelToCameraMatrixUL(), 1, false,
                                normalMatrix.toFloatArray(), 0);

                        gl3.glActiveTexture(GL3.GL_TEXTURE0 + gaussianTextureUnit);
                        gl3.glBindTexture(GL3.GL_TEXTURE_1D, gaussianTextures[currentTexture]);
                        {
                            gl3.glBindSampler(gaussianTextureUnit, gaussianSampler[0]);
                            {
                                objectMesh.render(gl3, "lit");
                            }
                            gl3.glBindSampler(gaussianTextureUnit, 0);
                        }
                        gl3.glBindTexture(GL3.GL_TEXTURE_1D, 0);
                    }
                    programStandard.unbind(gl3);
                    gl3.glBindBufferBase(GL3.GL_UNIFORM_BUFFER, materialUBO[0], 0);
                }
                modelMatrix.pop();
            }
            if (drawLights) {

                unlitProgram.bind(gl3);
                {
                    modelMatrix.push();
                    {
//                        modelMatrix.top().print("modelmatrix");
//                        calcLightPosition().print("calcLightPosition()");
                        modelMatrix.translate(new Vec3(calcLightPosition()));
                        modelMatrix.scale(new Vec3(0.25f, 0.25f, 0.25f));

                        gl3.glUniformMatrix4fv(unlitProgram.getModelToCameraMatrixUL(),
                                1, false, modelMatrix.top().toFloatArray(), 0);

                        gl3.glUniform4f(unlitProgram.getObjectColorUL(), 1f, 1f, 1f, 1f);

                        cubeMesh.render(gl3, "flat");
                    }
                    modelMatrix.pop();
                    modelMatrix.push();
                    {
                        modelMatrix.translate(globalLightDirection.times(100f));
                        modelMatrix.scale(new Vec3(5.0f, 5.0f, 5.0f));

                        gl3.glUniformMatrix4fv(unlitProgram.getModelToCameraMatrixUL(),
                                1, false, modelMatrix.top().toFloatArray(), 0);

                        cubeMesh.render(gl3, "flat");
                    }
                    modelMatrix.pop();
                }
                unlitProgram.unbind(gl3);
            }
            if (drawCameraPos) {

                modelMatrix.setTop(new Mat4(1f));
                modelMatrix.translate(new Vec3(0f, 0f, -viewPole.getCurrView().getRadius()));
                modelMatrix.scale(new Vec3(.25f, .25f, .25f));

                gl3.glDisable(GL3.GL_DEPTH_TEST);
                gl3.glDepthMask(false);

                unlitProgram.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(unlitProgram.getModelToCameraMatrixUL(), 1, false,
                            modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(unlitProgram.getObjectColorUL(), .25f, .25f, .25f, 1f);

                    cubeMesh.render(gl3, "flat");

                    gl3.glDepthMask(true);
                    gl3.glEnable(GL3.GL_DEPTH_TEST);
                    gl3.glUniform4f(unlitProgram.getObjectColorUL(), 1f, 1f, 1f, 1f);

                    cubeMesh.render(gl3, "flat");
                }
                unlitProgram.unbind(gl3);
            }
        }
    }

    private Vec4 calcLightPosition() {

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

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
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
                useTexture = !useTexture;
                String string = useTexture ? "Texture" : "Shader";
                System.out.println("" + string);
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
                
            case KeyEvent.VK_1:
                currentTexture = 0;
                break;
                
            case KeyEvent.VK_2:
                currentTexture = 1;
                break;
                
            case KeyEvent.VK_3:
                currentTexture = 2;
                break;
                
            case KeyEvent.VK_4:
                currentTexture = 3;
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

    class MaterialBlock {

        Vec4 diffuseColor;
        Vec4 specularColor;
        float specularShininess;
        Vec3 padding;

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

    class LightBlock {

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
}
