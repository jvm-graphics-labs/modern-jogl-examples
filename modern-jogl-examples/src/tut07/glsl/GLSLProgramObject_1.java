/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.glsl;

import glsl.GLSLProgramObject;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class GLSLProgramObject_1 extends GLSLProgramObject {

    private int modelToWorldMatUnLoc;
    private int worldToCameraMatUnLoc;
    private int cameraToClipMatUnLoc;
    private int baseColorUnLoc;

    public GLSLProgramObject_1(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToWorldMatUnLoc = gl3.glGetUniformLocation(getProgId(), "modelToWorldMatrix");
        worldToCameraMatUnLoc = gl3.glGetUniformLocation(getProgId(), "worldToCameraMatrix");
        cameraToClipMatUnLoc = gl3.glGetUniformLocation(getProgId(), "cameraToClipMatrix");
        baseColorUnLoc = gl3.glGetUniformLocation(getProgId(), "baseColor");
    }

    public int getModelToWorldMatUnLoc() {
        return modelToWorldMatUnLoc;
    }

    public int getWorldToCameraMatUnLoc() {
        return worldToCameraMatUnLoc;
    }

    public int getCameraToClipMatUnLoc() {
        return cameraToClipMatUnLoc;
    }

    public int getBaseColorUnLoc() {
        return baseColorUnLoc;
    }

    public void setModelToWorldMatUnLoc(int modelToWorldMatUnLoc) {
        this.modelToWorldMatUnLoc = modelToWorldMatUnLoc;
    }

    public void setWorldToCameraMatUnLoc(int worldToCameraMatUnLoc) {
        this.worldToCameraMatUnLoc = worldToCameraMatUnLoc;
    }

    public void setCameraToClipMatUnLoc(int cameraToClipMatUnLoc) {
        this.cameraToClipMatUnLoc = cameraToClipMatUnLoc;
    }

    public void setBaseColorUnLoc(int baseColorUnLoc) {
        this.baseColorUnLoc = baseColorUnLoc;
    }
}