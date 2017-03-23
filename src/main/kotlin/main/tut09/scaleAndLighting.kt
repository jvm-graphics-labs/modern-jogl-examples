package main.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import com.jogamp.opengl.util.GLBuffers
import glm.L
import glm.f
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import org.xml.sax.SAXException
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*
import java.io.IOException
import java.net.URISyntaxException
import java.util.logging.Level
import java.util.logging.Logger
import javax.xml.parsers.ParserConfigurationException

/**
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    ScaleAndLighting_()
}

class ScaleAndLighting_() : Framework("Tutorial 09 - Scale and Lighting") {

    lateinit var whiteDiffuseColor: ProgramData
    lateinit var vertexDiffuseColor: ProgramData
    lateinit var cylinder: Mesh
    lateinit var plane: Mesh

    val projectionUniformBuffer = intBufferBig(1)

    val lightDirection = Vec4(0.866f, 0.5f, 0.0f, 0.0f)

    var scaleCylinder = false
    var doInverseTranspose = true

    val initialViewData = ViewData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            5.0f,
            0.0f)

    val viewScale = ViewScale(
            3.0f, 20.0f,
            1.5f, 0.5f,
            0.0f, 0.0f, //No camera movement.
            90.0f / 250.0f)

    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)

    val initialObjectData = ObjectData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(1.0f, 0.0f, 0.0f, 0.0f))

    val objectPole = ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        cylinder = Mesh(gl, this::class.java, "tut09/UnitCylinder.xml")
        plane = Mesh(gl, this::class.java, "tut09/LargePlane.xml")

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(0.0f, 1.0f)
        glEnable(GL_DEPTH_CLAMP)

        glGenBuffers(1, projectionUniformBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer[0])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, null, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer[0], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }

    fun initializeProgram(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val modelMatrix = MatrixStack().setMatrix(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        glUseProgram(whiteDiffuseColor.theProgram)
        glUniform3fv(whiteDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace to vecBuffer)
        glUseProgram(vertexDiffuseColor.theProgram)
        glUniform3fv(vertexDiffuseColor.dirToLightUnif, 1, vecBuffer)
        glUseProgram(0)

        modelMatrix run {

            //Render the ground plane.
            run {

                top() to matBuffer

                glUseProgram(whiteDiffuseColor.theProgram)
                glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer)
                top().toMat3() to matBuffer
                glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, matBuffer)
                glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                plane.render(gl)
                glUseProgram(0)

            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCylinder)
                    scale(1.0f, 1.0f, 0.2f)

                top() to matBuffer

                glUseProgram(vertexDiffuseColor.theProgram)
                glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer)

                val normMatrix = top().toMat3()
                if (doInverseTranspose)
                    normMatrix.inverse().transpose()

                normMatrix to matBuffer
                glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false, matBuffer)
                glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                cylinder.render(gl, "lit-color")
                glUseProgram(0)
            }
        }
        return@with
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        perspMatrix.perspective(45.0f, w.f / h, zNear, zFar)

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer[0])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE.L, perspMatrix.top() to matBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glViewport(0, 0, w, h)
    }

    override fun mousePressed(e: MouseEvent) {
        viewPole.mousePressed(e)
        objectPole.mousePressed(e)
    }

    override fun mouseDragged(e: MouseEvent) {
        viewPole.mouseDragged(e)
        objectPole.mouseDragged(e)
    }

    override fun mouseReleased(e: MouseEvent) {
        viewPole.mouseReleased(e)
        objectPole.mouseReleased(e)
    }

    override fun mouseWheelMoved(e: MouseEvent) {
        viewPole.mouseWheel(e)
    }

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> scaleCylinder = !scaleCylinder

            KeyEvent.VK_T -> {
                doInverseTranspose = !doInverseTranspose
                println(if (doInverseTranspose) "Doing Inverse Transpose." else "Bad Lighting.")
            }
        }
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(vertexDiffuseColor.theProgram)
        glDeleteProgram(whiteDiffuseColor.theProgram)

        glDeleteBuffers(1, projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut09", vertex, fragment)

        val dirToLightUnif = gl.glGetUniformLocation(theProgram, "dirToLight")
        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }
}