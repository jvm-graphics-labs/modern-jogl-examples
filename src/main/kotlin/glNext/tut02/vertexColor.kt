package glNext.tut02

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.vec._4.Vec4
import main.framework.Framework
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    VertexColor_Next().setup("Tutorial 02 - Vertex Colors")
}

class VertexColor_Next : Framework() {

    val VERTEX_SHADER = "tut02/vertex-colors.vert"
    val FRAGMENT_SHADER = "tut02/vertex-colors.frag"

    var theProgram = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexData = floatBufferOf(
            +0.0f, +0.500f, 0.0f, 1.0f,
            +0.5f, -0.366f, 0.0f, 1.0f,
            -0.5f, -0.366f, 0.0f, 1.0f,
            +1.0f, +0.000f, 0.0f, 1.0f,
            +0.0f, +1.000f, 0.0f, 1.0f,
            +0.0f, +0.000f, 1.0f, 1.0f)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)
    }

    fun initializeProgram(gl: GL3) {
        theProgram = programOf(gl, javaClass, "tut02", "vertex-colors.vert", "vertex-colors.frag")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffers(vertexBufferObject)

        withArrayBuffer(vertexBufferObject) { data(vertexData, GL_STATIC_DRAW) }
    }

    override fun display(gl: GL3) = with(gl) {

        clear { color(0) }

        usingProgram(theProgram) {
            withVertexLayout(vertexBufferObject, glf.pos4_col4, 0, Vec4.SIZE * 3) { glDrawArrays(3) }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {
        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffer(vertexBufferObject)
        glDeleteVertexArray(vao)

        destroyBuffers(vertexBufferObject, vao, vertexData)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}
