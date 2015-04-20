/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut11.phongLighting.glslProgram;

import com.jogamp.opengl.GL3;
import glsl.GLSLProgramObject;

/**
 *
 * @author gbarbieri
 */
public class LitProgram extends GLSLProgramObject {

    private int unLocModelToCameraMatrix;
    private int unLocLightDiffuseIntensity;
    private int unLocLightAmbientIntensity;
    private int unLocNormalModelToCameraMatrix;
    private int unLocLightCameraSpacePosition;
    private int unLocLightAttenuation;
    private int unLocShininessFactor;
    private int unLocBaseDiffuseColor;

    public LitProgram(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionUBB) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        unLocModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        unLocLightDiffuseIntensity = gl3.glGetUniformLocation(getProgramId(), "lightDiffuseIntensity");
        unLocLightAmbientIntensity = gl3.glGetUniformLocation(getProgramId(), "lightAmbientIntensity");

        unLocNormalModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "normalModelToCameraMatrix");
        unLocLightCameraSpacePosition = gl3.glGetUniformLocation(getProgramId(), "cameraSpaceLightPosition");
        unLocLightAttenuation = gl3.glGetUniformLocation(getProgramId(), "lightAttenuation");
        unLocShininessFactor = gl3.glGetUniformLocation(getProgramId(), "shininessFactor");
        unLocBaseDiffuseColor = gl3.glGetUniformLocation(getProgramId(), "baseDiffuseColor");

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);
    }

    public int getUnLocModelToCameraMatrix() {
        return unLocModelToCameraMatrix;
    }

    public int getUnLocLightDiffuseIntensity() {
        return unLocLightDiffuseIntensity;
    }

    public int getUnLocLightAmbientIntensity() {
        return unLocLightAmbientIntensity;
    }

    public int getUnLocNormalModelToCameraMatrix() {
        return unLocNormalModelToCameraMatrix;
    }

    public int getUnLocLightCameraSpacePosition() {
        return unLocLightCameraSpacePosition;
    }

    public int getUnLocLightAttenuation() {
        return unLocLightAttenuation;
    }

    public int getUnLocShininessFactor() {
        return unLocShininessFactor;
    }

    public int getUnLocBaseDiffuseColor() {
        return unLocBaseDiffuseColor;
    }
}
