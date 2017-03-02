package main.tut04

import buffer.BufferUtils
import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.glsl.ShaderProgram
import extensions.intBufferBig
import extensions.toFloatBuffer
import glsl.programOf
import glsl.shaderCodeOf
import main.L
import main.SIZE
import main.framework.Framework
import main.framework.Semantic
import vec._4.Vec4

/**
 * Created by GBarbieri on 22.02.2017.
 */

fun main(args: Array<String>) {
    OrthoCube_()
}

class OrthoCube_ : Framework("Tutorial 04 - Ortho Cube") {

    var theProgram = 0
    var offsetUniform = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)
    }

    fun initializeProgram(gl: GL3) {

        theProgram = programOf(gl, this::class.java, "tut04", "ortho-with-offset.vert", "standard-colors.frag")

        offsetUniform = gl.glGetUniformLocation(theProgram, "offset")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()

        glGenBuffers(1, vertexBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f))

        glUseProgram(theProgram)

        glUniform2f(offsetUniform, 0.5f, 0.25f)

        val colorData = vertexData.size * java.lang.Float.BYTES / 2
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)
        glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorData.L)

        glDrawArrays(GL_TRIANGLES, 0, 36)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glDisableVertexAttribArray(Semantic.Attr.COLOR)

        glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, vertexBufferObject)
        glDeleteVertexArrays(1, vao)

        vao.destroy()
        vertexBufferObject.destroy()
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
    }

    val vertexData = floatArrayOf(
            +0.25f, +0.25f, +0.75f, 1.0f,
            +0.25f, -0.25f, +0.75f, 1.0f,
            -0.25f, +0.25f, +0.75f, 1.0f,

            +0.25f, -0.25f, +0.75f, 1.0f,
            -0.25f, -0.25f, +0.75f, 1.0f,
            -0.25f, +0.25f, +0.75f, 1.0f,

            +0.25f, +0.25f, -0.75f, 1.0f,
            -0.25f, +0.25f, -0.75f, 1.0f,
            +0.25f, -0.25f, -0.75f, 1.0f,

            +0.25f, -0.25f, -0.75f, 1.0f,
            -0.25f, +0.25f, -0.75f, 1.0f,
            -0.25f, -0.25f, -0.75f, 1.0f,

            -0.25f, +0.25f, +0.75f, 1.0f,
            -0.25f, -0.25f, +0.75f, 1.0f,
            -0.25f, -0.25f, -0.75f, 1.0f,

            -0.25f, +0.25f, +0.75f, 1.0f,
            -0.25f, -0.25f, -0.75f, 1.0f,
            -0.25f, +0.25f, -0.75f, 1.0f,

            +0.25f, +0.25f, +0.75f, 1.0f,
            +0.25f, -0.25f, -0.75f, 1.0f,
            +0.25f, -0.25f, +0.75f, 1.0f,

            +0.25f, +0.25f, +0.75f, 1.0f,
            +0.25f, +0.25f, -0.75f, 1.0f,
            +0.25f, -0.25f, -0.75f, 1.0f,

            +0.25f, +0.25f, -0.75f, 1.0f,
            +0.25f, +0.25f, +0.75f, 1.0f,
            -0.25f, +0.25f, +0.75f, 1.0f,

            +0.25f, +0.25f, -0.75f, 1.0f,
            -0.25f, +0.25f, +0.75f, 1.0f,
            -0.25f, +0.25f, -0.75f, 1.0f,

            +0.25f, -0.25f, -0.75f, 1.0f,
            -0.25f, -0.25f, +0.75f, 1.0f,
            +0.25f, -0.25f, +0.75f, 1.0f,

            +0.25f, -0.25f, -0.75f, 1.0f,
            -0.25f, -0.25f, -0.75f, 1.0f,
            -0.25f, -0.25f, +0.75f, 1.0f,


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
}