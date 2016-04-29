/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut06.rotations;

import glm.mat._3.Mat3;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

/**
 *
 * @author GBarbieri
 */
public class Instance {

    private Rotations.Mode mode;
    private Vec3 offset;
    private Mat3 mat;

    public Instance(Rotations.Mode mode, Vec3 offset) {
        this.mode = mode;
        this.offset = offset;
        mat = new Mat3();
    }

    public Mat4 constructMatrix(float elapsedTime) {

        Mat3 rotMatrix = calcRotation(elapsedTime);
        Mat4 theMat = new Mat4(rotMatrix);
        theMat.c3(new Vec4(offset, 1.0f));
        
        return theMat;
    }

    private Mat3 calcRotation(float elapsedTime) {

        switch (mode) {

            default:
                return mat.identity();

            case RotateX:
                return mat.rotationX(computeAngRad(elapsedTime, 3.0f));
                
            case RotateY:
                return mat.rotationY(computeAngRad(elapsedTime, 2.0f));
                
            case RotateZ:
                return mat.rotationZ(computeAngRad(elapsedTime, 2.0f));
                
            case RotateAxis:
                return mat.rotation(computeAngRad(elapsedTime, 2.0f), new Vec3(1.0f).normalize());
        }
    }

    private float computeAngRad(float elapsedTime, float loopDuration) {
        float scale = (float) (Math.PI * 2.0f / loopDuration);
        float currentTimeThroughLoop = elapsedTime % loopDuration;
        return currentTimeThroughLoop * scale;
    }
}
