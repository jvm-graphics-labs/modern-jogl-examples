/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut11.gaussianSpecularLighting;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import glutil.MatrixStack;
import glutil.ObjectData;
import glutil.ObjectPole;
import glutil.Timer;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import jglm.Jglm;
import jglm.Mat3;
import jglm.Quat;
import jglm.Vec3;
import jglm.Vec4;
import mesh.Mesh;
import tut10.glsl.UnlitProgram;
import tut11.phongLighting.glslProgram.LitProgram;

/**
 *
 * @author gbarbieri
 */
public class GaussianSpecularLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private GLCanvas canvas;
    private int imageWidth;
    private int imageHeight;
    private LitProgram whiteProgram;
    private LitProgram colorProgram;
    private UnlitProgram unlit;
    private ViewPole viewPole;
    private ObjectPole objectPole;
    private Mesh cylinder;
    private Mesh plane;
    private Mesh cube;
    private int[] projectionUBO;
    private Timer lightTimer;
    private float lightHeight;
    private float lightRadius;
    private boolean coloredCylinder;
    private boolean drawLight;
    private boolean scaleCylinder;
    private boolean drawDark;
    private float lightAttenuation;
    private MaterialParameters materialParameters;
    private Vec4 darkColor;
    private Vec4 lightColor;
    private LightingModel lightingModel;
    private ProgramPairs[] programs;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        GaussianSpecularLighting gaussianSpecularLighting = new GaussianSpecularLighting();

        Frame frame = new Frame("Tutorial 11 - Gaussian Specular Lighting");

        frame.add(gaussianSpecularLighting.getCanvas());

        frame.setSize(gaussianSpecularLighting.getCanvas().getWidth(), gaussianSpecularLighting.getCanvas().getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(gaussianSpecularLighting.canvas, 30);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                fPSAnimator.stop();
                System.exit(0);
            }
        });

        fPSAnimator.start();

        frame.setVisible(true);
    }

    public GaussianSpecularLighting() {
        initGL();
    }

    private void initGL() {

        imageWidth = 800;
        imageHeight = 600;

        GLProfile profile = GLProfile.getDefault();

        GLCapabilities capabilities = new GLCapabilities(profile);

        canvas = new GLCanvas(capabilities);

        canvas.setSize(imageWidth, imageHeight);

        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        int projectionUBB = 2;

        canvas.setAutoSwapBufferMode(false);

        initializePrograms(gl3, projectionUBB);

        ViewData initialViewData = new ViewData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.3826834f, 0.0f, 0.0f, 0.92387953f), 5.0f, 0.0f);

        ViewScale viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.5f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale);

        ObjectData initialObjectData = new ObjectData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(1.0f, 0.0f, 0.0f, 0.0f));

        objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, viewPole);

        initializeMeshes(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
        gl3.glEnable(GL3.GL_DEPTH_CLAMP);

        initUBO(gl3, projectionUBB);

        lightHeight = 1.5f;
        lightRadius = 1.0f;

        lightTimer = new Timer(Timer.Type.Loop, 5.0f);

        lightingModel = LightingModel.DiffuseAndGaussian;

        coloredCylinder = false;
        drawLight = false;

        scaleCylinder = false;

        drawDark = false;

        lightAttenuation = 1.2f;

        darkColor = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
        lightColor = new Vec4(1.0f);

        materialParameters = new MaterialParameters();
    }

    private void initializePrograms(GL3 gl3, int projectionUBB) {

        String shadersFilepath = "/tut11/gaussianSpecularLighting/shaders/";
        programs = new ProgramPairs[6];

        ShaderPairs[] shadersFiles = new ShaderPairs[]{
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "DiffusePhong_FS.glsl"),
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "Phong_FS.glsl"),
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "DiffuseBlinn_FS.glsl"),
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "Blinn_FS.glsl"),
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "DiffuseGaussian_FS.glsl"),
            new ShaderPairs("PN_VS.glsl", "PCN_VS.glsl", "Gaussian_FS.glsl")};

        for (int i = 0; i < 6; i++) {

            ProgramPairs programPair = new ProgramPairs();

            programPair.whiteProgram = new LitProgram(gl3, shadersFilepath, shadersFiles[i].whiteVS, shadersFiles[i].fS, projectionUBB);
            programPair.colorProgram = new LitProgram(gl3, shadersFilepath, shadersFiles[i].colorVS, shadersFiles[i].fS, projectionUBB);

            programs[i] = programPair;
        }

        unlit = new UnlitProgram(gl3, shadersFilepath, "PosTransform_VS.glsl", "UniformColor_FS.glsl", projectionUBB);
    }

    private void initializeMeshes(GL3 gl3) {

        String dataFilepath = "/tut10/data/";

        cylinder = new Mesh(dataFilepath + "UnitCylinder.xml", gl3);

        plane = new Mesh(dataFilepath + "LargePlane.xml", gl3);

        cube = new Mesh(dataFilepath + "UnitCube.xml", gl3);
    }

    private void initUBO(GL3 gl3, int projectionUBB) {

        int size = 16 * 4;

        projectionUBO = new int[1];

        gl3.glGenBuffers(1, projectionUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionUBB, projectionUBO[0], 0, size);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
//        System.out.println("display");

        GL3 gl3 = drawable.getGL().getGL3();

        lightTimer.update();

        gl3.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        MatrixStack modelMatrix = new MatrixStack();

        modelMatrix.setTop(viewPole.calcMatrix());

        Vec4 lightPositionWorldSpace = calculateLightPosition();

        Vec4 lightPositionCameraSpace = modelMatrix.top().mult(lightPositionWorldSpace);

        whiteProgram = programs[lightingModel.ordinal()].whiteProgram;
        colorProgram = programs[lightingModel.ordinal()].colorProgram;

        setLights(gl3, lightPositionCameraSpace);

        modelMatrix.push();
        {
            renderGround(gl3, modelMatrix);
        }
        modelMatrix.pop();

        modelMatrix.push();
        {
            renderCylinder(gl3, modelMatrix);
        }
        modelMatrix.pop();

        if (drawLight) {

            modelMatrix.push();
            {
                renderLight(gl3, modelMatrix, lightPositionWorldSpace);
            }
            modelMatrix.pop();
        }

        drawable.swapBuffers();
    }

    private Vec4 calculateLightPosition() {

        float currentTimeThroughLoop = lightTimer.getAlpha();

        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = (float) (Math.cos(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);
        ret.z = (float) (Math.sin(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);

        return ret;
    }

    private void setLights(GL3 gl3, Vec4 lightPositionCameraSpace) {

        whiteProgram.bind(gl3);
        {
            gl3.glUniform4f(whiteProgram.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(whiteProgram.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(whiteProgram.getUnLocLightCameraSpacePosition(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform1f(whiteProgram.getUnLocLightAttenuation(), lightAttenuation);

            gl3.glUniform1f(whiteProgram.getUnLocShininessFactor(), materialParameters.getSpecularValue());

            gl3.glUniform4fv(whiteProgram.getUnLocBaseDiffuseColor(), 1, (drawDark ? darkColor : lightColor).toFloatArray(), 0);

        }
        whiteProgram.unbind(gl3);

        colorProgram.bind(gl3);
        {
            gl3.glUniform4f(colorProgram.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(colorProgram.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(colorProgram.getUnLocLightCameraSpacePosition(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform1f(colorProgram.getUnLocLightAttenuation(), lightAttenuation);

            gl3.glUniform1f(colorProgram.getUnLocShininessFactor(), materialParameters.getSpecularValue());
        }
        colorProgram.unbind(gl3);
    }

    private void renderGround(GL3 gl3, MatrixStack modelMatrix) {

        Mat3 normalMatrix = new Mat3(modelMatrix.top());

        normalMatrix = normalMatrix.inverse();

        normalMatrix = normalMatrix.transpose();

        whiteProgram.bind(gl3);
        {
            gl3.glUniformMatrix4fv(whiteProgram.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

            gl3.glUniformMatrix3fv(whiteProgram.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

            plane.render(gl3);
        }
        whiteProgram.unbind(gl3);
    }

    private void renderCylinder(GL3 gl3, MatrixStack modelMatrix) {

        modelMatrix.applyMat(objectPole.calcMatrix());

        if (scaleCylinder) {
            modelMatrix.scale(new Vec3(1.0f, 1.0f, 0.2f));
        }

        Mat3 normalMatrix = new Mat3(modelMatrix.top());
        normalMatrix = normalMatrix.inverse();
        normalMatrix = normalMatrix.transpose();

        LitProgram program = coloredCylinder ? colorProgram : whiteProgram;

        program.bind(gl3);
        {
            gl3.glUniformMatrix4fv(program.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

            gl3.glUniformMatrix3fv(program.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

            if (coloredCylinder) {
                cylinder.render(gl3, "lit-color");
            } else {
                cylinder.render(gl3, "lit");
            }
        }
        program.unbind(gl3);
    }

    private void renderLight(GL3 gl3, MatrixStack modelStack, Vec4 lightPositionWorldSpace) {
        modelStack.translate(new Vec3(lightPositionWorldSpace));
        modelStack.scale(new Vec3(0.1f, 0.1f, 0.1f));

        unlit.bind(gl3);
        {
            gl3.glUniformMatrix4fv(unlit.getUnLocModelToCameraMatrix(), 1, false, modelStack.top().toFloatArray(), 0);

            gl3.glUniform4f(unlit.getUnLocObjectColor(), 0.8078f, 0.8078f, 0.9922f, 1.0f);

            cube.render(gl3, "flat");
        }
        unlit.unbind(gl3);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        float zNear = 1.0f;
        float zFar = 1000.0f;
        GL3 gl3 = drawable.getGL().getGL3();

        int size = 16 * 4;

        MatrixStack perspectiveMatrix = new MatrixStack();
        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) width / (float) height, zNear, zFar));

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, size, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, width, height);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        float offset;

        if (e.isShiftDown()) {

            offset = 0.05f;

        } else {

            offset = 0.2f;
        }

        switch (e.getKeyCode()) {

            case KeyEvent.VK_SPACE:
                coloredCylinder = !coloredCylinder;
                break;

            case KeyEvent.VK_Y:
                drawLight = !drawLight;
                break;

            case KeyEvent.VK_I:
                lightHeight += offset;
                break;

            case KeyEvent.VK_K:
                lightHeight -= offset;
                break;

            case KeyEvent.VK_L:
                lightRadius += offset;
                break;

            case KeyEvent.VK_J:
                lightRadius -= offset;
                if (lightRadius < 0.2f) {
                    lightRadius = 0.2f;
                }
                break;

            case KeyEvent.VK_O:
                materialParameters.increment(e.isShiftDown());
                break;

            case KeyEvent.VK_U:
                materialParameters.decrement(e.isShiftDown());
                break;

            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_H:
                if (e.isShiftDown()) {
                    switch (lightingModel) {
                        case Blinn:
                            lightingModel = LightingModel.DiffuseAndBlinn;
                            break;
                        case DiffuseAndBlinn:
                            lightingModel = LightingModel.Blinn;
                            break;
                        case Phong:
                            lightingModel = LightingModel.DiffuseAndPhong;
                            break;
                        case DiffuseAndPhong:
                            lightingModel = LightingModel.Phong;
                            break;
                        case Gaussian:
                            lightingModel = LightingModel.DiffuseAndGaussian;
                            break;
                        case DiffuseAndGaussian:
                            lightingModel = LightingModel.Gaussian;
                            break;
                    }
                } else {
                    switch (lightingModel) {
                        case Phong:
                            lightingModel = LightingModel.Blinn;
                            break;
                        case Blinn:
                            lightingModel = LightingModel.Gaussian;
                            break;
                        case Gaussian:
                            lightingModel = LightingModel.Phong;
                            break;
                        case DiffuseAndPhong:
                            lightingModel = LightingModel.DiffuseAndBlinn;
                            break;
                        case DiffuseAndBlinn:
                            lightingModel = LightingModel.DiffuseAndGaussian;
                            break;
                        case DiffuseAndGaussian:
                            lightingModel = LightingModel.DiffuseAndPhong;
                            break;
                    }
                    printLightingModel();
                }
                break;

            case KeyEvent.VK_T:
                scaleCylinder = !scaleCylinder;
                break;

            case KeyEvent.VK_G:
                drawDark = !drawDark;
                break;
        }
    }

    private void printLightingModel() {

        switch (lightingModel) {

            case Phong:
                System.out.println("Phong");
                break;
            case DiffuseAndPhong:
                System.out.println("DiffuseAndPhong");
                break;
            case Blinn:
                System.out.println("Blinn");
                break;
            case DiffuseAndBlinn:
                System.out.println("DiffuseAndBlinn");
                break;
            case Gaussian:
                System.out.println("Gaussian");
                break;
            case DiffuseAndGaussian:
                System.out.println("DiffuseAndGaussian");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
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
    public void mouseWheelMoved(MouseWheelEvent e) {
        viewPole.mouseWheel(e);
    }

    public GLCanvas getCanvas() {
        return canvas;


    }

    private enum LightingModel {

        DiffuseAndPhong,
        Phong,
        DiffuseAndBlinn,
        Blinn,
        DiffuseAndGaussian,
        Gaussian;
    }

    private class ProgramPairs {

        public LitProgram whiteProgram;
        public LitProgram colorProgram;
    }

    private class ShaderPairs {

        public String whiteVS;
        public String colorVS;
        public String fS;

        public ShaderPairs(String whiteVS, String colorVS, String fS) {

            this.whiteVS = whiteVS;
            this.colorVS = colorVS;
            this.fS = fS;
        }
    }

    private class MaterialParameters {

        private float shininessFactorPhong;
        private float shininessFactorBlinn;
        private float roughnessGaussian;

        public MaterialParameters() {

            shininessFactorPhong = 4.0f;
            shininessFactorBlinn = 4.0f;
            roughnessGaussian = 0.5f;
        }

        public float getSpecularValue() {

            switch (lightingModel) {

                case DiffuseAndBlinn:
                case Blinn:
                    return shininessFactorBlinn;

                case DiffuseAndPhong:
                case Phong:
                    return shininessFactorPhong;

                default:
                    return roughnessGaussian;
            }
        }

        public void increment(boolean small) {

            float increment;

            if (small) {
                increment = 0.1f;
            } else {
                increment = 0.5f;
            }

            switch (lightingModel) {

                case Blinn:
                case DiffuseAndBlinn:
                    shininessFactorBlinn += increment;
                    System.out.println("shininessFactorBlinn: " + shininessFactorBlinn);
                    break;

                case Phong:
                case DiffuseAndPhong:
                    shininessFactorPhong += increment;
                    System.out.println("shininessFactorPhong: " + shininessFactorPhong);
                    break;

                default:
                    if (small) {
                        increment = 0.01f;
                    } else {
                        increment = 0.1f;
                    }
                    roughnessGaussian += increment;
                    System.out.println("roughnessGaussian: " + roughnessGaussian);
            }
        }

        public void decrement(boolean small) {

            float decrement;

            if (small) {
                decrement = 0.1f;
            } else {
                decrement = 0.5f;
            }

            switch (lightingModel) {

                case Blinn:
                case DiffuseAndBlinn:
                    shininessFactorBlinn -= decrement;
                    shininessFactorBlinn = Jglm.clamp(shininessFactorBlinn, 0.0001f, shininessFactorBlinn);
                    System.out.println("shininessFactorBlinn: " + shininessFactorBlinn);
                    break;

                case Phong:
                case DiffuseAndPhong:
                    shininessFactorPhong -= decrement;
                    shininessFactorPhong = Jglm.clamp(shininessFactorPhong, 0.0001f, shininessFactorPhong);
                    System.out.println("shininessFactorPhong: " + shininessFactorPhong);
                    break;

                default:
                    if (small) {
                        decrement = 0.01f;
                    } else {
                        decrement = 0.1f;
                    }
                    roughnessGaussian -= decrement;
                    roughnessGaussian = Jglm.clamp(roughnessGaussian, 0.00001f, 1.0f);
                    System.out.println("roughnessGaussian: " + roughnessGaussian);
            }
        }
    }
}