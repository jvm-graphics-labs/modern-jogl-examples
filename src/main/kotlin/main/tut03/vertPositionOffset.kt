package main.tut03

import uno.buffer.*
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import uno.glsl.programOf
import uno.glsl.shaderCodeOf
import glm.*
import main.framework.Framework
import main.framework.Semantic
import glm.vec._2.Vec2
import glm.vec._4.Vec4

/**
 * Created by elect on 21/02/17.
 */

fun main(args: Array<String>) {
    VertPositionOffset_()
}

class VertPositionOffset_ : Framework("Tutorial 03 - Shader Position Offset") {

    var theProgram = 0
    var offsetLocation = 0
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

    fun initializeProgram(gl: GL3) {

        theProgram = programOf(gl, this::class.java, "tut03", "position-offset.vert", "standard.frag")

        offsetLocation = gl.glGetUniformLocation(theProgram, "offset")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl){

        val vertexBuffer = vertexPositions.toFloatBuffer()

        glGenBuffers(1, positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl){

        val offset = Vec2(0f)
        computePositionOffsets(offset)

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0f, 0f, 0f, 1f))

        glUseProgram(theProgram)

        glUniform2f(offsetLocation, offset.x, offset.y)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4.length, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)

        glUseProgram(0)
    }

    fun computePositionOffsets(offset: Vec2) {

        val loopDuration = 5.0f
        val scale = glm.PIf * 2f / loopDuration

        val elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f

        val currTimeThroughLoop = elapsedTime % loopDuration

        offset.x = glm.cos(currTimeThroughLoop * scale) * .5f
        offset.y = glm.sin(currTimeThroughLoop * scale) * .5f
    }

    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, positionBufferObject)
        glDeleteVertexArrays(1, vao)

        destroyBuffers(positionBufferObject, vao)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}