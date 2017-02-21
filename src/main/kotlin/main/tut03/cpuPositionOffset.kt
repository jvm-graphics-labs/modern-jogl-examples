package main.tut03

import buffer.BufferUtils
import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderProgram
import extensions.intBufferBig
import extensions.toFloatBuffer
import glsl.shaderCodeOf
import main.*
import main.framework.Framework
import main.framework.Semantic
import vec._2.Vec2
import vec._4.Vec4

/**
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    CpuPositionOffset_()
}

class CpuPositionOffset_ : Framework("Tutorial 03 - CPU Position Offset") {

    val VERTEX_SHADER = "tut03/standard.vert"
    val FRAGMENT_SHADER = "tut03/standard.frag"

    var theProgram = 0
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

        val shaderProgram = ShaderProgram()

        val vertex = shaderCodeOf(VERTEX_SHADER, gl, this::class.java)
        val fragment = shaderCodeOf(FRAGMENT_SHADER, gl, this::class.java)

        shaderProgram.add(vertex)
        shaderProgram.add(fragment)

        shaderProgram.link(gl, System.err)

        vertex.destroy(gl)
        fragment.destroy(gl)

        theProgram = shaderProgram.program()
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexPositions)

        glGenBuffers(1, positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexPositions.size * Float.BYTES.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl) {

        val offset = Vec2(0f)

        computePositionOffsets(offset)
        adjustVertexData(gl, offset)

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f))

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL3.GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram(0)
    }

    fun computePositionOffsets(offset: Vec2) {

        val loopDuration = 5.0f
        val scale = (Math.PI * 2.0f / loopDuration).f

        val elapsedTime = (System.currentTimeMillis() - startingTime) / 1_000f

        val fCurrTimeThroughLoop = elapsedTime % loopDuration

        offset.x = glm.cos(fCurrTimeThroughLoop * scale) * 0.5f
        offset.y = glm.sin(fCurrTimeThroughLoop * scale) * 0.5f
    }

    fun adjustVertexData(gl: GL3, offset: Vec2) = with(gl) {

        val newData = vertexPositions.clone()

        for (iVertex in vertexPositions.indices step 4) {

            newData[iVertex + 0] += offset.x
            newData[iVertex + 1] += offset.y
        }

        val buffer = newData.toFloatBuffer()

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject.get(0))
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer.SIZE.L, buffer)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        buffer.destroy()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, positionBufferObject)
        glDeleteVertexArrays(1, vao)

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