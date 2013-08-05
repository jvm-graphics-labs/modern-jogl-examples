/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut06;

import glsl.GLSLProgramObject;
import com.jogamp.opengl.util.GLBuffers;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.IntBuffer;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec3;
import glutil.MatrixStack;

/**
 *
 * @author gbarbieri
 */
public class Hierarchy implements GLEventListener, KeyListener {

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
    //
    private float[] YELLOW_COLOR = new float[]{1.0f, 1.0f, 0.0f, 1.0f};
    private float[] CYAN_COLOR = new float[]{0.0f, 1.0f, 1.0f, 1.0f};
    private float[] MAGENTA_COLOR = new float[]{1.0f, 0.0f, 1.0f, 1.0f};
    private float[] vertexData = new float[]{
        //Front
        +1.0f, +1.0f, +1.0f,
        +1.0f, -1.0f, +1.0f,
        -1.0f, -1.0f, +1.0f,
        -1.0f, +1.0f, +1.0f,
        //Top
        +1.0f, +1.0f, +1.0f,
        -1.0f, +1.0f, +1.0f,
        -1.0f, +1.0f, -1.0f,
        +1.0f, +1.0f, -1.0f,
        //Left
        +1.0f, +1.0f, +1.0f,
        +1.0f, +1.0f, -1.0f,
        +1.0f, -1.0f, -1.0f,
        +1.0f, -1.0f, +1.0f,
        //Back
        +1.0f, +1.0f, -1.0f,
        -1.0f, +1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        +1.0f, -1.0f, -1.0f,
        //Bottom
        +1.0f, -1.0f, +1.0f,
        +1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, +1.0f,
        //Right
        -1.0f, +1.0f, +1.0f,
        -1.0f, -1.0f, +1.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, +1.0f, -1.0f,
        //
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
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        //
        YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
        YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
        YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
        YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
        //
        CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
        CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
        CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
        CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
        //
        MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
        MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
        MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
        MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3]};
    private int[] indexData = new int[]{
        0, 1, 2,
        2, 3, 0,
        //
        4, 5, 6,
        6, 7, 4,
        //
        8, 9, 10,
        10, 11, 8,
        //
        12, 13, 14,
        14, 15, 12,
        //
        16, 17, 18,
        18, 19, 16,
        //
        20, 21, 22,
        22, 23, 20};
    private String shadersFilepath = "/tut06/shaders/";
    private Mat4 cameraToClipMatrix = new Mat4();
    private float fFrustumScale = calculatFrustumScale(45.0f);
    private int numberOfVertices = 24;
    private int modelToCameraMatrixLocation;
    private Armature armature;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Hierarchy hierarchy = new Hierarchy();

        Frame frame = new Frame("Tutorial 06 - Hierarchy");

        frame.add(hierarchy.getCanvas());

        frame.setSize(hierarchy.getCanvas().getWidth(), hierarchy.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public Hierarchy() {
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

        initializeVertexArrayObject(gl3);

        armature = new Armature();

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

        armature.draw(gl3);

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
            int perspectiveMatrixLocation = gl3.glGetUniformLocation(programObject.getProgId(), "perspectiveMatrix");
            gl3.glUniformMatrix4fv(perspectiveMatrixLocation, 1, false, cameraToClipMatrix.toFloatArray(), 0);
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
            float fzFar = 100.0f;

            cameraToClipMatrix.c0.x = fFrustumScale;
            cameraToClipMatrix.c1.y = fFrustumScale;
            cameraToClipMatrix.c2.z = (fzFar + fzNear) / (fzNear - fzFar);
            cameraToClipMatrix.c2.w = -1.0f;
            cameraToClipMatrix.c3.z = (2 * fzFar * fzNear) / (fzNear - fzFar);

            int cameraToClipMatrixLocation = gl3.glGetUniformLocation(programObject.getProgId(), "cameraToClipMatrix");
            gl3.glUniformMatrix4fv(cameraToClipMatrixLocation, 1, false, cameraToClipMatrix.toFloatArray(), 0);

            modelToCameraMatrixLocation = gl3.glGetUniformLocation(programObject.getProgId(), "modelToCameraMatrix");
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

    class Armature {

        private Vec3 posBase = new Vec3(3.0f, -5.0f, -40.f);
        private float angBase = -45.0f;
        private Vec3 posBaseLeft = new Vec3(2.0f, 0.0f, 0.0f);
        private Vec3 posBaseRight = new Vec3(-2.0f, 0.0f, 0.0f);
        private float scaleBaseZ = 3.0f;
//        
        private float angUpperArm = -33.75f;
        private float sizeUpperArm = 9.0f;
        private Vec3 posLowerArm = new Vec3(0.0f, 0.0f, 8.0f);
        private float angLowerArm = 146.25f;
        private float lengthLowerArm = 5.0f;
        private float widthLowerArm = 1.5f;
//        
        private Vec3 posWrist = new Vec3(0.0f, 0.0f, 5.0f);
        private float angWristRoll = 0.0f;
        private float angWristPitch = 67.5f;
        private float lengthWrist = 2.0f;
        private float widthWrist = 2.0f;
//        
        private Vec3 posLeftFinger = new Vec3(1.0f, 0.0f, 1.0f);
        private Vec3 posRightFinger = new Vec3(-1.0f, 0.0f, 1.0f);
        private float angFingerOpen = 180.0f;
        private float lengthFinger = 2.0f;
        private float widthFinger = 0.5f;
        private float angLowerFinger = 45.0f;
//        
        private float standardAngleIncrement = 11.25f;
        private float smallAngleIncrement = 9.0f;

        public Armature() {
        }

        public void draw(GL3 gl3) {

            MatrixStack modelToCameraStack = new MatrixStack();

            programObject.bind(gl3);
            {
                gl3.glBindVertexArray(VAO[0]);
                {
                    modelToCameraStack.translate(posBase);
                    modelToCameraStack.rotateY(angBase);

                    //  Draw left base
                    modelToCameraStack.push();
                    {
                        modelToCameraStack.translate(posBaseLeft);
                        modelToCameraStack.scale(new Vec3(1.0f, 1.0f, scaleBaseZ));

                        gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                        gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                    }
                    modelToCameraStack.pop();

                    //  Draw right base
                    modelToCameraStack.push();
                    {
                        modelToCameraStack.translate(posBaseRight);
                        modelToCameraStack.scale(new Vec3(1.0f, 1.0f, scaleBaseZ));

                        gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                        gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                    }
                    modelToCameraStack.pop();

                    //  Draw the rest
                    drawUpperArm(gl3, modelToCameraStack);
                }
                gl3.glBindVertexArray(0);
            }
            programObject.unbind(gl3);
        }

        private void drawUpperArm(GL3 gl3, MatrixStack modelToCameraStack) {

            modelToCameraStack.push();
            {
                modelToCameraStack.rotateX(angUpperArm);

                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, sizeUpperArm / 2.0f - 1.0f));
                    modelToCameraStack.scale(new Vec3(1.0f, 1.0f, sizeUpperArm / 2.0f));

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
                modelToCameraStack.pop();

                drawLowerArm(gl3, modelToCameraStack);
            }
            modelToCameraStack.pop();
        }

        private void drawLowerArm(GL3 gl3, MatrixStack modelToCameraStack) {

            modelToCameraStack.push();
            {
                modelToCameraStack.translate(posLowerArm);
                modelToCameraStack.rotateX(angLowerArm);

                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthLowerArm / 2.0f));
                    modelToCameraStack.scale(new Vec3(widthLowerArm / 2.0f, widthLowerArm / 2.0f, lengthLowerArm / 2.0f));

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
                modelToCameraStack.pop();

                drawWrist(gl3, modelToCameraStack);
            }
            modelToCameraStack.pop();
        }

        private void drawWrist(GL3 gl3, MatrixStack modelToCameraStack) {

            modelToCameraStack.push();
            {
                modelToCameraStack.translate(posWrist);
                modelToCameraStack.rotateZ(angWristRoll);
                modelToCameraStack.rotateX(angWristPitch);

                modelToCameraStack.push();
                {
                    modelToCameraStack.scale(new Vec3(widthWrist / 2.0f, widthWrist / 2.0f, lengthWrist / 2.0f));

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
                modelToCameraStack.pop();

                drawFingers(gl3, modelToCameraStack);
            }
            modelToCameraStack.pop();
        }

        private void drawFingers(GL3 gl3, MatrixStack modelToCameraStack) {
            //  Draw left finger
            modelToCameraStack.push();
            {
                modelToCameraStack.translate(posLeftFinger);
                modelToCameraStack.rotateY(angFingerOpen);

                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
                    modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
                modelToCameraStack.pop();

                //  Draw left lower finger
                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger));
                    modelToCameraStack.rotateY(-angLowerFinger);

                    modelToCameraStack.push();
                    {
                        modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
                        modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));

                        gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                        gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                    }
                    modelToCameraStack.pop();
                }
                modelToCameraStack.pop();
            }
            modelToCameraStack.pop();

            //  Draw right finger
            modelToCameraStack.push();
            {
                modelToCameraStack.translate(posRightFinger);
                modelToCameraStack.rotateY(-angFingerOpen);

                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
                    modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));

                    gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                    gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                }
                modelToCameraStack.pop();

                //  Draw left lower finger
                modelToCameraStack.push();
                {
                    modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger));
                    modelToCameraStack.rotateY(angLowerFinger);

                    modelToCameraStack.push();
                    {
                        modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
                        modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));

                        gl3.glUniformMatrix4fv(modelToCameraMatrixLocation, 1, false, modelToCameraStack.top().toFloatArray(), 0);

                        gl3.glDrawElements(GL3.GL_TRIANGLES, indexData.length, GL3.GL_UNSIGNED_INT, 0);
                    }
                    modelToCameraStack.pop();
                }
                modelToCameraStack.pop();
            }
            modelToCameraStack.pop();
        }

        public void adjBase(boolean incrementing) {
            angBase += incrementing ? standardAngleIncrement : -standardAngleIncrement;
            angBase = angBase % 360.0f;
        }

        public void adjUpperArm(boolean incrementing) {
            angUpperArm += incrementing ? standardAngleIncrement : -standardAngleIncrement;
            angUpperArm = Jglm.clamp(angUpperArm, -90.0f, 0.0f);
        }

        public void adjLowerArm(boolean incrementing) {
            angLowerArm += incrementing ? standardAngleIncrement : -standardAngleIncrement;
            angLowerArm = Jglm.clamp(angLowerArm, 0.0f, 146.25f);
        }

        public void adjWristPitch(boolean incrementing) {
            angWristPitch += incrementing ? standardAngleIncrement : -standardAngleIncrement;
            angWristPitch = Jglm.clamp(angWristPitch, 0.0f, 90.0f);
        }

        public void adjWristRoll(boolean incrementing) {
            angWristRoll += incrementing ? standardAngleIncrement : -standardAngleIncrement;
            angWristRoll = angWristRoll % 360.0f;
        }

        public void adjFingerOpen(boolean incrementing) {
            angFingerOpen += incrementing ? smallAngleIncrement : -smallAngleIncrement;
            angFingerOpen = Jglm.clamp(angFingerOpen, 9.0f, 180.0f);
        }

        public void printPose() {
            System.out.println("angBase: " + angBase);
            System.out.println("angUpperArm: " + angUpperArm);
            System.out.println("angLowerArm: " + angLowerArm);
            System.out.println("angWristPitch: " + angWristPitch);
            System.out.println("angWristRoll: " + angWristRoll);
            System.out.println("angFingerOpen: " + angFingerOpen);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keyPressed");

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                armature.adjBase(true);
                canvas.display();
                break;
            case KeyEvent.VK_D:
                armature.adjBase(false);
                canvas.display();
                break;
            case KeyEvent.VK_W:
                armature.adjUpperArm(false);
                canvas.display();
                break;
            case KeyEvent.VK_S:
                armature.adjUpperArm(true);
                canvas.display();
                break;
            case KeyEvent.VK_R:
                armature.adjLowerArm(false);
                canvas.display();
                break;
            case KeyEvent.VK_F:
                armature.adjLowerArm(true);
                canvas.display();
                break;
            case KeyEvent.VK_T:
                armature.adjWristPitch(false);
                canvas.display();
                break;
            case KeyEvent.VK_G:
                armature.adjWristPitch(true);
                canvas.display();
                break;
            case KeyEvent.VK_Z:
                armature.adjWristRoll(true);
                canvas.display();
                break;
            case KeyEvent.VK_C:
                armature.adjWristRoll(false);
                canvas.display();
                break;
            case KeyEvent.VK_Q:
                armature.adjFingerOpen(true);
                canvas.display();
                break;
            case KeyEvent.VK_E:
                armature.adjFingerOpen(false);
                canvas.display();
                break;
            case KeyEvent.VK_SPACE:
                armature.printPose();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}