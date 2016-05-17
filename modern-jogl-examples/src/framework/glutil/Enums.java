/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

/**
 * The possible buttons that Poles can use.
 *
 * @author elect
 */
public interface Enums {

    public interface MouseButtons {

        public static final int LEFT = 0;   // The left mouse button.
        public static final int MIDDLE = 1; // The middle mouse button.
        public static final int RIGHT = 2;  // The right mouse button.
    }
    
    public interface RotateMode {
        
        public static final int DUAL_AXIS = 0;
        public static final int BIAXIAL = 1;
        public static final int SPIN = 2;
    }
}
