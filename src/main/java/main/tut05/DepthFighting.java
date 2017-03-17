
package main.tut05;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.Glm;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import main.framework.Framework;
import main.framework.Semantic;
import uno.glsl.ShaderProgramKt;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * Unfinished.
 * @author gbarbieri
 */
public class DepthFighting extends Framework {

    public static void main(String[] args) {
        new DepthFighting("Tutorial 05 - Depth Clamping");
    }

    public DepthFighting(String title) {
        super(title);
    }

    private interface Buffer {

        int VERTEX = 0;
        int INDEX = 1;
        int MAX = 2;
    }

    private int theProgram, offsetUniform, perspectiveMatrixUnif;
    private final int numberOfVertices = 8;

    private FloatBuffer perspectiveMatrix = GLBuffers.newDirectFloatBuffer(16);
    private float frustumScale = 1.0f, delta = 0.0f;

    private IntBuffer bufferObject = GLBuffers.newDirectIntBuffer(Buffer.MAX), vao = GLBuffers.newDirectIntBuffer(1);

    private final float Z_OFFSET = 0.5f;
    private final float[] GREEN_COLOR = {0.75f, 0.75f, 1.0f, 1.0f}, BLUE_COLOR = {0.0f, 0.5f, 0.0f, 1.0f},
            RED_COLOR = {1.0f, 0.0f, 0.0f, 1.0f};

    private float[] vertexData = {
            //Front face positions
            -400.0f, +400.0f, 0.0f,
            +400.0f, +400.0f, 0.0f,
            +400.0f, -400.0f, 0.0f,
            -400.0f, -400.0f, 0.0f,

            //Rear face positions
            -200.0f, +600.0f, -Z_OFFSET,
            +600.0f, +600.0f, 0.0f - Z_OFFSET,
            +600.0f, -200.0f, 0.0f - Z_OFFSET,
            -200.0f, -200.0f, -Z_OFFSET,

            //Front face colors.
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            //Rear face colors.
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3]};

    private short[] indexData = {

            0, 1, 3,
            1, 2, 3,

            4, 5, 7,
            5, 6, 7};

    private boolean depthClampingActive = false;
    private long timeStart;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeBuffers(gl);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        int colorData = Float.BYTES * 3 * numberOfVertices;
        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, Vec3.length, GL_FLOAT, false, Vec3.SIZE, 0);
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, Vec4.length, GL_FLOAT, false, Vec4.SIZE, colorData);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl.glBindVertexArray(0);

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRange(0.0f, 1.0f);

        timeStart = System.currentTimeMillis();
    }

    private void initializeProgram(GL3 gl) {

        theProgram = programOf(gl, getClass(), "tut05", "standard.vert", "standard.frag");

        offsetUniform = gl.glGetUniformLocation(theProgram, "offset");

        perspectiveMatrixUnif = gl.glGetUniformLocation(theProgram, "perspectiveMatrix");

        float zNear = 1.0f, zFar = 3.0f;

        perspectiveMatrix.put(0, frustumScale);
        perspectiveMatrix.put(5, frustumScale);
        perspectiveMatrix.put(10, (zFar + zNear) / (zNear - zFar));
        perspectiveMatrix.put(14, (2 * zFar * zNear) / (zNear - zFar));
        perspectiveMatrix.put(11, -1.0f);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix);
        gl.glUseProgram(0);
    }

    private void initializeBuffers(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);

        gl.glGenBuffers(Buffer.MAX, bufferObject);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));
        gl.glBufferData(GL_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        destroyBuffers(vertexBuffer, indexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        if (depthClampingActive)
            gl.glDisable(GL_DEPTH_CLAMP);
        else
            gl.glEnable(GL_DEPTH_CLAMP);

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        gl.glUseProgram(theProgram);
        gl.glBindVertexArray(vao.get(0));

        float zOffset = calcZOffset();
        gl.glUniform3f(offsetUniform, 0.0f, 0.0f, zOffset);
        gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    private float calcZOffset() {

        float start = 2534.0f;
        float loopDuration = 5.0f;
        float scale = (float) Glm.pi * 2.0f / loopDuration;

        float elapsedTime = (System.currentTimeMillis() - timeStart) / 1_000.0f;

        float currTimeThroughLoop = elapsedTime % loopDuration;

//        float ret = Math.cos(currTimeThroughLoop * scale) * 500.0f - start;
        float ret = delta - start;

        return ret;
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        perspectiveMatrix.put(0, frustumScale * (h / (float) w));
        perspectiveMatrix.put(5, frustumScale);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix);
        gl.glUseProgram(0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);
        gl.glDeleteBuffers(Buffer.MAX, bufferObject);
        gl.glDeleteVertexArrays(1, vao);

        destroyBuffers(vao, bufferObject, perspectiveMatrix);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                quit();
                break;
            case KeyEvent.VK_SPACE:
                depthClampingActive = !depthClampingActive;
                break;
        }
    }
}
