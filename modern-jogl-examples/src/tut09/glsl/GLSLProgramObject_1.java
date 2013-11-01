/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.glsl;

import glsl.GLSLProgramObject;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class GLSLProgramObject_1 extends GLSLProgramObject {

    private int modelToCameraMatUnLoc;
    private int normalModelToCameraMatUnLoc;
    private int dirToLightUnLoc;
    private int lightIntensityUnLoc;

    public GLSLProgramObject_1(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionBlockIndex) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToCameraMatUnLoc = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        normalModelToCameraMatUnLoc = gl3.glGetUniformLocation(getProgramId(), "normalModelToCameraMatrix");

        dirToLightUnLoc = gl3.glGetUniformLocation(getProgramId(), "dirToLight");
        lightIntensityUnLoc = gl3.glGetUniformLocation(getProgramId(), "lightIntensity");

        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionBlockIndex);
    }

    public int getModelToCameraMatUnLoc() {
        return modelToCameraMatUnLoc;
    }

    public void setModelToCameraMatUnLoc(int modelToCameraMatUnLoc) {
        this.modelToCameraMatUnLoc = modelToCameraMatUnLoc;
    }

    public int getDirToLightUnLoc() {
        return dirToLightUnLoc;
    }

    public int getNormalModelToCameraMatUnLoc() {
        return normalModelToCameraMatUnLoc;
    }

    public int getLightIntensityUnLoc() {
        return lightIntensityUnLoc;
    }
}