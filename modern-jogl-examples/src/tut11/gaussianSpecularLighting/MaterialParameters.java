/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut11.gaussianSpecularLighting;

/**
 *
 * @author elect
 */
public class MaterialParameters {

    private static float phongExponent = 4.0f;
    private static float blinnExponent = 4.0f;
    private static float gaussianRoughness = 0.5f;

    static float getSpecularValue(GaussianSpecularLighting.LightingModel model) {

        switch (model) {

            case PhongSpecular:
            case PhongOnly:
                return phongExponent;

            case BlinnSpecular:
            case BlinnOnly:
                return blinnExponent;
                
            default:
                return gaussianRoughness;
        }
    }

    static void increment(GaussianSpecularLighting.LightingModel model, boolean isLarge) {

        switch (model) {

            case PhongSpecular:
            case PhongOnly:
                phongExponent += isLarge ? 0.5f : 0.1f;
                break;

            case BlinnSpecular:
            case BlinnOnly:
                blinnExponent += isLarge ? 0.5f : 0.1f;
                break;

            default:
                gaussianRoughness += isLarge ? 0.1f : 0.01f;
                break;
        }

        clampParam(model);
    }

    static void decrement(GaussianSpecularLighting.LightingModel model, boolean isLarge) {

        switch (model) {

            case PhongSpecular:
            case PhongOnly:
                phongExponent -= isLarge ? 0.5f : 0.1f;
                break;

            case BlinnSpecular:
            case BlinnOnly:
                blinnExponent -= isLarge ? 0.5f : 0.1f;
                break;

            default:
                gaussianRoughness -= isLarge ? 0.1f : 0.01f;
                break;
        }

        clampParam(model);
    }

    private static void clampParam(GaussianSpecularLighting.LightingModel model) {

        switch (model) {

            case PhongSpecular:
            case PhongOnly:
                if (phongExponent <= 0.0f) {
                    phongExponent = 0.0001f;
                }
                break;

            case BlinnSpecular:
            case BlinnOnly:
                if (blinnExponent <= 0.0f) {
                    blinnExponent = 0.0001f;
                }
                break;

            default:
                gaussianRoughness = glm.glm.clamp(gaussianRoughness, 0.00001f, 1.0f);
                break;
        }
    }
}
