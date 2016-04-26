/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut01.main;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
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
import framework.BufferUtils;
import framework.Semantic;
import framework.Framework;
import glm.vec._4.Vec4;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author gbarbieri
 */
public class HelloTriangle extends Framework {

    private final String SHADERS_ROOT = "src/tut01/main/shaders";
    private final String VERT_SHADER_SOURCE = "vertex-shader";
    private final String FRAG_SHADER_SOURCE = "fragment-shader";

    public static void main(String[] args) {
        HelloTriangle helloTriangle = new HelloTriangle("Tutorial 01 - Hello Triangle");
    }

    public HelloTriangle(String title) {
        super(title);
    }

    private int theProgram;
    private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1), vao = GLBuffers.newDirectIntBuffer(1);
    private float[] vertexPositions = new float[]{
        +0.75f, +0.75f, 0.0f, 1.0f,
        +0.75f, -0.75f, 0.0f, 1.0f,
        -0.75f, -0.75f, 0.0f, 1.0f};

    /**
     * Called after the window and OpenGL are initialized. Called exactly once, before the main loop.
     *
     * @param gl3
     */
    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        initializeVertexBuffer(gl3);

        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));
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
    }

    private void initializeVertexBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions);

        gl3.glGenBuffers(1, positionBufferObject);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
    }

    /**
     * Called to update the display.
     * You don't need to swap the buffers after all of your rendering to display
     * what you rendered, it is done automatically.
     *
     * @param gl3
     */
    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 1.0f));

        gl3.glUseProgram(theProgram);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0);

        gl3.glDrawArrays(GL_TRIANGLES, 0, 3);

        gl3.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl3.glUseProgram(0);
    }

    /**
     * Called whenever the window is resized. The new window size is given, in pixels.
     * This is an opportunity to call glViewport or glScissor to keep up with the change in size.
     *
     * @param gl3
     * @param w
     * @param h
     */
    @Override
    public void reshape(GL3 gl3, int w, int h) {

        gl3.glViewport(0, 0, w, h);
    }
    
    /**
     * Called at the end, here you want to clean all the resources.
     * @param gl3 
     */
    @Override
    protected void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);
        gl3.glDeleteBuffers(1, positionBufferObject);
        gl3.glDeleteVertexArrays(1, vao);
        
        BufferUtils.destroyDirectBuffer(positionBufferObject);
        BufferUtils.destroyDirectBuffer(vao);
    }

    /**
     * Called whenever a key on the keyboard was pressed.
     * The key is given by the KeyCode().
     * It's often a good idea to have the escape key to exit the program.
     *
     * @param keyEvent
     */
    @Override
    protected void keyboard(KeyEvent keyEvent) {

        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.stop();
                glWindow.destroy();
                break;
        }
    }
}