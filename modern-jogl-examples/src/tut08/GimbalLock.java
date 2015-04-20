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
import jglm.Mat4;
import jglm.Vec3;
import jglm.Vec4;
import mesh.Mesh;
import glutil.MatrixStack;
import tut08.glsl.GLSLProgramObject_1;

/**
 *
 * @author gbarbieri
 */
public class GimbalLock implements GLEventListener, KeyListener {

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
    private Mesh[] gimbals;
    private Mesh ship;
    private float frustumScale = MatrixStack.calculateFrustumScale(20.0f);
    private GimbalAngles gimbalAngles;
    private boolean drawGimbals;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GimbalLock gimbalLock = new GimbalLock();

        Frame frame = new Frame("Tutorial 08 - Gimbal Lock");

        frame.add(gimbalLock.getCanvas());

        frame.setSize(gimbalLock.getCanvas().getWidth(), gimbalLock.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public GimbalLock() {
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

        initializePrograms(gl3);

        initializeObjects(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);

        gimbalAngles = new GimbalAngles();
        drawGimbals = true;
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
        matrixStack.rotateX(gimbalAngles.angleX);

        drawGimbal(gl3, matrixStack, GimbalAxis.GIMBAL_X_AXIS, new Vec4(0.4f, 0.4f, 1.0f, 1.0f));

        matrixStack.rotateY(gimbalAngles.angleY);

        drawGimbal(gl3, matrixStack, GimbalAxis.GIMBAL_Y_AXIS, new Vec4(0.0f, 1.0f, 0.0f, 1.0f));

        matrixStack.rotateZ(gimbalAngles.angleZ);

        drawGimbal(gl3, matrixStack, GimbalAxis.GIMBAL_Z_AXIS, new Vec4(1.0f, 0.3f, 0.3f, 1.0f));

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

    private void drawGimbal(GL3 gl3, MatrixStack matrixStack, GimbalAxis gimbalAxis, Vec4 baseColor) {

        if (!drawGimbals) {
            return;
        }

        int index = -1;

        matrixStack.push();
        {
            switch (gimbalAxis) {

                case GIMBAL_X_AXIS:
                    index = 0;
                    break;

                case GIMBAL_Y_AXIS:
                    index = 1;
                    matrixStack.rotateZ(90.0f);
                    matrixStack.rotateX(90.0f);
                    break;

                case GIMBAL_Z_AXIS:
                    index = 2;
                    matrixStack.rotateY(90.0f);
                    matrixStack.rotateX(90.0f);
                    break;
            }

            programObject.bind(gl3);
            {
                gl3.glUniform4fv(programObject.getBaseColorUnLoc(), 1, baseColor.toFloatArray(), 0);
                gl3.glUniformMatrix4fv(programObject.getModelToCameraMatUnLoc(), 1, false, matrixStack.top().toFloatArray(), 0);

                gimbals[index].render(gl3);
            }
            programObject.unbind(gl3);
        }
        matrixStack.pop();
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

        gimbals = new Mesh[3];

        String[] gimbalNames = new String[]{"LargeGimbal.xml", "MediumGimbal.xml", "SmallGimbal.xml"};

        for (int i = 0; i < gimbals.length; i++) {

            gimbals[i] = new Mesh(dataFilepath + gimbalNames[i], gl3);
        }

        ship = new Mesh(dataFilepath + "Ship.xml", gl3);
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
                gimbalAngles.angleX += smallAngleIncrement;
                break;

            case KeyEvent.VK_S:
                gimbalAngles.angleX -= smallAngleIncrement;
                break;

            case KeyEvent.VK_A:
                gimbalAngles.angleY += smallAngleIncrement;
                break;

            case KeyEvent.VK_D:
                gimbalAngles.angleY -= smallAngleIncrement;
                break;

            case KeyEvent.VK_Q:
                gimbalAngles.angleZ += smallAngleIncrement;
                break;

            case KeyEvent.VK_E:
                gimbalAngles.angleZ -= smallAngleIncrement;
                break;

            case KeyEvent.VK_SPACE:
                drawGimbals = !drawGimbals;
                break;
        }
        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    enum GimbalAxis {

        GIMBAL_X_AXIS,
        GIMBAL_Y_AXIS,
        GIMBAL_Z_AXIS
    }
}