package main.tut02

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import glNext.*
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    VertexColor_().setup("Tutorial 02 - Vertex Colors")
}

class VertexColor_ : Framework() {

    val VERTEX_SHADER = "tut02/vertex-colors.vert"
    val FRAGMENT_SHADER = "tut02/vertex-colors.frag"

    var theProgram = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexData = floatBufferOf(
            +0.0f, +0.500f, 0.0f, 1.0f,
            +0.5f, -0.366f, 0.0f, 1.0f,
            -0.5f, -0.366f, 0.0f, 1.0f,
            +1.0f, +0.0f, 0.0f, 1.0f,
            +0.0f, +1.0f, 0.0f, 1.0f,
            +0.0f, +0.0f, 1.0f, 1.0f)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArray(vao)
        glBindVertexArray(vao)
    }

    fun initializeProgram(gl: GL3) {
        theProgram = programOf(gl, javaClass, "tut02", "vertex-colors.vert", "vertex-colors.frag")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl){

        glGenBuffer(vertexBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl){

        glClearBufferf(GL_COLOR, 0)

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glEnableVertexAttribArray(glf.pos4_col4)
        glEnableVertexAttribArray(glf.pos4_col4[1])
        glVertexAttribPointer(glf.pos4_col4, 0)
        glVertexAttribPointer(glf.pos4_col4[1], Vec4.SIZE * 3)

        glDrawArrays(3)

        glDisableVertexAttribArray(glf.pos4_col4)
        glDisableVertexAttribArray(glf.pos4_col4[1])
        glUseProgram()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){
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
