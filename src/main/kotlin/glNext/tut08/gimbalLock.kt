package glNext.tut08

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL3
import glNext.*
import glm.f
import glm.glm
import glm.rad
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.component.Mesh
import uno.glm.MatrixStack
import uno.glsl.programOf
import glm.mat.Mat4

/**
 * Created by GBarbieri on 10.03.2017.
 */

fun main(args: Array<String>) {
    GimbalLock_Next().setup("Tutorial 08 - Gimbal Lock")
}

class GimbalLock_Next : Framework() {

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

        gimbals = Array(Gimbal.MAX, { Mesh(gl, javaClass, "tut08/${GIMBALS_SCR[it]}") })
        `object` = Mesh(gl, javaClass, "tut08/Ship.xml")

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

        theProgram = programOf(gl, javaClass, "tut08", "pos-color-local-transform.vert", "color-mult-uniform.frag")

        withProgram(theProgram) {

            modelToCameraMatrixUnif = "modelToCameraMatrix".location
            cameraToClipMatrixUnif = "cameraToClipMatrix".location
            baseColorUnif = "baseColor".location

            val zNear = 1.0f
            val zFar = 600.0f

            cameraToClipMatrix[0].x = frustumScale
            cameraToClipMatrix[1].y = frustumScale
            cameraToClipMatrix[2].z = (zFar + zNear) / (zNear - zFar)
            cameraToClipMatrix[2].w = -1.0f
            cameraToClipMatrix[3].z = 2f * zFar * zNear / (zNear - zFar)

            use { cameraToClipMatrixUnif.mat4 = cameraToClipMatrix }
        }
    }

    public override fun display(gl: GL3) = with(gl) {

        clear {
            color(0)
            depth()
        }

        val currMatrix = MatrixStack()
                .translate(0.0f, 0.0f, -200.0f)
                .rotateX(angles.angleX)
        drawGimbal(gl, currMatrix, GimbalAxis.X, Vec4(0.4f, 0.4f, 1.0f, 1.0f))

        currMatrix.rotateY(angles.angleY)
        drawGimbal(gl, currMatrix, GimbalAxis.Y, Vec4(0.0f, 1.0f, 0.0f, 1.0f))

        currMatrix.rotateY(angles.angleZ)
        drawGimbal(gl, currMatrix, GimbalAxis.Z, Vec4(1.0f, 0.3f, 0.3f, 1.0f))

        usingProgram(theProgram) {
            currMatrix
                    .scale(3.0f)
                    .rotateX(-90f)
            //Set the base color for this object.
            glUniform4f(baseColorUnif, 1.0f)
            modelToCameraMatrixUnif.mat4 = currMatrix.top()

            `object`.render(gl, "tint")
        }
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

            usingProgram(theProgram) {
                //Set the base color for this object.
                glUniform4f(baseColorUnif, baseColor)
                modelToCameraMatrixUnif.mat4 = top()

                gimbals[axis.ordinal].render(gl)
            }
        }
    }

    public override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        cameraToClipMatrix[0].x = frustumScale * (h / w.f)
        cameraToClipMatrix[1].y = frustumScale

        usingProgram(theProgram) { cameraToClipMatrixUnif.mat4 = cameraToClipMatrix }

        glViewport(0, 0, w, h)
    }

    public override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)

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