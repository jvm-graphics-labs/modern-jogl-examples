/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import glm.quat.Quat;
import glm.vec._3.Vec3;

/**
 * Utility object containing the ViewPole's view information.
 *
 * @author gbarbieri
 */
public class ViewData {

    // The starting target position position.
    private Vec3 targetPos;
    // The initial orientation aroudn the target position.
    private Quat orient;
    // The initial radius of the camera from the target point.
    private float radius;
    // The initial spin rotation of the "up" axis, relative to \a orient
    private float degSpinRotation;

    public ViewData(Vec3 targetPos, Quat orient, float radius, float degSpinRotation) {

        this.targetPos = targetPos;
        this.orient = orient;
        this.radius = radius;
        this.degSpinRotation = degSpinRotation;
    }

    public float radius() {
        return radius;
    }

    public void radius(float radius) {
        this.radius = radius;
    }

    public float degSpinRotation() {
        return degSpinRotation;
    }

    public void degSpinRotation(float degSpinRotation) {
        this.degSpinRotation = degSpinRotation;
    }

    public Quat orient() {
        return orient;
    }

    public void orient(Quat orient) {
        this.orient = orient;
    }

    public Vec3 targetPos() {
        return targetPos;
    }
}
