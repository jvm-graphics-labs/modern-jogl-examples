/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.util.ArrayList;

/**
 *
 * @author gbarbieri
 */
public class MatrixStack_ {

    private ArrayList<Mat4> matrices;

    public MatrixStack_() {
        matrices = new ArrayList<>();
        matrices.add(new Mat4(1.0f));
    }

    public MatrixStack_ translate(Vec3 offset) {
        top().translate(offset);
        return this;
    }

    public MatrixStack_ scale(Vec3 scaling) {
        top().scale(scaling);
        return this;
    }

    public MatrixStack_ rotateX(float angDeg) {
        top().rotateX(Math.toRadians(angDeg));
        return this;
    }

    public MatrixStack_ rotateY(float angDeg) {
        top().rotateY(Math.toRadians(angDeg));
        return this;
    }

    public MatrixStack_ rotateZ(float angDeg) {
        top().rotateZ(Math.toRadians(angDeg));
        return this;
    }

    public MatrixStack_ applyMat(Mat4 mat4) {
        top().mul(mat4);
        return this;
    }

    public MatrixStack_ push() {
        matrices.add(new Mat4(top()));
        return this;
    }

    public MatrixStack_ pop() {
        matrices.remove(matrices.size() - 1);
        return this;
    }

    public Mat4 top() {
        return matrices.get(matrices.size() - 1);
    }

    public MatrixStack_ setMatrix(Mat4 mat4) {
        matrices.set(matrices.size() - 1, mat4);
        return this;
    }
    
    public void perspective(float defFOV, float aspectRatio, float zNear, float zFar) {
        top().mulPerspective(defFOV, aspectRatio, zNear, zFar);
    }
}
