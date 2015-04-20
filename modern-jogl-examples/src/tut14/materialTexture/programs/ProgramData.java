/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.materialTexture.programs;

import com.jogamp.opengl.GL3;
import tut14.materialTexture.MaterialTexture;

/**
 *
 * @author gbarbieri
 */
public class ProgramData extends glsl.GLSLProgramObject {

    private int modelToCameraMatrixUL;
    private int normalModelToCameraMatrixUL;
    private int gaussianTextureUL;
    private int shininessTextureUL;

    public ProgramData(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        modelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        normalModelToCameraMatrixUL = gl3.glGetUniformLocation(getProgramId(), "normalModelToCameraMatrix");

        int materialUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Material");
        int lightUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Light");
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");

        int materialUBB = MaterialTexture.UniformBlockBinding.material.ordinal();
        int lightUBB = MaterialTexture.UniformBlockBinding.light.ordinal();
        int projectionUBB = MaterialTexture.UniformBlockBinding.projection.ordinal();

        gl3.glUniformBlockBinding(getProgramId(), materialUBI, materialUBB);
        gl3.glUniformBlockBinding(getProgramId(), lightUBI, lightUBB);
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);

        gaussianTextureUL = gl3.glGetUniformLocation(getProgramId(), "gaussianTexture");
        shininessTextureUL = gl3.glGetUniformLocation(getProgramId(), "shininessTexture");
//        System.out.println("gaussianTextureUL "+gaussianTextureUL);
        bind(gl3);
        {
            gl3.glUniform1i(gaussianTextureUL, MaterialTexture.TexUnit.gaussian.ordinal());
            gl3.glUniform1i(shininessTextureUL, MaterialTexture.TexUnit.shininess.ordinal());
        }
        unbind(gl3);
    }

    public int getModelToCameraMatrixUL() {
        return modelToCameraMatrixUL;
    }

    public int getNormalModelToCameraMatrixUL() {
        return normalModelToCameraMatrixUL;
    }

    public int getGaussianTextureUL() {
        return gaussianTextureUL;
    }

    public int getShininessTextureUL() {
        return shininessTextureUL;
    }

}
