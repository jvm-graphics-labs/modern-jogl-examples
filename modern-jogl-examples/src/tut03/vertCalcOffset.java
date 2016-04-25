/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut03;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import framework.GLSLProgramObject;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class vertCalcOffset implements GLEventListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject programObject;
    private int[] positionBufferObject = new int[1];
    private int[] vertexArrayObject = new int[1];
    private float[] vertexPositions = new float[]{
        0.25f, 0.25f, 0.0f, 1.0f,
        0.25f, -0.25f, 0.0f, 1.0f,
        -0.25f, -0.25f, 0.0f, 1.0f};
    private String shadersFilepath = "/tut03/shaders/";
    private long startingTime;
    private FPSAnimator animator;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        vertCalcOffset calcOffset = new vertCalcOffset();

        Frame frame = new Frame("Tutorial 03 - Shader Calc Offset");

        frame.add(calcOffset.getCanvas());

        frame.setSize(calcOffset.getCanvas().getWidth(), calcOffset.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public vertCalcOffset() {
        initGL();
    }

    private void initGL() {
        GLProfile profile = GLProfile.getDefault();

        GLCapabilities capabilities = new GLCapabilities(profile);

        canvas = new GLCanvas(capabilities);

        canvas.setSize(imageWidth, imageHeight);

        canvas.addGLEventListener(this);

        animator = new FPSAnimator(canvas, 60);
        animator.start();
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        startingTime = System.currentTimeMillis();

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        buildShaders(gl3);

        initializeVertexBuffer(gl3);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(vertexArrayObject));
        gl3.glBindVertexArray(vertexArrayObject[0]);

        programObject.bind(gl3);
        {
            programObject.setUniform(gl3, "loopDuration", new float[]{5.0f}, 1);
        }
        programObject.unbind(gl3);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);

        float elapsedTime = (System.currentTimeMillis() - startingTime) / 1000.0f;

        programObject.bind(gl3);
        {
            programObject.setUniform(gl3, "time", new float[]{elapsedTime}, 1);

            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, positionBufferObject[0]);

            gl3.glEnableVertexAttribArray(0);
            {
                gl3.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0);

                gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, 3);
            }
            gl3.glDisableVertexAttribArray(0);
        }
        programObject.unbind(gl3);

        glad.swapBuffers();
    }
    
    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glViewport(x, y, w, h);
    }

    private void buildShaders(GL3 gl3) {
        System.out.print("Building shaders...");

        programObject = new GLSLProgramObject(gl3);
        programObject.attachVertexShader(gl3, shadersFilepath + "calcOffset_VS.glsl");
        programObject.attachFragmentShader(gl3, shadersFilepath + "standard_FS.glsl");
        programObject.initializeProgram(gl3, true);

        System.out.println("ok");
    }

    private void initializeVertexBuffer(GL3 gl3) {
        gl3.glGenBuffers(1, IntBuffer.wrap(positionBufferObject));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, positionBufferObject[0]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexPositions);

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexPositions.length * 4, buffer, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }
}
