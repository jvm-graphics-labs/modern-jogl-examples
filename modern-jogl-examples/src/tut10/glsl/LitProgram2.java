/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut10.glsl;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class LitProgram2 extends glsl.GLSLProgramObject{
    
    private int unLocModelToCameraMatrix;
    
    private int unLocLightPositionModelSpace;
    private int unLocLightDiffuseIntensity;
    private int unLocLightAmbientIntensity;
    
    public LitProgram2 (GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionUBB){
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader);
        
        unLocModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        
        unLocLightPositionModelSpace = gl3.glGetUniformLocation(getProgramId(), "lightPositionModelSpace");
        
        unLocLightDiffuseIntensity = gl3.glGetUniformLocation(getProgramId(), "lightDiffuseIntensity");
        
        unLocLightAmbientIntensity = gl3.glGetUniformLocation(getProgramId(), "lightAmbientIntensity");
        
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);
    }

    public int getUnLocModelToCameraMatrix() {
        return unLocModelToCameraMatrix;
    }

    public int getUnLocLightPosition() {
        return unLocLightPositionModelSpace;
    }

    public int getUnLocLightDiffuseIntensity() {
        return unLocLightDiffuseIntensity;
    }

    public int getUnLocLightAmbientIntensity() {
        return unLocLightAmbientIntensity;
    }
}
