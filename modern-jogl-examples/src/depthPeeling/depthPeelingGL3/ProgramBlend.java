/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import javax.media.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramBlend extends ProgramFullScreenQuad {
    
    private int tempTexUnLoc;
    
    public ProgramBlend(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        init(gl3);
    }

    public ProgramBlend(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        init(gl3);
    }
    
    private void init(GL3 gl3){
        
        tempTexUnLoc = gl3.glGetUniformLocation(getProgramId(), "TempTex");
    }

    public int getTempTexUnLoc() {
        return tempTexUnLoc;
    }
}
