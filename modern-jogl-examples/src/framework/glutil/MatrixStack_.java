/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

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

    public void translate(Vec3 offset) {
        top().translate(offset);
    }

    public void scale(Vec3 scaling) {
        top().scale(scaling);
    }

    public void rotateX(float angDeg) {
        top().rotateX(angDeg);
    }

    public void rotateY(float angDeg) {
        top().rotateY(angDeg);
    }

    public void rotateZ(float angDeg) {
        top().rotateZ(angDeg);
    }

    public void applyMat(Mat4 mat4) {
        top().mul(mat4);
    }

    public void push() {
        matrices.add(new Mat4(top()));
    }

    public void pop() {
        matrices.remove(matrices.size() - 1);
    }

    public Mat4 top() {
        return matrices.get(matrices.size() - 1);
    }

    public void setMatrix(Mat4 mat4) {
        matrices.set(matrices.size() - 1, mat4);
    }
}
