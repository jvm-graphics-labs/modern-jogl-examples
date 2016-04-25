/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.glsl;

import com.jogamp.opengl.GL3;
import framework.GLSLProgramObject;

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

        modelToWorldMatUnLoc = gl3.glGetUniformLocation(getProgramId(), "modelToWorldMatrix");
        worldToCameraMatUnLoc = gl3.glGetUniformLocation(getProgramId(), "worldToCameraMatrix");
        cameraToClipMatUnLoc = gl3.glGetUniformLocation(getProgramId(), "cameraToClipMatrix");
        baseColorUnLoc = gl3.glGetUniformLocation(getProgramId(), "baseColor");
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