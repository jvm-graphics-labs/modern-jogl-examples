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

    Vec3 position;
    Quat orientation;

    public ObjectData(Vec3 position, Quat orientation) {

        this.position = position;
        this.orientation = orientation;
    }
}