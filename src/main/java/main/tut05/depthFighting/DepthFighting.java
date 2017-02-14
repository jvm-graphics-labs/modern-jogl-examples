/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut05.depthFighting;

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
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import buffer.BufferUtils;
import main.framework.Framework;
import main.framework.Semantic;
import vec._3.Vec3;
import vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * ********************************************* TO FINISH
 *
 *********************************************************
 * @author gbarbieri
 */
public class DepthFighting extends Framework {

    private final String SHADERS_ROOT = "src/tut05/depthClamping/shaders";
    private final String SHADERS_SOURCE = "standard";

    public static void main(String[] args) {
        new DepthFighting("Tutorial 05 - Depth Clamping");
    }

    public DepthFighting(String title) {
        super(title);
    }

    private interface Buffer {

        public final static int VERTEX = 0;
        public final static int INDEX = 1;
        public final static int MAX = 2;
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
        //
        4, 5, 7,
        5, 6, 7};
    private boolean depthClampingActive = false;
    private long timeStart;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);
        initializeBuffers(gl3);

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        int colorData = Float.BYTES * 3 * numberOfVertices;
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferObject.get(Buffer.VERTEX));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0);
        gl3.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorData);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject.get(Buffer.INDEX));

        gl3.glBindVertexArray(0);

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRange(0.0f, 1.0f);

        timeStart = System.currentTimeMillis();
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

    @Override
    public void display(GL3 gl3) {

        if (depthClampingActive) {
            gl3.glDisable(GL_DEPTH_CLAMP);
        } else {
            gl3.glEnable(GL_DEPTH_CLAMP);
        }

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        gl3.glUseProgram(theProgram);
        gl3.glBindVertexArray(vao.get(0));

        float zOffset = calcZOFfset();
        gl3.glUniform3f(offsetUniform, 0.0f, 0.0f, zOffset);
        gl3.glDrawElements(GL_TRIANGLES, indexData.length, GL_UNSIGNED_SHORT, 0);

        gl3.glBindVertexArray(0);
        gl3.glUseProgram(0);
    }

    private float calcZOFfset() {

        float start = 2534.0f;
        float loopDuration = 5.0f;
        float scale = (float) (Math.PI * 2.0f / loopDuration);

        float elapsedTime = (System.currentTimeMillis() - timeStart) / 1_000.0f;

        float currTimeThroughLoop = elapsedTime % loopDuration;

//        float ret = Math.cos(currTimeThroughLoop * scale) * 500.0f - start;
        float ret = delta - start;

        return ret;
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
        gl3.glDeleteVertexArrays(1, vao);

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
            case KeyEvent.VK_SPACE:
                depthClampingActive = !depthClampingActive;
                break;
        }
    }
}
