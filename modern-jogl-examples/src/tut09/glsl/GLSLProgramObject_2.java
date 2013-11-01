/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.glsl;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class GLSLProgramObject_2 extends GLSLProgramObject_1 {
    
    private int unLocAmbientIntensity;
    
    public GLSLProgramObject_2(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int projectionBlockIndex){
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader, projectionBlockIndex);
        
        unLocAmbientIntensity = gl3.glGetUniformLocation(getProgramId(), "ambientIntensity");
    }

    public int getUnLocAmbientIntensity() {
        return unLocAmbientIntensity;
    }
}