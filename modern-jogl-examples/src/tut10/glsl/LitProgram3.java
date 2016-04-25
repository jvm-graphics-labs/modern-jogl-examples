/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut10.glsl;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class LitProgram3 extends framework.GLSLProgramObject {

    private int unLocModelToCameraMatrix;
    private int unLocNormalModelToCameraMatrix;
    private int unLocLightPositionCameraSpace;
    private int unLocLightDiffuseIntensity;
    private int unLocLightAmbientIntensity;
    private int unLocLightAttenuation;
    private int unLocRsquare;

    public LitProgram3(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionUBB, int unProjectionUBB) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        unLocModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        unLocNormalModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "normalModelToCameraMatrix");

//        System.out.println("unLocModelToCameraMatrix: " + unLocModelToCameraMatrix);
//        System.out.println("unLocNormalModelToCameraMatrix: " + unLocNormalModelToCameraMatrix);

        unLocLightPositionCameraSpace = gl3.glGetUniformLocation(getProgramId(), "lightPositionCameraSpace");
        unLocLightDiffuseIntensity = gl3.glGetUniformLocation(getProgramId(), "lightDiffuseIntensity");
        unLocLightAmbientIntensity = gl3.glGetUniformLocation(getProgramId(), "lightAmbientIntensity");

//        System.out.println("unLocLightPositionCameraSpace: " + unLocLightPositionCameraSpace);
//        System.out.println("unLocLightDiffuseIntensity: " + unLocLightDiffuseIntensity);
//        System.out.println("unLocLightAmbientIntensity: " + unLocLightAmbientIntensity);

        unLocLightAttenuation = gl3.glGetUniformLocation(getProgramId(), "lightAttenuation");
        unLocRsquare = gl3.glGetUniformLocation(getProgramId(), "rSquare");

//        System.out.println("unLocLightAttenuation: " + unLocLightAttenuation);
//        System.out.println("unLocRsquare: " + unLocRsquare);

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);

        int unProjectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "UnProjection");
        gl3.glUniformBlockBinding(getProgramId(), unProjectionUBI, unProjectionUBB);
    }

    public int getUnLocModelToCameraMatrix() {
        return unLocModelToCameraMatrix;
    }

    public int getUnLocLightPositionCameraSpace() {
        return unLocLightPositionCameraSpace;
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

    public int getUnLocLightAttenuation() {
        return unLocLightAttenuation;
    }

    public int getUnLocRsquare() {
        return unLocRsquare;
    }
}
