/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3lights;

import glsl.GLSLProgramObject;
import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramFullScreenQuad extends GLSLProgramObject{
    
    private int modelToClipMatrixUnLoc;
    
    public ProgramFullScreenQuad(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        init(gl3);
    }

    public ProgramFullScreenQuad(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        init(gl3);
    }
    
    private void init(GL3 gl3){
        
        modelToClipMatrixUnLoc = gl3.glGetUniformLocation(getProgramId(), "modelToClipMatrix");
    }

    public int getModelToClipMatrixUnLoc() {
        return modelToClipMatrixUnLoc;
    }
}