/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut06;

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
import java.nio.IntBuffer;
import java.util.ArrayList;
import framework.jglm.Jglm;
import framework.jglm.Mat4;
import framework.jglm.Vec3;
import framework.jglm.Vec4;

/**
 *
 * @author gbarbieri
 */
public class Scale implements GLEventListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject programObject;
    private int[] VBO = new int[1];
    private int[] IBO = new int[1];
    private int[] VAO = new int[1];
    private float[] GREEN_COLOR = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
    private float[] BLUE_COLOR = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    private float[] RED_COLOR = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
    private float[] BROWN_COLOR = new float[]{0.5f, 0.5f, 0.0f, 1.0f};
    private float[] vertexData = new float[]{
        +1.0f, +1.0f, +1.0f,
        -1.0f, -1.0f, +1.0f,
        -1.0f, +1.0f, -1.0f,
        +1.0f, -1.0f, -1.0f,
        //
        -1.0f, -1.0f, -1.0f,
        +1.0f, +1.0f, -1.0f,
        +1.0f, -1.0f, +1.0f,
        -1.0f, +1.0f, +1.0f,
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3]};
    private int[] indexData = new int[]{
        0, 1, 2,
        1, 0, 3,
        2, 3, 0,
        3, 2, 1,
        //
        5, 4, 6,
        4, 5, 7,
        7, 6, 4,
        6, 7, 5
    };
    private String shadersFilepath = "/tut06/shaders/";
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private float fFrustumScale = calculatFrustumScale(45.0f);
    private int numberOfVertices = 8;
    private long startingTime;
    private ArrayList<Instance> instances;
    private int modelToCameraMatrixLocation;
    private FPSAnimator animator;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scale scale = new Scale();

        Frame frame = new Frame("Tutorial 06 - Scale");

        frame.add(scale.getCanvas());

        frame.setSize(scale.getCanvas().getWidth(), scale.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public Scale() {
        startingTime = System.currentTimeMillis();
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

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        initializeProgram(gl3);

        initializeVertexBuffer(gl3);

        initializeVertexArrayObject(gl3);

        initializeInstances();

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

        programObject.bind(gl3);
        {
            gl3.glBindVertexArray(VAO[0]);
            {
                float fElapsedTime = (System.currentTimeMillis() - startingTime) / 1000.0f;

                for (Instance instance : instances) {
                    Mat4 transformMatrix = instance.constructMatrix(fElapsedTime);

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, transformMatrix.toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
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

        cameraToClipMatrix.c0.x = fFrustumScale / (w / (float) h);
        cameraToClipMatrix.c1.y = fFrustumScale;

        programObject.bind(gl3);
        {
            int cameraToClipMatrixLocation = gl3.glGetUniformLocation(programObject.getProgramId(), "cameraToClipMatrix");
            gl3.glUniformMatrix4fv(cameraToClipMatrixLocation, 1, false, cameraToClipMatrix.toFloatArray(), 0);
        }
        programObject.unbind(gl3);

        gl3.glViewport(x, y, w, h);
    }

    private void buildShaders(GL3 gl3) {
        System.out.print("Building shaders...");

        programObject = new GLSLProgramObject(gl3);
        programObject.attachVertexShader(gl3, shadersFilepath + "PosColorLocalTransform_VS.glsl");
        programObject.attachFragmentShader(gl3, shadersFilepath + "ColorPassthrough_FS.glsl");
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
            float fzFar = 61.0f;

            cameraToClipMatrix.c0.x = fFrustumScale;
            cameraToClipMatrix.c1.y = fFrustumScale;
            cameraToClipMatrix.c2.z = (fzFar + fzNear) / (fzNear - fzFar);
            cameraToClipMatrix.c2.w = -1.0f;
            cameraToClipMatrix.c3.z = (2 * fzFar * fzNear) / (fzNear - fzFar);


            int cameraToClipMatrixLocation = gl3.glGetUniformLocation(programObject.getProgramId(), "cameraToClipMatrix");
            gl3.glUniformMatrix4fv(cameraToClipMatrixLocation, 1, false, cameraToClipMatrix.toFloatArray(), 0);

            modelToCameraMatrixLocation = gl3.glGetUniformLocation(programObject.getProgramId(), "modelToCameraMatrix");
        }
        programObject.unbind(gl3);
    }

    private void initializeVertexArrayObject(GL3 gl3) {
        int colorDataOffset;

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO[0]);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(VAO));
        gl3.glBindVertexArray(VAO[0]);
        {
//        vertices * 3 coordinate * 4 Bytes/Float
            colorDataOffset = numberOfVertices * 3 * 4;

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

    private float calculatFrustumScale(float fFovDeg) {
        float degToRad = 3.14159f * 2.0f / 360.0f;
        float fFovRad = fFovDeg * degToRad;
        return (float) (1.0f / Math.tan(fFovRad / 2.0f));
    }

    public void initializeInstances() {
        instances = new ArrayList<>();

        instances.add(new NullScale(new Vec3(0.0f, 0.0f, -45.0f)));
        instances.add(new StaticUniformScale(new Vec3(-10.0f, -10.0f, -45.0f)));
        instances.add(new StaticNonUniformScale(new Vec3(-10.f, 10.0f, -45.0f)));
        instances.add(new DynamicUniformScale(new Vec3(10.0f, 10.0f, -45.0f)));
        instances.add(new DynamicNonUniformScale(new Vec3(10.0f, -10.0f, -45.0f)));
    }

    class Instance implements ScaleCalculation {

        private Vec3 offset;

        public Instance(Vec3 offset) {
            this.offset = offset;
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            return null;
        }

        public Mat4 constructMatrix(float fElapsedTime) {
            Mat4 matrix = new Mat4();

            Vec3 scale = calculateScale(fElapsedTime);
            
            matrix.setDiagonal(scale);

            matrix.c3 = new Vec4(offset, 1.0f);

            return matrix;
        }

        public float CalculateLerpFactor(float fElapsedTime, float fLoopDuration) {
            float fValue = fElapsedTime % fLoopDuration / fLoopDuration;
            if (fValue > 0.5f) {
                fValue = 1.0f - fValue;
            }
            return fValue * 2.0f;
        }
    }

    public interface ScaleCalculation {

        public Vec3 calculateScale(float fElapsedTime);
    }

    class NullScale extends Instance {

        public NullScale(Vec3 offset) {
            super(offset);
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            return new Vec3(1.0f, 1.0f, 1.0f);
        }
    }

    class StaticUniformScale extends Instance {

        public StaticUniformScale(Vec3 offset) {
            super(offset);
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            return new Vec3(4.0f, 4.0f, 4.0f);
        }
    }

    class StaticNonUniformScale extends Instance {

        public StaticNonUniformScale(Vec3 offset) {
            super(offset);
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            return new Vec3(0.5f, 1.0f, 10.0f);
        }
    }

    class DynamicUniformScale extends Instance {

        private float fLoopDuration = 3.0f;

        public DynamicUniformScale(Vec3 offset) {
            super(offset);
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            float lerpValue = CalculateLerpFactor(fElapsedTime, fLoopDuration);
            float value = Jglm.mix(1.0f, 4.0f, lerpValue);
            return new Vec3(value, value, value);
        }
    }

    class DynamicNonUniformScale extends Instance {

        private float fXLoopDuration = 3.0f;
        private float fYLoopDuration = 5.0f;

        public DynamicNonUniformScale(Vec3 offset) {
            super(offset);
        }

        @Override
        public Vec3 calculateScale(float fElapsedTime) {
            float xLerpValue = CalculateLerpFactor(fElapsedTime, fXLoopDuration);
            float yLerpValue = CalculateLerpFactor(fElapsedTime, fYLoopDuration);
            float x = Jglm.mix(1.0f, 0.5f, xLerpValue);
            float y = 1.0f;
            float z = Jglm.mix(1.0f, 10.0f, yLerpValue);
            return new Vec3(x, y, z);
        }
    }
}