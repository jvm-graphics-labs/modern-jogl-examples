/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

/**
 *
 * @author gbarbieri
 */
public class Vec3 extends Vec {

    public float x;
    public float y;
    public float z;

    public Vec3() {
    }

    public Vec3(float[] floatArray) {
        vector = floatArray;
        x = vector[0];
        y = vector[1];
        z = vector[2];
    }

    public Vec3(float x, float y, float z) {

        this.x = x;
        this.y = y;
        this.z = z;
        vector = new float[]{x, y, z};
    }

    public Vec3(float[] floatArray, int i) {

        vector = new float[]{floatArray[i], floatArray[i + 1], floatArray[i + 2]};
        x = vector[0];
        y = vector[1];
        z = vector[2];
    }
    
    public Vec3(Vec4 vec4){
        
        x = vec4.x;
        y = vec4.y;
        z = vec4.z;
        vector = new float[]{x, y, z};
    }

    public Vec3 normalize() {

        float length = ((float) Math.sqrt(x * x + y * y + z * z));

        return new Vec3(x / length, y / length, z / length);
    }

    public Vec3 times(float scalar) {

        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    public Vec3 times(Quat quat) {
        
        Quat inverse = quat.conjugate();
        
        return inverse.mult(this);
    }

    public Vec3 plus(Vec3 vec3) {

        return new Vec3(x + vec3.x, y + vec3.y, z + vec3.z);
    }

    public Vec3 minus(Vec3 vec3) {

        return new Vec3(x - vec3.x, y - vec3.y, z - vec3.z);
    }

    public Vec3 crossProduct(Vec3 vec3) {

        return new Vec3(y * vec3.z - z * vec3.y, z * vec3.x - x * vec3.z, x * vec3.y - y * vec3.x);
    }

    public Vec3 negated() {

        return new Vec3(-x, -y, -z);
    }

    public float length() {
        
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public void print() {
        System.out.println("(" + x + ", " + y + ", " + z + ")");
    }

    public void print(String title) {
        System.out.println(title + " (" + x + ", " + y + ", " + z + ")");
    }
    
    public float[] toFloatArray(){
        
        return new float[]{x, y, z};
    }
}
