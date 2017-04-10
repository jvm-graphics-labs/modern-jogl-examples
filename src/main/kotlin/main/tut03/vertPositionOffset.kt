package main.tut03

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import glNext.*
import glm.glm
import glm.vec._2.Vec2
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
    VertPositionOffset_().setup("Tutorial 03 - Shader Position Offset")
}

class VertPositionOffset_ : Framework() {

    var theProgram = 0
    var offsetLocation = 0
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

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        startingTime = System.currentTimeMillis()
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut03", "position-offset.vert", "standard.frag")

        offsetLocation = glGetUniformLocation(theProgram, "offset")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl){

        glGenBuffer(positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexPositions, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl){

        val offset = Vec2(0f)
        computePositionOffsets(offset)

        glClearBufferf(GL_COLOR)

        glUseProgram(theProgram)

        glUniform2f(offsetLocation, offset)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4::class)

        glDrawArrays(3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)

        glUseProgram()
    }

    fun computePositionOffsets(offset: Vec2) {

        val loopDuration = 5.0f
        val scale = glm.PIf * 2f / loopDuration

        val elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f

        val currTimeThroughLoop = elapsedTime % loopDuration

        offset.x = glm.cos(currTimeThroughLoop * scale) * .5f
        offset.y = glm.sin(currTimeThroughLoop * scale) * .5f
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){
        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffer(positionBufferObject)
        glDeleteVertexArray(vao)

        destroyBuffers(positionBufferObject, vao, vertexPositions)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}