/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut06.translation;

import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

/**
 *
 * @author GBarbieri
 */
public class Instance {

    private Translation.Mode mode;
    private Vec3 vec = new Vec3();

    public Instance(Translation.Mode mode) {
        this.mode = mode;
    }

    public Mat4 constructMatrix(float elapsedTime) {

        Mat4 theMat = new Mat4(1.0f);
        theMat.c3(new Vec4(calcOffset(elapsedTime), 1.0f));

        return theMat;
    }

    private Vec3 calcOffset(float elapsedTime) {

        switch (mode) {

            default:
                return vec.set(0.0f, 0.0f, -20.0f);

            case OvalOffset:
                float loopDuration = 3.0f;
                float scale = (float) (Math.PI * 2.0f / loopDuration);

                float currTimeThroughLoop = elapsedTime % loopDuration;

                return vec.set(
                        Math.cos(currTimeThroughLoop * scale) * 4,
                        Math.sin(currTimeThroughLoop * scale) * 6,
                        -20.0f);

            case BottomCircleOffset:
                loopDuration = 12.0f;
                scale = (float) (Math.PI * 2.0f / loopDuration);

                currTimeThroughLoop = elapsedTime % loopDuration;
                return vec.set(
                        Math.cos(currTimeThroughLoop * scale) * 5,
                        -3.5f,
                        Math.sin(currTimeThroughLoop * scale) * 5 - 20.0f);
        }
    }
}
