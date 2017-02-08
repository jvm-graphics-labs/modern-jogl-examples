///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package tut16.gammaCheckers.program;
//
//import com.jogamp.opengl.GL3;
//import tut16.gammaCheckers.GammaCheckers.TexUnit;
//import tut16.gammaCheckers.GammaCheckers.UniformBlockBuffers;
//
///**
// *
// * @author gbarbieri
// */
//public class ProgramData extends framework.GLSLProgramObject {
//
//    private int modelToCameraUL;
//
//    public ProgramData(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {
//
//        super(gl3, shadersFilepath, vertexShader, fragmentShader);
//
//        modelToCameraUL = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
//
//        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
//        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, UniformBlockBuffers.projection.ordinal());
//
//        int colorTextureUL = gl3.glGetUniformLocation(getProgramId(), "colorTexture");
//
//        gl3.glUseProgram(getProgramId());
//        {
//            gl3.glUniform1i(colorTextureUL, TexUnit.color.ordinal());
//        }
//        gl3.glUseProgram(0);
//    }
//
//    public int getModelToCameraUL() {
//        return modelToCameraUL;
//    }
//}
