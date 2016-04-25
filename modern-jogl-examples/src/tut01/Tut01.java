/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut01;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.Semantic;
import framework.Test;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class Tut01 extends Test {

    public static void main(String[] args) {
        Tut01 tut01 = new Tut01("Tutorial 01 - Main");
    }

    public Tut01(String title) {
        super(title);
    }

    private final String SHADERS_ROOT = "src/tut01/shaders";
    private final String VERT_SHADER_SOURCE = "vertex-shader";
    private final String FRAG_SHADER_SOURCE = "fragment-shader";

    private int theProgram;
    private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexPositions = new float[]{
        +0.75f, +0.75f, 0.0f, 1.0f,
        +0.75f, -0.75f, 0.0f, 1.0f,
        -0.75f, -0.75f, 0.0f, 1.0f};

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        initializeVertexBuffer(gl3);

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));
    }

    private void initializeProgram(GL3 gl3) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                VERT_SHADER_SOURCE, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                FRAG_SHADER_SOURCE, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);
    }

    private void initializeVertexBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions);

        gl3.glGenBuffers(1, positionBufferObject);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL_COLOR_BUFFER_BIT);

        gl3.glUseProgram(theProgram);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, 0, 0);

        gl3.glDrawArrays(GL_TRIANGLES, 0, 3);

        gl3.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glUseProgram(0);        
    }

    @Override
    public void reshape(GL3 gl3, int width, int height) {

        gl3.glViewport(0, 0, width, height);
    }
}
