package main.tut06

import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import extensions.intBufferBig
import extensions.toFloatBuffer
import extensions.toShortBuffer
import glsl.programOf
import main.*
import main.framework.Framework
import main.framework.Semantic
import mat.Mat3x3
import mat.Mat4x4
import vec._3.Vec3
import vec._4.Vec4
import java.nio.FloatBuffer
import java.util.*

/**
 * Created by elect on 24/02/17.
 */

fun main(args: Array<String>) {
    Hierarchy_()
}

class Hierarchy_ : Framework("Tutorial 06 - Hierarchy") {

    object Buffer {
        val VERTEX = 0
        val INDEX = 1
        val MAX = 2
    }

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0

    val cameraToClipMatrix = Mat4x4(0.0f)
    val frustumScale = calcFrustumScale(45.0f)

    fun calcFrustumScale(fovDeg: Float) = 1.0f / glm.tan(fovDeg.rad / 2.0f)

    val bufferObject = intBufferBig(Buffer.MAX)
    val vao = intBufferBig(1)

    val numberOfVertices = 24

    val GREEN_COLOR = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f)
    val BLUE_COLOR = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    val RED_COLOR = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)
    val YELLOW_COLOR = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)
    val CYAN_COLOR = floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f)
    val MAGENTA_COLOR = floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f)

    val vertexData = floatArrayOf(

            //Front
            +1.0f, +1.0f, +1.0f,
            +1.0f, -1.0f, +1.0f,
            -1.0f, -1.0f, +1.0f,
            -1.0f, +1.0f, +1.0f,

            //Top
            +1.0f, +1.0f, +1.0f,
            -1.0f, +1.0f, +1.0f,
            -1.0f, +1.0f, -1.0f,
            +1.0f, +1.0f, -1.0f,

            //Left
            +1.0f, +1.0f, +1.0f,
            +1.0f, +1.0f, -1.0f,
            +1.0f, -1.0f, -1.0f,
            +1.0f, -1.0f, +1.0f,

            //Back
            +1.0f, +1.0f, -1.0f,
            -1.0f, +1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            +1.0f, -1.0f, -1.0f,

            //Bottom
            +1.0f, -1.0f, +1.0f,
            +1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, +1.0f,

            //Right
            -1.0f, +1.0f, +1.0f,
            -1.0f, -1.0f, +1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, +1.0f, -1.0f,


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
            RED_COLOR[0], RED_COLOR[1], RED_COLOR[2], RED_COLOR[3],

            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],
            YELLOW_COLOR[0], YELLOW_COLOR[1], YELLOW_COLOR[2], YELLOW_COLOR[3],

            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],
            CYAN_COLOR[0], CYAN_COLOR[1], CYAN_COLOR[2], CYAN_COLOR[3],

            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3],
            MAGENTA_COLOR[0], MAGENTA_COLOR[1], MAGENTA_COLOR[2], MAGENTA_COLOR[3])

    val indexData = shortArrayOf(

            0, 1, 2,
            2, 3, 0,

            4, 5, 6,
            6, 7, 4,

            8, 9, 10,
            10, 11, 8,

            12, 13, 14,
            14, 15, 12,

            16, 17, 18,
            18, 19, 16,

            20, 21, 22,
            22, 23, 20)

    val armature = Armature()

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVAO(gl)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRange(0.0, 1.0)
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, this::class.java, "tut06", "pos-color-local-transform.vert", "color-passthrough.frag")

        modelToCameraMatrixUnif = glGetUniformLocation(theProgram, "modelToCameraMatrix")
        cameraToClipMatrixUnif = glGetUniformLocation(theProgram, "cameraToClipMatrix")

        val zNear = 1.0f
        val zFar = 100.0f

        cameraToClipMatrix[0].x = frustumScale
        cameraToClipMatrix[1].y = frustumScale
        cameraToClipMatrix[2].z = (zFar + zNear) / (zNear - zFar)
        cameraToClipMatrix[2].w = -1.0f
        cameraToClipMatrix[3].z = 2f * zFar * zNear / (zNear - zFar)

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to Framework.matBuffer)
        glUseProgram(0)
    }

    fun initializeVAO(gl: GL3) = with(gl) {

        val vertexBuffer = vertexData.toFloatBuffer()
        val indexBuffer = indexData.toShortBuffer()

        glGenBuffers(Buffer.MAX, bufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.INDEX])
        glBufferData(GL_ARRAY_BUFFER, indexBuffer.SIZE.L, indexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])

        val colorDataOffset = Vec3.SIZE * numberOfVertices
        glBindBuffer(GL_ARRAY_BUFFER, bufferObject[Buffer.VERTEX])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glEnableVertexAttribArray(Semantic.Attr.COLOR)
        glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vec3.SIZE, 0)
        glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vec4.SIZE, colorDataOffset.L)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferObject[Buffer.INDEX])

        glBindVertexArray(0)

        vertexBuffer.destroy()
        indexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        armature.draw(gl)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        cameraToClipMatrix[0].x = frustumScale * (h / w.f)
        cameraToClipMatrix[1].y = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to Framework.matBuffer)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(Buffer.MAX, bufferObject)
        glDeleteVertexArrays(1, vao)

        vao.destroy()
        bufferObject.destroy()
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
            KeyEvent.VK_A -> armature.adjBase(true)
            KeyEvent.VK_D -> armature.adjBase(false)
            KeyEvent.VK_W -> armature.adjUpperArm(false)
            KeyEvent.VK_S -> armature.adjUpperArm(true)
            KeyEvent.VK_R -> armature.adjLowerArm(false)
            KeyEvent.VK_F -> armature.adjLowerArm(true)
            KeyEvent.VK_T -> armature.adjWristPitch(false)
            KeyEvent.VK_G -> armature.adjWristPitch(true)
            KeyEvent.VK_Z -> armature.adjWristRoll(true)
            KeyEvent.VK_C -> armature.adjWristRoll(false)
            KeyEvent.VK_Q -> armature.adjFingerOpen(true)
            KeyEvent.VK_E -> armature.adjFingerOpen(false)
            KeyEvent.VK_SPACE -> armature.writePose()
        }
    }

    inner class Armature {

        val posBase = Vec3(3.0f, -5.0f, -40f)
        var angBase = -45.0f

        val posBaseLeft = Vec3(2.0f, 0.0f, 0.0f)
        val posBaseRight = Vec3(-2.0f, 0.0f, 0.0f)
        val scaleBaseZ = 3.0f

        var angUpperArm = -33.75f
        val sizeUpperArm = 9.0f

        val posLowerArm = Vec3(0.0f, 0.0f, 8.0f)
        var angLowerArm = 146.25f
        val lengthLowerArm = 5.0f
        val widthLowerArm = 1.5f

        val posWrist = Vec3(0.0f, 0.0f, 5.0f)
        var angWristRoll = 0.0f
        var angWristPitch = 67.5f
        val lenWrist = 2.0f
        val widthWrist = 2.0f

        val posLeftFinger = Vec3(1.0f, 0.0f, 1.0f)
        val posRightFinger = Vec3(-1.0f, 0.0f, 1.0f)
        var angFingerOpen = 180.0f
        val lengthFinger = 2.0f
        val widthFinger = 0.5f
        val angLowerFinger = 45.0f

        val STANDARD_ANGLE_INCREMENT = 11.25f
        val SMALL_ANGLE_INCREMENT = 9.0f

        fun draw(gl: GL3) = with(gl) {

            val modelToCameraStack = MatrixStack()

            glUseProgram(theProgram)
            glBindVertexArray(vao[0])

            modelToCameraStack
                    .translate(posBase)
                    .rotateY(angBase)

            //  Draw left base.
            run {
                modelToCameraStack
                        .push()
                        .translate(posBaseLeft)
                        .scale(Vec3(1.0f, 1.0f, scaleBaseZ))
                glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
                glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
                modelToCameraStack.pop()
            }

            //  Draw right base.
            run {
                modelToCameraStack
                        .push()
                        .translate(posBaseRight)
                        .scale(Vec3(1.0f, 1.0f, scaleBaseZ))
                glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
                glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
                modelToCameraStack.pop()
            }

            //  Draw main arm.
            drawUpperArm(gl, modelToCameraStack)

            glBindVertexArray(0)
            glUseProgram(0)
        }

        fun drawUpperArm(gl: GL3, modelToCameraStack: MatrixStack) = with(gl) {

            modelToCameraStack
                    .push()
                    .rotateX(angUpperArm)

            run {
                modelToCameraStack
                        .push()
                        .translate(Vec3(0.0f, 0.0f, sizeUpperArm / 2.0f - 1.0f))
                        .scale(Vec3(1.0f, 1.0f, sizeUpperArm / 2.0f))
                glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
                glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
                modelToCameraStack.pop()
            }

            drawLowerArm(gl, modelToCameraStack)

            modelToCameraStack.pop()
        }

        fun drawLowerArm(gl: GL3, modelToCameraStack: MatrixStack) = with(gl) {

            modelToCameraStack
                    .push()
                    .translate(posLowerArm)
                    .rotateX(angLowerArm)

            modelToCameraStack
                    .push()
                    .translate(Vec3(0.0f, 0.0f, lengthLowerArm / 2.0f))
                    .scale(Vec3(widthLowerArm / 2.0f, widthLowerArm / 2.0f, lengthLowerArm / 2.0f))
            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
            glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
            modelToCameraStack.pop()

            drawWrist(gl, modelToCameraStack)

            modelToCameraStack.pop()
        }

        fun drawWrist(gl: GL3, modelToCameraStack: MatrixStack) = with(gl) {

            modelToCameraStack
                    .push()
                    .translate(posWrist)
                    .rotateZ(angWristRoll)
                    .rotateX(angWristPitch)

            modelToCameraStack
                    .push()
                    .scale(Vec3(widthWrist / 2.0f, widthWrist / 2.0f, lenWrist / 2.0f))
            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
            glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
            modelToCameraStack.pop()

            drawFingers(gl, modelToCameraStack)

            modelToCameraStack.pop()
        }

        fun drawFingers(gl: GL3, modelToCameraStack: MatrixStack) = with(gl) {

            //  Draw left finger
            modelToCameraStack
                    .push()
                    .translate(posLeftFinger)
                    .rotateY(angFingerOpen)

            modelToCameraStack
                    .push()
                    .translate(Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                    .scale(Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f))
            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
            glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
            modelToCameraStack.pop()

            run {
                //  Draw left lower finger
                modelToCameraStack
                        .push()
                        .translate(Vec3(0.0f, 0.0f, lengthFinger))
                        .rotateY(-angLowerFinger)

                modelToCameraStack
                        .push()
                        .translate(Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                        .scale(Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f))
                glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
                glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
                modelToCameraStack.pop()

                modelToCameraStack.pop()
            }

            modelToCameraStack.pop()

            //  Draw right finger
            modelToCameraStack
                    .push()
                    .translate(posRightFinger)
                    .rotateY(-angFingerOpen)

            modelToCameraStack
                    .push()
                    .translate(Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                    .scale(Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f))
            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
            glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
            modelToCameraStack.pop()

            run {
                //  Draw left lower finger
                modelToCameraStack
                        .push()
                        .translate(Vec3(0.0f, 0.0f, lengthFinger))
                        .rotateY(angLowerFinger)

                modelToCameraStack
                        .push()
                        .translate(Vec3(0.0f, 0.0f, lengthFinger / 2.0f))
                        .scale(Vec3(widthFinger / 2.0f, widthFinger / 2.0f, lengthFinger / 2.0f))
                glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, modelToCameraStack to Framework.matBuffer)
                glDrawElements(GL_TRIANGLES, indexData.size, GL_UNSIGNED_SHORT, 0)
                modelToCameraStack.pop()

                modelToCameraStack.pop()
            }
            modelToCameraStack.pop()
        }

        fun adjBase(increment: Boolean) {
            angBase += if (increment) STANDARD_ANGLE_INCREMENT else -STANDARD_ANGLE_INCREMENT
            angBase %= 360.0f
        }

        fun adjUpperArm(increment: Boolean) {
            angUpperArm += if (increment) STANDARD_ANGLE_INCREMENT else -STANDARD_ANGLE_INCREMENT
            angUpperArm = glm.clamp(angUpperArm, -90.0f, 0.0f)
        }

        fun adjLowerArm(increment: Boolean) {
            angLowerArm += if (increment) STANDARD_ANGLE_INCREMENT else -STANDARD_ANGLE_INCREMENT
            angLowerArm = glm.clamp(angLowerArm, 0.0f, 146.25f)
        }

        fun adjWristPitch(increment: Boolean) {
            angWristPitch += if (increment) STANDARD_ANGLE_INCREMENT else -STANDARD_ANGLE_INCREMENT
            angWristPitch = glm.clamp(angWristPitch, 0.0f, 90.0f)
        }

        fun adjWristRoll(increment: Boolean) {
            angWristRoll += if (increment) STANDARD_ANGLE_INCREMENT else -STANDARD_ANGLE_INCREMENT
            angWristRoll %= 360.0f
        }

        fun adjFingerOpen(increment: Boolean) {
            angFingerOpen += if (increment) SMALL_ANGLE_INCREMENT else -SMALL_ANGLE_INCREMENT
            angFingerOpen = glm.clamp(angFingerOpen, 9.0f, 180.0f)
        }

        fun writePose() {
            println("angBase:\t$angBase")
            println("angUpperArm:\t$angUpperArm")
            println("angLowerArm:\t$angLowerArm")
            println("angWristPitch:\t$angWristPitch")
            println("angWristRoll:\t$angWristRoll")
            println("angFingerOpen:\t$angFingerOpen")
        }
    }

    inner class MatrixStack {

        val matrices = Stack<Mat4x4>()
        var currMat = Mat4x4(1f)

        internal fun top() = currMat

        internal fun rotateX(angDeg: Float): MatrixStack {
            currMat.mul_(Mat4x4(this@Hierarchy_.rotateX(angDeg)))
            return this
        }

        internal fun rotateY(angDeg: Float): MatrixStack {
            currMat.mul_(Mat4x4(this@Hierarchy_.rotateY(angDeg)))
            return this
        }

        internal fun rotateZ(angDeg: Float): MatrixStack {
            currMat.mul_(Mat4x4(this@Hierarchy_.rotateZ(angDeg)))
            return this
        }

        internal fun scale(scaleVec: Vec3): MatrixStack {

            val scaleMat = Mat4x4(scaleVec)

            currMat.mul_(scaleMat)

            return this
        }

        internal fun translate(offsetVec: Vec3): MatrixStack {

            val translateMat = Mat4x4(1f)
            translateMat[3] = Vec4(offsetVec)

            currMat.mul_(translateMat)

            return this
        }

        internal fun push(): MatrixStack {
            matrices.push(Mat4x4(currMat))
            return this
        }

        internal fun pop(): MatrixStack {
            currMat = matrices.pop()
            return this
        }

        infix fun to(buffer: FloatBuffer) = currMat.to(buffer)
    }

    fun rotateX(angDeg: Float): Mat3x3 {

        val andRad = angDeg.rad
        val cos = glm.cos(andRad)
        val sin = glm.sin(andRad)

        val theMat = Mat3x3(1f)
        theMat[1].y = cos; theMat[2].y = -sin
        theMat[1].z = sin; theMat[2].z = cos
        return theMat
    }

    fun rotateY(angDeg: Float): Mat3x3 {

        val andRad = angDeg.rad
        val cos = glm.cos(andRad)
        val sin = glm.sin(andRad)

        val theMat = Mat3x3(1f)
        theMat[0].x = cos; theMat[2].x = sin
        theMat[0].z = -sin; theMat[2].z = cos
        return theMat
    }

    fun rotateZ(angDeg: Float): Mat3x3 {

        val andRad = angDeg.rad
        val cos = glm.cos(andRad)
        val sin = glm.sin(andRad)

        val theMat = Mat3x3(1f)
        theMat[0].x = cos; theMat[1].x = -sin
        theMat[0].y = sin; theMat[1].y = cos
        return theMat
    }
}