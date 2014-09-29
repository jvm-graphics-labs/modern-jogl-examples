/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tut14.materialTexture.programs;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class UnlitProgData extends glsl.GLSLProgramObject {

    private int modelToCameraMatrixUL;
    private int objectColorUL;
    
    public UnlitProgData(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader,
            int projectionUBB) {
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader);
        
        modelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        objectColorUL = gl3.glGetUniformLocation(getProgramId(), "objectColor");
        
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);
    }

    public int getModelToCameraMatrixUL() {
        return modelToCameraMatrixUL;
    }

    public int getObjectColorUL() {
        return objectColorUL;
    }
    
}
