package glNext.tut04

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.size
import main.framework.Framework
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 22.02.2017.
 */

fun main(args: Array<String>) {
    OrthoCube_Next().setup("Tutorial 04 - Ortho Cube")
}

class OrthoCube_Next : Framework() {

    var theProgram = 0
    var offsetUniform = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        faceCull {
            enable()
            cullFace = back
            frontFace = cw
        }
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut04", "ortho-with-offset.vert", "standard-colors.frag")

        withProgram(theProgram) { offsetUniform = "offset".location }
    }

    fun initializeVertexBuffer(gl: GL3) = gl.initArrayBuffer(vertexBufferObject) { data(vertexData, GL_STATIC_DRAW) }

    override fun display(gl: GL3) = with(gl) {

        clear { color(0) }

        usingProgram(theProgram) {

            glUniform2f(offsetUniform, 0.5f, 0.25f)

            val colorData = vertexData.size / 2

            withVertexLayout(vertexBufferObject, glf.pos4_col4, 0, colorData) { glDrawArrays(36) }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {
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