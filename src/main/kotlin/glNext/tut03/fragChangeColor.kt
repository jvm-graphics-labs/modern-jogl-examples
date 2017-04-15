package glNext.tut03

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
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

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        startingTime = System.currentTimeMillis()
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut03", "calc-offset.vert", "calc-color.frag")

        withProgram(theProgram) {

            elapsedTimeUniform = "time".location

            use {
                "loopDuration".location.float = 5f
                "fragLoopDuration".location.float = 5f
            }
        }
    }

    fun initializeVertexBuffer(gl: GL3) =
            gl.initArrayBuffer(positionBufferObject) { data(vertexPositions, GL_STATIC_DRAW) }

    override fun display(gl: GL3) = with(gl) {

        clear { color(0) }

        usingProgram(theProgram) {

            elapsedTimeUniform.float = (System.currentTimeMillis() - startingTime) / 1_000f

            withVertexLayout(positionBufferObject, glf.pos4) { glDrawArrays(3) }
        }
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