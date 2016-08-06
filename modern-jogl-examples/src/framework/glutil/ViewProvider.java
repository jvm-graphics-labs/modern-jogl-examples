/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import glm.mat._4.Mat4;

/**
 * Abstract base class used by ViewPole to identify that it provides a viewing
 * matrix.
 *
 * @author elect
 */
abstract class ViewProvider {

    //Computes the camera matrix.
    abstract Mat4 calcMatrix();

    public interface MouseButtons {

        public static final int LEFT = 0;   // The left mouse button.
        public static final int MIDDLE = 1; // The middle mouse button.
        public static final int RIGHT = 2;  // The right mouse button.
    }
}
