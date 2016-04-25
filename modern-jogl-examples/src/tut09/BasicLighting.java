/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
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
import tut09.glsl.GLSLProgramObject_1;

/**
 *
 * @author gbarbieri
 */
public class BasicLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject_1 whiteDiffuseColor;
    private GLSLProgramObject_1 vertexDiffuseColor;
    private String shadersFilepath = "/tut09/shaders/";
    private String dataFilepath = "/tut09/data/";
    private Mesh cylinder;
    private Mesh plane;
    private float zNear;
    private float zFar;
    private int projectionBlockIndex;
    private int[] projectionUBO;
    private ViewPole viewPole;
    private ViewData initialViewData;
    private ViewScale viewScale;
    private ObjectPole objectPole;
    private ObjectData initialObjectData;
    private Vec4 lightDirection;
    private boolean drawColoredCyl;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        BasicLighting basicLighting = new BasicLighting();

        Frame frame = new Frame("Tutorial 09 - Basic Lighting");

        frame.add(basicLighting.getCanvas());

        frame.setSize(basicLighting.getCanvas().getWidth(), basicLighting.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public BasicLighting() {
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

        projectionBlockIndex = 2;

        initializePrograms(gl3);

        initializeObjects(gl3);

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

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionBlockIndex, projectionUBO[0], 0, 16 * 4);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        initialViewData = new ViewData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.3826834f, 0.0f, 0.0f, 0.92387953f), 5.0f, 0.0f);

        viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.5f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale);

        initialObjectData = new ObjectData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.0f, 0.0f, 0.0f, 1.0f));

        objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, viewPole);

        lightDirection = new Vec4(0.866f, 0.5f, 0.0f, 0.0f);

        zNear = 1.0f;
        zFar = 1000.0f;

        drawColoredCyl = true;
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        MatrixStack modelMatrix = new MatrixStack();

        modelMatrix.setTop(viewPole.calcMatrix());

        Vec4 lightDirCameraSpace = modelMatrix.top().mult(lightDirection);

        whiteDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(whiteDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(vertexDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexDiffuseColor.unbind(gl3);

        modelMatrix.push();
        {
            //  Render the ground plane
            modelMatrix.push();
            {
                whiteDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    gl3.glUniform4f(whiteDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                    plane.render(gl3);
                }
                whiteDiffuseColor.unbind(gl3);
            }
            modelMatrix.pop();

            //  Render the Cylinder
            modelMatrix.push();
            {
                modelMatrix.applyMat(objectPole.calcMatrix());

                if (drawColoredCyl) {

                    vertexDiffuseColor.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(vertexDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                        Mat3 normalMatrix = new Mat3(modelMatrix.top());
                        gl3.glUniformMatrix3fv(vertexDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                        gl3.glUniform4f(vertexDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                        cylinder.render(gl3, "lit-color");
                    }
                    vertexDiffuseColor.unbind(gl3);

                } else {

                    whiteDiffuseColor.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                        Mat3 normalMatrix = new Mat3(modelMatrix.top());
                        gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                        gl3.glUniform4f(whiteDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                        cylinder.render(gl3, "lit");
                    }
                    whiteDiffuseColor.unbind(gl3);
                }
            }
            modelMatrix.pop();
        }
        modelMatrix.pop();

        glad.swapBuffers();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        MatrixStack perspectiveMatrix = new MatrixStack();

        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) w / (float) h, zNear, zFar));

        perspectiveMatrix.top().print("perspectiveMatrix.top()");

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    private void initializeObjects(GL3 gl3) {
        System.out.println("initializeObjects");

        cylinder = new Mesh(dataFilepath + "UnitCylinder.xml", gl3);
        plane = new Mesh(dataFilepath + "LargePlane.xml", gl3);
    }

    private void initializePrograms(GL3 gl3) {

        System.out.println("initializePrograms...");

        whiteDiffuseColor = new GLSLProgramObject_1(gl3, shadersFilepath, "DirVertexLighting_PN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
        vertexDiffuseColor = new GLSLProgramObject_1(gl3, shadersFilepath, "DirVertexLighting_PCN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
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

        canvas.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        viewPole.mouseReleased(e);
        objectPole.mouseReleased(e);

        canvas.display();
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

        switch (e.getKeyCode()) {

            case KeyEvent.VK_SPACE:
                drawColoredCyl = !drawColoredCyl;
                break;
        }

        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        viewPole.mouseMove(e);
        objectPole.mouseMove(e);

        canvas.display();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        
        viewPole.mouseWheel(e);
        
        canvas.display();
    }
}