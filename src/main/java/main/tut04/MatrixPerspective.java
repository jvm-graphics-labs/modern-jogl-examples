/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut04;

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
import glsl.ShaderCodeKt;
import glsl.ShaderProgramKt;
import main.framework.Framework;
import main.framework.Semantic;
import vec._4.Vec4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * @author gbarbieri
 */
public class MatrixPerspective extends Framework {

    public static void main(String[] args) {
        new MatrixPerspective("Tutorial 04 - Matrix Perspective");
    }

    public MatrixPerspective(String title) {
        super(title);
    }

    private int theProgram, offsetUniform;
    private IntBuffer vertexBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer perspectiveMatrix = GLBuffers.newDirectFloatBuffer(16);

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

    private void initializeProgram(GL3 gl) {

        theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut04", "matrix-perspective.vert", "standard-colors.frag");

        offsetUniform = gl.glGetUniformLocation(theProgram, "offset");

        int perspectiveMatrixUnif = gl.glGetUniformLocation(theProgram, "perspectiveMatrix");

        float frustumScale = 1.0f, zNear = 0.5f, zFar = 3.0f;

        perspectiveMatrix.put(0, frustumScale);
        perspectiveMatrix.put(5, frustumScale);
        perspectiveMatrix.put(10, (zFar + zNear) / (zNear - zFar));
        perspectiveMatrix.put(14, (2 * zFar * zNear) / (zNear - zFar));
        perspectiveMatrix.put(11, -1.0f);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix);
        gl.glUseProgram(0);
    }

    private void initializeVertexBuffer(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        gl.glGenBuffers(1, vertexBufferObject);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f));

        gl.glUseProgram(theProgram);

        gl.glUniform2f(offsetUniform, 0.5f, 0.5f);

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
        BufferUtils.destroyDirectBuffer(perspectiveMatrix);
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

    private float[] vertexData = {
            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,

            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,

            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,

            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,


            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,

            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,

            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,

            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,

            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,

            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,

            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,

            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f};
}
