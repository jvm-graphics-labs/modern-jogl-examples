/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut04.orthoCube;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import buffer.BufferUtils;
import main.framework.Framework;
import main.framework.Semantic;
import vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class OrthoCube extends Framework {

    private final String SHADERS_ROOT = "src/tut04/orthoCube/shaders";
    private final String VERT_SHADER_SOURCE = "ortho-with-offset";
    private final String FRAG_SHADER_SOURCE = "standard-colors";

    public static void main(String[] args) {
        new OrthoCube("Tutorial 04 - Ortho Cube");
    }

    public OrthoCube(String title) {
        super(title);
    }

    private int theProgram, offsetUniform;
    private IntBuffer vertexBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexData = {
        +0.25f, +0.25f, +0.75f, 1.0f,
        +0.25f, -0.25f, +0.75f, 1.0f,
        -0.25f, +0.25f, +0.75f, 1.0f,
        //        
        +0.25f, -0.25f, +0.75f, 1.0f,
        -0.25f, -0.25f, +0.75f, 1.0f,
        -0.25f, +0.25f, +0.75f, 1.0f,
        //        
        +0.25f, +0.25f, -0.75f, 1.0f,
        -0.25f, +0.25f, -0.75f, 1.0f,
        +0.25f, -0.25f, -0.75f, 1.0f,
        //        
        +0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, +0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        //        
        -0.25f, +0.25f, +0.75f, 1.0f,
        -0.25f, -0.25f, +0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        //        
        -0.25f, +0.25f, +0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, +0.25f, -0.75f, 1.0f,
        //        
        +0.25f, +0.25f, +0.75f, 1.0f,
        +0.25f, -0.25f, -0.75f, 1.0f,
        +0.25f, -0.25f, +0.75f, 1.0f,
        //        
        +0.25f, +0.25f, +0.75f, 1.0f,
        +0.25f, +0.25f, -0.75f, 1.0f,
        +0.25f, -0.25f, -0.75f, 1.0f,
        //        
        +0.25f, +0.25f, -0.75f, 1.0f,
        +0.25f, +0.25f, +0.75f, 1.0f,
        -0.25f, +0.25f, +0.75f, 1.0f,
        //        
        +0.25f, +0.25f, -0.75f, 1.0f,
        -0.25f, +0.25f, +0.75f, 1.0f,
        -0.25f, +0.25f, -0.75f, 1.0f,
        //        
        +0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, +0.75f, 1.0f,
        +0.25f, -0.25f, +0.75f, 1.0f,
        //        
        +0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, -0.75f, 1.0f,
        -0.25f, -0.25f, +0.75f, 1.0f,
        //        
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        //        
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        0.0f, 0.0f, 1.0f, 1.0f,
        //        
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        //        
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        0.8f, 0.8f, 0.8f, 1.0f,
        //        
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        //        
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        0.0f, 1.0f, 0.0f, 1.0f,
        //        
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        //        
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, 0.0f, 1.0f,
        //        
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        //        
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        1.0f, 0.0f, 0.0f, 1.0f,
        //        
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        //        
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f,
        0.0f, 1.0f, 1.0f, 1.0f};

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeVertexBuffer(gl);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);
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

        offsetUniform = gl3.glGetUniformLocation(theProgram, "offset");
    }

    private void initializeVertexBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        gl3.glGenBuffers(1, vertexBufferObject);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));

        gl.glUseProgram(theProgram);

        gl.glUniform2f(offsetUniform, 0.5f, 0.25f);

        int colorData = vertexData.length * Float.BYTES / 2;
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorData);

        gl.glDrawArrays(GL_TRIANGLES, 0, 36);

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glDisableVertexAttribArray(Semantic.Attr.COLOR);

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);
        gl.glDeleteBuffers(1, vertexBufferObject);
        gl.glDeleteVertexArrays(1, vao);

        BufferUtils.destroyDirectBuffer(vao);
        BufferUtils.destroyDirectBuffer(vertexBufferObject);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.remove(window);
                window.destroy();
                break;
        }
    }
}
