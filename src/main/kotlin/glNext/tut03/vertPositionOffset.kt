package glNext.tut03

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.glm
import glm.vec._2.Vec2
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.glsl.programOf
import glm.vec._4.Vec4

/**
 * Created by elect on 21/02/17.
 */

fun main(args: Array<String>) {
    VertPositionOffset_Next().setup("Tutorial 03 - Shader Position Offset")
}

class VertPositionOffset_Next : Framework() {

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

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffers(positionBufferObject)

        withArrayBuffer(positionBufferObject) {
            data(vertexPositions, GL_STATIC_DRAW)
        }
    }

    override fun display(gl: GL3) = with(gl) {

        val offset = Vec2(0f)
        computePositionOffsets(offset)

        clear { color(0, 0, 0, 1) }

        usingProgram(theProgram) {

            offsetLocation.vec2 = offset

            withVertexLayout(positionBufferObject, Vec4::class, Semantic.Attr.POSITION) {
                glDrawArrays(3)
            }
        }
    }

    fun computePositionOffsets(offset: Vec2) {

        val loopDuration = 5.0f
        val scale = glm.PIf * 2f / loopDuration

        val elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f

        val currTimeThroughLoop = elapsedTime % loopDuration

        offset.x = glm.cos(currTimeThroughLoop * scale) * .5f
        offset.y = glm.sin(currTimeThroughLoop * scale) * .5f
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