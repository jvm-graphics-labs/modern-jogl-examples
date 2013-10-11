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
public class ProgramInit extends ProgramUBO {

    private int alphaUnLoc;

    public ProgramInit(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader, int mvpMatrixBlockIndex) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader, mvpMatrixBlockIndex);

        init(gl3);
    }

    public ProgramInit(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders, int mvpMatrixBlockIndex) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders, mvpMatrixBlockIndex);

        init(gl3);
    }

    private void init(GL3 gl3) {
        
        alphaUnLoc = gl3.glGetUniformLocation(getProgramId(), "Alpha");
    }

    public int getAlphaUnLoc() {
        return alphaUnLoc;
    }
}