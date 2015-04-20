/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut11.phongLighting;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
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
public class PhongLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private GLCanvas canvas;
    private int imageWidth;
    private int imageHeight;
    private LitProgram whiteNoPhong;
    private LitProgram colorNoPhong;
    private LitProgram whitePhong;
    private LitProgram colorPhong;
    private LitProgram whitePhongOnly;
    private LitProgram colorPhongOnly;
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
    private float shininessFactor;
    private Vec4 darkColor;
    private Vec4 lightColor;
    private LightingModel lightingModel;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        PhongLighting phongLighting = new PhongLighting();

        Frame frame = new Frame("Tutorial 10 - Phong Lighting");

        frame.add(phongLighting.getCanvas());

        frame.setSize(phongLighting.getCanvas().getWidth(), phongLighting.getCanvas().getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(phongLighting.canvas, 30);

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

    public PhongLighting() {
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
        String shadersFilepath = "/tut11/phongLighting/shaders/";

        canvas.setAutoSwapBufferMode(false);

        initializePrograms(gl3, shadersFilepath, projectionUBB);

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

        lightingModel = LightingModel.DiffuseAndSpecular;

        coloredCylinder = false;
        drawLight = false;

        scaleCylinder = false;

        drawDark = false;

        lightAttenuation = 1.2f;
        shininessFactor = 4.0f;

        darkColor = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
        lightColor = new Vec4(1.0f);
    }

    private void initializePrograms(GL3 gl3, String shadersFilepath, int projectionUBB) {

        whiteNoPhong = new LitProgram(gl3, shadersFilepath, "PN_VS.glsl", "NoPhong_FS.glsl", projectionUBB);
        colorNoPhong = new LitProgram(gl3, shadersFilepath, "PCN_VS.glsl", "NoPhong_FS.glsl", projectionUBB);

        whitePhong = new LitProgram(gl3, shadersFilepath, "PN_VS.glsl", "Phong_FS.glsl", projectionUBB);
        colorPhong = new LitProgram(gl3, shadersFilepath, "PCN_VS.glsl", "Phong_FS.glsl", projectionUBB);

        whitePhongOnly = new LitProgram(gl3, shadersFilepath, "PN_VS.glsl", "PhongOnly_FS.glsl", projectionUBB);
        colorPhongOnly = new LitProgram(gl3, shadersFilepath, "PCN_VS.glsl", "PhongOnly_FS.glsl", projectionUBB);

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

        setPrograms();

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

    private void setPrograms() {

        switch (lightingModel) {

            case DiffuseOnly:
                whiteProgram = whiteNoPhong;
                colorProgram = colorNoPhong;
                break;

            case DiffuseAndSpecular:
                whiteProgram = whitePhong;
                colorProgram = colorPhong;
                break;

            case SpecularOnly:
                whiteProgram = whitePhongOnly;
                colorProgram = colorPhongOnly;
                break;
        }
    }

    private void setLights(GL3 gl3, Vec4 lightPositionCameraSpace) {

        whiteProgram.bind(gl3);
        {
            gl3.glUniform4f(whiteProgram.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);
            
            gl3.glUniform4f(whiteProgram.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
            
            gl3.glUniform3fv(whiteProgram.getUnLocLightCameraSpacePosition(), 1, lightPositionCameraSpace.toFloatArray(), 0);
            
            gl3.glUniform1f(whiteProgram.getUnLocLightAttenuation(), lightAttenuation);
            
            gl3.glUniform1f(whiteProgram.getUnLocShininessFactor(), shininessFactor);
            
            gl3.glUniform4fv(whiteProgram.getUnLocBaseDiffuseColor(), 1, (drawDark ? darkColor : lightColor).toFloatArray(), 0);
            
        }
        whiteProgram.unbind(gl3);

        colorProgram.bind(gl3);
        {
            gl3.glUniform4f(colorProgram.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(colorProgram.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(colorProgram.getUnLocLightCameraSpacePosition(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform1f(colorProgram.getUnLocLightAttenuation(), lightAttenuation);

            gl3.glUniform1f(colorProgram.getUnLocShininessFactor(), shininessFactor);
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
                float delta;
                if (e.isShiftDown()) {
                    delta = 0.1f;
                } else {
                    delta = 0.5f;
                }
                shininessFactor += delta;
                System.out.println("shininessFactor: " + shininessFactor);
                break;

            case KeyEvent.VK_U:
                if (e.isShiftDown()) {
                    delta = 1.1f;
                } else {
                    delta = 1.5f;
                }
                shininessFactor -= delta;
                if (shininessFactor <= 0.0f) {
                    shininessFactor = 0.0001f;
                }
                System.out.println("shininessFactor: " + shininessFactor);
                break;

            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_H:
                if (e.isShiftDown()) {
                    if (lightingModel == LightingModel.DiffuseOnly) {
                        lightingModel = LightingModel.DiffuseAndSpecular;
                        System.out.println("DiffuseAndSpecular");
                    } else {
                        lightingModel = LightingModel.DiffuseOnly;
                        System.out.println("DiffuseOnly");
                    }
                } else {
                    switch (lightingModel) {
                        case DiffuseAndSpecular:
                            lightingModel = LightingModel.SpecularOnly;
                            System.out.println("SpecularOnly");
                            break;
                        case SpecularOnly:
                            lightingModel = LightingModel.DiffuseOnly;
                            System.out.println("DiffuseOnly");
                            break;
                        case DiffuseOnly:
                            lightingModel = LightingModel.DiffuseAndSpecular;
                            System.out.println("DiffuseAndSpecular");
                            break;
                    }
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

        DiffuseOnly,
        DiffuseAndSpecular,
        SpecularOnly;
    }
}