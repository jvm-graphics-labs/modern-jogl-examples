package main.tut06

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import glNext.*
import glm.*
import glm.mat.Mat4
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 24.02.2017.
 */

fun main(args: Array<String>) {
    Translation_().setup("Tutorial 06 - Translation")
}

class Translation_ : Framework() {

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

    val vertexData = floatBufferOf(

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

    val indexData = shortBufferOf(

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

        glGenVertexArray(vao)
        glBindVertexArray(vao)

        val colorDataOffset = Vec3.SIZE * numberOfVertices
        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glEnableVertexAttribArray(glf.pos3_col4)
        glEnableVertexAttribArray(glf.pos3_col4[1])
        glVertexAttribPointer(glf.pos3_col4, 0)
        glVertexAttribPointer(glf.pos3_col4[1], colorDataOffset)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject[Buffer.INDEX])

        glBindVertexArray()

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

        theProgram = programOf(gl, javaClass, "tut06", "pos-color-local-transform.vert", "color-passthrough.frag")

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
        glUniformMatrix4f(cameraToClipMatrixUnif, cameraToClipMatrix)
        glUseProgram()
    }

    fun initializeVertexBuffers(gl: GL3) = with(gl) {

        glGenBuffers(bufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.INDEX])
        glBufferData(GL_ARRAY_BUFFER, indexData, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0)
        glClearBufferf(GL_DEPTH)

        glUseProgram(theProgram)

        glBindVertexArray(vao)

        val elapsedTime = (System.currentTimeMillis() - start) / 1_000f
        instanceList.forEach {

            val transformMatrix = it.constructMatrix(elapsedTime)

            glUniformMatrix4f(modelToCameraMatrixUnif, transformMatrix)
            glDrawElements(indexData.size, GL_UNSIGNED_SHORT)
        }

        glBindVertexArray()
        glUseProgram()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        cameraToClipMatrix[0].x = frustumScale * (h / w.f)
        cameraToClipMatrix[1].y = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4f(cameraToClipMatrixUnif, cameraToClipMatrix)
        glUseProgram()

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
        val scale = glm.PIf * 2.0f / loopDuration

        val currTimeThroughLoop = elapsedTime % loopDuration

        return Vec3(
                glm.cos(currTimeThroughLoop * scale) * 4.f,
                glm.sin(currTimeThroughLoop * scale) * 6.f,
                -20.0f)
    }

    fun bottomCircleOffset(elapsedTime: Float): Vec3 {

        val loopDuration = 12.0f
        val scale = glm.PIf * 2.0f / loopDuration

        val currTimeThroughLoop = elapsedTime % loopDuration

        return Vec3(
                glm.cos(currTimeThroughLoop * scale) * 5.f,
                -3.5f,
                glm.sin(currTimeThroughLoop * scale) * 5.f - 20.0f)
    }
}