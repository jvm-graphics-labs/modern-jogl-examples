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
    Vec3 targetPos;
    // The initial orientation aroudn the target position.
    Quat orient;
    // The initial radius of the camera from the target point.
    float radius;
    // The initial spin rotation of the "up" axis, relative to \a orient
    float degSpinRotation;

    public ViewData(Vec3 targetPos, Quat orient, float radius, float degSpinRotation) {

        this.targetPos = targetPos;
        this.orient = orient;
        this.radius = radius;
        this.degSpinRotation = degSpinRotation;
    }
}
