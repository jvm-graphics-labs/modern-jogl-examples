///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut06.rotations;
//
//import glm.mat._3.Mat3;
//import glm.mat._4.Mat4;
//import glm.vec._3.Vec3;
//import glm.vec._4.Vec4;
//
///**
// *
// * @author GBarbieri
// */
//public class Instance {
//
//    private Rotations.Mode mode;
//    private Vec3 offset;
//    private Mat3 theMat = new Mat3();
//
//    public Instance(Rotations.Mode mode, Vec3 offset) {
//        this.mode = mode;
//        this.offset = offset;
//    }
//
//    public Mat4 constructMatrix(float elapsedTime) {
//
//        Mat3 rotMatrix = calcRotation(elapsedTime);
//        Mat4 theMat = new Mat4(rotMatrix);
//        theMat.c3(new Vec4(offset, 1.0f));
//
//        return theMat;
//    }
//
//    private Mat3 calcRotation(float elapsedTime) {
//
//        float angRad, cos, sin;
//
//        switch (mode) {
//
//            default:
//                return theMat.identity();
//
//            case RotateX:
//
//                angRad = computeAngleRad(elapsedTime, 3.0f);
//                cos = (float) Math.cos(angRad);
//                sin = (float) Math.sin(angRad);
//
//                theMat.identity();
//                theMat.m11 = cos;
//                theMat.m12 = sin;
//                theMat.m21 = -sin;
//                theMat.m22 = cos;
//                return theMat;
//
//            case RotateY:
//
//                angRad = computeAngleRad(elapsedTime, 2.0f);
//                cos = (float) Math.cos(angRad);
//                sin = (float) Math.sin(angRad);
//
//                theMat.identity();
//                theMat.m00 = cos;
//                theMat.m02 = -sin;
//                theMat.m20 = sin;
//                theMat.m22 = cos;
//                return theMat;
//
//            case RotateZ:
//
//                angRad = computeAngleRad(elapsedTime, 2.0f);
//                cos = (float) Math.cos(angRad);
//                sin = (float) Math.sin(angRad);
//
//                theMat.identity();
//                theMat.m00 = cos;
//                theMat.m01 = sin;
//                theMat.m10 = -sin;
//                theMat.m11 = cos;
//                return theMat;
//
//            case RotateAxis:
//
//                angRad = computeAngleRad(elapsedTime, 2.0f);
//                cos = (float) Math.cos(angRad);
//                sin = (float) Math.sin(angRad);
//                float invCos = 1.0f - cos,
//                 invSin = 1.0f - sin;
//
//                Vec3 axis = new Vec3(1.0f).normalize();
//                theMat.identity();
//
//                theMat.m00 = (axis.x * axis.x) + ((1 - axis.x * axis.x) * cos);
//                theMat.m10 = axis.x * axis.y * (invCos) - (axis.z * sin);
//                theMat.m20 = axis.x * axis.z * (invCos) + (axis.y * sin);
//
//                theMat.m01 = axis.x * axis.y * (invCos) + (axis.z * sin);
//                theMat.m11 = (axis.y * axis.y) + ((1 - axis.y * axis.y) * cos);
//                theMat.m21 = axis.y * axis.z * (invCos) - (axis.x * sin);
//
//                theMat.m02 = axis.x * axis.z * (invCos) - (axis.y * sin);
//                theMat.m12 = axis.y * axis.z * (invCos) + (axis.x * sin);
//                theMat.m22 = (axis.z * axis.z) + ((1 - axis.z * axis.z) * cos);
//
//                return theMat;
//        }
//    }
//
//    private float computeAngleRad(float elapsedTime, float loopDuration) {
//        float scale = (float) (Math.PI * 2.0f / loopDuration);
//        float currentTimeThroughLoop = elapsedTime % loopDuration;
//        return currentTimeThroughLoop * scale;
//    }
//}
