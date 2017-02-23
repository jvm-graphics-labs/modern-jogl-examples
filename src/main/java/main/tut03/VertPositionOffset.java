/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut03;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
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
import vec._2.Vec2;
import vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static main.GlmKt.glm;

/**
 *
 * @author gbarbieri
 */
public class VertPositionOffset extends Framework {

    public static void main(String[] args) {
        new VertPositionOffset("Tutorial 03 - Shader Position Offset");
    }

    public VertPositionOffset(String title) {
        super(title);
    }

    private int theProgram, offsetLocation;
    private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexPositions = {
        +0.25f, +0.25f, 0.0f, 1.0f,
        +0.25f, -0.25f, 0.0f, 1.0f,
        -0.25f, -0.25f, 0.0f, 1.0f};
    private long startingTime;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeVertexBuffer(gl);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        startingTime = System.currentTimeMillis();
    }

    private void initializeProgram(GL3 gl) {

        theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut03", "position-offset.vert", "standard.frag");

        offsetLocation = gl.glGetUniformLocation(theProgram, "offset");
    }

    private void initializeVertexBuffer(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions);

        gl.glGenBuffers(1, positionBufferObject);

        gl.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        Vec2 offset = new Vec2(0f);
        computePositionOffsets(offset);

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f));

        gl.glUseProgram(theProgram);

        gl.glUniform2f(offsetLocation, offset.x(), offset.y());

        gl.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);

        gl.glDrawArrays(GL_TRIANGLES, 0, 3);

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION);

        gl.glUseProgram(0);
    }

    private void computePositionOffsets(Vec2 offset) {

        float loopDuration = 5.0f;
        float scale = (float)Math.PI * 2f / loopDuration; // todo glm

        float elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f;

        float currTimeThroughLoop = elapsedTime % loopDuration;

        offset.x(glm.cos(currTimeThroughLoop * scale) * .5f);
        offset.y(glm.sin(currTimeThroughLoop * scale) * .5f);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);
        gl.glDeleteBuffers(1, positionBufferObject);
        gl.glDeleteVertexArrays(1, vao);

        BufferUtils.destroyDirectBuffer(positionBufferObject);
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