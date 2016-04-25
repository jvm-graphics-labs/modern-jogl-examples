/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.basicTexture.programs;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramStandard extends framework.GLSLProgramObject {

    private int modelToCameraMatrixUL;
    private int normalModelToCameraMatrixUL;
    private int gaussianTextureUL;

    public ProgramStandard(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader,
            int materialUBB, int lightUBB, int projectionUBB, int gaussianTextureUnit) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");

        normalModelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "normalModelToCameraMatrix");

        int materialUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Material");
        int lightUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Light");
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");

        gl3.glUniformBlockBinding(getProgramId(), materialUBI, materialUBB);
        gl3.glUniformBlockBinding(getProgramId(), lightUBI, lightUBB);
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);

        gaussianTextureUL = gl3.glGetUniformLocation(getProgramId(), "gaussianTexture");
        bind(gl3);
        {
            gl3.glUniform1i(gaussianTextureUL, gaussianTextureUnit);
        }
        unbind(gl3);
    }

    public int getModelToCameraMatrixUL() {
        return modelToCameraMatrixUL;
    }

    public int getNormalModelToCameraMatrixUL() {
        return normalModelToCameraMatrixUL;
    }
}
