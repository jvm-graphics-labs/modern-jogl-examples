/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08;

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
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec3;
import mesh.Mesh;
import glutil.MatrixStack;
import tut08.glsl.GLSLProgramObject_1;

/**
 *
 * @author gbarbieri
 */
public class Interpolation implements GLEventListener, KeyListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject_1 programObject;
    private String shadersFilepath = "/tut08/shaders/";
    private String dataFilepath = "/tut08/data/";
    private Mat4 cameraToClipMatrix;
    private float zNear;
    private float zFar;
    private Mesh ship;
    private float frustumScale;
    private Quat orientation;
    private Vec3 sphereCamRelPos;
    private Vec3 camTarget;
    private final int MODEL_RELATIVE = 0;
    private final int WORLD_RELATIVE = 1;
    private final int CAMERA_RELATIVE = 2;
    private int offsetType;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Interpolation interpolation = new Interpolation();

        Frame frame = new Frame("Tutorial 08 - Interpolation");

        frame.add(interpolation.getCanvas());

        frame.setSize(interpolation.getCanvas().getWidth(), interpolation.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public Interpolation() {
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

        frustumScale = MatrixStack.calculatFrustumScale(20.0f);

        initializePrograms(gl3);

        initializeObjects(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);

        orientation = new Quat(0.0f, 0.0f, 0.0f, 1.0f);

        camTarget = new Vec3(0.0f, 10.0f, 0.0f);
        sphereCamRelPos = new Vec3(90.0f, 0.0f, 66.0f);

        offsetType = MODEL_RELATIVE;
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

        programObject.bind(gl3);
        {
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

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        float smallAngleIncrement = 9.0f;
        float angleIncrement = 11.25f;

        switch (e.getKeyCode()) {
            
        }

        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}