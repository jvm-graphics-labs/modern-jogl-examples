/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import framework.jglm.Jglm;
import glm.glm;

/**
 *
 * @author gbarbieri
 */
public class Timer {

    public interface Type {

        public static final int LOOP = 0;
        public static final int SINGLE = 1;
        public static final int INFINITE = 2;
        public static final int MAX = 3;
    }

    private int type;
    private float secDuration;
    private boolean hasUpdated;
    private boolean isPaused;
    private float absPreviousTime;
    private float secAccumTime;
    private long start;

    /**
     * Creates a timer with the given type.
     *
     * LOOP and SINGLE timers need an explicit duration. This represents the
     * time in seconds through a loop, or the time in seconds until the timer
     * expires.
     *
     * INFINITE timers ignore the duration.
     *
     * @param duration
     */
    public Timer(float duration) {
        this(Type.INFINITE, duration);
    }

    public Timer(int type, float duration) {

        this.type = type;
        this.secDuration = duration;
        hasUpdated = false;
        isPaused = false;
        absPreviousTime = 0.0f;
        secAccumTime = 0.0f;

        if (type != Type.INFINITE) {
            if (secDuration <= 0.0f) {
                throw new Error("secDuration <= 0.0f");
            }
        }

        start = System.nanoTime();
    }

    public void reset() {
        hasUpdated = false;
        secAccumTime = 0.0f;
    }

    public boolean togglePause() {
        isPaused = !isPaused;
        return isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPause(boolean pause) {
        isPaused = pause;
    }

    public boolean update() {

        float absCurrenTime = (System.nanoTime() - start) / 1_000_000_000.0f;

        if (!hasUpdated) {

            absPreviousTime = absCurrenTime;
            hasUpdated = true;
        }

        if (isPaused) {

            absPreviousTime = absCurrenTime;
            return true;
        }

        float deltaTime = absCurrenTime - absPreviousTime;
        secAccumTime += deltaTime;

        absPreviousTime = absCurrenTime;
        if (type == Type.SINGLE) {
            return secAccumTime > secDuration;
        }
        return false;
    }

    public void rewind(float secRewind) {

        secAccumTime -= secRewind;
        if (secAccumTime < 0) {
            secAccumTime = 0;
        }
    }

    public void fastForward(float secFF) {
        secAccumTime += secFF;
    }

    public float getAlpha() {

        switch (type) {

            case Type.LOOP:
                return (secAccumTime % secDuration) / secDuration;
            case Type.SINGLE:
                return glm.clamp(secAccumTime / secDuration, 0.0f, 1.0f);
        }

        return -1.0f;   //Garbage.
    }

    public float getProgression() {

        switch (type) {

            case Type.LOOP:
                return secAccumTime % secDuration;
            case Type.SINGLE:
                return glm.clamp(secAccumTime, 0.0f, secDuration);
        }

        return -1.0f;   //Garbage.
    }

    private float getTimeSinceStart() {
        return secAccumTime;
    }
}
