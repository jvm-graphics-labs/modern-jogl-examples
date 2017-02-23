package main.tut05

import buffer.BufferUtils
import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import extensions.floatBufferBig
import extensions.intBufferBig
import extensions.toFloatBuffer
import extensions.toShortBuffer
import glsl.programOf
import main.*
import main.framework.Framework
import main.framework.Semantic
import vec._3.Vec3
import vec._4.Vec4

/**
 * Created by GBarbieri on 23.02.2017.
 */

fun main(args: Array<String>) {
    DepthClamping_()
}

class DepthClamping_ : Framework("Tutorial 05 - Depth Clamping") {

    object Buffer {
        val VERTEX = 0
        val INDEX = 1
        val MAX = 2
    }

    var theProgram = 0
    var offsetUniform = 0
    var perspectiveMatrixUnif = 0
    val numberOfVertices = 36

    val perspectiveMatrix = floatBufferBig(16)
    val frustumScale = 1.0f

    val bufferObject = intBufferBig(Buffer.MAX)
    val vao = intBufferBig(1)

    val RIGHT_EXTENT = 0.8f
    val LEFT_EXTENT = -RIGHT_EXTENT
    val TOP_EXTENT = 0.20f
    val MIDDLE_EXTENT = 0.0f
    val BOTTOM_EXTENT = -TOP_EXTENT
    val FRONT_EXTENT = -1.25f
    val REAR_EXTENT = -1.75f

    val GREEN_COLOR = floatArrayOf(0.75f, 0.75f, 1.0f, 1.0f)
    val BLUE_COLOR = floatArrayOf(0.0f, 0.5f, 0.0f, 1.0f)
    val RED_COLOR = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    val GREY_COLOR = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f)
    val BROWN_COLOR = floatArrayOf(0.5f, 0.5f, 0.0f, 1.0f)

    val vertexData = floatArrayOf(
            //Object 1 positions
            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, MIDDLE_EXTENT, FRONT_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            LEFT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,
            LEFT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, TOP_EXTENT, REAR_EXTENT,
            RIGHT_EXTENT, BOTTOM_EXTENT, REAR_EXTENT,

            //Object 2 positions
            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, RIGHT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,

            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
            MIDDLE_EXTENT, LEFT_EXTENT, FRONT_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            BOTTOM_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            TOP_EXTENT, RIGHT_EXTENT, REAR_EXTENT,
            TOP_EXTENT, LEFT_EXTENT, REAR_EXTENT,
            BOTTOM_EXTENT, LEFT_EXTENT, REAR_EXTENT,

            //Object 1 colors
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],

            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],

            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],

            //Object 2 colors
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],
            BROWN_COLOR[0], BROWN_COLOR[1], BROWN_COLOR[2], BROWN_COLOR[3],

            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],
            BLUE_COLOR[0], BLUE_COLOR[1], BLUE_COLOR[2], BLUE_COLOR[3],

            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],
            GREEN_COLOR[0], GREEN_COLOR[1], GREEN_COLOR[2], GREEN_COLOR[3],

            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3],
            GREY_COLOR[0], GREY_COLOR[1], GREY_COLOR[2], GREY_COLOR[3])

    val indexData = shortArrayOf(

            0, 2, 1,
            3, 2, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            11, 13, 12,

            14, 16, 15,
            17, 16, 14)

    var depthClampingActive = false

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeBuffers(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        val colorData = Float.BYTES * 3 * numberOfVertices
        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0)
        glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorData.L)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject[Buffer.INDEX])

        glBindVertexArray(0)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRange(0.0, 1.0)
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, this::class.java, "tut05", "standard.vert", "standard.frag")

        offsetUniform = glGetUniformLocation(theProgram, "offset")

        perspectiveMatrixUnif = glGetUniformLocation(theProgram, "perspectiveMatrix")

        val zNear = 1.0f
        val zFar = 3.0f

        perspectiveMatrix[0] = frustumScale
        perspectiveMatrix[5] = frustumScale
        perspectiveMatrix[10] = (zFar + zNear) / (zNear - zFar)
        perspectiveMatrix[14] = 2f * zFar * zNear / (zNear - zFar)
        perspectiveMatrix[11] = -1.0f

        glUseProgram(theProgram)
        glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix)
        glUseProgram(0)
    }

    fun initializeBuffers(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val indexBuffer = indexData.toShortBuffer()

        glGenBuffers(Buffer.MAX, bufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.INDEX])
        glBufferData(GL_ARRAY_BUFFER, indexBuffer.SIZE.L, indexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
        indexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl){

        if (depthClampingActive)
            glDisable(GL_DEPTH_CLAMP)
        else
            glEnable(GL_DEPTH_CLAMP)
        
        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        glUseProgram(theProgram)

        glBindVertexArray(vao[0])

        glUniform3f(offsetUniform, 0.0f, 0.0f, 0.5f)
        glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)

        glUniform3f(offsetUniform, 0.0f, 0.0f, -1.0f)
        glDrawElementsBaseVertex(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0, numberOfVertices / 2)

        glBindVertexArray(0)
        glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        perspectiveMatrix[0] = frustumScale * (h / w.f)
        perspectiveMatrix[5] = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4fv(perspectiveMatrixUnif, 1, false, perspectiveMatrix)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(Buffer.MAX, bufferObject)
        glDeleteVertexArrays(1, vao)

        vao.destroy()
        bufferObject.destroy()
        perspectiveMatrix.destroy()
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
            KeyEvent.VK_SPACE -> depthClampingActive = !depthClampingActive
        }
    }
}