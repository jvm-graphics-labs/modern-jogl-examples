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
import framework.jglm.Mat4;
import framework.jglm.Quat;
import framework.jglm.Vec3;
import framework.jglm.Vec4;
import framework.component.Mesh;
import tut10.glsl.LitProgram2;
import tut10.glsl.UnlitProgram;

/**
 *
 * @author gbarbieri
 */
public class FragmentPointLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private GLCanvas canvas;
    private int imageWidth;
    private int imageHeight;
    private LitProgram2 whiteDiffuseColor;
    private LitProgram2 vertexDiffuseColor;
    private LitProgram2 fragWhiteDiffuseColor;
    private LitProgram2 fragVertexDiffuseColor;
    private UnlitProgram unlitProgram;
    private ViewPole viewPole;
    private ObjectPole objectPole;
    private Mesh cylinder;
    private Mesh plane;
    private Mesh cube;
    private int[] projectionUBO;
    private Timer lightTimer;
    private float lightHeight;
    private float lightRadius;
    private boolean fragmentLighting;
    private boolean coloredCylinder;
    private boolean drawLight;
    private boolean scaleCylinder;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        FragmentPointLighting fragmentPointLighting = new FragmentPointLighting();

        Frame frame = new Frame("Tutorial 10 - Fragment Point Lighting");

        frame.add(fragmentPointLighting.getCanvas());

        frame.setSize(fragmentPointLighting.getCanvas().getWidth(), fragmentPointLighting.getCanvas().getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(fragmentPointLighting.canvas, 30);

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

    public FragmentPointLighting() {
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
        int projectionUBB = 0;
        String shadersFilepath = "/tut10/shaders/";

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

        fragmentLighting = true;
        coloredCylinder = false;
        drawLight = false;
        scaleCylinder = false;
    }

    private void initializePrograms(GL3 gl3, String shadersFilepath, int projectionUBB) {

        whiteDiffuseColor = new LitProgram2(gl3, shadersFilepath, "ModelPosVertexLighting_PN_VS.glsl", "ColorPassthrough_FS.glsl", projectionUBB);

        vertexDiffuseColor = new LitProgram2(gl3, shadersFilepath, "ModelPosVertexLighting_PCN_VS.glsl", "ColorPassthrough_FS.glsl", projectionUBB);

        fragWhiteDiffuseColor = new LitProgram2(gl3, shadersFilepath, "FragmentLighting_PN_VS.glsl", "FragmentLighting_FS.glsl", projectionUBB);

        fragVertexDiffuseColor = new LitProgram2(gl3, shadersFilepath, "FragmentLighting_PCN_VS.glsl", "FragmentLighting_FS.glsl", projectionUBB);

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

        setLights(gl3);

        modelMatrix.push();
        {
            renderGround(gl3, modelMatrix, lightPositionCameraSpace);
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

        System.out.println("currentTimeThroughLoop: "+currentTimeThroughLoop+" x: "+ret.x+" lightRadius: "+lightRadius);
        
        return ret;
    }

    private void setLights(GL3 gl3) {

        if (fragmentLighting) {

            fragWhiteDiffuseColor.bind(gl3);
            {
                gl3.glUniform4f(fragWhiteDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

                gl3.glUniform4f(fragWhiteDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
            }
            fragVertexDiffuseColor.bind(gl3);
            {
                gl3.glUniform4f(fragVertexDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

                gl3.glUniform4f(fragVertexDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
            }
            fragVertexDiffuseColor.unbind(gl3);
        } else {

            whiteDiffuseColor.bind(gl3);
            {
                gl3.glUniform4f(whiteDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

                gl3.glUniform4f(whiteDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
            }
            vertexDiffuseColor.bind(gl3);
            {
                gl3.glUniform4f(vertexDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

                gl3.glUniform4f(vertexDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
            }
            vertexDiffuseColor.unbind(gl3);
        }
    }

    private void renderGround(GL3 gl3, MatrixStack modelMatrix, Vec4 lightPositionCameraSpace) {

        Mat4 inverseTransform = modelMatrix.top().inverse();
        Vec4 lightPositionModelSpace = inverseTransform.mult(lightPositionCameraSpace);

        if (fragmentLighting) {

            fragWhiteDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(fragWhiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniform3fv(fragWhiteDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                plane.render(gl3);
            }
            fragWhiteDiffuseColor.unbind(gl3);

        } else {

            whiteDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(whiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniform3fv(whiteDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                plane.render(gl3);
            }
            whiteDiffuseColor.unbind(gl3);
        }
    }

    private void renderCylinder(GL3 gl3, MatrixStack modelMatrix, Vec4 lightPositionCameraSpace) {

        modelMatrix.applyMat(objectPole.calcMatrix());

        if (scaleCylinder) {
            modelMatrix.scale(new Vec3(1.0f, 1.0f, 0.2f));
        }

        Mat4 inverseTransform = modelMatrix.top().inverse();
        Vec4 lightPositionModelSpace = inverseTransform.mult(lightPositionCameraSpace);

        if (fragmentLighting) {

            if (coloredCylinder) {

                fragVertexDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(fragVertexDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniform3fv(fragVertexDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                    cylinder.render(gl3, "lit-color");
                }
                fragVertexDiffuseColor.unbind(gl3);

            } else {

                fragWhiteDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(fragWhiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniform3fv(fragWhiteDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                    cylinder.render(gl3, "lit");
                }
                fragWhiteDiffuseColor.unbind(gl3);
            }
        } else {

            if (coloredCylinder) {

                vertexDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(vertexDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniform3fv(vertexDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                    cylinder.render(gl3, "lit-color");
                }
                vertexDiffuseColor.unbind(gl3);

            } else {

                whiteDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(whiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniform3fv(whiteDiffuseColor.getUnLocLightPosition(), 1, lightPositionModelSpace.toFloatArray(), 0);

                    cylinder.render(gl3, "lit");
                }
                whiteDiffuseColor.unbind(gl3);
            }
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

        MatrixStack perspectiveMatrix = new MatrixStack();
        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) width / (float) height, zNear, zFar));

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
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

            case KeyEvent.VK_H:
                fragmentLighting = !fragmentLighting;
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

            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_T:
                scaleCylinder = !scaleCylinder;
                break;
        }

        System.out.println("drawLight: " + drawLight);

        if (lightRadius < 0.2f) {
            lightRadius = 0.2f;
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
