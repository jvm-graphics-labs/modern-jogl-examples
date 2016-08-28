/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import glm.quat.Quat;
import glm.vec._3.Vec3;

/**
 *
 * @author gbarbieri
 */
public class ObjectData {

    private Vec3 position;
    private Quat orientation;

    public ObjectData(Vec3 position, Quat orientation) {

        this.position = position;
        this.orientation = orientation;
    }

    public Vec3 position() {
        return position;
    }

    public Quat orientation() {
        return orientation;
    }

    public void orientation(Quat orientation) {
        this.orientation = orientation;
    }
}
