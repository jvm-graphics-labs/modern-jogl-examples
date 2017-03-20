package main.tut06

import uno.buffer.*
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import uno.glsl.programOf
import glm.*
import main.framework.Framework
import main.framework.Semantic
import glm.mat.Mat4
import glm.vec._3.Vec3
import glm.vec._4.Vec4

/**
 * Created by GBarbieri on 24.02.2017.
 */

fun main(args: Array<String>) {
    Translation_()
}

class Translation_ : Framework("Tutorial 06 - Translation") {

    object Buffer {
        val VERTEX = 0
        val INDEX = 1
        val MAX = 2
    }

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0

    val cameraToClipMatrix = Mat4(0f)
    val frustumScale = calcFrustumScale(45.0f)

    fun calcFrustumScale(fovDeg: Float) = 1.0f / glm.tan(fovDeg.rad / 2.0f)

    val bufferObject = intBufferBig(Buffer.MAX)
    val vao = intBufferBig(1)

    val numberOfVertices = 8

    val GREEN_COLOR = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)
    val BLUE_COLOR = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    val RED_COLOR = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    val BROWN_COLOR = floatArrayOf(0.5f, 0.5f, 0.0f, 1.0f)

    val vertexData = floatArrayOf(

            +1.0f, +1.0f, +1.0f,
            -1.0f, -1.0f, +1.0f,
            -1.0f, +1.0f, -1.0f,
            +1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            +1.0f, +1.0f, -1.0f,
            +1.0f, -1.0f, +1.0f,
            -1.0f, +1.0f, +1.0f,


            *GREEN_COLOR,
            *BLUE_COLOR,
            *RED_COLOR,
            *BROWN_COLOR,

            *GREEN_COLOR,
            *BLUE_COLOR,
            *RED_COLOR,
            *BROWN_COLOR)

    val indexData = shortArrayOf(

            0, 1, 2,
            1, 0, 3,
            2, 3, 0,
            3, 2, 1,

            5, 4, 6,
            4, 5, 7,
            7, 6, 4,
            6, 7, 5)

    private val instanceList = arrayOf(
            Instance(this::stationaryOffset),
            Instance(this::ovalOffset),
            Instance(this::bottomCircleOffset))

    var start = 0L

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffers(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        val colorDataOffset = Vec3.SIZE * numberOfVertices
        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec3.length, GL_FLOAT, false, Vec3.SIZE, 0)
        glVertexAttribPointer(Semantic.Attr.COLOR, Vec4.length, GL_FLOAT, false, Vec4.SIZE, colorDataOffset.L)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject[Buffer.INDEX])

        glBindVertexArray(0)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRange(0.0, 1.0)

        start = System.currentTimeMillis()
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, this::class.java, "tut06", "pos-color-local-transform.vert", "color-passthrough.frag")

        modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
        cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix")

        val zNear = 1.0f
        val zFar = 61.0f

        cameraToClipMatrix[0].x = frustumScale
        cameraToClipMatrix[1].y = frustumScale
        cameraToClipMatrix[2].z = (zFar + zNear) / (zNear - zFar)
        cameraToClipMatrix[2].w = -1.0f
        cameraToClipMatrix[3].z = 2f * zFar * zNear / (zNear - zFar)

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to matBuffer)
        glUseProgram(0)
    }

    fun initializeVertexBuffers(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val indexBuffer = indexData.toShortBuffer()

        glGenBuffers(Buffer.MAX, bufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.INDEX])
        glBufferData(GL_ARRAY_BUFFER, indexBuffer.SIZE.L, indexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        destroyBuffers(vertexBuffer, indexBuffer)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        glUseProgram(theProgram)

        glBindVertexArray(vao[0])

        val elapsedTime = (System.currentTimeMillis() - start) / 1_000f
        instanceList.forEach {

            val transformMatrix = it.constructMatrix(elapsedTime)

            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, transformMatrix to matBuffer)
            glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
        }

        glBindVertexArray(0)
        glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        cameraToClipMatrix[0].x = frustumScale * (h / w.f)
        cameraToClipMatrix[1].y = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to matBuffer)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(Buffer.MAX, bufferObject)
        glDeleteVertexArrays(1, vao)

        destroyBuffers(vao, bufferObject)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }

    private class Instance(val calcOffset: (Float) -> Vec3) {

        fun constructMatrix(elapsedTime: Float): Mat4 {

            val theMat = Mat4(1.0f)

            theMat[3] = Vec4(calcOffset(elapsedTime), 1f)

            return theMat
        }
    }

    fun stationaryOffset(elapsedTime: Float) = Vec3(0.0f, 0.0f, -20.0f)

    fun ovalOffset(elapsedTime: Float): Vec3 {

        val loopDuration = 3.0f
        val scale = glm.pi.f * 2.0f / loopDuration

        val currTimeThroughLoop = elapsedTime % loopDuration

        return Vec3(
                glm.cos(currTimeThroughLoop * scale) * 4.f,
                glm.sin(currTimeThroughLoop * scale) * 6.f,
                -20.0f)
    }

    fun bottomCircleOffset(elapsedTime: Float): Vec3 {

        val loopDuration = 12.0f
        val scale = glm.pi.f * 2.0f / loopDuration

        val currTimeThroughLoop = elapsedTime % loopDuration

        return Vec3(
                glm.cos(currTimeThroughLoop * scale) * 5.f,
                -3.5f,
                glm.sin(currTimeThroughLoop * scale) * 5.f - 20.0f)
    }
}