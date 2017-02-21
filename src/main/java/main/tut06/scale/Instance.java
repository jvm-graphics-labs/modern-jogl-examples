///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut06.scale;
//
//import glm.Glm;
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
//    private Scale.Mode mode;
//    private Vec3 offset;
//    private Vec3 vec = new Vec3();
//
//    public Instance(Scale.Mode mode, Vec3 offset) {
//        this.mode = mode;
//        this.offset = offset;
//    }
//
//    public Mat4 constructMatrix(float elapsedTime) {
//
//        Vec3 theScale = calcScale(elapsedTime);
//        Mat4 theMat = new Mat4(1.0f);
//        theMat.set(theScale);
//        theMat.c3(new Vec4(offset, 1.0f));
//
//        return theMat;
//    }
//
//    private Vec3 calcScale(float elapsedTime) {
//
//        switch (mode) {
//
//            default:
//                return vec.set(1.0f);
//
//            case StaticUniformScale:
//                return vec.set(4.0f);
//
//            case StaticNonUniformScale:
//                return vec.set(0.5f, 1.0f, 10.0f);
//
//            case DynamicUniformScale:
//                final float loopDuration = 3.0f;
//                return new Vec3(Glm.mix(1.0f, 4.0f, calculateLerpFactor(elapsedTime, loopDuration)));
//
//            case DynamicNonUniformScale:
//                final float xLoopDuration = 3.0f;
//                final float zLoopDuration = 5.0f;
//                return new Vec3(
//                        Glm.mix(1.0f, 0.5f, calculateLerpFactor(elapsedTime, xLoopDuration)),
//                        1.0f,
//                        Glm.mix(1.0f, 10.0f, calculateLerpFactor(elapsedTime, zLoopDuration)));
//        }
//    }
//
//    private float calculateLerpFactor(float elapsedTime, float loopDuration) {
//        float value = elapsedTime % loopDuration / loopDuration;
//        if (value > 0.5f) {
//            value = 1.0f - value;
//        }
//        return value * 2.0f;
//    }
//}
