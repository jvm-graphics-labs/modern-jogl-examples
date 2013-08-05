/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

/**
 *
 * @author gbarbieri
 */
public class Jglm {

    public static float mix(float start, float end, float lerp) {
        return (start + lerp * (end - start));
    }

    public static Vec normalize(Vec vec) {

        float length = 0;

        for (int i = 0; i < vec.order; i++) {
            length += vec.toFloatArray()[i] * vec.toFloatArray()[i];
        }

        length = (float) Math.sqrt(length);

        float[] result = new float[vec.order];

        for (int i = 0; i < vec.order; i++) {
            result[i] = vec.toFloatArray()[i] / length;
        }

        return new Vec3(result);

//        float length = ((float) Math.sqrt((vec3[0] * vec3[0]) + (vec3[1] * vec3[1]) + (vec3[2] * vec3[2])));
//
//        return new float[]{vec3[0] / length, vec3[1] / length, vec3[2] / length};
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }

        return value;
    }

    public static Mat4 translate(Mat4 mat, Vec3 vec3) {

        Mat4 result = mat;

        result.c3 = mat.times(new Vec4(vec3, 1.0f));

        return result;
    }

    public static Quat angleAxis(Vec3 vec3, float angle) {

        Quat result = new Quat();

        float a = (float) Math.toRadians(angle);

        float s = (float) Math.sin(a * 0.5f);

        result.x = vec3.x * s;
        result.y = vec3.y * s;
        result.z = vec3.z * s;
        result.w = (float) Math.cos(a * 0.5f);

        return result;
    }
}