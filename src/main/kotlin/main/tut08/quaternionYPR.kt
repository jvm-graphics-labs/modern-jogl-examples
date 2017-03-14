package main.tut08

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import glm.*
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import main.framework.Framework
import main.framework.component.Mesh
import org.xml.sax.SAXException
import uno.glm.MatrixStack
import java.io.IOException
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.ParserConfigurationException
import kotlin.properties.Delegates
import uno.buffer.put
import uno.glsl.programOf

/**
 * Created by GBarbieri on 14.03.2017.
 */

fun main(args: Array<String>) {
    QuaternionYPR_()
}

class QuaternionYPR_() : Framework("Tutorial 08 - Quaternion YPR") {

    class GimbalAngles(
            var angleX: Float = 0f,
            var angleY: Float = 0f,
            var angleZ: Float = 0f)

    var ship by Delegates.notNull<Mesh>()

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0
    var baseColorUnif = 0

    val frustumScale = calcFrustumScale(20f)

    fun calcFrustumScale(fovDeg: Float) = 1.0f / glm.tan(fovDeg.rad / 2.0f)

    val cameraToClipMatrix = Mat4(0.0f)

    val angles = GimbalAngles()

    var orientation = Quat(1.0f, 0.0f, 0.0f, 0.0f)

    var rightMultiply = true

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        ship = Mesh(gl, this::class.java, "tut08/Ship.xml")

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

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val matrixStack = MatrixStack()
                .translate(Vec3(0.0f, 0.0f, -200.0f))
                .applyMatrix(orientation.toMat4())

        glUseProgram(theProgram)

        matrixStack
                .scale(3.0f, 3.0f, 3.0f)
                .rotateX(-90.0f)

        glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f)
        glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matrixStack.top() to matBuffer)

        ship.render(gl, "tint")

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

    override fun end(gl: GL3) {

        gl.glDeleteProgram(theProgram)

        ship.dispose(gl)
    }

    fun offsetOrientation(axis: Vec3, angDeg: Float) {

        axis.normalize_()

        axis times_ (angDeg.rad / 2.0f).sin
        val scalar = (angDeg.rad / 2.0f).cos

        val offset = Quat(scalar, axis)

        if (rightMultiply)
            orientation times_ offset
        else
            orientation = offset * orientation
    }

    override fun keyPressed(e: KeyEvent) {

        val smallAngleIncrement = 9.0f

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }

            KeyEvent.VK_W -> offsetOrientation(Vec3(1.0f, 0.0f, 0.0f), smallAngleIncrement)
            KeyEvent.VK_S -> offsetOrientation(Vec3(1.0f, 0.0f, 0.0f), -smallAngleIncrement)

            KeyEvent.VK_A -> offsetOrientation(Vec3(0.0f, 0.0f, 1.0f), smallAngleIncrement)
            KeyEvent.VK_D -> offsetOrientation(Vec3(0.0f, 0.0f, 1.0f), -smallAngleIncrement)

            KeyEvent.VK_Q -> offsetOrientation(Vec3(0.0f, 1.0f, 0.0f), smallAngleIncrement)
            KeyEvent.VK_E -> offsetOrientation(Vec3(0.0f, 1.0f, 0.0f), -smallAngleIncrement)

            KeyEvent.VK_SPACE -> {
                rightMultiply = !rightMultiply
                println("${if (rightMultiply) "Right" else "Left"}-multiply");
            }
        }
    }
}
