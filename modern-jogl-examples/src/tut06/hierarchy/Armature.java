/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut06.hierarchy;

import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import framework.BufferUtils;
import framework.Framework;
import glm.glm;
import glm.vec._3.Vec3;
import java.nio.FloatBuffer;

/**
 *
 * @author GBarbieri
 */
public class Armature {

    private Vec3 posBase = new Vec3(3.0f, -5.0f, -40.f);
    private float angBase = -45.0f;
    private Vec3 posBaseLeft = new Vec3(2.0f, 0.0f, 0.0f);
    private Vec3 posBaseRight = new Vec3(-2.0f, 0.0f, 0.0f);
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
    private float lengthWrist = 2.0f;
    private float widthWrist = 2.0f;

    private Vec3 posLeftFinger = new Vec3(1.0f, 0.0f, 1.0f);
    private Vec3 posRightFinger = new Vec3(-1.0f, 0.0f, 1.0f);
    private float angFingerOpen = 180.0f;
    private float lengthFinger = 2.0f;
    private float widthFinger = 0.5f;
    private float angLowerFinger = 45.0f;

    private final float STANDARD_ANGLE_INCREMENT = 11.25f;
    private final float SMALL_ANGLE_INCREMENT = 9.0f;

    public void draw(GL3 gl3, int theProgram, int vao, int modelToCameraMatrixUnif, int indexCount) {

        MatrixStack modelToCameraStack = new MatrixStack();

        gl3.glUseProgram(theProgram);
        gl3.glBindVertexArray(vao);

        modelToCameraStack.translate(posBase);
        modelToCameraStack.rotateY(angBase);

        //  Draw left base.
        {
            modelToCameraStack.push();
            modelToCameraStack.translate(posBaseLeft);
            modelToCameraStack.scale(new Vec3(1.0f, 1.0f, scaleBaseZ));
            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                    modelToCameraStack.top().toDfb(Framework.matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();
        }

        //  Draw right base.
        {
            modelToCameraStack.push();
            modelToCameraStack.translate(posBaseRight);
            modelToCameraStack.scale(new Vec3(1.0f, 1.0f, scaleBaseZ));
            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                    modelToCameraStack.top().toDfb(Framework.matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();
        }

        //  Draw main arm.
        drawUpperArm(gl3, modelToCameraStack, modelToCameraMatrixUnif, indexCount);

        gl3.glBindVertexArray(0);
        gl3.glUseProgram(0);
    }

    private void drawUpperArm(GL3 gl3, MatrixStack modelToCameraStack, int modelToCameraMatrixUnif, int indexCount) {

        modelToCameraStack.push();
        modelToCameraStack.rotateX(angUpperArm);

        {
            modelToCameraStack.push();
            modelToCameraStack.translate(new Vec3(0.0f, 0.0f, sizeUpperArm / 2.0f - 1.0f));
            modelToCameraStack.scale(new Vec3(1.0f, 1.0f, sizeUpperArm / 2.0f));
            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                    modelToCameraStack.top().toDfb(Framework.matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();
        }

        drawLowerArm(gl3, modelToCameraStack, modelToCameraMatrixUnif, indexCount);

        modelToCameraStack.pop();
    }

    private void drawLowerArm(GL3 gl3, MatrixStack modelToCameraStack, int modelToCameraMatrixUnif, int indexCount) {

        modelToCameraStack.push();
        modelToCameraStack.translate(posLowerArm);
        modelToCameraStack.rotateX(angLowerArm);

        modelToCameraStack.push();
        modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthLowerArm / 2.0f));
        modelToCameraStack.scale(new Vec3(widthLowerArm / 2.0f, widthLowerArm / 2.0f, lengthLowerArm / 2.0f));
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                modelToCameraStack.top().toDfb(Framework.matBuffer));
        gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
        modelToCameraStack.pop();

        drawWrist(gl3, modelToCameraStack, modelToCameraMatrixUnif, indexCount);

        modelToCameraStack.pop();
    }

    private void drawWrist(GL3 gl3, MatrixStack modelToCameraStack, int modelToCameraMatrixUnif, int indexCount) {

        modelToCameraStack.push();
        modelToCameraStack.translate(posWrist);
        modelToCameraStack.rotateZ(angWristRoll);
        modelToCameraStack.rotateX(angWristPitch);

        modelToCameraStack.push();
        modelToCameraStack.scale(new Vec3(widthWrist / 2.0f, widthWrist / 2.0f, lengthWrist / 2.0f));
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                modelToCameraStack.top().toDfb(Framework.matBuffer));
        gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
        modelToCameraStack.pop();

        drawFingers(gl3, modelToCameraStack, modelToCameraMatrixUnif, indexCount);

        modelToCameraStack.pop();
    }

    private void drawFingers(GL3 gl3, MatrixStack modelToCameraStack, int modelToCameraMatrixUnif, int indexCount) {

        //  Draw left finger
        modelToCameraStack.push();
        modelToCameraStack.translate(posLeftFinger);
        modelToCameraStack.rotateY(angFingerOpen);

        modelToCameraStack.push();
        modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
        modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                modelToCameraStack.top().toDfb(Framework.matBuffer));
        gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
        modelToCameraStack.pop();

        {
            //  Draw left lower finger
            modelToCameraStack.push();
            modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger));
            modelToCameraStack.rotateY(-angLowerFinger);

            modelToCameraStack.push();
            modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
            modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                    modelToCameraStack.top().toDfb(Framework.matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
            modelToCameraStack.pop();

            modelToCameraStack.pop();
        }

        modelToCameraStack.pop();

        //  Draw right finger
        modelToCameraStack.push();
        modelToCameraStack.translate(posRightFinger);
        modelToCameraStack.rotateY(-angFingerOpen);

        modelToCameraStack.push();
        modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
        modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                modelToCameraStack.top().toDfb(Framework.matBuffer));
        gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
        modelToCameraStack.pop();

        {
            //  Draw left lower finger
            modelToCameraStack.push();
            modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger));
            modelToCameraStack.rotateY(angLowerFinger);

            modelToCameraStack.push();
            modelToCameraStack.translate(new Vec3(0.0f, 0.0f, lengthFinger / 2.0f));
            modelToCameraStack.scale(new Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f));
            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, 
                    modelToCameraStack.top().toDfb(Framework.matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, 0);
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
        System.out.println("angBase: " + angBase);
        System.out.println("angUpperArm: " + angUpperArm);
        System.out.println("angLowerArm: " + angLowerArm);
        System.out.println("angWristPitch: " + angWristPitch);
        System.out.println("angWristRoll: " + angWristRoll);
        System.out.println("angFingerOpen: " + angFingerOpen);
    }
}
