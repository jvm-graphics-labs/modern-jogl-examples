package main.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glNext.*
import glm.L
import glm.f
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*

/**
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    ScaleAndLighting_().setup("Tutorial 09 - Scale and Lighting")
}

class ScaleAndLighting_() : Framework() {

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

        cylinder = Mesh(gl, javaClass, "tut09/UnitCylinder.xml")
        plane = Mesh(gl, javaClass, "tut09/LargePlane.xml")

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(0.0f, 1.0f)
        glEnable(GL_DEPTH_CLAMP)

        glGenBuffer(projectionUniformBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer)
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer, 0, Mat4.SIZE)

        glBindBuffer(GL_UNIFORM_BUFFER)
    }

    fun initializeProgram(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0)
        glClearBufferf(GL_DEPTH)

        val modelMatrix = MatrixStack().setMatrix(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        glUseProgram(whiteDiffuseColor.theProgram)
        glUniform3f(whiteDiffuseColor.dirToLightUnif, lightDirCameraSpace)
        glUseProgram(vertexDiffuseColor.theProgram)
        glUniform3f(vertexDiffuseColor.dirToLightUnif, lightDirCameraSpace)
        glUseProgram()

        modelMatrix run {

            //Render the ground plane.
            run {

                glUseProgram(whiteDiffuseColor.theProgram)
                glUniformMatrix4f(whiteDiffuseColor.modelToCameraMatrixUnif, top())
                glUniformMatrix3f(whiteDiffuseColor.normalModelToCameraMatrixUnif, top())
                glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f)
                plane.render(gl)
                glUseProgram()

            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCylinder)
                    scale(1.0f, 1.0f, 0.2f)

                glUseProgram(vertexDiffuseColor.theProgram)
                glUniformMatrix4f(vertexDiffuseColor.modelToCameraMatrixUnif, top())

                val normMatrix = top().toMat3()
                if (doInverseTranspose)
                    normMatrix.inverse().transpose()

                glUniformMatrix3f(vertexDiffuseColor.normalModelToCameraMatrixUnif, normMatrix)
                glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f)
                cylinder.render(gl, "lit-color")
                glUseProgram()
            }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        perspMatrix.perspective(45.0f, w.f / h, zNear, zFar)

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer)
        glBufferSubData(GL_UNIFORM_BUFFER, perspMatrix.top())
        glBindBuffer(GL_UNIFORM_BUFFER)

        glViewport(w, h)
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