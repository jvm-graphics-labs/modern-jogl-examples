/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08.glsl;

import glsl.GLSLProgramObject;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class GLSLProgramObject_1 extends GLSLProgramObject{
    
    private int modelToCameraMatUnLoc;
    private int cameraToClipMatUnLoc;
    private int baseColorUnLoc;

    public GLSLProgramObject_1(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToCameraMatUnLoc = gl3.glGetUniformLocation(getProgId(), "modelToCameraMatrix");
        cameraToClipMatUnLoc = gl3.glGetUniformLocation(getProgId(), "cameraToClipMatrix");
        baseColorUnLoc = gl3.glGetUniformLocation(getProgId(), "baseColor");
    }

    public int getModelToCameraMatUnLoc() {
        return modelToCameraMatUnLoc;
    }

    public int getCameraToClipMatUnLoc() {
        return cameraToClipMatUnLoc;
    }

    public int getBaseColorUnLoc() {
        return baseColorUnLoc;
    }

    public void setModelToCameraMatUnLoc(int modelToCameraMatUnLoc) {
        this.modelToCameraMatUnLoc = modelToCameraMatUnLoc;
    }

    public void setCameraToClipMatUnLoc(int cameraToClipMatUnLoc) {
        this.cameraToClipMatUnLoc = cameraToClipMatUnLoc;
    }

    public void setBaseColorUnLoc(int baseColorUnLoc) {
        this.baseColorUnLoc = baseColorUnLoc;
    }
}
