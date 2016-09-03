/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut05.overlapNoDepth;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.Framework;
import framework.Semantic;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author gbarbieri
 */
public class OverlapNoDepth extends Framework {

    private final String SHADERS_ROOT = "src/tut05/overlapNoDepth/shaders";
    private final String SHADERS_SOURCE = "standard";

    public static void main(String[] args) {
        new OverlapNoDepth("Tutorial 05 - Overlap No Depth");
    }

    public OverlapNoDepth(String title) {
        super(title);
    }

    private interface Buffer {

        public final static int VERTEX = 0;
        public final static int INDEX = 1;
        public final static int MAX = 2;
    }

    private interface VertexArray {

        public final static int _1 = 0;
        public final static int _2 = 1;
        public final static int MAX = 2;
    }

    private int theProgram, offsetUniform, perspectiveMatrixUnif;
    private final int numberOfVertices = 36;
    private FloatBuffer perspectiveMatrix = GLBuffers.newDirectFloatBuffer(16);
    private float frustumScale = 1.0f;
    private IntBuffer bufferObject = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vao = GLBuffers.newDirectIntBuffer(VertexArray.MAX);
    private final float RIGHT_EXTENT = 0.8f, LEFT_EXTENT = -RIGHT_EXTENT, TOP_EXTENT = 0.20f, MIDDLE_EXTENT = 0.0f,
            BOTTOM_EXTENT = -TOP_EXTENT, FRONT_EXTENT = -1.25f, REAR_EXTENT = -1.75f;
    private final float[] GREEN_COLOR = {0.75f, 0.75f, 1.0f, 1.0f}, BLUE_COLOR = {0.0f, 0.5f, 0.0f, 1.0f},
            RED_COLOR = {1.0f, 0.0f, 0.0f, 1.0f}, GREY_COLOR = {0.8f, 0.8f, 0.8f, 1.0f},
            BROWN_COLOR = {0.5f, 0.5f, 0.0f, 1.0f};
    private float[] vertexData = {
        //Object 1 positions
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
        RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
        //
        //Object 2 positions
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        //					
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //					
        BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
        TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,
        //
        //Object 1 colors
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        //	
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        //
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        //
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        //
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        //Object 2 colors
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        //
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        //
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
        GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3]};
    private short[] indexData = {
        0, 2, 1,
        3, 2, 0,
        //
        4, 5, 6,
        6, 7, 4,
        //
        8, 9, 10,
        11, 13, 12,
        //
        14, 16, 15,
        17, 16, 14};

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);
        initializeBuffers(gl3);
        initializeVertexArrays(gl3);

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);
    }

    private void initializeProgram(GL3 gl3) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

        offsetUniform = gl3.glGetUniformLocation(theProgram, "offset");

        perspectiveMatrixUnif = gl3.glGetUniformLocation(theProgram, "perspectiveMatrix");

        float zNear = 1.0f, zFar = 3.0f;

        perspectiveMatrix.put(0, frustumScale);
        perspectiveMatrix.put(5, frustumScale);
        perspectiveMatrix.put(10, (zFar + zNear) / (zNear - zFar));
        perspectiveMatrix.put(14, (2 * zFar * zNear) / (zNear - zFar));
        perspectiveMatrix.put(11, -1.0f);

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix);
        gl3.glUseProgram(0);
    }

    private void initializeBuffers(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);

        gl3.glGenBuffers(Buffer.MAX, bufferObject);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(indexBuffer);
    }

    private void initializeVertexArrays(GL3 gl3) {

        gl3.glGenVertexArrays(VertexArray.MAX, vao);

        gl3.glBindVertexArray(vao.get(VertexArray._1));

        int colorDataOffset = Float.BYTES * 3 * numberOfVertices;

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0);
        gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorDataOffset);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl3.glBindVertexArray(vao.get(VertexArray._2));

        int positionDataOffset = Float.BYTES * 3 * (numberOfVertices / 2);
        colorDataOffset += Float.BYTES * 4 * (numberOfVertices / 2);

        //Use the same buffer object previously bound to GL_ARRAY_BUFFER.
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, positionDataOffset);
        gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorDataOffset);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl3.glBindVertexArray(0);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        gl3.glUseProgram(theProgram);

        gl3.glBindVertexArray(vao.get(VertexArray._1));
        gl3.glUniform3f(offsetUniform, 0.0f, 0.0f, 0.0f);
        gl3.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);

        gl3.glBindVertexArray(vao.get(VertexArray._2));
        gl3.glUniform3f(offsetUniform, 0.0f, 0.0f, -1.0f);
        gl3.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);

        gl3.glBindVertexArray(0);
        gl3.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        perspectiveMatrix.put(0, frustumScale * (h / (float) w));
        perspectiveMatrix.put(5, frustumScale);

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix);
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);
        gl3.glDeleteBuffers(Buffer.MAX, bufferObject);
        gl3.glDeleteVertexArrays(VertexArray.MAX, vao);

        BufferUtils.destroyDirectBuffer(vao);
        BufferUtils.destroyDirectBuffer(bufferObject);
        BufferUtils.destroyDirectBuffer(perspectiveMatrix);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;
        }
    }
}
