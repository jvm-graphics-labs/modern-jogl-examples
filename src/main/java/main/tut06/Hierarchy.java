
package main.tut06;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import main.framework.Framework;
import main.framework.Semantic;
import glm.mat.Mat3;
import glm.mat.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Stack;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class Hierarchy extends Framework {

    public static void main(String[] args) {
        new Hierarchy().setup("Tutorial 06 - Hierarchy");
    }

    private interface Buffer {

        int VERTEX = 0;
        int INDEX = 1;
        int MAX = 2;
    }

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif;

    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private float frustumScale = calcFrustumScale(45.0f);

    private float calcFrustumScale(float fovDeg) {
        float fovRad = glm.toRad(fovDeg);
        return 1.0f / glm.tan(fovRad / 2.0f);
    }

    private IntBuffer bufferObject = GLBuffers.newDirectIntBuffer(Buffer.MAX), vao = GLBuffers.newDirectIntBuffer(1);

    private final int numberOfVertices = 24;

    private final float[] GREEN_COLOR = {0.0f, 1.0f, 0.0f, 1.0f}, BLUE_COLOR = {0.0f, 0.0f, 1.0f, 1.0f},
            RED_COLOR = {1.0f, 0.0f, 0.0f, 1.0f}, YELLOW_COLOR = {1.0f, 1.0f, 0.0f, 1.0f},
            CYAN_COLOR = {0.0f, 1.0f, 1.0f, 1.0f}, MAGENTA_COLOR = {1.0f, 0.0f, 1.0f, 1.0f};

    private float[] vertexData = {

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


            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],

            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],

            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],

            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3]};

    private short[] indexData = {

            0, 1, 2,
            2, 3, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            10, 11, 8,

            12, 13, 14,
            14, 15, 12,

            16, 17, 18,
            18, 19, 16,

            20, 21, 22,
            22, 23, 20};

    private Armature armature = new Armature();

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeVAO(gl);

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRange(0.0f, 1.0f);
    }

    private void initializeProgram(GL3 gl) {

        theProgram = programOf(gl, getClass(), "tut06", "pos-color-local-transform.vert", "color-passthrough.frag");

        modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix");

        float zNear = 1.0f, zFar = 100.0f;

        cameraToClipMatrix.v00(frustumScale);
        cameraToClipMatrix.v11(frustumScale);
        cameraToClipMatrix.v22((zFar + zNear) / (zNear - zFar));
        cameraToClipMatrix.v23(-1.0f);
        cameraToClipMatrix.v32((2 * zFar * zNear) / (zNear - zFar));

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.to(matBuffer));
        gl.glUseProgram(0);
    }

    private void initializeVAO(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);

        gl.glGenBuffers(Buffer.MAX, bufferObject);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));
        gl.glBufferData(GL_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        int colorDataOffset = Vec3.SIZE * numberOfVertices;
        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, Vec3.length, GL_FLOAT, false, Vec3.SIZE, 0);
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, Vec4.length, GL_FLOAT, false, Vec4.SIZE, colorDataOffset);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl.glBindVertexArray(0);

        destroyBuffers(vertexBuffer, indexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        armature.draw(gl);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        cameraToClipMatrix.v00(frustumScale * (h / (float) w));
        cameraToClipMatrix.v11(frustumScale);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.to(matBuffer));
        gl.glUseProgram(0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);
        gl.glDeleteBuffers(Buffer.MAX, bufferObject);
        gl.glDeleteVertexArrays(1, vao);

        destroyBuffers(vao, bufferObject);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                quit();
                break;
            case KeyEvent.VK_A:
                armature.adjBase(true);
                break;
            case KeyEvent.VK_D:
                armature.adjBase(false);
                break;
            case KeyEvent.VK_W:
                armature.adjUpperArm(false);
                break;
            case KeyEvent.VK_S:
                armature.adjUpperArm(true);
                break;
            case KeyEvent.VK_R:
                armature.adjLowerArm(false);
                break;
            case KeyEvent.VK_F:
                armature.adjLowerArm(true);
                break;
            case KeyEvent.VK_T:
                armature.adjWristPitch(false);
                break;
            case KeyEvent.VK_G:
                armature.adjWristPitch(true);
                break;
            case KeyEvent.VK_Z:
                armature.adjWristRoll(true);
                break;
            case KeyEvent.VK_C:
                armature.adjWristRoll(false);
                break;
            case KeyEvent.VK_Q:
                armature.adjFingerOpen(true);
                break;
            case KeyEvent.VK_E:
                armature.adjFingerOpen(false);
                break;
            case KeyEvent.VK_SPACE:
                armature.writePose();
                break;
        }
    }

    class Armature {

        private Vec3 posBase = new Vec3(3.0f, -5.0f, -40.f);
        private float angBase = -45.0f;

        private Vec3 posBaseLeft = new Vec3(2.0f, 0.0f, 0.0f), posBaseRight = new Vec3(-2.0f, 0.0f, 0.0f);
        private float scaleBaseZ = 3.0f;

        private float angUpperArm = -33.75f;
        private float sizeUpperArm = 9.0f;

        private Vec3 posLowerArm = new Vec3(0.0f, 0.0f, 8.0f);
        private float angLowerArm = 146.25f;
        private float lengthLowerArm = 5.0f;
        private float widthLowerArm = 1.5f;

        private Vec3 posWrist = new Vec3(0.0f, 0.0f, 5.0f);
        private float angWristRoll = 0.0f;
        private float angWristPitch = 67.5f;
        private float lenWrist = 2.0f;
        private float widthWrist = 2.0f;

        private Vec3 posLeftFinger = new Vec3(1.0f, 0.0f, 1.0f), posRightFinger = new Vec3(-1.0f, 0.0f, 1.0f);
        private float angFingerOpen = 180.0f;
        private float lengthFinger = 2.0f;
        private float widthFinger = 0.5f;
        private float angLowerFinger = 45.0f;

        private final float STANDARD_ANGLE_INCREMENT = 11.25f;
        private final float SMALL_ANGLE_INCREMENT = 9.0f;

        void draw(GL3 gl) {

            MatrixStack modelToCameraStack = new MatrixStack();

            gl.glUseProgram(theProgram);
            gl.glBindVertexArray(vao.get(0));

            modelToCameraStack
                    .translate(posBase)
                    .rotateY(angBase);

            //  Draw left base.
            {
                modelToCameraStack
                        .push()
                        .translate(posBaseLeft)
                        .scale(new Vec3(1.0f, 1.0f, scaleBaseZ));
                gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
                gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
                modelToCameraStack.pop();
            }

            //  Draw right base.
            {
                modelToCameraStack
                        .push()
                        .translate(posBaseRight)
                        .scale(new Vec3(1.0f, 1.0f, scaleBaseZ));
                gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
                gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
                modelToCameraStack.pop();
            }

            //  Draw main arm.
            drawUpperArm(gl, modelToCameraStack);

            gl.glBindVertexArray(0);
            gl.glUseProgram(0);
        }

        private void drawUpperArm(GL3 gl, MatrixStack modelToCameraStack) {

            modelToCameraStack
                    .push()
                    .rotateX(angUpperArm);

            {
                modelToCameraStack
                        .push()
                        .translate(new Vec3(0.0f, 0.0f, sizeUpperArm / 2.0f - 1.0f))
                        .scale(new Vec3(1.0f, 1.0f, sizeUpperArm / 2.0f));
                gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
                gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
                modelToCameraStack.pop();
            }

            drawLowerArm(gl, modelToCameraStack);

            modelToCameraStack.pop();
        }

        private void drawLowerArm(GL3 gl, MatrixStack modelToCameraStack) {

            modelToCameraStack
                    .push()
                    .translate(posLowerArm)
                    .rotateX(angLowerArm);

            modelToCameraStack
                    .push()
                    .translate(new Vec3(0.0f, 0.0f, lengthLowerArm / 2.0f))
                    .scale(new Vec3(widthLowerArm / 2.0f, widthLowerArm / 2.0f, lengthLowerArm / 2.0f));
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
            gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();

            drawWrist(gl, modelToCameraStack);

            modelToCameraStack.pop();
        }

        private void drawWrist(GL3 gl, MatrixStack modelToCameraStack) {

            modelToCameraStack
                    .push()
                    .translate(posWrist)
                    .rotateZ(angWristRoll)
                    .rotateX(angWristPitch);

            modelToCameraStack
                    .push()
                    .scale(new Vec3(widthWrist / 2.0f, widthWrist / 2.0f, lenWrist / 2.0f));
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
            gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();

            drawFingers(gl, modelToCameraStack);

            modelToCameraStack.pop();
        }

        private void drawFingers(GL3 gl, MatrixStack modelToCameraStack) {

            //  Draw left finger
            modelToCameraStack
                    .push()
                    .translate(posLeftFinger)
                    .rotateY(angFingerOpen);

            modelToCameraStack
                    .push()
                    .translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                    .scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
            gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();

            {
                //  Draw left lower finger
                modelToCameraStack
                        .push()
                        .translate(new Vec3(0.0f, 0.0f, lengthFinger))
                        .rotateY(-angLowerFinger);

                modelToCameraStack
                        .push()
                        .translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                        .scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
                gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
                gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
                modelToCameraStack.pop();

                modelToCameraStack.pop();
            }

            modelToCameraStack.pop();

            //  Draw right finger
            modelToCameraStack
                    .push()
                    .translate(posRightFinger)
                    .rotateY(-angFingerOpen);

            modelToCameraStack
                    .push()
                    .translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                    .scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
            gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();

            {
                //  Draw left lower finger
                modelToCameraStack
                        .push()
                        .translate(new Vec3(0.0f, 0.0f, lengthFinger))
                        .rotateY(angLowerFinger);

                modelToCameraStack
                        .push()
                        .translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                        .scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
                gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack.to(matBuffer));
                gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
                modelToCameraStack.pop();

                modelToCameraStack.pop();
            }
            modelToCameraStack.pop();
        }

        public void adjBase(boolean increment) {
            angBase += increment ? STANDARD_ANGLE_INCREMENT : -STANDARD_ANGLE_INCREMENT;
            angBase %= 360.0f;
        }

        public void adjUpperArm(boolean increment) {
            angUpperArm += increment ? STANDARD_ANGLE_INCREMENT : -STANDARD_ANGLE_INCREMENT;
            angUpperArm = glm.clamp(angUpperArm, -90.0f, 0.0f);
        }

        public void adjLowerArm(boolean increment) {
            angLowerArm += increment ? STANDARD_ANGLE_INCREMENT : -STANDARD_ANGLE_INCREMENT;
            angLowerArm = glm.clamp(angLowerArm, 0.0f, 146.25f);
        }

        public void adjWristPitch(boolean increment) {
            angWristPitch += increment ? STANDARD_ANGLE_INCREMENT : -STANDARD_ANGLE_INCREMENT;
            angWristPitch = glm.clamp(angWristPitch, 0.0f, 90.0f);
        }

        public void adjWristRoll(boolean increment) {
            angWristRoll += increment ? STANDARD_ANGLE_INCREMENT : -STANDARD_ANGLE_INCREMENT;
            angWristRoll %= 360.0f;
        }

        public void adjFingerOpen(boolean increment) {
            angFingerOpen += increment ? SMALL_ANGLE_INCREMENT : -SMALL_ANGLE_INCREMENT;
            angFingerOpen = glm.clamp(angFingerOpen, 9.0f, 180.0f);
        }

        public void writePose() {
            System.out.println("angBase:\t" + angBase);
            System.out.println("angUpperArm:\t" + angUpperArm);
            System.out.println("angLowerArm:\t" + angLowerArm);
            System.out.println("angWristPitch:\t" + angWristPitch);
            System.out.println("angWristRoll:\t" + angWristRoll);
            System.out.println("angFingerOpen:\t" + angFingerOpen);
        }
    }

    private class MatrixStack {

        private Stack<Mat4> matrices = new Stack<>();
        private Mat4 currMat = new Mat4(1f);

        Mat4 top() {
            return currMat;
        }

        MatrixStack rotateX(float angDeg) {
            currMat.times_(new Mat4(Hierarchy.this.rotateX(angDeg)));
            return this;
        }

        MatrixStack rotateY(float angDeg) {
            currMat.times_(new Mat4(Hierarchy.this.rotateY(angDeg)));
            return this;
        }

        MatrixStack rotateZ(float angDeg) {
            currMat.times_(new Mat4(Hierarchy.this.rotateZ(angDeg)));
            return this;
        }

        MatrixStack scale(Vec3 scaleVec) {

            Mat4 scaleMat = new Mat4(scaleVec);

            currMat.times_(scaleMat);

            return this;
        }

        MatrixStack translate(Vec3 offsetVec) {

            Mat4 translateMat = new Mat4(1f);
            translateMat.set(3, new Vec4(offsetVec));

            currMat.times_(translateMat);

            return this;
        }

        MatrixStack push() {
            matrices.push(new Mat4(currMat));
            return this;
        }

        MatrixStack pop() {
            currMat = matrices.pop();
            return this;
        }

        FloatBuffer to(FloatBuffer buffer) {
            return currMat.to(buffer);
        }
    }

    Mat3 rotateX(float angDeg) {

        float andRad = glm.toRad(angDeg);
        float cos = glm.cos(andRad);
        float sin = glm.sin(andRad);

        Mat3 theMat = new Mat3(1f);
        theMat.v11(cos);
        theMat.v12(sin);
        theMat.v21(-sin);
        theMat.v22(cos);
        return theMat;
    }

    Mat3 rotateY(float angDeg) {

        float andRad = glm.toRad(angDeg);
        float cos = glm.cos(andRad);
        float sin = glm.sin(andRad);

        Mat3 theMat = new Mat3(1f);
        theMat.v00(cos);
        theMat.v02(-sin);
        theMat.v20(sin);
        theMat.v22(cos);
        return theMat;
    }

    Mat3 rotateZ(float angDeg) {

        float andRad = glm.toRad(angDeg);
        float cos = glm.cos(andRad);
        float sin = glm.sin(andRad);

        Mat3 theMat = new Mat3(1f);
        theMat.v00(cos);
        theMat.v01(sin);
        theMat.v10(-sin);
        theMat.v11(cos);
        return theMat;
    }
}
