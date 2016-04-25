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
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import framework.jglm.Quat;
import framework.jglm.Vec3;
import framework.component.Mesh;
import framework.glutil.MatrixStack;
import framework.glutil.ObjectData;
import framework.glutil.ObjectPole;
import framework.glutil.Timer;
import framework.glutil.ViewData;
import framework.glutil.ViewPole;
import framework.glutil.ViewScale;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import framework.jglm.Jglm;
import framework.jglm.Mat3;
import framework.jglm.Vec4;
import tut10.glsl.LitProgram;
import tut10.glsl.UnlitProgram;

/**
 *
 * @author gbarbieri
 */
public class VertexPointLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private LitProgram whiteDiffuseColor;
    private LitProgram vertexDiffuseColor;
    private UnlitProgram unlit;
    private String shadersFilepath = "/tut10/shaders/";
    private String dataFilepath = "/tut10/data/";
    private Mesh cylinder;
    private Mesh plane;
    private Mesh cube;
    private float zNear;
    private float zFar;
    private int projectionUBB;
    private int[] projectionUBO;
    private ViewPole viewPole;
    private ViewData initialViewData;
    private ViewScale viewScale;
    private ObjectPole objectPole;
    private ObjectData initialObjectData;
    private boolean drawColoredCylinder;
    private boolean drawLight;
    private float lightHeight;
    private float lightRadius;
    private Timer lightTimer;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        VertexPointLighting vertexPointLighting = new VertexPointLighting();

        Frame frame = new Frame("Tutorial 10 - Vertex Point Lighting");

        frame.add(vertexPointLighting.getCanvas());

        frame.setSize(vertexPointLighting.getCanvas().getWidth(), vertexPointLighting.getCanvas().getHeight());

        final FPSAnimator fPSAnimator = new FPSAnimator(vertexPointLighting.canvas, 60);

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

    public VertexPointLighting() {
        initGL();
    }

    private void initGL() {
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
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        projectionUBB = 0;

        initializePrograms(gl3);

        initializeMeshes(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
        gl3.glEnable(GL3.GL_DEPTH_CLAMP);

        projectionUBO = new int[1];
        gl3.glGenBuffers(1, projectionUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, 16 * 4, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionUBB, projectionUBO[0], 0, 16 * 4);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        initialViewData = new ViewData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.3826834f, 0.0f, 0.0f, 0.92387953f), 5.0f, 0.0f);

        viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.5f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale);

        initialObjectData = new ObjectData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.0f, 0.0f, 0.0f, 1.0f));

        objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, viewPole);

        lightHeight = 1.5f;

        lightRadius = 1.0f;

        lightTimer = new Timer(Timer.Type.Loop, 5.0f);

        zNear = 1.0f;
        zFar = 1000.0f;

        drawColoredCylinder = false;
        drawLight = false;
    }

    private void initializePrograms(GL3 gl3) {

        System.out.println("initializePrograms...");

        whiteDiffuseColor = new LitProgram(gl3, shadersFilepath, "PosVertexLighting_PN_VS.glsl", "ColorPassthrough_FS.glsl", projectionUBB);
        vertexDiffuseColor = new LitProgram(gl3, shadersFilepath, "PosVertexLighting_PCN_VS.glsl", "ColorPassthrough_FS.glsl", projectionUBB);
        unlit = new UnlitProgram(gl3, shadersFilepath, "PosTransform_VS.glsl", "UniformColor_FS.glsl", projectionUBB);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        lightTimer.update();

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        MatrixStack modelMatrix = new MatrixStack();

        modelMatrix.setTop(viewPole.calcMatrix());

        Vec4 worldLightPosition = calcLightPosition();

        Vec4 lightPositionCameraSpace = modelMatrix.top().mult(worldLightPosition);

        setLight(gl3, lightPositionCameraSpace);

        modelMatrix.push();
        {
            renderGroundPlane(gl3, modelMatrix);
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
                renderLight(gl3, modelMatrix, worldLightPosition);
            }
            modelMatrix.pop();
        }
        glad.swapBuffers();
    }

    private Vec4 calcLightPosition() {

        float currentTimeThroughLoop = lightTimer.getAlpha();

        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = (float) (Math.cos(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);

        ret.z = (float) (Math.sin(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);

        System.out.println("currentTimeThroughLoop: "+currentTimeThroughLoop+" x: "+ret.x+" lightRadius: "+lightRadius);
        
        return ret;
    }

    private void setLight(GL3 gl3, Vec4 lightPositionCameraSpace) {

        Vec3 test = new Vec3(lightPositionCameraSpace.x, lightPositionCameraSpace.y, lightPositionCameraSpace.z);

        whiteDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(whiteDiffuseColor.getUnLocLightPosition(), 1, test.toFloatArray(), 0);

            gl3.glUniform4f(whiteDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(whiteDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
        }
        vertexDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(vertexDiffuseColor.getUnLocLightPosition(), 1, lightPositionCameraSpace.toFloatArray(), 0);

            gl3.glUniform4f(vertexDiffuseColor.getUnLocLightDiffuseIntensity(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(vertexDiffuseColor.getUnLocLightAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);
        }
        vertexDiffuseColor.unbind(gl3);
    }

    private void renderGroundPlane(GL3 gl3, MatrixStack modelMatrix) {

        whiteDiffuseColor.bind(gl3);
        {
            gl3.glUniformMatrix4fv(whiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

            Mat3 normalMatrix = new Mat3(modelMatrix.top());

            gl3.glUniformMatrix3fv(whiteDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

            plane.render(gl3);
        }
        whiteDiffuseColor.unbind(gl3);
    }

    private void renderCylinder(GL3 gl3, MatrixStack modelMatrix) {

        modelMatrix.applyMat(objectPole.calcMatrix());

        if (drawColoredCylinder) {

            vertexDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(vertexDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                Mat3 normalMatrix = new Mat3(modelMatrix.top());

                gl3.glUniformMatrix3fv(vertexDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

                cylinder.render(gl3, "lit-color");
            }
            vertexDiffuseColor.unbind(gl3);
        } else {

            whiteDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(whiteDiffuseColor.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

                Mat3 normalMatrix = new Mat3(modelMatrix.top());

                gl3.glUniformMatrix3fv(whiteDiffuseColor.getUnLocNormalModelToCameraMatrix(), 1, false, normalMatrix.toFloatArray(), 0);

                cylinder.render(gl3, "lit");
            }
            whiteDiffuseColor.unbind(gl3);
        }
    }

    private void renderLight(GL3 gl3, MatrixStack modelMatrix, Vec4 worldLightPosition) {

        modelMatrix.translate(new Vec3(worldLightPosition));
        modelMatrix.scale(new Vec3(0.1f, 0.1f, 0.1f));

//        System.out.println("unlit.getUnLocModelToCameraMatrix(): " + unlit.getUnLocModelToCameraMatrix());

        unlit.bind(gl3);
        {
            gl3.glUniformMatrix4fv(unlit.getUnLocModelToCameraMatrix(), 1, false, modelMatrix.top().toFloatArray(), 0);

            gl3.glUniform4f(unlit.getUnLocObjectColor(), 0.8078f, 0.876f, 0.9922f, 1.0f);

            cube.render(gl3, "flat");
        }
        unlit.unbind(gl3);
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        MatrixStack perspectiveMatrix = new MatrixStack();

        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) w / (float) h, zNear, zFar));

        perspectiveMatrix.top().print("perspectiveMatrix");

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    private void initializeMeshes(GL3 gl3) {
        System.out.println("initializeObjects");

        cylinder = new Mesh(dataFilepath + "UnitCylinder.xml", gl3);
        plane = new Mesh(dataFilepath + "LargePlane.xml", gl3);
        cube = new Mesh(dataFilepath + "UnitCube.xml", gl3);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {

        viewPole.mousePressed(e);
        objectPole.mousePressed(e);

//        canvas.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        viewPole.mouseReleased(e);
        objectPole.mouseReleased(e);

//        canvas.display();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
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
                drawColoredCylinder = !drawColoredCylinder;
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
                
            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;
        }

        System.out.println("drawLight: " + drawLight);

        if(lightRadius<0.2f)
            lightRadius = 0.2f;
//        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        viewPole.mouseMove(e);
        objectPole.mouseMove(e);

//        canvas.display();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        viewPole.mouseWheel(e);

//        canvas.display();
    }
}