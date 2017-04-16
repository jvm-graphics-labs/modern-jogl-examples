package glNext.tut04

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.size
import main.framework.Framework
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.glsl.programOf

/**
 * Created by GBarbieri on 22.02.2017.
 */

fun main(args: Array<String>) {
    MatrixPerspective_Next().setup("Tutorial 04 - Matrix Perspective")
}

class MatrixPerspective_Next : Framework() {

    var theProgram = 0
    var offsetUniform = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)

    var perspectiveMatrix = FloatArray(16)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        cullFace {
            enable()
            cullFace = back
            frontFace = cw
        }
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut04", "matrix-perspective.vert", "standard-colors.frag")

        withProgram(theProgram) {

            offsetUniform = "offset".location
            val perspectiveMatrixUnif = "perspectiveMatrix".location

            val frustumScale = 1.0f
            val zNear = 0.5f
            val zFar = 3.0f

            perspectiveMatrix[0] = frustumScale
            perspectiveMatrix[5] = frustumScale
            perspectiveMatrix[10] = (zFar + zNear) / (zNear - zFar)
            perspectiveMatrix[14] = 2f * zFar * zNear / (zNear - zFar)
            perspectiveMatrix[11] = -1.0f

            use { glUniformMatrix4f(perspectiveMatrixUnif, perspectiveMatrix) }
        }
    }

    fun initializeVertexBuffer(gl: GL3) = gl.initArrayBuffer(vertexBufferObject){data(vertexData, GL_STATIC_DRAW)}

    override fun display(gl: GL3) = with(gl) {

        clear { color(0) }

        usingProgram(theProgram) {

            glUniform2f(offsetUniform, 0.5f)

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
}