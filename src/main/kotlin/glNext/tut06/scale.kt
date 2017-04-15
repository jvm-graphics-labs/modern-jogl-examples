package glNext.tut06

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.*
import glm.mat.Mat4
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import uno.buffer.*
import uno.glsl.programOf

/**
 * Created by GBarbieri on 24.02.2017.
 */

fun main(args: Array<String>) {
    Scale_().setup("Tutorial 06 - Scale")
}

class Scale_ : Framework() {

    object Buffer {
        val VERTEX = 0
        val INDEX = 1
        val MAX = 2
    }

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0

    val cameraToClipMatrix = Mat4(0.0f)
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
            Instance(this::nullScale, Vec3(0.0f, 0.0f, -45.0f)),
            Instance(this::staticUniformScale, Vec3(-10.0f, -10.0f, -45.0f)),
            Instance(this::staticNonUniformScale, Vec3(-10.0f, 10.0f, -45.0f)),
            Instance(this::dynamicUniformScale, Vec3(10.0f, 10.0f, -45.0f)),
            Instance(this::dynamicNonUniformScale, Vec3(10.0f, -10.0f, -45.0f))
    )

    var start = 0L

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffers(gl)

        initVertexArray(vao) {

            val colorDataOffset = Vec3.SIZE * numberOfVertices
            array(bufferObject[Buffer.VERTEX], glf.pos3_col4, 0, colorDataOffset)
            element(bufferObject[Buffer.INDEX])
        }

        faceCull {
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

        start = System.currentTimeMillis()
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, javaClass, "tut06", "pos-color-local-transform.vert", "color-passthrough.frag")

        withProgram(theProgram) {

            modelToCameraMatrixUnif = "modelToCameraMatrix".location
            cameraToClipMatrixUnif = "cameraToClipMatrix".location

            val zNear = 1.0f
            val zFar = 61.0f

            cameraToClipMatrix[0].x = frustumScale
            cameraToClipMatrix[1].y = frustumScale
            cameraToClipMatrix[2].z = (zFar + zNear) / (zNear - zFar)
            cameraToClipMatrix[2].w = -1.0f
            cameraToClipMatrix[3].z = 2f * zFar * zNear / (zNear - zFar)

            use { cameraToClipMatrixUnif.mat4 = cameraToClipMatrix }
        }
    }

    fun initializeVertexBuffers(gl: GL3) = with(gl) {

        glGenBuffers(bufferObject)

        withArrayBuffer(bufferObject[Buffer.VERTEX]) { data(vertexData, GL_STATIC_DRAW) }

        withElementBuffer(bufferObject[Buffer.INDEX]) { data(indexData, GL_STATIC_DRAW) }
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(0)
            depth()
        }

        usingProgram(theProgram) {

            withVertexArray(vao) {

                val elapsedTime = (System.currentTimeMillis() - start) / 1_000f
                instanceList.forEach {

                    val transformMatrix = it.constructMatrix(elapsedTime)

                    modelToCameraMatrixUnif.mat4 = transformMatrix
                    glDrawElements( indexData.size, GL_UNSIGNED_SHORT)
                }
            }
        }
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
        glDeleteVertexArrays(vao)

        destroyBuffers(vao, bufferObject, vertexData, indexData)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }

    private class Instance(val calcScale: (Float) -> Vec3, val offset: Vec3) {

        fun constructMatrix(elapsedTime: Float): Mat4 {

            val theScale = calcScale(elapsedTime)
            val theMat = Mat4(theScale, 1f)
            theMat[3] = Vec4(offset, 1f)

            return theMat
        }
    }

    fun nullScale(elapsedTime: Float) = Vec3(1f)

    fun staticUniformScale(elapsedTime: Float) = Vec3(4f)

    fun staticNonUniformScale(elapsedTime: Float) = Vec3(0.5f, 1.0f, 10.0f)

    fun dynamicUniformScale(elapsedTime: Float): Vec3 {

        val loopDuration = 3.0f

        return Vec3(glm.mix(1.0f, 4.0f, calcLerpFactor(elapsedTime, loopDuration)))
    }

    fun dynamicNonUniformScale(elapsedTime: Float): Vec3 {

        val xLoopDuration = 3.0f
        val zLoopDuration = 5.0f

        return Vec3(
                glm.mix(1.0f, 0.5f, calcLerpFactor(elapsedTime, xLoopDuration)),
                1.0f,
                glm.mix(1.0f, 10.0f, calcLerpFactor(elapsedTime, zLoopDuration)))
    }

    fun calcLerpFactor(elapsedTime: Float, loopDuration: Float): Float {

        var value = glm.mod(elapsedTime, loopDuration) / loopDuration
        if (value > 0.5f)
            value = 1.0f - value

        return value * 2.0f
    }
}