/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut15.manyImages.program;

import com.jogamp.opengl.GL3;
import tut15.manyImages.ManyImages;

/**
 *
 * @author gbarbieri
 */
public class ProgramData extends glsl.GLSLProgramObject {

    private int modelToCameraMatrixUL;
    
    public ProgramData(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader);
        
        modelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");        
        int projectionUBB = ManyImages.UniformBlockBinding.projection.ordinal();
        
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);
        
        int colorTextureUL = gl3.glGetUniformLocation(getProgramId(), "colorTexture");
        
        bind(gl3);
        {
            gl3.glUniform1i(colorTextureUL, ManyImages.TexUnit.color.ordinal());
        }
        unbind(gl3);
    }

    public int getModelToCameraMatrixUL() {
        return modelToCameraMatrixUL;
    }
}
