/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class GLSLProgramObject_2 extends GLSLProgramObject_1 {

    private int globalUniformBlockIndex;

    public GLSLProgramObject_2(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int globalMatricesBindingIndex) {
        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        setModelToWorldMatUnLoc(gl3.glGetUniformLocation(getProgId(), "modelToWorldMatrix"));
        setWorldToCameraMatUnLoc(gl3.glGetUniformLocation(getProgId(), "worldToCameraMatrix"));
        setCameraToClipMatUnLoc(gl3.glGetUniformLocation(getProgId(), "cameraToClipMatrix"));
        setBaseColorUnLoc(gl3.glGetUniformLocation(getProgId(), "baseColor"));

        globalUniformBlockIndex = gl3.glGetUniformBlockIndex(getProgId(), "GlobalMatrices");

        gl3.glUniformBlockBinding(getProgId(), globalUniformBlockIndex, globalMatricesBindingIndex);
    }
}