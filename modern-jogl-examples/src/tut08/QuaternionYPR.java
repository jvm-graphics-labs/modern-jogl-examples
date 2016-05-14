/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import framework.jglm.Mat4;
import framework.jglm.Quat;
import framework.jglm.Vec3;
import framework.component.Mesh;
import framework.glutil.MatrixStack;
import tut08.glsl.GLSLProgramObject_1;

/**
 *
 * @author gbarbieri
 */
public class QuaternionYPR implements GLEventListener, KeyListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject_1 programObject;
    private int[] VBO = new int[1];
    private int[] IBO = new int[1];
    private int[] VAO = new int[1];
    private String shadersFilepath = "/tut08/shaders/";
    private String dataFilepath = "/tut08/data/";
    private Mat4 cameraToClipMatrix;
    private Mesh cone;
    private Mesh cylinder;
    private Mesh cubeTint;
    private Mesh cubeColor;
    private Mesh plane;
    private float zNear;
    private float zFar;
    private boolean drawLookAtPoint;
    private Mesh ship;
    private float frustumScale;
    private Quat orientation;
    private boolean rightMultiply;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        QuaternionYPR quaternionYPR = new QuaternionYPR();

        Frame frame = new Frame("Tutorial 08 - Quaternion YPR");

        frame.add(quaternionYPR.getCanvas());

        frame.setSize(quaternionYPR.getCanvas().getWidth(), quaternionYPR.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public QuaternionYPR() {
        initGL();
    }

    private void initGL() {
        GLProfile profile = GLProfile.getDefault();

        GLCapabilities capabilities = new GLCapabilities(profile);

        canvas = new GLCanvas(capabilities);

        canvas.setSize(imageWidth, imageHeight);

        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        frustumScale = MatrixStack.calculateFrustumScale(20.0f);

        initializePrograms(gl3);

        initializeObjects(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);

        rightMultiply = true;
        orientation = new Quat(0.0f, 0.0f, 0.0f, 1.0f);
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

        MatrixStack matrixStack = new MatrixStack();

        matrixStack.translate(new Vec3(0.0f, 0.0f, -200.0f));
        matrixStack.applyMat(orientation.toMatrix());

//        orientation.toMatrix().print("Orientation.toMartix():");
//
//        matrixStack.top().print("matrixStack:");

        programObject.bind(gl3);
        {
            matrixStack.scale(new Vec3(3.0f, 3.0f, 3.0f));
            matrixStack.rotateX(-90.0f);

            gl3.glUniform4f(programObject.getBaseColorUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);
            gl3.glUniformMatrix4fv(programObject.getModelToCameraMatUnLoc(), 1, false, matrixStack.top().toFloatArray(), 0);

            ship.render(gl3, "tint");
        }
        programObject.unbind(gl3);

        glad.swapBuffers();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        cameraToClipMatrix.c0.x = frustumScale * (h / (float) w);
        cameraToClipMatrix.c1.y = frustumScale;

        programObject.bind(gl3);
        {
            gl3.glUniformMatrix4fv(programObject.getCameraToClipMatUnLoc(), 1, false, cameraToClipMatrix.toFloatArray(), 0);
        }
        programObject.unbind(gl3);

        gl3.glViewport(x, y, w, h);
    }

    private void initializeObjects(GL3 gl3) {
        System.out.println("initializeObjects");

//        ship = new Mesh(dataFilepath + "Ship.xml", gl3);
    }

    private void initializePrograms(GL3 gl3) {

        System.out.println("initializePrograms...");

        programObject = new GLSLProgramObject_1(gl3, shadersFilepath, "PosColorLocalTransform_VS.glsl", "ColorMultUniform_FS.glsl");

        zNear = 1.0f;
        zFar = 600.0f;

        cameraToClipMatrix = new Mat4();

        cameraToClipMatrix.c0.x = frustumScale;
        cameraToClipMatrix.c1.y = frustumScale;
        cameraToClipMatrix.c2.z = (zFar + zNear) / (zNear - zFar);
        cameraToClipMatrix.c2.w = -1.0f;
        cameraToClipMatrix.c3.z = (2 * zFar * zNear) / (zNear - zFar);

        programObject.bind(gl3);
        {
            gl3.glUniformMatrix4fv(programObject.getCameraToClipMatUnLoc(), 1, false, cameraToClipMatrix.toFloatArray(), 0);
        }
        programObject.unbind(gl3);
    }

    private void offsetOrientation(Vec3 axis, float angDeg) {

        float angRad = (float) Math.toRadians(angDeg);
//        System.out.println("angRad: " + angRad);
        axis = axis.normalize();
//        axis.print("axis:");
        axis = axis.times((float) Math.sin(angRad / 2.0f));

        float scalar = (float) Math.cos(angRad / 2.0f);

        Quat offset = new Quat(axis, scalar);

        if (rightMultiply) {
            orientation = orientation.mult(offset);
//            orientation.print("orientation:");
        } else {
            orientation = offset.mult(orientation);
        }
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }

    class GimbalAngles {

        public float angleX;
        public float angleY;
        public float angleZ;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), smallAngleIncrement);
                break;

            case KeyEvent.VK_S:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_A:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), smallAngleIncrement);
                break;

            case KeyEvent.VK_D:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_Q:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), smallAngleIncrement);
                break;

            case KeyEvent.VK_E:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_SPACE:
                rightMultiply = !rightMultiply;
                System.out.println("rightMultiply: " + rightMultiply);
                break;
        }
        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}