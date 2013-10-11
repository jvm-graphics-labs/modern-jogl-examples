/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import glsl.GLSLProgramObject;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramUBO extends GLSLProgramObject{
    
    public ProgramUBO(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int mvpMatrixBlockIndex){
        
        super(gl3, shadersFilepath, vertexShader, fragmentShader);
        
        init(gl3, mvpMatrixBlockIndex);
    }
    
    public ProgramUBO(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int mvpMatrixBlockIndex){
        
        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);
        
        init(gl3, mvpMatrixBlockIndex);
    }
    
    private void init(GL3 gl3, int mvpMatrixBlockBlinding){
        
        int mvpMatrixUBI = gl3.glGetUniformBlockIndex(getProgramId(), "mvpMatrixes");
        gl3.glUniformBlockBinding(getProgramId(), mvpMatrixUBI, mvpMatrixBlockBlinding);
    }
}