/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

import java.util.ArrayList;
import jglm.Mat3;
import jglm.Mat4;
import jglm.Vec3;
import jglm.Vec4;

/**
 *
 * @author gbarbieri
 */
public class MatrixStack {

    private ArrayList<Mat4> matrices;

    public MatrixStack() {
        matrices = new ArrayList<>();

        matrices.add(new Mat4(1.0f));
    }

    public void translate(Vec3 offset) {

        Mat4 translationMat = new Mat4(1.0f);
        translationMat.c3 = new Vec4(offset, 1.0f);

        Mat4 newMat = top().mult(translationMat);

//        matrices.set(matrices.size() - 1, newMat);
        setTop(newMat);
    }

    public void scale(Vec3 scaling) {
        
        Mat4 scalingMat = new Mat4(1.0f);

        scalingMat.c0.x = scaling.x;
        scalingMat.c1.y = scaling.y;
        scalingMat.c2.z = scaling.z;

        Mat4 newMat = top().mult(scalingMat);
        
//        newMat.print("scaling mat");

//        matrices.set(matrices.size() - 1, newMat);
        setTop(newMat);
    }

    public void rotateX(float fAngDeg) {

        Mat4 rotationMat = new Mat4(Mat3.rotateX(fAngDeg));

        Mat4 newMat = top().mult(rotationMat);

//        matrices.set(matrices.size() - 1, newMat);
        setTop(newMat);
    }

    public void rotateY(float fAngDeg) {

        Mat4 rotationMat = new Mat4(Mat3.rotateY(fAngDeg));

        Mat4 newMat = top().mult(rotationMat);

//        matrices.set(matrices.size() - 1, newMat);
        setTop(newMat);
    }

    public void rotateZ(float fAngDeg) {

        Mat4 rotationMat = new Mat4(Mat3.rotateZ(fAngDeg));

        Mat4 newMat = top().mult(rotationMat);

//        matrices.set(matrices.size() - 1, newMat);
        setTop(newMat);
    }

    public void applyMat(Mat4 mat4) {

//        setTop(mat4.mult(top()));
        setTop(top().mult(mat4));
    }

    public static float calculateFrustumScale(float fFovDeg) {

//        float degToRad = (float) (Math.PI * 2.0f / 360.0f);
        float fFovRad = (float) Math.toRadians(fFovDeg);
        return (float) (1.0f / Math.tan(fFovRad / 2.0f));
    }

    public void push() {
        Mat4 topMat = matrices.get(matrices.size() - 1);
        matrices.add(topMat);
    }

    public void pop() {
        matrices.remove(matrices.size() - 1);
    }

    public Mat4 top() {
        return matrices.get(matrices.size() - 1);
    }

    public void setTop(Mat4 mat4) {
        matrices.set(matrices.size() - 1, mat4);
    }
}
