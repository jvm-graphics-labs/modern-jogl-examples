/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jglm;

/**
 *
 * @author gbarbieri
 */
public class Mat4 extends Mat {

    public Vec4 c0;
    public Vec4 c1;
    public Vec4 c2;
    public Vec4 c3;

    public Mat4() {

        order = 4;

        c0 = new Vec4();
        c1 = new Vec4();
        c2 = new Vec4();
        c3 = new Vec4();
//        c0 = new Vec4(matrix, 0);
//        c1 = new Vec4(matrix, order);
//        c2 = new Vec4(matrix, order * 2);
//        c3 = new Vec4(matrix, order * 3);
    }

    public Mat4(float value) {

        this();

        for (int i = 0; i < order; i++) {
            set(i * (order + 1), value);
        }

//        c0 = new Vec4(matrix, 0);
//        c1 = new Vec4(matrix, order);
//        c2 = new Vec4(matrix, order * 2);
//        c3 = new Vec4(matrix, order * 3);
    }

    public Mat4(Mat3 mat3) {

        order = 4;

//        for (int i = 0; i < mat3.order; i++) {
//            for (int j = 0; j < mat3.order; j++) {
//                matrix[j + i * order] = mat3.toFloatArray()[j + i * mat3.order];
//            }
//        }

        c0 = new Vec4(mat3.c0, 0.0f);
        c1 = new Vec4(mat3.c1, 0.0f);
        c2 = new Vec4(mat3.c2, 0.0f);
        c3 = new Vec4(new Vec3(), 1.0f);
    }

    public Mat4(float[] floatArray) {

        order = 4;

        c0 = new Vec4(floatArray, 0);
        c1 = new Vec4(floatArray, order);
        c2 = new Vec4(floatArray, order * 2);
        c3 = new Vec4(floatArray, order * 3);
    }

    public Mat4(Vec3 vec3) {
    }

    public float[] toFloatArray() {
        return new float[]{
            c0.x, c0.y, c0.z, c0.w,
            c1.x, c1.y, c1.z, c1.w,
            c2.x, c2.y, c2.z, c2.w,
            c3.x, c3.y, c3.z, c3.w,};
    }

    public void setDiagonal(Vec3 vec3) {
        c0.x = vec3.x;
        c1.y = vec3.y;
        c2.z = vec3.z;
    }

    public Mat4 times(Mat4 second) {
        float[] newMatrix = new float[16];
        float partial;

//        System.out.println("this: ");
//        print();
//        System.out.println("second: ");
//        second.print();

        for (int i = 0; i < order; i++) {
//            System.out.println("i: " + i);
            for (int j = 0; j < order; j++) {
//                System.out.println("j: " + j);
                partial = 0;

                for (int k = 0; k < order; k++) {
                    partial += this.toFloatArray()[4 * k + j] * second.toFloatArray()[4 * i + k];
//                    System.out.println("k: " + k + " first: " + this.toFloatArray()[4 * k + j] + " second: " + second.toFloatArray()[4 * i + k] + " = "
//                            + (this.toFloatArray()[4 * k + j] * second.toFloatArray()[4 * i + k]) + " partial: " + partial);
                }

                newMatrix[4 * i + j] = partial;
            }
        }

        return new Mat4(newMatrix);
    }

    public Mat4 transpose() {

        float[] transposed = new float[]{
            c0.x, c1.x, c2.x, c3.x,
            c0.y, c1.y, c2.y, c3.y,
            c0.z, c1.z, c2.z, c3.z,
            c0.w, c1.w, c2.w, c3.w};

        return new Mat4(transposed);
    }

    public Quat toQuaternion() {

        float trace;
        float s;
        float x;
        float y;
        float z;
        float w;

//        trace = c0.x + c1.y + c2.z + 1;
//
//        if (trace > 0) {
//
//            s = 0.5f / (float) Math.sqrt(trace);
//
//            x = (c2.y - c1.z) * s;
//
//            y = (c0.z - c2.x) * s;
//
//            z = (c1.x - c0.y) * s;
//
//            w = 0.25f / s;
//
//        } else if (c0.x > c1.y && c0.x > c2.z) {
//
//            s = (float) Math.sqrt(1.0f + c0.x - c1.y - c2.z) * 2;
//
//            x = 0.5f / s;
//
//            y = (c0.y + c1.x) / s;
//
//            z = (c0.z + c2.x) / s;
//
//            w = (c1.z + c2.y) / s;
//
//        } else if (c1.y > c2.z) {
//
//            s = (float) Math.sqrt(1.0f + c1.y - c0.x - c2.z) * 2;
//
//            x = (c0.y + c1.x) / s;
//
//            y = 0.5f / s;
//
//            z = (c1.z + c2.y) / s;
//
//            w = (c0.z + c2.x) / s;
//
//        } else {
//
//            s = (float) Math.sqrt(1.0f + c2.z - c0.x - c1.y) * 2;
//
//            x = (c0.z + c2.x) / s;
//
//            y = (c1.z + c2.y) / s;
//
//            z = 0.5f / s;
//
//            w = (c0.y + c1.x) / s;
//        }

        trace = c0.x + c1.y + c2.z + 1;

        if (trace > 0) {

            s = 0.5f / (float) Math.sqrt(trace);

            x = (c1.z - c2.y) * s;

            y = (c2.x - c0.z) * s;

            z = (c0.y - c1.x) * s;

            w = 0.25f / s;

        } else if (c0.x > c1.y && c0.x > c2.z) {

            s = (float) Math.sqrt(1.0f + c0.x - c1.y - c2.z) * 2;

            x = 0.5f / s;

            y = (c1.x + c0.y) / s;

            z = (c2.x + c0.z) / s;

            w = (c2.y + c1.z) / s;

        } else if (c1.y > c2.z) {

            s = (float) Math.sqrt(1.0f + c1.y - c0.x - c2.z) * 2;

            x = (c1.x + c0.y) / s;

            y = 0.5f / s;

            z = (c2.y + c1.z) / s;

            w = (c2.x + c0.z) / s;

        } else {

            s = (float) Math.sqrt(1.0f + c2.z - c0.x - c1.y) * 2;

            x = (c2.x + c0.z) / s;

            y = (c2.y + c1.z) / s;

            z = 0.5f / s;

            w = (c1.x + c0.y) / s;
        }

        
        Quat quat = new Quat(x, y, z, w);
        
        quat.normalize();
        
        return quat;
    }
    
    public static Mat4 CalcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.minus(cameraPt);
        lookDir = lookDir.normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 crossProduct = lookDir.crossProduct(upDir);
        Vec3 rightDir = crossProduct.normalize();
        Vec3 perpUpDir = rightDir.crossProduct(lookDir);

        Mat4 rotationMat = new Mat4(1.0f);
        rotationMat.c0 = new Vec4(rightDir, 0.0f);
        rotationMat.c1 = new Vec4(perpUpDir, 0.0f);
        rotationMat.c2 = new Vec4(lookDir.negated(), 0.0f);

        rotationMat = rotationMat.transpose();

        Mat4 translationMat = new Mat4(1.0f);
        translationMat.c3 = new Vec4(cameraPt.negated(), 1.0f);

        return rotationMat.times(translationMat);
    }

    public void print() {
        System.out.println(c0.x + " " + c1.x + " " + c2.x + " " + c3.x + "\n");
        System.out.println(c0.y + " " + c1.y + " " + c2.y + " " + c3.y + "\n");
        System.out.println(c0.z + " " + c1.z + " " + c2.z + " " + c3.z + "\n");
        System.out.println(c0.w + " " + c1.w + " " + c2.w + " " + c3.w + "\n");
    }

    public void print(String title) {
        System.out.println("" + title);
        System.out.println(c0.x + " " + c1.x + " " + c2.x + " " + c3.x + "\n");
        System.out.println(c0.y + " " + c1.y + " " + c2.y + " " + c3.y + "\n");
        System.out.println(c0.z + " " + c1.z + " " + c2.z + " " + c3.z + "\n");
        System.out.println(c0.w + " " + c1.w + " " + c2.w + " " + c3.w + "\n");
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
                c0.w = value;
                break;
            case 4:
                c1.x = value;
                break;
            case 5:
                c1.y = value;
                break;
            case 6:
                c1.z = value;
                break;
            case 7:
                c1.w = value;
                break;
            case 8:
                c2.x = value;
                break;
            case 9:
                c2.y = value;
                break;
            case 10:
                c2.z = value;
                break;
            case 11:
                c2.w = value;
                break;
            case 12:
                c3.x = value;
                break;
            case 13:
                c3.y = value;
                break;
            case 14:
                c3.z = value;
                break;
            case 15:
                c3.w = value;
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
                return c0.w;
            case 4:
                return c1.x;
            case 5:
                return c1.y;
            case 6:
                return c1.z;
            case 7:
                return c1.w;
            case 8:
                return c2.x;
            case 9:
                return c2.y;
            case 10:
                return c2.z;
            case 11:
                return c2.w;
            case 12:
                return c3.x;
            case 13:
                return c3.y;
            case 14:
                return c3.z;
            case 15:
                return c3.w;
        }
        return -1;
    }
}