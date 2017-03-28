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
import glm.mat.Mat3
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
    AmbientLighting_()
}

class AmbientLighting_() : Framework("Tutorial 09 - Ambient Lighting") {

    lateinit var whiteDiffuseColor: ProgramData
    lateinit var vertexDiffuseColor: ProgramData
    lateinit var whiteAmbDiffuseColor: ProgramData
    lateinit var vertexAmbDiffuseColor: ProgramData

    lateinit var cylinder: Mesh
    lateinit var plane: Mesh

    val projectionUniformBuffer = intBufferBig(1)

    val lightDirection = Vec4(0.866f, 0.5f, 0.0f, 0.0f)

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

    var drawColoredCyl = true
    var showAmbient = false

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
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer[0], 0, Mat4.SIZE.toLong())

        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }

    fun initializeProgram(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag")
        whiteAmbDiffuseColor = ProgramData(gl, "dir-amb-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexAmbDiffuseColor = ProgramData(gl, "dir-amb-vertex-lighting-PCN.vert", "color-passthrough.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val modelMatrix = MatrixStack().setMatrix(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        val whiteDiffuse = if (showAmbient) whiteAmbDiffuseColor else whiteDiffuseColor
        val vertexDiffuse = if (showAmbient) vertexAmbDiffuseColor else vertexDiffuseColor

        if (showAmbient) {

            glUseProgram(whiteDiffuse.theProgram)
            glUniform4f(whiteDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(whiteDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
            glUseProgram(vertexDiffuse.theProgram)
            glUniform4f(vertexDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(vertexDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)

        } else {

            glUseProgram(whiteDiffuse.theProgram)
            glUniform4f(whiteDiffuse.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
            glUseProgram(vertexDiffuse.theProgram)
            glUniform4f(vertexDiffuse.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
        }

        glUseProgram(whiteDiffuse.theProgram)
        glUniform3fv(whiteDiffuse.dirToLightUnif, 1, lightDirCameraSpace to vecBuffer)
        glUseProgram(vertexDiffuse.theProgram)
        glUniform3fv(vertexDiffuse.dirToLightUnif, 1, vecBuffer)
        glUseProgram(0)

        modelMatrix run {

            //Render the ground plane.
            run {

                glUseProgram(whiteDiffuse.theProgram)
                glUniformMatrix4fv(whiteDiffuse.modelToCameraMatrixUnif, 1, false, top() to matBuffer)
                val normMatrix = top().toMat3()
                glUniformMatrix3fv(whiteDiffuse.normalModelToCameraMatrixUnif, 1, false, normMatrix to matBuffer)
                plane.render(gl)
                glUseProgram(0)

            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (drawColoredCyl) {
                    glUseProgram(vertexDiffuse.theProgram)
                    glUniformMatrix4fv(vertexDiffuse.modelToCameraMatrixUnif, 1, false, top() to matBuffer)
                    val normMatrix = top().toMat3()
                    glUniformMatrix3fv(vertexDiffuse.normalModelToCameraMatrixUnif, 1, false, normMatrix to matBuffer)
                    cylinder.render(gl, "lit-color")
                } else {
                    glUseProgram(whiteDiffuse.theProgram)
                    glUniformMatrix4fv(whiteDiffuse.modelToCameraMatrixUnif, 1, false, top() to matBuffer)
                    val normMatrix = top().toMat3()
                    glUniformMatrix3fv(whiteDiffuse.normalModelToCameraMatrixUnif, 1, false, normMatrix to matBuffer)
                    cylinder.render(gl, "lit")
                }
                glUseProgram(0)
            }
        }
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

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> drawColoredCyl = !drawColoredCyl

            KeyEvent.VK_T -> {
                showAmbient = !showAmbient
                println("Ambient Lighting " + if (showAmbient) "On." else "Off.")
            }
        }
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

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(vertexDiffuseColor.theProgram)
        glDeleteProgram(whiteDiffuseColor.theProgram)
        glDeleteProgram(vertexAmbDiffuseColor.theProgram)
        glDeleteProgram(whiteAmbDiffuseColor.theProgram)

        glDeleteBuffers(1, projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, this::class.java, "tut09", vertex, fragment)

        val dirToLightUnif = gl.glGetUniformLocation(theProgram, "dirToLight")
        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")
        val ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity")

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