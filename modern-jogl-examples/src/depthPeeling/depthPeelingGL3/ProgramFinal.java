/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import com.jogamp.opengl.GL3;

/**
 *
 * @author gbarbieri
 */
public class ProgramFinal extends ProgramFullScreenQuad{
    
    private int colorTexUnLoc;
    private int backgroundColorUnLoc;
    
    public ProgramFinal(GL3 gl3, String shadersFilepath, String vertexShader, String fragmentShader) {

        super(gl3, shadersFilepath, vertexShader, fragmentShader);

        init(gl3);
    }

    public ProgramFinal(GL3 gl3, String shadersFilepath, String[] vertexShaders, String[] fragmentShaders) {

        super(gl3, shadersFilepath, vertexShaders, fragmentShaders);

        init(gl3);
    }
    
    private void init(GL3 gl3){
        
        colorTexUnLoc = gl3.glGetUniformLocation(getProgramId(), "ColorTex");
        
        backgroundColorUnLoc = gl3.glGetUniformLocation(getProgramId(), "BackgroundColor");
    }

    public int getColorTexUnLoc() {
        return colorTexUnLoc;
    }

    public int getBackgroundColorUnLoc() {
        return backgroundColorUnLoc;
    }
}