/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut10.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class UnlitProgram extends glsl.GLSLProgramObject{
    
    private int unLocModelToCameraMatrix;
    private int unLocObjectColor;
    
    public UnlitProgram (GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionUBB){
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader);
        
        unLocModelToCameraMatrix = gl3.glGetUniformLocation(getProgramId(), "modelToCameraMatrix");
        
        unLocObjectColor = gl3.glGetUniformLocation(getProgramId(), "objectColor");
        
        int projectionUBI = gl3.glGetUniformBlockIndex(getProgramId(), "Projection");
        gl3.glUniformBlockBinding(getProgramId(), projectionUBI, projectionUBB);
    }

    public int getUnLocModelToCameraMatrix() {
        return unLocModelToCameraMatrix;
    }

    public int getUnLocObjectColor() {
        return unLocObjectColor;
    }
}
