package main.tut04

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import glNext.*
import glm.size
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 22.02.2017.
 */

fun main(args: Array<String>) {
    OrthoCube_().setup("Tutorial 04 - Ortho Cube")
}

class OrthoCube_ : Framework() {

    var theProgram = 0
    var offsetUniform = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut04", "ortho-with-offset.vert", "standard-colors.frag")

        offsetUniform = glGetUniformLocation(theProgram, "offset")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffer(vertexBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0)

        glUseProgram(theProgram)

        glUniform2f(offsetUniform, 0.5f, 0.25f)

        val colorData = vertexData.size / 2
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4::class)
        glVertexAttribPointer(Semantic.Attr.COLOR, Vec4::class, colorData)

        glDrawArrays(36)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glDisableVertexAttribArray(Semantic.Attr.COLOR)

        glUseProgram()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){
        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffer(vertexBufferObject)
        glDeleteVertexArray(vao)

        destroyBuffers(vao, vertexBufferObject, vertexData)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }

    val vertexData = floatBufferOf(
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