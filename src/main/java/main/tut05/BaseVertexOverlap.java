
package main.tut05;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import main.framework.Framework;
import main.framework.Semantic;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class BaseVertexOverlap extends Framework {

    public static void main(String[] args) {
        new BaseVertexOverlap().setup("Tutorial 05 - Base Vertex With Overlap");
    }

    private interface Buffer {

        int VERTEX = 0;
        int INDEX = 1;
        int MAX = 2;
    }

    private int theProgram, offsetUniform, perspectiveMatrixUnif;
    private final int numberOfVertices = 36;
    private FloatBuffer perspectiveMatrix = GLBuffers.newDirectFloatBuffer(16);
    private float frustumScale = 1.0f;
    private IntBuffer bufferObject = GLBuffers.newDirectIntBuffer(Buffer.MAX), vao = GLBuffers.newDirectIntBuffer(1);

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

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        destroyBuffers(vertexBuffer, indexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));

        gl.glUseProgram(theProgram);

        gl.glBindVertexArray(vao.get(0));

        gl.glUniform3f(offsetUniform, 0.0f, 0.0f, 0.0f);
        gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);

        gl.glUniform3f(offsetUniform, 0.0f, 0.0f, -1.0f);
        gl.glDrawElementsBaseVertex(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0, numberOfVertices / 2);

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
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
        }
    }

    private final float RIGHT_EXTENT = 0.8f, LEFT_EXTENT = -RIGHT_EXTENT, TOP_EXTENT = 0.20f, MIDDLE_EXTENT = 0.0f,
            BOTTOM_EXTENT = -TOP_EXTENT, FRONT_EXTENT = -1.25f, REAR_EXTENT = -1.75f;

    private final float[] GREEN_COLOR = {0.75f, 0.75f, 1.0f, 1.0f}, BLUE_COLOR = {0.0f, 0.5f, 0.0f, 1.0f},
            RED_COLOR = {1.0f, 0.0f, 0.0f, 1.0f}, GREY_COLOR = {0.8f, 0.8f, 0.8f, 1.0f},
            BROWN_COLOR = {0.5f, 0.5f, 0.0f, 1.0f};

    private final float[] vertexData = {
            //Object 1 positions
            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,


            //Object 2 positions
            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,

            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,


            //Object 1 colors
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],

            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],

            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],


            //Object 2 colors
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],

            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],

            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3]};

    private short[] indexData = {

            0, 2, 1,
            3, 2, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            11, 13, 12,

            14, 16, 15,
            17, 16, 14};
}