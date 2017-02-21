/**
 * Created by elect on 21/02/17.
 */

package main.tut01


import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderProgram
import extensions.intBufferBig
import glsl.shaderCodeOf
import main.BYTES
import main.L
import main.SIZE
import main.framework.Framework
import main.framework.Semantic
import vec._4.Vec4


fun main(args: Array<String>) {
    HelloTriangle_()
}

class HelloTriangle_ : Framework("Tutorial 01 - Hello Triangle") {

    val VERTEX_SHADER = "tut01/shader.vert"
    val FRAGMENT_SHADER = "tut01/shader.frag"

    val vertexPositions = floatArrayOf(
            +0.75f, +0.75f, 0.0f, 1.0f,
            +0.75f, -0.75f, 0.0f, 1.0f,
            -0.75f, -0.75f, 0.0f, 1.0f)

    var theProgram = 0
    val positionBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)


    /**
     * Called after the window and OpenGL are initialized. Called exactly once, before the main loop.
     * @param gl
     */
    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])
    }

    fun initializeProgram(gl: GL3) {

        val shaderProgram = ShaderProgram()

        val vertex = shaderCodeOf(VERTEX_SHADER, gl, this::class.java)
        val fragment = shaderCodeOf(FRAGMENT_SHADER, gl, this::class.java)

        shaderProgram.add(vertex)
        shaderProgram.add(fragment)

        shaderProgram.link(gl, System.err)

        vertex.destroy(gl)
        fragment.destroy(gl)

        theProgram = shaderProgram.program()
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions)

        glGenBuffers(1, positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    /**
     * Called to update the display. You don't need to swap the buffers after all of your rendering to display what you rendered,
     * it is done automatically.
     * @param gl
     */
    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f))

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram(0)
    }

    /**
     * Called whenever the window is resized. The new window size is given, in pixels. This is an opportunity to call glViewport or
     * glScissor to keep up with the change in size.
     * @param gl
     * @param w
     * @param h
     */
    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    /**
     * Called at the end, here you want to clean all the resources.
     * @param gl
     */
    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, positionBufferObject)
        glDeleteVertexArrays(1, vao)

        positionBufferObject.destroy()
        vao.destroy()
    }

    /**
     * Called whenever a key on the keyboard was pressed. The key is given by the KeyCode(). It's often a good idea to have the escape
     * key to exit the program.
     * @param keyEvent
     */
    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
    }
}