package main.tut04

import buffer.BufferUtils
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderProgram
import extensions.intBufferBig
import glsl.shaderCodeOf
import main.framework.Framework
import main.framework.Semantic
import vec._4.Vec4

/**
 * Created by GBarbieri on 22.02.2017.
 */

fun main(args: Array<String>) {
    ShaderPerspective_()
}

class ShaderPerspective_ : Framework("Tutorial 04 - Shader Perspective") {

    val VERTEX_SHADER = "tut04/manual-perspective.vert"
    val FRAGMENT_SHADER = "tut04/standard-colors.frag"

    var theProgram = 0
    var offsetUniform = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexData = floatArrayOf(
            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,

            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,

            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,

            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -1.25f, 1.0f,
            +0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, -0.25f, -2.75f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            +0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,

            +0.25f, +0.25f, -2.75f, 1.0f,
            -0.25f, +0.25f, -1.25f, 1.0f,
            -0.25f, +0.25f, -2.75f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,
            +0.25f, -0.25f, -1.25f, 1.0f,

            +0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -2.75f, 1.0f,
            -0.25f, -0.25f, -1.25f, 1.0f,


            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,

            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,

            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,

            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,
            0.8f, 0.8f, 0.8f, 1.0f,

            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,

            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.0f, 1.0f,

            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,

            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f,
            0.0f, 1.0f, 1.0f, 1.0f)

    override fun init(gl: GL3) = with(gl){

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)
    }

    fun initializeProgram(gl: GL3) =with(gl){

        val shaderProgram = ShaderProgram()

        val vertex = shaderCodeOf(VERTEX_SHADER, gl, this::class.java)
        val fragment = shaderCodeOf(FRAGMENT_SHADER, gl, this::class.java)

        shaderProgram.add(vertex)
        shaderProgram.add(fragment)

        shaderProgram.link(gl, System.err)

        vertex.destroy(gl)
        fragment.destroy(gl)

        theProgram = shaderProgram.program()

        offsetUniform = gl.glGetUniformLocation(theProgram, "offset")

        gl.glUseProgram(theProgram)
        gl.glUniform1f(
                gl.glGetUniformLocation(theProgram, "frustumScale"),
                1.0f)
        gl.glUniform1f(
                gl.glGetUniformLocation(theProgram, "zNear"),
                1.0f)
        gl.glUniform1f(
                gl.glGetUniformLocation(theProgram, "zFar"),
                3.0f)
        gl.glUseProgram(0)
    }

    fun initializeVertexBuffer(gl3: GL3) {

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData)

        gl3.glGenBuffers(1, vertexBufferObject)

        gl3.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0))
        gl3.glBufferData(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * java.lang.Float.BYTES).toLong(), vertexBuffer, GL_STATIC_DRAW)
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0)

        BufferUtils.destroyDirectBuffer(vertexBuffer)
    }

    override fun display(gl: GL3) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f))

        gl.glUseProgram(theProgram)

        gl.glUniform2f(offsetUniform, 0.5f, 0.5f)

        val colorData = vertexData.size * java.lang.Float.BYTES / 2
        gl.glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject.get(0))
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION)
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR)
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorData.toLong())

        gl.glDrawArrays(GL_TRIANGLES, 0, 36)

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION)
        gl.glDisableVertexAttribArray(Semantic.Attr.COLOR)

        gl.glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) {

        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) {

        gl.glDeleteProgram(theProgram)
        gl.glDeleteBuffers(1, vertexBufferObject)
        gl.glDeleteVertexArrays(1, vao)

        BufferUtils.destroyDirectBuffer(vao)
        BufferUtils.destroyDirectBuffer(vertexBufferObject)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
    }
}