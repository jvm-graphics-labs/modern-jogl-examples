/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut06.scale;

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
import glutil.BufferUtils;
import main.framework.Framework;
import main.framework.Semantic;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author gbarbieri
 */
public class Scale extends Framework {

    private final String SHADERS_ROOT = "src/tut06/scale/shaders";
    private final String VERT_SHADER_SOURCE = "pos-color-local-transform";
    private final String FRAG_SHADER_SOURCE = "color-passthrough";

    public static void main(String[] args) {
        new Scale("Tutorial 06 - Scale");
    }

    public Scale(String title) {
        super(title);
    }

    private interface Buffer {

        public final static int VERTEX = 0;
        public final static int INDEX = 1;
        public final static int MAX = 2;
    }

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif;
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
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
        //
        -1.0f, -1.0f, -1.0f,
        +1.0f, +1.0f, -1.0f,
        +1.0f, -1.0f, +1.0f,
        -1.0f, +1.0f, +1.0f,
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
        //
        GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
        BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
        RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
        BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3]};
    private short[] indexData = {
        0, 1, 2,
        1, 0, 3,
        2, 3, 0,
        3, 2, 1,
        //
        5, 4, 6,
        4, 5, 7,
        7, 6, 4,
        6, 7, 5};

    private Instance[] instanceList = {
        new Instance(Mode.NullScale, new Vec3(0.0f, 0.0f, -45.0f)),
        new Instance(Mode.StaticUniformScale, new Vec3(-10.0f, -10.0f, -45.0f)),
        new Instance(Mode.StaticNonUniformScale, new Vec3(-10.0f, 10.0f, -45.0f)),
        new Instance(Mode.DynamicUniformScale, new Vec3(10.0f, 10.0f, -45.0f)),
        new Instance(Mode.DynamicNonUniformScale, new Vec3(10.0f, -10.0f, -45.0f))};
    private long start;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);
        initializeVertexBuffers(gl3);

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        int colorDataOffset = Vec3.SIZE * numberOfVertices;
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0);
        gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorDataOffset);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl3.glBindVertexArray(0);

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRange(0.0f, 1.0f);

        start = System.currentTimeMillis();
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

        modelToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        cameraToClipMatrixUnif = gl3.glGetUniformLocation(theProgram, "cameraToClipMatrix");

        float zNear = 1.0f, zFar = 61.0f;

        cameraToClipMatrix.m00 = frustumScale;
        cameraToClipMatrix.m11 = frustumScale;
        cameraToClipMatrix.m22 = (zFar + zNear) / (zNear - zFar);
        cameraToClipMatrix.m23 = -1.0f;
        cameraToClipMatrix.m32 = (2 * zFar * zNear) / (zNear - zFar);

        cameraToClipMatrix.toDfb(matBuffer);

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, matBuffer);
        gl3.glUseProgram(0);
    }

    private void initializeVertexBuffers(GL3 gl3) {

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
    
    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        gl3.glUseProgram(theProgram);

        gl3.glBindVertexArray(vao.get(0));

        float elapsedTime = (System.currentTimeMillis() - start) / 1_000f;
        for (Instance instance : instanceList) {

            Mat4 transformMatrix = instance.constructMatrix(elapsedTime);

            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, transformMatrix.toDfb(matBuffer));
            gl3.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);
        }

        gl3.glBindVertexArray(0);
        gl3.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        cameraToClipMatrix.m00 = frustumScale * (h / (float) w);
        cameraToClipMatrix.m11 = frustumScale;

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matBuffer));
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
                animator.remove(glWindow);
                glWindow.destroy();
                break;
        }
    }
    
    public enum Mode {

        NullScale,
        StaticUniformScale,
        StaticNonUniformScale,
        DynamicUniformScale,
        DynamicNonUniformScale
    }
}
