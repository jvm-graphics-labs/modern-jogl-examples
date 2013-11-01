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

    public Mat4(Vec4 v0, Vec4 v1, Vec4 v2, Vec4 v3) {

        order = 4;

        c0 = v0;
        c1 = v1;
        c2 = v2;
        c3 = v3;
    }

    public float[] toFloatArray() {
        return new float[]{
            c0.x, c0.y, c0.z, c0.w,
            c1.x, c1.y, c1.z, c1.w,
            c2.x, c2.y, c2.z, c2.w,
            c3.x, c3.y, c3.z, c3.w};
    }

    public void setDiagonal(Vec3 vec3) {
        c0.x = vec3.x;
        c1.y = vec3.y;
        c2.z = vec3.z;
    }

    public Mat4 mult(Mat4 second) {
        float[] result = new float[16];
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

                result[4 * i + j] = partial;
            }
        }

        return new Mat4(result);
    }

    public Vec4 mult(Vec4 second) {

        float[] result = new float[4];
        float partial;

        for (int i = 0; i < order; i++) {

            partial = 0;

            for (int j = 0; j < order; j++) {

                partial += toFloatArray()[4 * j + i] * second.toFloatArray()[j];
            }
            result[i] = partial;
        }

        return new Vec4(result);
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

        return rotationMat.mult(translationMat);
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

    public Mat4 inverse() {

        float coeff00 = c2.z * c3.w - c3.z * c2.w;
        float coeff02 = c1.z * c3.w - c3.z * c1.w;
        float coeff03 = c1.z * c2.w - c2.z * c1.w;

        float coeff04 = c2.y * c3.w - c3.y * c2.w;
        float coeff06 = c1.y * c3.w - c3.y * c1.w;
        float coeff07 = c1.y * c2.w - c2.y * c1.w;

        float coeff08 = c2.y * c3.z - c3.y * c2.z;
        float coeff10 = c1.y * c3.z - c3.y * c1.z;
        float coeff11 = c1.y * c2.z - c2.y * c1.z;

        float coeff12 = c2.x * c3.w - c3.x * c2.w;
        float coeff14 = c1.x * c3.w - c3.x * c1.w;
        float coeff15 = c1.x * c2.w - c2.x * c1.w;

        float coeff16 = c2.x * c3.z - c3.x * c2.z;
        float coeff18 = c1.x * c3.z - c3.x * c1.z;
        float coeff19 = c1.x * c2.z - c2.x * c1.z;

        float coeff20 = c2.x * c3.y - c3.x * c2.y;
        float coeff22 = c1.x * c3.y - c3.x * c1.y;
        float coeff23 = c1.x * c2.y - c2.x * c1.y;

        Vec4 signA = new Vec4(1.0f, -1.0f, 1.0f, -1.0f);
        Vec4 signB = new Vec4(-1.0f, 1.0f, -1.0f, 1.0f);

        Vec4 fac0 = new Vec4(coeff00, coeff00, coeff02, coeff03);
        Vec4 fac1 = new Vec4(coeff04, coeff04, coeff06, coeff07);
        Vec4 fac2 = new Vec4(coeff08, coeff08, coeff10, coeff11);
        Vec4 fac3 = new Vec4(coeff12, coeff12, coeff14, coeff15);
        Vec4 fac4 = new Vec4(coeff16, coeff16, coeff18, coeff19);
        Vec4 fac5 = new Vec4(coeff20, coeff20, coeff22, coeff23);

        Vec4 vec0 = new Vec4(c1.x, c0.x, c0.x, c0.x);
        Vec4 vec1 = new Vec4(c1.y, c0.y, c0.y, c0.y);
        Vec4 vec2 = new Vec4(c1.z, c0.z, c0.z, c0.z);
        Vec4 vec3 = new Vec4(c1.w, c0.w, c0.w, c0.w);

        Vec4 one = vec1.mult(fac0);
        Vec4 two = vec2.mult(fac1);
        Vec4 three = vec3.mult(fac2);
        Vec4 inv0 = signA.mult(one.minus(two).plus(three));

        one = vec0.mult(fac0);
        two = vec2.mult(fac3);
        three = vec3.mult(fac4);
        Vec4 inv1 = signB.mult(one.minus(two).plus(three));

        one = vec0.mult(fac1);
        two = vec1.mult(fac3);
        three = vec3.mult(fac5);
        Vec4 inv2 = signA.mult(one.minus(two).plus(three));

        one = vec0.mult(fac2);
        two = vec1.mult(fac4);
        three = vec2.mult(fac5);
        Vec4 inv3 = signB.mult(one.minus(two).plus(three));

//        inv0.print("inv0");
//        inv1.print("inv1");
//        inv2.print("inv2");
//        inv3.print("inv3");

        Mat4 inverse = new Mat4(inv0, inv1, inv2, inv3);

//        inverse.print("inverse");

        Vec4 row0 = new Vec4(inverse.c0.x, inverse.c1.x, inverse.c2.x, inverse.c3.x);

//        row0.print("row0");

        float determinant = Jglm.dot(c0, row0);

//        System.out.println("det: "+determinant);

        return inverse.divide(determinant);
    }

    public Mat4 divide(float s) {

        Vec4 newC0 = new Vec4(c0.x / s, c0.y / s, c0.z / s, c0.w / s);
        Vec4 newC1 = new Vec4(c1.x / s, c1.y / s, c1.z / s, c1.w / s);
        Vec4 newC2 = new Vec4(c2.x / s, c2.y / s, c2.z / s, c2.w / s);
        Vec4 newC3 = new Vec4(c3.x / s, c3.y / s, c3.z / s, c3.w / s);

        return new Mat4(newC0, newC1, newC2, newC3);
    }
}