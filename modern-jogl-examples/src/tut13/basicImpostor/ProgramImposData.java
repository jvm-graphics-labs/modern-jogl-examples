/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut13.basicImpostor;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Semantic;

/**
 *
 * @author elect
 */
class ProgramImposData {

    public int theProgram;

    public int sphereRadiusUnif;
    public int cameraSpherePosUnif;

    public ProgramImposData(GL3 gl3, String shaderRoot, String shaderSrc) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), shaderRoot, null,
                shaderSrc, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), shaderRoot, null,
                shaderSrc, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

        sphereRadiusUnif = gl3.glGetUniformLocation(theProgram, "sphereRadius");
        cameraSpherePosUnif = gl3.glGetUniformLocation(theProgram, "cameraSpherePos");

        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Projection"),
                Semantic.Uniform.PROJECTION);
        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Light"),
                Semantic.Uniform.LIGHT);
        gl3.glUniformBlockBinding(theProgram,
                gl3.glGetUniformBlockIndex(theProgram, "Material"),
                Semantic.Uniform.MATERIAL);
    }
}
