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
    private int cameraToClipMatUnLoc;
    private int dirToLightUnLoc;
    private int lightIntensityUnLoc;
    private int projectionUBI;

    public GLSLProgramObject_1(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionBlockIndex) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToCameraMatUnLoc = gl3.glGetUniformLocation(getProgId(), "modelToCameraMatrix");
        normalModelToCameraMatUnLoc = gl3.glGetUniformLocation(getProgId(), "normalModelToCameraMatrix");
        cameraToClipMatUnLoc = gl3.glGetUniformLocation(getProgId(), "cameraToClipMatrix");

        dirToLightUnLoc = gl3.glGetUniformLocation(getProgId(), "dirToLight");
        lightIntensityUnLoc = gl3.glGetUniformLocation(getProgId(), "lightIntensity");

        projectionUBI = gl3.glGetUniformBlockIndex(getProgId(), "Projection");
        gl3.glUniformBlockBinding(getProgId(), projectionUBI, projectionBlockIndex);
    }

    public int getModelToCameraMatUnLoc() {
        return modelToCameraMatUnLoc;
    }

    public int getCameraToClipMatUnLoc() {
        return cameraToClipMatUnLoc;
    }

    public void setModelToCameraMatUnLoc(int modelToCameraMatUnLoc) {
        this.modelToCameraMatUnLoc = modelToCameraMatUnLoc;
    }

    public void setCameraToClipMatUnLoc(int cameraToClipMatUnLoc) {
        this.cameraToClipMatUnLoc = cameraToClipMatUnLoc;
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
