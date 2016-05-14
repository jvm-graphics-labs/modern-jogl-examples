/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut10;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import framework.jglm.Jglm;
import framework.jglm.Mat3;
import framework.jglm.Mat4;
import framework.jglm.Quat;
import framework.jglm.Vec2i;
import framework.jglm.Vec3;
import framework.jglm.Vec4;
import framework.component.Mesh;
import tut10.glsl.LitProgram3;
import tut10.glsl.UnlitProgram;

/**
 *
 * @author gbarbieri
 */
public class FragmentAttenuation implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private GLCanvas canvas;
    private int imageWidth;
    private int imageHeight;
    private LitProgram3 fragWhiteDiffuseColor;
    private LitProgram3 fragVertexDiffuseColor;
    private UnlitProgram unlitProgram;
    private ViewPole viewPole;
    private ObjectPole objectPole;
    private Mesh cylinder;
    private Mesh plane;
    private Mesh cube;
    private int[] projectionUBO;
    private int[] unProjectionUBO;
    private Timer lightTimer;
    private float lightHeight;
    private float lightRadius;
    private boolean coloredCylinder;
    private boolean drawLight;
    private boolean rSquare;
    private float lightAttenuation;
    private boolean scaleCylinder;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        FragmentAttenuation fragmentAttenuation = new FragmentAttenuation();

        Frame frame = new Frame("Tutorial 10 - Fragment Attenuation");

        frame.add(fragmentAttenuation.getCanvas());

        frame.setSize(fragmentAttenuation.getCanvas().getWidth(), fragmentAttenuation.getCanvas().getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(fragmentAttenuation.canvas, 30);

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

    public FragmentAttenuation() {
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
        int unProjectionUBB = 1;
        String shadersFilepath = "/tut10/shaders/";

        canvas.setAutoSwapBufferMode(false);

        initializePrograms(gl3, shadersFilepath, projectionUBB, unProjectionUBB);

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

        initUBO(gl3, projectionUBB, unProjectionUBB);

        lightHeight = 1.5f;
        lightRadius = 1.0f;

        lightTimer = new Timer(Timer.Type.Loop, 5.0f);

        coloredCylinder = false;
        drawLight = false;
        rSquare = false;

        lightAttenuation = 1.0f;

        scaleCylinder = false;
    }

    private void initializePrograms(GL3 gl3, String shadersFilepath, int projectionUBB, int unProjectionUBB) {

        fragWhiteDiffuseColor = new LitProgram3(gl3, shadersFilepath, "FragLightAtten_PN_VS.glsl", "FragLightAtten_FS.glsl", projectionUBB, unProjectionUBB);

        fragVertexDiffuseColor = new LitProgram3(gl3, shadersFilepath, "FragLightAtten_PCN_VS.glsl", "FragLightAtten_FS.glsl", projectionUBB, unProjectionUBB);

        unlitProgram = new UnlitProgram(gl3, shadersFilepath, "PosTransform_VS.glsl", "UniformColor_FS.glsl", projectionUBB);
    }

    private void initializeMeshes(GL3 gl3) {

        String dataFilepath = "/tut10/data/";

//        cylinder = new Mesh(dataFilepath + "UnitCylinder.xml", gl3);
//
//        plane = new Mesh(dataFilepath + "LargePlane.xml", gl3);
//
//        cube = new Mesh(dataFilepath + "UnitCube.xml", gl3);
    }

    private void initUBO(GL3 gl3, int projectionUBB, int unProjectionUBB) {

        int size = 16 * 4;

        projectionUBO = new int[1];

        gl3.glGenBuffers(1, projectionUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionUBB, projectionUBO[0], 0, size);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        size = (16 + 2) * 4;

        unProjectionUBO = new int[1];

        gl3.glGenBuffers(1, unProjectionUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, unProjectionUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, unProjectionUBB, unProjectionUBO[0], 0, size);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        System.out.println("display");

        GL3 gl3 = drawable.getGL().getGL3();

        lightTimer.update();

        gl3.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        MatrixStack modelMatrix = new MatrixStack();

        modelMatrix.setTop(viewPole.calcMatrix());

        Vec4 lightPositionWorldSpace = calculateLightPosition();

        Vec4 lightPositionCameraSpace = modelMatrix.top().mult(lightPositionWorldSpace);

        setLights(gl3, lightPositionCameraSpace);

        modelMatrix.push();
        {
            renderGround(gl3, modelMatrix);
        }
        modelMatrix.pop();

        modelMatrix.push();
        {
            renderCylinder(gl3, modelMatrix, lightPositionCameraSpace);
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

        fragVertexDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(fragVertexDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(fragVertexDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(fragVertexDiffuseColor.getUnLocLightPositionCameraSpace(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform1f(fragVertexDiffuseColor.getUnLocLightAttenuation(), lightAttenuation);

            gl3.glUniform1i(fragVertexDiffuseColor.getUnLocRsquare(), rSquare ? 1 : 0);
        }
        fragVertexDiffuseColor.unbind(gl3);

        fragWhiteDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(fragWhiteDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(fragWhiteDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(fragWhiteDiffuseColor.getUnLocLightPositionCameraSpace(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform1f(fragWhiteDiffuseColor.getUnLocLightAttenuation(), lightAttenuation);

            gl3.glUniform1i(fragWhiteDiffuseColor.getUnLocRsquare(), rSquare ? 1 : 0);
        }
        fragWhiteDiffuseColor.unbind(gl3);
    }

    private void renderGround(GL3 gl3, MatrixStack modelMatrix) {

        Mat3 normalMatrix = new Mat3(modelMatrix.top());

        normalMatrix = normalMatrix.inverse();

        normalMatrix = normalMatrix.transpose();

        fragWhiteDiffuseColor.bind(gl3);
        {
            gl3.glUniformMatrix4fv(fragWhiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

            gl3.glUniformMatrix3fv(fragWhiteDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

            plane.render(gl3);
        }
        fragWhiteDiffuseColor.unbind(gl3);
    }

    private void renderCylinder(GL3 gl3, MatrixStack modelMatrix, Vec4 lightPositionCameraSpace) {

        modelMatrix.applyMat(objectPole.calcMatrix());

        if (scaleCylinder) {
            modelMatrix.scale(new Vec3(1.0f, 1.0f, 0.2f));
        }

        Mat3 normalMatrix = new Mat3(modelMatrix.top());

        normalMatrix = normalMatrix.inverse();

        normalMatrix = normalMatrix.transpose();

        if (coloredCylinder) {

            fragVertexDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(fragVertexDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniformMatrix3fv(fragVertexDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

                cylinder.render(gl3, "lit-color");
            }
            fragVertexDiffuseColor.unbind(gl3);

        } else {

            fragWhiteDiffuseColor.bind(gl3);
            {

                gl3.glUniformMatrix4fv(fragWhiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniformMatrix3fv(fragWhiteDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

                cylinder.render(gl3, "lit");
            }
            fragWhiteDiffuseColor.unbind(gl3);
        }
    }

    private void renderLight(GL3 gl3, MatrixStack modelStack, Vec4 lightPositionWorldSpace) {

        modelStack.translate(new Vec3(lightPositionWorldSpace));
        modelStack.scale(new Vec3(0.1f, 0.1f, 0.1f));

        unlitProgram.bind(gl3);
        {
            gl3.glUniformMatrix4fv(unlitProgram.getUnLocModelToCameraMatrix(), 1, false, modelStack.top().toFloatArray(), 0);

            gl3.glUniform4f(unlitProgram.getUnLocObjectColor(), 0.8078f, 0.8078f, 0.9922f, 1.0f);

            cube.render(gl3, "flat");
        }
        unlitProgram.unbind(gl3);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        float zNear = 1.0f;
        float zFar = 1000.0f;
        GL3 gl3 = drawable.getGL().getGL3();

        int size = 16 * 4;

        MatrixStack perspectiveMatrix = new MatrixStack();
        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) width / (float) height, zNear, zFar));

        Mat4 clipToCameraMatrix = perspectiveMatrix.top().inverse();
        Vec2i windowSize = new Vec2i(width, height);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, size, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, unProjectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, size, GLBuffers.newDirectFloatBuffer(clipToCameraMatrix.toFloatArray()));

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, size, 2 * 4, GLBuffers.newDirectIntBuffer(windowSize.toIntArray()));
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
                break;

            case KeyEvent.VK_O:
                float delta;
                if (e.isShiftDown()) {
                    delta = 1.1f;
                } else {
                    delta = 1.5f;
                }
                lightAttenuation *= delta;
                System.out.println("lightAttenuation: " + lightAttenuation);
                break;

            case KeyEvent.VK_U:
                if (e.isShiftDown()) {
                    delta = 1.1f;
                } else {
                    delta = 1.5f;
                }
                lightAttenuation /= delta;
                System.out.println("lightAttenuation: " + lightAttenuation);
                break;

            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_H:
                rSquare = !rSquare;
                if (rSquare) {
                    System.out.println("Inverse Squared Attenuation");
                } else {
                    System.out.println("Plain Inverse Attentuation");
                }
                break;

            case KeyEvent.VK_T:
                scaleCylinder = !scaleCylinder;
                break;
        }

        if (lightRadius < 0.2f) {
            lightRadius = 0.2f;
        }

        if (lightAttenuation < 0.1f) {
            lightAttenuation = 0.1f;
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
}