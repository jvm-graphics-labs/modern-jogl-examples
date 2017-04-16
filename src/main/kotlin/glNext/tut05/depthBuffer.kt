package glNext.tut05

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import glNext.*
import glm.f
import glm.size
import glm.vec._3.Vec3
import main.framework.Framework
import uno.buffer.destroyBuffers
import uno.buffer.floatBufferOf
import uno.buffer.intBufferBig
import uno.buffer.shortBufferOf
import uno.glsl.programOf

/**
 * Created by elect on 22/02/17.
 */

fun main(args: Array<String>) {
    DepthBuffer_Next().setup("Tutorial 05 - Depth Buffering")
}

class DepthBuffer_Next : Framework() {

    object Buffer {
        val VERTEX = 0
        val INDEX = 1
        val MAX = 2
    }

    var theProgram = 0
    var offsetUniform = 0
    var perspectiveMatrixUnif = 0
    val numberOfVertices = 36
    val perspectiveMatrix = FloatArray(16)
    val frustumScale = 1.0f
    val bufferObject = intBufferBig(Buffer.MAX)
    val vao = intBufferBig(1)


    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeBuffers(gl)

        initVertexArray(vao) {
            val colorData = Vec3.SIZE * numberOfVertices
            array(bufferObject[Buffer.VERTEX], glf.pos3_col4, 0, colorData)
            element(bufferObject[Buffer.INDEX])
        }

        cullFace {
            enable()
            cullFace = back
            frontFace = cw
        }

        depth {
            test = true
            mask = true
            func = lEqual
            range = 0.0 .. 1.0
        }
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut05", "standard.vert", "standard.frag")

        withProgram(theProgram) {

            offsetUniform = "offset".location
            perspectiveMatrixUnif = "perspectiveMatrix".location

            val zNear = 1.0f
            val zFar = 3.0f

            perspectiveMatrix[0] = frustumScale
            perspectiveMatrix[5] = frustumScale
            perspectiveMatrix[10] = (zFar + zNear) / (zNear - zFar)
            perspectiveMatrix[14] = 2f * zFar * zNear / (zNear - zFar)
            perspectiveMatrix[11] = -1.0f

            use { glUniformMatrix4f(perspectiveMatrixUnif, perspectiveMatrix) }
        }
    }

    fun initializeBuffers(gl: GL3) = with(gl) {

        glGenBuffers(bufferObject)

        withArrayBuffer(bufferObject[Buffer.VERTEX]) { data(vertexData, GL_STATIC_DRAW) }

        withElementBuffer(bufferObject[Buffer.INDEX]) { data(indexData, GL_STATIC_DRAW) }
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0)
        glClearBufferf(GL_DEPTH)

        usingProgram(theProgram) {

            withVertexArray(vao) {

                glUniform3f(offsetUniform)
                glDrawElements(indexData.size, GL_UNSIGNED_SHORT)

                glUniform3f(offsetUniform, 0.0f, 0.0f, -1.0f)
                glDrawElementsBaseVertex(indexData.size, GL_UNSIGNED_SHORT, 0, numberOfVertices / 2)
            }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        perspectiveMatrix[0] = frustumScale * (h / w.f)
        perspectiveMatrix[5] = frustumScale

        usingProgram(theProgram) { glUniformMatrix4f(perspectiveMatrixUnif, perspectiveMatrix) }

        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(bufferObject)
        glDeleteVertexArray(vao)

        destroyBuffers(vao, bufferObject, vertexData, indexData)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }

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

    val vertexData = floatBufferOf(
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
            *GREEN_COLOR,
            *GREEN_COLOR,
            *GREEN_COLOR,
            *GREEN_COLOR,

            *BLUE_COLOR,
            *BLUE_COLOR,
            *BLUE_COLOR,
            *BLUE_COLOR,

            *RED_COLOR,
            *RED_COLOR,
            *RED_COLOR,

            *GREY_COLOR,
            *GREY_COLOR,
            *GREY_COLOR,

            *BROWN_COLOR,
            *BROWN_COLOR,
            *BROWN_COLOR,
            *BROWN_COLOR,


            //Object 2 colors
            *RED_COLOR,
            *RED_COLOR,
            *RED_COLOR,
            *RED_COLOR,

            *BROWN_COLOR,
            *BROWN_COLOR,
            *BROWN_COLOR,
            *BROWN_COLOR,

            *BLUE_COLOR,
            *BLUE_COLOR,
            *BLUE_COLOR,
            *BLUE_COLOR,

            *GREEN_COLOR,
            *GREEN_COLOR,
            *GREEN_COLOR,

            *GREY_COLOR,
            *GREY_COLOR,
            *GREY_COLOR,
            *GREY_COLOR)

    val indexData = shortBufferOf(

            0, 2, 1,
            3, 2, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            11, 13, 12,

            14, 16, 15,
            17, 16, 14)
}