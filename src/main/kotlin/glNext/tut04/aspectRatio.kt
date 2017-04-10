package glNext.tut04

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.f
import glm.size
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.glsl.programOf

/**
 * Created by elect on 21/02/17.
 */

fun main(args: Array<String>) {
    AspectRatio_Next().setup("Tutorial 04 - Aspect Ratio")
}

class AspectRatio_Next : Framework() {

    var theProgram = 0
    var offsetUniform = 0
    var perspectiveMatrixUnif = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)

    var perspectiveMatrix = FloatArray(16)
    val frustumScale = 1.0f

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        faceCulling(true, frontFace = GL_CW)
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut04", "matrix-perspective.vert", "standard-colors.frag")

        usingProgram(theProgram) {

            offsetUniform = "offset".location

            perspectiveMatrixUnif = "perspectiveMatrix".location

            val zNear = 0.5f
            val zFar = 3.0f

            perspectiveMatrix[0] = frustumScale
            perspectiveMatrix[5] = frustumScale
            perspectiveMatrix[10] = (zFar + zNear) / (zNear - zFar)
            perspectiveMatrix[14] = 2f * zFar * zNear / (zNear - zFar)
            perspectiveMatrix[11] = -1.0f

            glUniformMatrix4f(perspectiveMatrixUnif, perspectiveMatrix)
        }
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffers(vertexBufferObject)

        withArrayBuffer(vertexBufferObject) {
            data(vertexData, GL_STATIC_DRAW)
        }
    }

    override fun display(gl: GL3) = with(gl) {

        clear { color(0) }

        usingProgram(theProgram) {

            glUniform2f(offsetUniform, 1.5f, 0.5f)

            val colorData = vertexData.size / 2

            withVertexLayout(vertexBufferObject, Vec4::class,
                    Semantic.Attr.POSITION to 0,
                    Semantic.Attr.COLOR to colorData){

                glDrawArrays(36)

                glDisableVertexAttribArray(Semantic.Attr.POSITION)
                glDisableVertexAttribArray(Semantic.Attr.COLOR)
            }

        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        perspectiveMatrix[0] = frustumScale / (w / h.f)
        perspectiveMatrix[5] = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4f(perspectiveMatrixUnif, perspectiveMatrix)
        glUseProgram(theProgram)

        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(vertexBufferObject)
        glDeleteVertexArrays(vao)

        destroyBuffers(vertexBufferObject, vao, vertexData)
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