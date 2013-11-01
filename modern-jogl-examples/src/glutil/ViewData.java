/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

import jglm.Quat;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class ViewData {

    private Vec3 targetPos;
    private Quat orient;
    private float radius;
    private float degSpinRotation;

    public ViewData(Vec3 targetPos, Quat orient, float radius, float degSpinRotation) {

        this.targetPos = targetPos;
        this.orient = orient;
        this.radius = radius;
        this.degSpinRotation = degSpinRotation;
    }

    public Vec3 getTargetPos() {
        return targetPos;
    }

    public Quat getOrient() {
        return orient;
    }

    public float getRadius() {
        return radius;
    }

    public float getDegSpinRotation() {
        return degSpinRotation;
    }

    public void setOrient(Quat orient) {
        this.orient = orient;
    }

    public void setDegSpinRotation(float degSpinRotation) {
        this.degSpinRotation = degSpinRotation;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}