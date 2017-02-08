/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut13.geomImpostor;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3ES3.GL_GEOMETRY_SHADER;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import main.framework.Semantic;

/**
 *
 * @author elect
 */
class ProgramImposData {

    public int theProgram;

    public ProgramImposData(GL3 gl3, String shaderRoot, String shaderSrc) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), shaderRoot, null,
                shaderSrc, "vert", null, true);
        ShaderCode geomShaderCode = ShaderCode.create(gl3, GL_GEOMETRY_SHADER, this.getClass(), shaderRoot, null,
                shaderSrc, "geom", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), shaderRoot, null,
                shaderSrc, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(geomShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        geomShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

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
