/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut05;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import glsl.GLSLProgramObject;
import com.jogamp.opengl.util.GLBuffers;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class DepthClamping implements GLEventListener, KeyListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject programObject;
    private int[] VBO = new int[1];
    private int[] IBO = new int[1];
    private int[] VAO = new int[1];
    private float RIGHT_EXTENT = 0.8f;
    private float LEFT_EXTENT = -RIGHT_EXTENT;
    private float TOP_EXTENT = 0.20f;
    private float MIDDLE_EXTENT = 0.0f;
    private float BOTTOM_EXTENT = -TOP_EXTENT;
    private float FRONT_EXTENT = -1.25f;
    private float REAR_EXTENT = -1.75f;
    private float[] GREEN_COLOR = new float[]{0.75f, 0.75f, 1.0f, 1.0f};
    private float[] BLUE_COLOR = new float[]{0.0f, 0.5f, 0.0f, 1.0f};
    private float[] RED_COLOR = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
    private float[] GREY_COLOR = new float[]{0.8f, 0.8f, 0.8f, 1.0f};
    private float[] BROWN_COLOR = new float[]{0.5f, 0.5f, 0.0f, 1.0f};
    private float[] vertexData = new float[]{
        //Object 1 positions
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        //Object 2 positions
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        //					
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //					
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        //Object 1 colors
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        //	
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        //
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        //
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        //
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        //Object 2 colors
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        //
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        //
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3]};
    private int[] indexData = new int[]{
        0, 2, 1,
        3, 2, 0,
        //
        4, 5, 6,
        6, 7, 4,
        //
        8, 9, 10,
        11, 13, 12,
        //
        14, 16, 15,
        17, 16, 14
    };
    private String shadersFilepath = "/tut05/shaders/";
    private float[] perspectiveMatrix = new float[16];
    private float fFrustumScale = 1.0f;
    private boolean depthClamping = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DepthClamping depthClamping = new DepthClamping();

        Frame frame = new Frame("Tutorial 05 - Depth Clamping");

        frame.add(depthClamping.getCanvas());

        frame.setSize(depthClamping.getCanvas().getWidth(), depthClamping.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public DepthClamping() {
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

        initializeProgram(gl3);

        initializeVertexBuffer(gl3);

        initializeVertexArrayObjects(gl3);

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
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
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        if (depthClamping) {
            gl3.glEnable(GL3.GL_DEPTH_CLAMP);
        } else {
            gl3.glDisable(GL3.GL_DEPTH_CLAMP);
        }

        programObject.bind(gl3);
        {
            gl3.glBindVertexArray(VAO[0]);
            {
                programObject.setUniform(gl3, "offset", new float[]{0.0f, 0.0f, 0.5f}, 3);

                gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);


                programObject.setUniform(gl3, "offset", new float[]{0.0f, 0.0f, -1.0f}, 3);

                gl3.glDrawElementsBaseVertex(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, IBO[0], 18);
            }
            gl3.glBindVertexArray(0);
        }
        programObject.unbind(gl3);

        glad.swapBuffers();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        perspectiveMatrix[0] = fFrustumScale / (w / (float) h);
        perspectiveMatrix[5] = fFrustumScale;

        programObject.bind(gl3);
        {
            int perspectiveMatrixLocation = gl3.glGetUniformLocation(programObject.getProgramId(), "perspectiveMatrix");
            gl3.glUniformMatrix4fv(perspectiveMatrixLocation, 1, false, perspectiveMatrix, 0);
        }
        programObject.unbind(gl3);

        gl3.glViewport(x, y, w, h);
    }

    private void buildShaders(GL3 gl3) {
        System.out.print("Building shaders...");

        programObject = new GLSLProgramObject(gl3);
        programObject.attachVertexShader(gl3, shadersFilepath + "MatrixPerspective_VS.glsl");
        programObject.attachFragmentShader(gl3, shadersFilepath + "StandardColor_FS.glsl");
        programObject.initializeProgram(gl3, true);

        System.out.println("ok");
    }

    private void initializeVertexBuffer(GL3 gl3) {
        gl3.glGenBuffers(1, IntBuffer.wrap(VBO));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO[0]);
        {
            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * 4, GLBuffers.newDirectFloatBuffer(vertexData), GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);


        gl3.glGenBuffers(1, IntBuffer.wrap(IBO));

        gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, IBO[0]);
        {
            gl3.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER, indexData.length * 4, GLBuffers.newDirectIntBuffer(indexData), GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void initializeProgram(GL3 gl3) {
        buildShaders(gl3);

        programObject.bind(gl3);
        {
            float fzNear = 1.0f;
            float fzFar = 3.0f;

            perspectiveMatrix[0] = fFrustumScale;
            perspectiveMatrix[5] = fFrustumScale;
            perspectiveMatrix[10] = (fzFar + fzNear) / (fzNear - fzFar);
            perspectiveMatrix[14] = (2 * fzFar * fzNear) / (fzNear - fzFar);
            perspectiveMatrix[11] = -1.0f;

            int matrixLocation = gl3.glGetUniformLocation(programObject.getProgramId(), "perspectiveMatrix");
            gl3.glUniformMatrix4fv(matrixLocation, 1, false, perspectiveMatrix, 0);
        }
        programObject.unbind(gl3);
    }

    private void initializeVertexArrayObjects(GL3 gl3) {
        int colorDataOffset;

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO[0]);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(VAO));
        gl3.glBindVertexArray(VAO[0]);
        {
//        2 objects * 18 vertices * 3 coordinate * 4 Bytes/Float
            colorDataOffset = 2 * 18 * 3 * 4;

            gl3.glEnableVertexAttribArray(0);
            gl3.glEnableVertexAttribArray(1);
            {
                gl3.glVertexAttribPointer(0, 3, GL3.GL_FLOAT, false, 0, 0);
                gl3.glVertexAttribPointer(1, 4, GL3.GL_FLOAT, false, 0, colorDataOffset);
            }
            gl3.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, IBO[0]);
        }
        gl3.glBindVertexArray(0);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void keyTyped(KeyEvent e) {
//        System.out.println("keyTyped: "+e.getKeyCode());        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keyPressed");

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            depthClamping = !depthClamping;
            canvas.display();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("keyReleased");
    }
}