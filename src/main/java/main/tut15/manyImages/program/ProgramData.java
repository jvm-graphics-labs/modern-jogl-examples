///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut15.manyImages.program;
//
//import com.jogamp.opengl.GL3;
//import glsl.Program;
//import main.tut15.manyImages.ManyImages;
//
///**
// *
// * @author gbarbieri
// */
//public class ProgramData extends Program {
//
//    private int modelToCameraMatrixUL;
//
//    public ProgramData(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {
//
//        super(gl3, shadersFilepath, vertexShader, fragmentShader);
//
//        modelToCameraMatrixUL = gl3.glGetUniformLocation(name, "modelToCameraMatrix");
//
//        int projectionUBI = gl3.glGetUniformBlockIndex(name, "Projection");
//        int projectionUBB = ManyImages.UniformBlockBinding.projection.ordinal();
//
//        gl3.glUniformBlockBinding(name, projectionUBI, projectionUBB);
//
//        int colorTextureUL = gl3.glGetUniformLocation(name, "colorTexture");
//
//        gl3.glUseProgram(name);
//        {
//            gl3.glUniform1i(colorTextureUL, ManyImages.TexUnit.color.ordinal());
//        }
//        gl3.glUseProgram(0);
//    }
//
//    public int getModelToCameraMatrixUL() {
//        return modelToCameraMatrixUL;
//    }
//}
