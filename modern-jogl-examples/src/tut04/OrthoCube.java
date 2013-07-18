/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut04;

import glsl.GLSLProgramObject;
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
public class OrthoCube implements GLEventListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject programObject;
    private int[] vertexBufferObject = new int[1];
    private int[] vertexArrayObject = new int[1];
    private float[] vertexData = new float[]{
        0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, -0.25f, 0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, -0.25f, 0.75f, 1.0f,
        -0.25f, -0.25f, 0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, 0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, -0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        -0.25f, -0.25f, 0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, -0.75f, 1.0f,
        0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        0.25f, -0.25f, 0.75f, 1.0f,
        0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, 0.25f, -0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        0.25f, 0.25f, -0.75f, 1.0f,
        0.25f, 0.25f, 0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        0.25f, 0.25f, -0.75f, 1.0f,
        -0.25f, 0.25f, 0.75f, 1.0f,
        -0.25f, 0.25f, -0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, 0.75f, 1.0f,
        0.25f, -0.25f, 0.75f, 1.0f,
        0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, 0.75f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f};
    private String shadersFilepath = "/tut04/shaders/";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        OrthoCube orthoCube = new OrthoCube();

        Frame frame = new Frame("Tutorial 04 - Orthographic Cube");

        frame.add(orthoCube.getCanvas());

        frame.setSize(orthoCube.getCanvas().getWidth(), orthoCube.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public OrthoCube() {
        initGL();
    }

    private void initGL() {
        GLProfile profile = GLProfile.getDefault();

        GLCapabilities capabilities = new GLCapabilities(profile);

        canvas = new GLCanvas(capabilities);

        canvas.setSize(imageWidth, imageHeight);

        canvas.addGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        buildShaders(gl3);

        initializeVertexBuffer(gl3);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(vertexArrayObject));
        gl3.glBindVertexArray(vertexArrayObject[0]);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);

        programObject.bind(gl3);
        {
            programObject.setUniform(gl3, "offset", new float[]{0.5f, 0.25f}, 2);

            gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);

            gl3.glEnableVertexAttribArray(0);
            gl3.glEnableVertexAttribArray(1);
            {
                gl3.glVertexAttribPointer(0, 4, GL3.GL_FLOAT, false, 0, 0);
                gl3.glVertexAttribPointer(1, 4, GL3.GL_FLOAT, false, 0, 36 * 4 * 4);

                gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, 36);
            }
            gl3.glDisableVertexAttribArray(0);
            gl3.glDisableVertexAttribArray(1);
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
        programObject.attachVertexShader(gl3, shadersFilepath + "OrthoWithOffset_VS.glsl");
        programObject.attachFragmentShader(gl3, shadersFilepath + "StandardColor_FS.glsl");
        programObject.initializeProgram(gl3, true);

        System.out.println("ok");
    }

    private void initializeVertexBuffer(GL3 gl3) {
        gl3.glGenBuffers(1, IntBuffer.wrap(vertexBufferObject));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexBufferObject[0]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexData);

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * 4, buffer, GL3.GL_STATIC_DRAW);
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
