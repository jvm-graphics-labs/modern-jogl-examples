/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut03;

import glsl.GLSLProgramObject;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

/**
 *
 * @author gbarbieri
 */
public class vertPositionOffset implements GLEventListener {

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
    private float fXoffset = 0.0f;
    private float fYoffset = 0.0f;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        vertPositionOffset positionOffset = new vertPositionOffset();

        Frame frame = new Frame("Tutorial 03 - Shader Position Offset");

        frame.add(positionOffset.getCanvas());

        frame.setSize(positionOffset.getCanvas().getWidth(), positionOffset.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public vertPositionOffset() {
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
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        ComputePositionOffsets();

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);

        programObject.bind(gl3);
        {
            programObject.setUniform(gl3, "offset", new float[]{fXoffset, fYoffset}, 2);
            
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

    private void ComputePositionOffsets() {
        float fLoopDuration = 5.0f;
        float fScale = 3.14159f * 2.0f / fLoopDuration;

        float fElapsedTime = (System.currentTimeMillis() - startingTime) / 1000.0f;

        float fCurrTimeThroughLoop = fElapsedTime % fLoopDuration;

        fXoffset = (float) (Math.cos(fCurrTimeThroughLoop * fScale) * 0.5f);
        fYoffset = (float) (Math.sin(fCurrTimeThroughLoop * fScale) * 0.5f);
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
        programObject.attachVertexShader(gl3, shadersFilepath + "positionOffset_VS.glsl");
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
