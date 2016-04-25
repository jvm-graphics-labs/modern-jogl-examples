/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import framework.jglm.Quat;
import framework.jglm.Vec3;

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

    public Vec3 getPosition() {
        return position;
    }

    public Quat getOrientation() {
        return orientation;
    }

    public void setOrientation(Quat orientation) {
        this.orientation = orientation;
    }
}