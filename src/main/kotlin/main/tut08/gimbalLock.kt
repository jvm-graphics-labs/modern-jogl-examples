package main.tut08

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import glm.f
import glm.glm
import glm.mat.Mat4
import glm.rad
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.component.Mesh
import uno.buffer.put
import uno.glm.MatrixStack
import uno.glsl.programOf
import kotlin.properties.Delegates

/**
 * Created by GBarbieri on 10.03.2017.
 */

fun main(args: Array<String>) {
    GimbalLock_()
}

class GimbalLock_ : Framework("Tutorial 08 - Gimbal Lock") {

    val GIMBALS_SCR = arrayOf("LargeGimbal.xml", "MediumGimbal.xml", "SmallGimbal.xml")

    object Gimbal {
        val LARGE = 0
        val MEDIUM = 1
        val SMALL = 2
        val MAX = 3
    }

    enum class GimbalAxis { X, Y, Z }

    inner class GimbalAngles(
            var angleX: Float = 0f,
            var angleY: Float = 0f,
            var angleZ: Float = 0f)

    lateinit var gimbals: Array<Mesh>

    lateinit var `object`: Mesh

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0
    var baseColorUnif = 0

    val frustumScale = calcFrustumScale(20.0f)

    fun calcFrustumScale(fovDeg: Float) = 1.0f / glm.tan(fovDeg.rad / 2.0f)

    val cameraToClipMatrix = Mat4(0.0f)

    val angles = GimbalAngles()

    var drawGimbals = true

    public override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        gimbals = Array(Gimbal.MAX, { Mesh(gl, this::class.java, "tut08/${GIMBALS_SCR[it]}") })
        `object` = Mesh(gl, this::class.java, "tut08/Ship.xml")

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(0.0f, 1.0f)
    }

    fun initializeProgram(gl: GL3) = with(gl) {

        theProgram = programOf(gl, this::class.java, "tut08", "pos-color-local-transform.vert", "color-mult-uniform.frag")

        modelToCameraMatrixUnif = glGetUniformLocation(theProgram, "modelToCameraMatrix")
        cameraToClipMatrixUnif = glGetUniformLocation(theProgram, "cameraToClipMatrix")
        baseColorUnif = glGetUniformLocation(theProgram, "baseColor")

        val zNear = 1.0f
        val zFar = 600.0f

        cameraToClipMatrix[0].x = frustumScale
        cameraToClipMatrix[1].y = frustumScale
        cameraToClipMatrix[2].z = (zFar + zNear) / (zNear - zFar)
        cameraToClipMatrix[2].w = -1.0f
        cameraToClipMatrix[3].z = 2f * zFar * zNear / (zNear - zFar)

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to matBuffer)
        glUseProgram(0)
    }

    public override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val currMatrix = MatrixStack()
                .translate(Vec3(0.0f, 0.0f, -200.0f))
                .rotateX(angles.angleX)
        drawGimbal(gl, currMatrix, GimbalAxis.X, Vec4(0.4f, 0.4f, 1.0f, 1.0f))

        currMatrix.rotateY(angles.angleY)
        drawGimbal(gl, currMatrix, GimbalAxis.Y, Vec4(0.0f, 1.0f, 0.0f, 1.0f))

        currMatrix.rotateY(angles.angleZ)
        drawGimbal(gl, currMatrix, GimbalAxis.Z, Vec4(1.0f, 0.3f, 0.3f, 1.0f))

        glUseProgram(theProgram)
        currMatrix
                .scale(Vec3(3.0f))
                .rotateX(-90f)
        //Set the base color for this object.
        glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f)
        glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().to(matBuffer))

        `object`.render(gl, "tint")

        glUseProgram(0)
    }

    fun drawGimbal(gl: GL3, matrixStack: MatrixStack, axis: GimbalAxis, baseColor: Vec4) = with(gl) {

        if (!drawGimbals)
            return@with

        matrixStack run {

            when (axis) {

                GimbalAxis.X -> {
                }

                GimbalAxis.Y -> {
                    rotateZ(90.0f)
                    rotateX(90.0f)
                }
                GimbalAxis.Z -> {
                    rotateY(90.0f)
                    rotateX(90.0f)
                }
            }

            glUseProgram(theProgram)
            //Set the base color for this object.
            glUniform4fv(baseColorUnif, 1, baseColor to vecBuffer)
            glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, top() to matBuffer)

            gimbals[axis.ordinal].render(gl)

            glUseProgram(0)
        }
    }

    public override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        cameraToClipMatrix[0].x = frustumScale * (h / w.f)
        cameraToClipMatrix[1].y = frustumScale

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to matBuffer)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    public override fun end(gl: GL3) {

        gl.glDeleteProgram(theProgram)

        `object`.dispose(gl)
        gimbals.forEach { it.dispose(gl) }
    }

    override fun keyPressed(e: KeyEvent) {

        val smallAngleIncrement = 9.0f

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_W -> angles.angleX += smallAngleIncrement
            KeyEvent.VK_S -> angles.angleX -= smallAngleIncrement

            KeyEvent.VK_A -> angles.angleY += smallAngleIncrement
            KeyEvent.VK_D -> angles.angleY -= smallAngleIncrement

            KeyEvent.VK_Q -> angles.angleZ += smallAngleIncrement
            KeyEvent.VK_E -> angles.angleZ -= smallAngleIncrement

            KeyEvent.VK_SPACE -> drawGimbals = !drawGimbals
        }
    }
}