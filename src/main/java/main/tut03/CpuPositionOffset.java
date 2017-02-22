/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut03;

import buffer.BufferUtils;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glsl.ShaderCodeKt;
import glsl.ShaderProgramKt;
import main.framework.Framework;
import main.framework.Semantic;
import vec._2.Vec2;
import vec._4.Vec4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

import static main.GlmKt.glm;

/**
 *
 * @author gbarbieri
 */
public class CpuPositionOffset extends Framework {

    public static void main(String[] args) {
        new CpuPositionOffset("Tutorial 03 - CPU Position Offset");
    }

    public CpuPositionOffset(String title) {
        super(title);
    }

    private int theProgram;
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
        theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut03", "standard.vert", "standard.frag");
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

        Vec2 offset = new Vec2(0.0f);

        computePositionOffsets(offset);
        adjustVertexData(gl, offset);

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f));

        gl.glUseProgram(theProgram);

        gl.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);

        gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 3);

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glUseProgram(0);
    }

    private void computePositionOffsets(Vec2 offset) {

        float loopDuration = 5.0f;
        float scale = (float) (Math.PI * 2.0f / loopDuration);

        float elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000.0f;

        float fCurrTimeThroughLoop = elapsedTime % loopDuration;

        offset.setX(glm.cos(fCurrTimeThroughLoop * scale) * 0.5f);
        offset.setY(glm.sin(fCurrTimeThroughLoop * scale) * 0.5f);
    }

    private void adjustVertexData(GL3 gl, Vec2 offset) {

        float[] newData = new float[vertexPositions.length];
        System.arraycopy(vertexPositions, 0, newData, 0, vertexPositions.length);

        for (int iVertex = 0; iVertex < vertexPositions.length; iVertex += 4) {

            newData[iVertex + 0] += offset.getX();
            newData[iVertex + 1] += offset.getY();
        }

        FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(newData);

        gl.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glBufferSubData(GL_ARRAY_BUFFER, 0, buffer.capacity() * Float.BYTES, buffer);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(buffer);
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
