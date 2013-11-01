/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09;

import com.jogamp.opengl.util.GLBuffers;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import jglm.Quat;
import jglm.Vec3;
import mesh.Mesh;
import glutil.MatrixStack;
import glutil.ObjectData;
import glutil.ObjectPole;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import jglm.Jglm;
import jglm.Mat3;
import jglm.Vec4;
import tut09.glsl.GLSLProgramObject_1;
import tut09.glsl.GLSLProgramObject_2;

/**
 *
 * @author gbarbieri
 */
public class AmbientLighting implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject_1 whiteDiffuseColor;
    private GLSLProgramObject_1 vertexDiffuseColor;
    private GLSLProgramObject_2 whiteAmbientDiffuseColor;
    private GLSLProgramObject_2 vertexAmbientDiffuseColor;
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
    private boolean drawColoredCylinder;
    private boolean ambientLighting;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        AmbientLighting ambientLighting = new AmbientLighting();

        Frame frame = new Frame("Tutorial 09 - Ambient Lighting");

        frame.add(ambientLighting.getCanvas());

        frame.setSize(ambientLighting.getCanvas().getWidth(), ambientLighting.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public AmbientLighting() {
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

        drawColoredCylinder = true;
        ambientLighting = false;
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

        setLights(gl3, lightDirCameraSpace);

        modelMatrix.push();
        {
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
        }
        modelMatrix.pop();

        glad.swapBuffers();
    }

    private void setLights(GL3 gl3, Vec4 lightDirCameraSpace) {

        vertexAmbientDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(vertexAmbientDiffuseColor.getLightIntensityUnLoc(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(vertexAmbientDiffuseColor.getUnLocAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(vertexAmbientDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexAmbientDiffuseColor.unbind(gl3);

        whiteAmbientDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(whiteAmbientDiffuseColor.getLightIntensityUnLoc(), 0.8f, 0.8f, 0.8f, 1.0f);

            gl3.glUniform4f(whiteAmbientDiffuseColor.getUnLocAmbientIntensity(), 0.2f, 0.2f, 0.2f, 1.0f);

            gl3.glUniform3fv(whiteAmbientDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        whiteAmbientDiffuseColor.unbind(gl3);

        vertexDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(vertexDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

            gl3.glUniform3fv(vertexDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexDiffuseColor.unbind(gl3);

        whiteDiffuseColor.bind(gl3);
        {
            gl3.glUniform4f(whiteDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

            gl3.glUniform3fv(whiteDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        whiteDiffuseColor.unbind(gl3);
    }

    private void renderGroundPlane(GL3 gl3, MatrixStack modelMatrix) {

        Mat3 normalMatrix = new Mat3(modelMatrix.top());

        if (ambientLighting) {

            whiteAmbientDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(whiteAmbientDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniformMatrix3fv(whiteAmbientDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                plane.render(gl3);
            }
            whiteAmbientDiffuseColor.unbind(gl3);

        } else {

            whiteDiffuseColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                plane.render(gl3);
            }
            whiteDiffuseColor.unbind(gl3);
        }
    }

    private void renderCylinder(GL3 gl3, MatrixStack modelMatrix) {

        modelMatrix.applyMat(objectPole.calcMatrix());

        Mat3 normalMatrix = new Mat3(modelMatrix.top());

        if (drawColoredCylinder) {

            if (ambientLighting) {

                vertexAmbientDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(vertexAmbientDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniformMatrix3fv(vertexAmbientDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    cylinder.render(gl3, "lit-color");
                }
                vertexAmbientDiffuseColor.unbind(gl3);

            } else {

                vertexDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(vertexDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniformMatrix3fv(vertexDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    cylinder.render(gl3, "lit-color");
                }
                vertexDiffuseColor.unbind(gl3);
            }

        } else {

            if (ambientLighting) {

                whiteAmbientDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(whiteAmbientDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniformMatrix3fv(whiteAmbientDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    cylinder.render(gl3, "lit");
                }
                whiteAmbientDiffuseColor.unbind(gl3);

            } else {

                whiteDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    cylinder.render(gl3, "lit");
                }
                whiteDiffuseColor.unbind(gl3);
            }
        }
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
        whiteAmbientDiffuseColor = new GLSLProgramObject_2(gl3, shadersFilepath, "DirAmbientVertexLighting_PN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
        vertexAmbientDiffuseColor = new GLSLProgramObject_2(gl3, shadersFilepath, "DirAmbientVertexLighting_PCN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
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
                drawColoredCylinder = !drawColoredCylinder;
                break;

            case KeyEvent.VK_T:
                ambientLighting = !ambientLighting;
                break;
        }

        System.out.println("ambientLighting: " + ambientLighting);

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