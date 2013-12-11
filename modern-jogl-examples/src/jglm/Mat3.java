/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

/**
 *
 * @author gbarbieri
 */
public class Mat3 extends Mat {

    public Vec3 c0;
    public Vec3 c1;
    public Vec3 c2;

    public Mat3() {
        order = 3;

        c0 = new Vec3();
        c1 = new Vec3();
        c2 = new Vec3();
    }

    public Mat3(float value) {

        this();

        for (int i = 0; i < order; i++) {
            set(i * (order + 1), value);
        }
    }

    public Mat3(Mat4 mat4) {

        this();

        c0 = new Vec3(mat4.c0.x, mat4.c0.y, mat4.c0.z);
        c1 = new Vec3(mat4.c1.x, mat4.c1.y, mat4.c1.z);
        c2 = new Vec3(mat4.c2.x, mat4.c2.y, mat4.c2.z);
    }
    
    public Mat3(Vec3 v0, Vec3 v1, Vec3 v2) {

        this();

        c0 = v0;
        c1 = v1;
        c2 = v2;
    }

    public float[] toFloatArray() {
        
        return new float[]{
            c0.x, c0.y, c0.z,
            c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z};
    }

    public void setDiagonal(Vec3 vec3) {
        c0.x = vec3.x;
        c1.y = vec3.y;
        c2.z = vec3.z;
    }

    public static Mat3 rotateX(float fAngDeg) {
        float fAngRad = (float) Math.toRadians(fAngDeg);
        float fCos = (float) Math.cos(fAngRad);
        float fSin = (float) Math.sin(fAngRad);

        Mat3 mat3 = new Mat3(1.0f);
        mat3.c1.y = fCos;
        mat3.c1.z = fSin;
        mat3.c2.y = -fSin;
        mat3.c2.z = fCos;

        return mat3;
    }

    public static Mat3 rotateY(float fAngDeg) {
        float fAngRad = (float) Math.toRadians(fAngDeg);
        float fCos = (float) Math.cos(fAngRad);
        float fSin = (float) Math.sin(fAngRad);

        Mat3 mat3 = new Mat3(1.0f);
        mat3.c0.x = fCos;
        mat3.c0.z = -fSin;
        mat3.c2.x = fSin;
        mat3.c2.z = fCos;

        return mat3;
    }

    public static Mat3 rotateZ(float fAngDeg) {
        float fAngRad = (float) Math.toRadians(fAngDeg);
        float fCos = (float) Math.cos(fAngRad);
        float fSin = (float) Math.sin(fAngRad);

        Mat3 mat3 = new Mat3(1.0f);
        mat3.c0.x = fCos;
        mat3.c0.y = fSin;
        mat3.c1.x = -fSin;
        mat3.c1.y = fCos;

        return mat3;
    }
    
    public Mat3 inverse(){
        
        Mat3 inverse = new Mat3();
        
        inverse.c0.x = + (c1.y * c2.z - c2.y * c1.z);
        inverse.c1.x = - (c1.x * c2.z - c2.x * c1.z);
        inverse.c2.x = + (c1.x * c2.y - c2.x * c1.y);
        inverse.c0.y = - (c0.y * c2.z - c2.y * c0.z);
        inverse.c1.y = + (c0.x * c2.z - c2.x * c0.z);
        inverse.c2.y = - (c0.x * c2.y - c2.x * c0.y);
        inverse.c0.z = + (c0.y * c1.z - c1.y * c0.z);
        inverse.c1.z = - (c0.x * c1.z - c1.x * c0.z);
        inverse.c2.z = + (c0.x * c1.y - c1.x * c0.y);
        
        inverse.divide(determinant());
        
        return inverse;
    }
    
    public Mat3 divide(float s) {

        Vec3 newC0 = new Vec3(c0.x / s, c0.y / s, c0.z / s);
        Vec3 newC1 = new Vec3(c1.x / s, c1.y / s, c1.z / s);
        Vec3 newC2 = new Vec3(c2.x / s, c2.y / s, c2.z / s);

        return new Mat3(newC0, newC1, newC2);
    }
    
    public float determinant(){
        
        return c0.x * (c1.y * c2.z - c2.y * c1.z) - c1.x * (c0.y * c2.z - c2.y * c0.z) + c2.x * (c0.y * c1.z - c1.y * c0.z);
    }
    
    public Mat3 transpose(){
        
        Mat3 result = new Mat3();
        
        result.c0.x = c0.x;
        result.c0.y = c1.x;
        result.c0.z = c2.x;
        
        result.c1.x = c0.y;
        result.c1.y = c1.y;
        result.c1.z = c2.y;
        
        result.c2.x = c0.z;
        result.c2.y = c1.z;
        result.c2.z = c2.z;
        
        return result;
    }

    public final void set(int index, float value) {
        switch (index) {
            case 0:
                c0.x = value;
                break;
            case 1:
                c0.y = value;
                break;
            case 2:
                c0.z = value;
                break;
            case 3:
                c1.x = value;
                break;
            case 4:
                c1.y = value;
                break;
            case 5:
                c1.z = value;
                break;
            case 6:
                c2.x = value;
                break;
            case 7:
                c2.y = value;
                break;
            case 8:
                c2.z = value;
                break;
        }
    }

    public final float get(int index) {
        switch (index) {
            case 0:
                return c0.x;
            case 1:
                return c0.y;
            case 2:
                return c0.z;
            case 3:
                return c1.x;
            case 4:
                return c1.y;
            case 5:
                return c1.z;
            case 6:
                return c2.x;
            case 7:
                return c1.y;
            case 8:
                return c1.z;
        }
        return -1;
    }

    public void print() {
        System.out.println(c0.x + " " + c1.x + " " + c2.x + "\n");
        System.out.println(c0.y + " " + c1.y + " " + c2.y + "\n");
        System.out.println(c0.z + " " + c1.z + " " + c2.z + "\n");
    }

    public void print(String title) {
        System.out.println("" + title);
        System.out.println(c0.x + " " + c1.x + " " + c2.x + "\n");
        System.out.println(c0.y + " " + c1.y + " " + c2.y + "\n");
        System.out.println(c0.z + " " + c1.z + " " + c2.z + "\n");
    }
}
