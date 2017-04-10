package main.tut03

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import glNext.*
import glm.glm
import glm.set
import glm.vec._2.Vec2
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    CpuPositionOffset_().setup("Tutorial 03 - CPU Position Offset")
}

class CpuPositionOffset_ : Framework() {

    var theProgram = 0
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

    fun initializeProgram(gl: GL3) {
        theProgram = programOf(gl, javaClass, "tut03", "standard.vert", "standard.frag")
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffer(positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexPositions, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl) {

        val offset = Vec2(0f)

        computePositionOffsets(offset)
        adjustVertexData(gl, offset)

        glClearBufferf(GL_COLOR)

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4::class)

        glDrawArrays(GL_TRIANGLES, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram()
    }

    fun computePositionOffsets(offset: Vec2) {

        val loopDuration = 5.0f
        val scale = glm.PIf * 2.0f / loopDuration

        val elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f

        val fCurrTimeThroughLoop = elapsedTime % loopDuration

        offset.x = glm.cos(fCurrTimeThroughLoop * scale) * 0.5f
        offset.y = glm.sin(fCurrTimeThroughLoop * scale) * 0.5f
    }

    fun adjustVertexData(gl: GL3, offset: Vec2) = with(gl) {

        val newData = floatBufferBig(vertexPositions.capacity())
        repeat(vertexPositions.capacity()) { newData[it] = vertexPositions[it] }

        for (iVertex in 0 until vertexPositions.capacity() step 4) {

            newData[iVertex + 0] = vertexPositions[iVertex + 0] + offset.x
            newData[iVertex + 1] = vertexPositions[iVertex + 1] + offset.y
        }

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glBufferSubData(GL_ARRAY_BUFFER, newData)
        glBindBuffer(GL_ARRAY_BUFFER)

        newData.destroy()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {
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