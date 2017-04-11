package main.tut03

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.GL_ARRAY_BUFFER
import com.jogamp.opengl.GL.GL_STATIC_DRAW
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import glNext.*
import main.framework.Framework
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.glsl.programOf

/**
 * Created by elect on 21/02/17.
 */

fun main(args: Array<String>) {
    FragChangeColor_().setup("Tutorial 03 - Frag Change Color")
}

class FragChangeColor_ : Framework() {

    var theProgram = 0
    var elapsedTimeUniform = 0
    val positionBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexPositions = floatBufferOf(
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

        theProgram = programOf(gl, javaClass, "tut03", "calc-offset.vert", "calc-color.frag")

        elapsedTimeUniform = glGetUniformLocation(theProgram, "time")

        val loopDurationUnf = glGetUniformLocation(theProgram, "loopDuration")
        val fragLoopDurUnf = glGetUniformLocation(theProgram, "fragLoopDuration")

        glUseProgram(theProgram)
        glUniform1f(loopDurationUnf, 5f)
        glUniform1f(fragLoopDurUnf, 10f)
        glUseProgram()
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffer(positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexPositions, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0)

        glUseProgram(theProgram)

        glUniform1f(elapsedTimeUniform, (System.currentTimeMillis() - startingTime) / 1_000f)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glEnableVertexAttribArray(glf.pos4)
        glVertexAttribPointer(glf.pos4)

        glDrawArrays(3)

        glDisableVertexAttribArray(glf.pos4)

        glUseProgram()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){
        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl){

        glDeleteProgram(theProgram)
        glDeleteBuffers(positionBufferObject)
        glDeleteVertexArrays(vao)

        destroyBuffers(positionBufferObject, vao, vertexPositions)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}