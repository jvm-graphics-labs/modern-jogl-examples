/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

import static glutil.MatrixStack.calculatFrustumScale;

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

    public static int clamp(int value, int min, int max) {
        
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
        
        vec3.normalize();

        result.x = vec3.x * s;
        result.y = vec3.y * s;
        result.z = vec3.z * s;
        result.w = (float) Math.cos(a * 0.5f);

        return result;
    }

    public static Mat4 perspective(float fovDeg, float aspect, float zNear, float zFar) {

        float frustumScale = calculatFrustumScale(fovDeg);

        Mat4 perspectiveMatrix = new Mat4();

        perspectiveMatrix.c0.x = frustumScale / aspect;
        perspectiveMatrix.c1.y = frustumScale;
        perspectiveMatrix.c2.z = (zFar + zNear) / (zNear - zFar);
        perspectiveMatrix.c2.w = -1.0f;
        perspectiveMatrix.c3.z = (2 * zFar * zNear) / (zNear - zFar);

//        matrices.set(matrices.size() - 1, perspectiveMatrix);
//        setTop(top().times(perspectiveMatrix));

        return perspectiveMatrix;
    }

    public static Mat4 perspective(float fovDeg, float zNear, float zFar) {

        float frustumScale = calculatFrustumScale(fovDeg);

        Mat4 perspectiveMatrix = new Mat4();

        perspectiveMatrix.c0.x = frustumScale;
        perspectiveMatrix.c1.y = frustumScale;
        perspectiveMatrix.c2.z = (zFar + zNear) / (zNear - zFar);
        perspectiveMatrix.c2.w = -1.0f;
        perspectiveMatrix.c3.z = (2 * zFar * zNear) / (zNear - zFar);

//        matrices.set(matrices.size() - 1, perspectiveMatrix);
//        setTop(top().times(perspectiveMatrix));

        return perspectiveMatrix;
    }

    public static Mat4 orthographic(float left, float right, float bottom, float top, float nearVal, float farVal) {

        Mat4 orthographicMatric = new Mat4(1.0f);

        orthographicMatric.c0.x = 2 / (right - left);

        orthographicMatric.c1.y = 2 / (top - bottom);

        orthographicMatric.c2.z = -2 / (farVal - nearVal);

        orthographicMatric.c3.x = -(right + left) / (right - left);

        orthographicMatric.c3.y = -(top + bottom) / (top - bottom);

        orthographicMatric.c3.z = -(farVal + nearVal) / (farVal - nearVal);
        
        return orthographicMatric;
    }
    
    public static Mat4 orthographic2D(float left, float right, float bottom, float top) {

        return orthographic(left, right, bottom, top, -1.0f, 1.0f);
    }
}