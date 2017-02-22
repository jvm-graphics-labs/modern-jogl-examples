/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut02;

import buffer.BufferUtils;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import extensions.ShaderProgramUtil;
import glsl.ShaderCodeKt;
import glsl.ShaderProgramKt;
import main.framework.Framework;
import main.framework.Semantic;
import vec._4.Vec4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

/**
 * @author gbarbieri
 */
public class FragPosition extends Framework {

    public static void main(String[] args) {
        new FragPosition("Tutorial 02 - Fragment Position");
    }

    public FragPosition(String title) {
        super(title);
    }

    private int theProgram;
    private IntBuffer vertexBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexData = {
            +0.75f, +0.75f, 0.0f, 1.0f,
            +0.75f, -0.75f, 0.0f, 1.0f,
            -0.75f, -0.75f, 0.0f, 1.0f};

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeVertexBuffer(gl);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));
    }

    private void initializeProgram(GL3 gl) {
        theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut02", "frag-position.vert", "frag-position.frag");
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

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f));

        gl.glUseProgram(theProgram);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);

        gl.glDrawArrays(GL_TRIANGLES, 0, 3);

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        gl.glViewport(0, 0, w, h);
    }

    @Override
    protected void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);
        gl.glDeleteBuffers(1, vertexBufferObject);
        gl.glDeleteVertexArrays(1, vao);

        BufferUtils.destroyDirectBuffer(vertexBufferObject);
        BufferUtils.destroyDirectBuffer(vao);
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
