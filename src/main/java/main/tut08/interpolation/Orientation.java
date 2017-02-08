/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut08.interpolation;

import glm.Glm;
import glm.quat.Quat;
import glm.vec._4.Vec4;
import glutil.Timer;

/**
 *
 * @author elect
 */
public class Orientation {

    private boolean isAnimating = false;
    private int currentOrient = 0;
    private boolean slerp = false;
    private final Animation anim = new Animation();

    public boolean toggleSlerp() {
        slerp = !slerp;
        return slerp;
    }

    public Quat getOrient() {
        if (isAnimating) {
            return anim.getOrient(orients[currentOrient], slerp);
        } else {
            return orients[currentOrient];
        }
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public void updateTime() {
        if (isAnimating) {
            boolean isFinished = anim.updateTime();
            if (isFinished) {
                isAnimating = false;
                currentOrient = anim.getFinalX();
            }
        }
    }

    public void animateToOrient(int destination) {
        if (currentOrient == destination) {
            return;
        }
        anim.startAnimation(destination, 1.0f);
        isAnimating = true;
    }

    private class Animation {

        private int finalOrient;
        private Timer currTimer;

        public boolean updateTime() {
            return currTimer.update();
        }

        public Quat getOrient(Quat initial, boolean slerp) {
            if (slerp) {
                return slerp(initial, orients[finalOrient], currTimer.getAlpha());
            } else {
                return lerp(initial, orients[finalOrient], currTimer.getAlpha());
            }
        }

        public void startAnimation(int destination, float duration) {
            finalOrient = destination;
            currTimer = new Timer(Timer.Type.SINGLE, duration);
        }

        public int getFinalX() {
            return finalOrient;
        }

        private Quat slerp(Quat v0, Quat v1, float alpha) {

            float dot = Glm.dot(v0, v1);
            final float DOT_THRESHOLD = 0.9995f;
            if (dot > DOT_THRESHOLD) {
                return lerp(v0, v1, alpha);
            }
            Glm.clamp(dot, -1.0f, 1.0f);
            float theta0 = (float) Math.acos(dot);
            float theta = theta0 * alpha;

            Quat v2 = v1.sub_(v0.mul_(dot));
            v2.normalize();

            return v0.mul_(Math.cos(theta)).add_(v2.mul_(Math.sin(theta)));
        }

        private Quat lerp(Quat v0, Quat v1, float alpha) {

            Vec4 start = vectorize(v0);
            Vec4 end = vectorize(v1);
            Vec4 interp = Glm.mix_(start, end, alpha);

            System.out.println("alpha: " + alpha + ", " + interp.toString());

            interp.normalize();
            return new Quat(interp.w, interp.x, interp.y, interp.z);
        }

        private Vec4 vectorize(Quat theQuat) {

            Vec4 ret = new Vec4();

            ret.x = theQuat.x;
            ret.y = theQuat.y;
            ret.z = theQuat.z;
            ret.w = theQuat.w;

            return ret;
        }
    }

    private final Quat[] orients = {
        new Quat(0.7071f, 0.7071f, 0.0f, 0.0f),
        new Quat(0.5f, 0.5f, -0.5f, 0.5f),
        new Quat(-0.4895f, -0.7892f, -0.3700f, -0.02514f),
        new Quat(0.4895f, 0.7892f, 0.3700f, 0.02514f),
        //
        new Quat(0.3840f, -0.1591f, -0.7991f, -0.4344f),
        new Quat(0.5537f, 0.5208f, 0.6483f, 0.0410f),
        new Quat(0.0f, 0.0f, 1.0f, 0.0f)};
}
