package main.tut02

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
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    VertexColor_()
}

class VertexColor_ : Framework("Tutorial 02 - Vertex Colors") {

    val VERTEX_SHADER = "tut02/vertex-colors.vert"
    val FRAGMENT_SHADER = "tut02/vertex-colors.frag"

    var theProgram = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexData = floatArrayOf(
            +0.0f, +0.500f, 0.0f, 1.0f,
            +0.5f, -0.366f, 0.0f, 1.0f,
            -0.5f, -0.366f, 0.0f, 1.0f,
            +1.0f, +0.000f, 0.0f, 1.0f,
            +0.0f, +1.000f, 0.0f, 1.0f,
            +0.0f, +0.000f, 1.0f, 1.0f)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])
    }

    fun initializeProgram(gl: GL3) {
        theProgram = programOf(gl, this::class.java, "tut02", "vertex-colors.vert", "vertex-colors.frag")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl){

        val vertexBuffer = vertexData.toFloatBuffer()

        glGenBuffers(1, vertexBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl){

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f))

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)
        glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, Vec4.SIZE * 3.L)

        glDrawArrays(GL_TRIANGLES, 0, 3)

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

        vertexBufferObject.destroy()
        vao.destroy()
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
