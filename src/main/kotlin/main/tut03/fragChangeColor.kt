package main.tut03

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
 * Created by elect on 21/02/17.
 */

fun main(args: Array<String>) {
    FragChangeColor_()
}

class FragChangeColor_ : Framework("Tutorial 03 - Frag Change Color") {

    var theProgram = 0
    var elapsedTimeUniform = 0
    val positionBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexPositions = floatArrayOf(
            +0.25f, +0.25f, 0.0f, 1.0f,
            +0.25f, -0.25f, 0.0f, 1.0f,
            -0.25f, -0.25f, 0.0f, 1.0f)
    var startingTime = 0L

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        startingTime = System.currentTimeMillis()
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, this::class.java, "tut03", "calc-offset.vert", "calc-color.frag")

        elapsedTimeUniform = glGetUniformLocation(theProgram, "time")

        val loopDurationUnf = glGetUniformLocation(theProgram, "loopDuration")
        val fragLoopDurUnf = glGetUniformLocation(theProgram, "fragLoopDuration")

        glUseProgram(theProgram)
        glUniform1f(loopDurationUnf, 5f)
        glUniform1f(fragLoopDurUnf, 10f)
        glUseProgram(0)
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        val vertexBuffer = vertexPositions.toFloatBuffer()

        glGenBuffers(1, positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f))

        glUseProgram(theProgram)

        glUniform1f(elapsedTimeUniform, (System.currentTimeMillis() - startingTime) / 1_000f)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)

        glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) {

        gl.glDeleteProgram(theProgram)
        gl.glDeleteBuffers(1, positionBufferObject)
        gl.glDeleteVertexArrays(1, vao)

        positionBufferObject.destroy()
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