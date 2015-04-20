/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3lights;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramPeel extends ProgramInit {

    private int unLocDepthTexture;
    
    public ProgramPeel(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int mvpMatrixBlockIndex) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader, mvpMatrixBlockIndex);

        init(gl3);
    }

    public ProgramPeel(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int mvpMatrixBlockIndex) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders, mvpMatrixBlockIndex);

        init(gl3);
    }

    private void init(GL3 gl3) {
        
        unLocDepthTexture = gl3.glGetUniformLocation(getProgramId(), "DepthTexture");
        System.out.println("unLocDepthTexture:"+unLocDepthTexture);
    }

    public int getUnLocDepthTexture() {
        return unLocDepthTexture;
    }
}