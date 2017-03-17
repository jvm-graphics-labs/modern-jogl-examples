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
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.component.Mesh
import org.xml.sax.SAXException
import uno.glm.MatrixStack
import uno.time.Timer
import java.io.IOException
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.ParserConfigurationException
import kotlin.properties.Delegates
import uno.buffer.put
import uno.glsl.programOf

/**
 * Created by GBarbieri on 17.03.2017.
 */

fun main(args: Array<String>) {
    Interpolation_()
}

class Interpolation_ : Framework("Tutorial 08 - Interpolation") {

    var ship by Delegates.notNull<Mesh>()

    var theProgram = 0
    var modelToCameraMatrixUnif = 0
    var cameraToClipMatrixUnif = 0
    var baseColorUnif = 0

    val frustumScale = calcFrustumScale(20f)

    fun calcFrustumScale(fovDeg: Float) = 1.0f / glm.tan(fovDeg.rad / 2.0f)

    val cameraToClipMatrix = Mat4(0.0f)

    val orient = Orientation()

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
        cameraToClipMatrix[3].z = (2 * zFar * zNear) / (zNear - zFar)

        glUseProgram(theProgram)
        glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix to matBuffer)
        glUseProgram(0)
    }

    override fun display(gl: GL3) = with(gl) {

        orient.updateTime()

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val matrixStack = MatrixStack()
                .translate(0.0f, 0.0f, -200.0f)
                .applyMatrix(orient.getOrient().toMat4())

        glUseProgram(theProgram)
        matrixStack
                .scale(3.0f, 3.0f, 3.0f)
                .rotateX(-90.0f)
        //Set the base color for this object.
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

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> {
                val slerp = orient.toggleSlerp()
                println(if (slerp) "Slerp" else "Lerp")
            }
        }

        orientKeys.indices.filter { e.keyCode == orientKeys[it] }.forEach { applyOrientation(it) }
    }

    fun applyOrientation(index: Int) {
        if (!orient.isAnimating)
            orient.animateToOrient(index)
    }

    val orientKeys = shortArrayOf(
            KeyEvent.VK_Q,
            KeyEvent.VK_W,
            KeyEvent.VK_E,
            KeyEvent.VK_R,

            KeyEvent.VK_T,
            KeyEvent.VK_Y,
            KeyEvent.VK_U)

    override fun end(gl: GL3) {

        gl.glDeleteProgram(theProgram)

        ship.dispose(gl)
    }

    inner class Orientation {

        var isAnimating = false
        var currentOrient = 0
        var slerp = false
        val anim = Animation()

        fun toggleSlerp(): Boolean {
            slerp = !slerp
            return slerp
        }

        fun getOrient() =
                if (isAnimating)
                    anim.getOrient(orients[currentOrient], slerp)
                else
                    orients[currentOrient]

        fun updateTime() {
            if (isAnimating) {
                val isFinished = anim.updateTime()
                if (isFinished) {
                    isAnimating = false
                    currentOrient = anim.finalX
                }
            }
        }

        fun animateToOrient(destination: Int) {
            if (currentOrient == destination) {
                return
            }
            anim.startAnimation(destination, 1.0f)
            isAnimating = true
        }

        inner class Animation {

            var finalX = 0
                private set
            var currTimer by Delegates.notNull<Timer>()

            fun updateTime() = currTimer.update()

            fun getOrient(initial: Quat, slerp: Boolean) =
                    if (slerp)
                        slerp(initial, orients[finalX], currTimer.getAlpha())
                    else
                        lerp(initial, orients[finalX], currTimer.getAlpha())

            fun startAnimation(destination: Int, duration: Float) {
                finalX = destination
                currTimer = Timer(Timer.Type.Single, duration)
            }
        }
    }

    fun slerp(v0: Quat, v1: Quat, alpha: Float): Quat {

        var dot = v0 dot v1
        val DOT_THRESHOLD = 0.9995f
        if (dot > DOT_THRESHOLD)
            return lerp(v0, v1, alpha)

        dot = glm.clamp(dot, -1.0f, 1.0f)
        val theta0 = glm.acos(dot)
        val theta = theta0 * alpha

        val v2 = v1 - v0 * dot
        v2.normalize_()

        return v0 * theta.cos + v2 * theta.sin
    }

    // TODO check lerp thinkness
    fun lerp(v0: Quat, v1: Quat, alpha: Float): Quat {

        val start = v0.vectorize()
        val end = v1.vectorize()
        val interp = glm.mix(start, end, alpha)

        println("alpha: $alpha, $interp")

        interp.normalize_()
        return Quat(interp)
    }

    val orients = arrayOf(
            Quat(0.7071f, 0.7071f, 0.0f, 0.0f),
            Quat(0.5f, 0.5f, -0.5f, 0.5f),
            Quat(-0.4895f, -0.7892f, -0.3700f, -0.02514f),
            Quat(0.4895f, 0.7892f, 0.3700f, 0.02514f),

            Quat(0.3840f, -0.1591f, -0.7991f, -0.4344f),
            Quat(0.5537f, 0.5208f, 0.6483f, 0.0410f),
            Quat(0.0f, 0.0f, 1.0f, 0.0f))
}
