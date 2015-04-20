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
public class ProgramBlend extends ProgramFullScreenQuad {

    private int unLocC0;
    private int unLocC1;
    private int unLocC2;

    public ProgramBlend(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        init(gl3);
    }

    public ProgramBlend(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        init(gl3);
    }

    private void init(GL3 gl3) {

        unLocC0 = gl3.glGetUniformLocation(getProgramId(), "C0texture");
        
        unLocC1 = gl3.glGetUniformLocation(getProgramId(), "C1texture");
        
        unLocC2 = gl3.glGetUniformLocation(getProgramId(), "C2texture");        
    }

    public int getUnLocC0() {
        return unLocC0;
    }

    public int getUnLocC1() {
        return unLocC1;
    }

    public int getUnLocC2() {
        return unLocC2;
    }
}
