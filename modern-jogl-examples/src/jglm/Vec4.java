/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

/**
 *
 * @author gbarbieri
 */
public class Vec4 extends Vec {

    public float x;
    public float y;
    public float z;
    public float w;

    public Vec4() {
        
        super();
    }

    public Vec4(float[] vec4) {
        vector = vec4;
        x = vector[0];
        y = vector[1];
        z = vector[2];
        w = vector[3];
    }
    
    public Vec4(float value) {
        vector = new float[]{value, value, value, value};
        x = vector[0];
        y = vector[1];
        z = vector[2];
        w = vector[3];
    }
    
     public Vec4(int[] floatArray) {
        
        x = floatArray[0];
        y = floatArray[1];
        z = floatArray[2];
        w = floatArray[3];
        
        vector = new float[]{x, y, z, w};
    }

    public Vec4(Vec3 vec3, float w) {
        x = vec3.x;
        y = vec3.y;
        z = vec3.z;
        this.w = w;
        vector = new float[]{x, y, z, this.w};
    }

    public Vec4(float[] floatArray, int i) {
        vector = new float[]{floatArray[i], floatArray[i + 1], floatArray[i + 2], floatArray[i + 3]};
        x = vector[0];
        y = vector[1];
        z = vector[2];
        w = vector[3];
    }

    public Vec4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        vector = new float[]{this.x, this.y, this.z, this.w};
    }

    public void print() {
        System.out.println("(" + x + ", " + y + ", " + z + ", " + w + ")");
    }

    public void print(String title) {
        System.out.println(title + " (" + x + ", " + y + ", " + z + ", " + w + ")");
    }

    public Vec4 mult(float scalar) {

        return new Vec4(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    public Vec4 mult(Vec4 vec4) {

        return new Vec4(x * vec4.x, y * vec4.y, z * vec4.z, w * vec4.w);
    }

    public Vec4 minus(Vec4 vec4) {

        return new Vec4(x - vec4.x, y - vec4.y, z - vec4.z, w - vec4.w);
    }

    public Vec4 minus(float scalar) {

        return new Vec4(x - scalar, y - scalar, z - scalar, w - scalar);
    }

    public Vec4 plus(Vec4 vec4) {

        return new Vec4(x + vec4.x, y + vec4.y, z + vec4.z, w + vec4.w);
    }

    public Vec4 divide(float scalar) {

        return new Vec4(x / scalar, y / scalar, z / scalar, w / scalar);
    }
    
    public float[] toFloatArray(){
        
        return new float[]{x, y, z, w};
    }
}