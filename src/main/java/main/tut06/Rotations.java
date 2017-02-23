/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut06;

import buffer.BufferUtils;
import com.jogamp.newt.event.KeyEvent;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
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
import glsl.ShaderProgramKt;
import main.framework.Framework;
import main.framework.Semantic;
import mat.Mat3x3;
import mat.Mat4x4;
import vec._3.Vec3;
import vec._4.Vec4;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * @author gbarbieri
 */
public class Rotations extends Framework {

    public static void main(String[] args) {
        new Rotations("Tutorial 06 - Rotations");
    }

    public Rotations(String title) {
        super(title);
    }

    private interface Buffer {

        int VERTEX = 0;
        int INDEX = 1;
        int MAX = 2;
    }

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif;

    private Mat4x4 cameraToClipMatrix = new Mat4x4(0.0f);
    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(45.0f) / 2.0));

    private IntBuffer bufferObject = GLBuffers.newDirectIntBuffer(Buffer.MAX), vao = GLBuffers.newDirectIntBuffer(1);
    private final int numberOfVertices = 8;

    private final float[] GREEN_COLOR = {0.0f, 1.0f, 0.0f, 1.0f}, BLUE_COLOR = {0.0f, 0.0f, 1.0f, 1.0f},
            RED_COLOR = {1.0f, 0.0f, 0.0f, 1.0f}, BROWN_COLOR = {0.5f, 0.5f, 0.0f, 1.0f};

    private float[] vertexData = {

            +1.0f, +1.0f, +1.0f,
            -1.0f, -1.0f, +1.0f,
            -1.0f, +1.0f, -1.0f,
            +1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            +1.0f, +1.0f, -1.0f,
            +1.0f, -1.0f, +1.0f,
            -1.0f, +1.0f, +1.0f,


            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],

            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3]};

    private short[] indexData = {

            0, 1, 2,
            1, 0, 3,
            2, 3, 0,
            3, 2, 1,

            5, 4, 6,
            4, 5, 7,
            7, 6, 4,
            6, 7, 5};

    private Instance[] instanceList = {
            new Instance(Mode.NullRotation, new Vec3(0.0f, 0.0f, -25.0f)),
            new Instance(Mode.RotateX, new Vec3(-5.0f, -5.0f, -25.0f)),
            new Instance(Mode.RotateY, new Vec3(-5.0f, +5.0f, -25.0f)),
            new Instance(Mode.RotateZ, new Vec3(+5.0f, +5.0f, -25.0f)),
            new Instance(Mode.RotateAxis, new Vec3(5.0f, -5.0f, -25.0f))};

    private long start;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);
        initializeVertexBuffers(gl);

        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        int colorDataOffset = Vec3.SIZE * numberOfVertices;
        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0);
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorDataOffset);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl.glBindVertexArray(0);

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRange(0.0f, 1.0f);

        start = System.currentTimeMillis();
    }

    private void initializeProgram(GL3 gl) {

        theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut06", "pos-color-local-transform.vert",
                "color-passthrough.frag");

        modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix");

        float zNear = 1.0f, zFar = 61.0f;

        cameraToClipMatrix.setA0(frustumScale);
        cameraToClipMatrix.setB1(frustumScale);
        cameraToClipMatrix.setC2((zFar + zNear) / (zNear - zFar));
        cameraToClipMatrix.setC3(-1.0f);
        cameraToClipMatrix.setD3((2 * zFar * zNear) / (zNear - zFar));

        cameraToClipMatrix.to(matBuffer);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, matBuffer);
        gl.glUseProgram(0);
    }

    private void initializeVertexBuffers(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer indexBuffer = GLBuffers.newDirectShortBuffer(indexData);

        gl.glGenBuffers(Buffer.MAX, bufferObject);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));
        gl.glBufferData(GL_ARRAY_BUFFER, indexBuffer.capacity() * Short.BYTES, indexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(indexBuffer);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        gl.glUseProgram(theProgram);

        gl.glBindVertexArray(vao.get(0));

        float elapsedTime = (System.currentTimeMillis() - start) / 1_000f;
        for (Instance currInst : instanceList) {

            Mat4x4 transformMatrix = currInst.constructMatrix(elapsedTime);

            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, transformMatrix.to(matBuffer));
            gl.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
        }

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        cameraToClipMatrix.setA0(frustumScale * (h / (float) w));
        cameraToClipMatrix.setB1(frustumScale);

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.to(matBuffer));
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);
        gl3.glDeleteBuffers(Buffer.MAX, bufferObject);
        gl3.glDeleteVertexArrays(1, vao);

        BufferUtils.destroyDirectBuffer(vao);
        BufferUtils.destroyDirectBuffer(bufferObject);
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

    public enum Mode {

        NullRotation,
        RotateX,
        RotateY,
        RotateZ,
        RotateAxis
    }

    private class Instance {

        private Rotations.Mode mode;
        private Vec3 offset;
        private Mat3x3 theMat = new Mat3x3();

        Instance(Rotations.Mode mode, Vec3 offset) {
            this.mode = mode;
            this.offset = offset;
        }

        Mat4x4 constructMatrix(float elapsedTime) {

            Mat3x3 rotMatrix = calcRotation(elapsedTime);
            Mat4x4 theMat = new Mat4x4(rotMatrix);
            theMat.set(3, new Vec4(offset, 1.0f));

            return theMat;
        }

        private Mat3x3 calcRotation(float elapsedTime) {

            float angRad, cos, sin;

            switch (mode) {

                default:
                    return theMat.identity();

                case RotateX:

                    angRad = computeAngleRad(elapsedTime, 3.0f);
                    cos = (float) Math.cos(angRad);
                    sin = (float) Math.sin(angRad);

                    theMat.set(1f);
                    theMat.m11 = cos;
                    theMat.m12 = sin;
                    theMat.m21 = -sin;
                    theMat.m22 = cos;
                    return theMat;

                case RotateY:

                    angRad = computeAngleRad(elapsedTime, 2.0f);
                    cos = (float) Math.cos(angRad);
                    sin = (float) Math.sin(angRad);

                    theMat.identity();
                    theMat.m00 = cos;
                    theMat.m02 = -sin;
                    theMat.m20 = sin;
                    theMat.m22 = cos;
                    return theMat;

                case RotateZ:

                    angRad = computeAngleRad(elapsedTime, 2.0f);
                    cos = (float) Math.cos(angRad);
                    sin = (float) Math.sin(angRad);

                    theMat.identity();
                    theMat.m00 = cos;
                    theMat.m01 = sin;
                    theMat.m10 = -sin;
                    theMat.m11 = cos;
                    return theMat;

                case RotateAxis:

                    angRad = computeAngleRad(elapsedTime, 2.0f);
                    cos = (float) Math.cos(angRad);
                    sin = (float) Math.sin(angRad);
                    float invCos = 1.0f - cos,
                            invSin = 1.0f - sin;

                    Vec3 axis = new Vec3(1.0f).normalize();
                    theMat.identity();

                    theMat.m00 = (axis.x * axis.x) + ((1 - axis.x * axis.x) * cos);
                    theMat.m10 = axis.x * axis.y * (invCos) - (axis.z * sin);
                    theMat.m20 = axis.x * axis.z * (invCos) + (axis.y * sin);

                    theMat.m01 = axis.x * axis.y * (invCos) + (axis.z * sin);
                    theMat.m11 = (axis.y * axis.y) + ((1 - axis.y * axis.y) * cos);
                    theMat.m21 = axis.y * axis.z * (invCos) - (axis.x * sin);

                    theMat.m02 = axis.x * axis.z * (invCos) - (axis.y * sin);
                    theMat.m12 = axis.y * axis.z * (invCos) + (axis.x * sin);
                    theMat.m22 = (axis.z * axis.z) + ((1 - axis.z * axis.z) * cos);

                    return theMat;
            }
        }

        private float computeAngleRad(float elapsedTime, float loopDuration) {
            float scale = (float) (Math.PI * 2.0f / loopDuration);
            float currentTimeThroughLoop = elapsedTime % loopDuration;
            return currentTimeThroughLoop * scale;
        }
    }

}
