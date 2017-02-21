///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut11.blinnVsPhongLighting;
//
///**
// *
// * @author elect
// */
//public class MaterialParameters {
//
//    private static float phongExponent = 4.0f;
//    private static float blinnExponent = 4.0f;
//
//    static float getSpecularValue(BlinnVsPhongLighting.LightingModel model) {
//
//        switch (model) {
//
//            case PhongSpecular:
//            case PhongOnly:
//                return phongExponent;
//
//            default:
//                return blinnExponent;
//        }
//    }
//
//    static void increment(BlinnVsPhongLighting.LightingModel model, boolean isLarge) {
//
//        switch (model) {
//
//            case PhongSpecular:
//            case PhongOnly:
//                phongExponent += isLarge ? 0.5f : 0.1f;
//                break;
//
//            default:
//                blinnExponent += isLarge ? 0.5f : 0.1f;
//                break;
//        }
//
//        clampParam(model);
//    }
//
//    static void decrement(BlinnVsPhongLighting.LightingModel model, boolean isLarge) {
//
//        switch (model) {
//
//            case PhongSpecular:
//            case PhongOnly:
//                phongExponent -= isLarge ? 0.5f : 0.1f;
//                break;
//
//            default:
//                blinnExponent -= isLarge ? 0.5f : 0.1f;
//                break;
//        }
//
//        clampParam(model);
//    }
//
//    private static void clampParam(BlinnVsPhongLighting.LightingModel model) {
//
//        switch (model) {
//
//            case PhongSpecular:
//            case PhongOnly:
//                if (phongExponent <= 0.0f) {
//                    phongExponent = 0.0001f;
//                }
//                break;
//
//            default:
//                if (blinnExponent <= 0.0f) {
//                    blinnExponent = 0.0001f;
//                }
//                break;
//        }
//    }
//}
