package main.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
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
 * Created by elect on 20/03/17.
 */

fun main(args: Array<String>) {
    BasicLighting_().setup("Tutorial 09 - Basic Lighting")
}

class BasicLighting_() : Framework() {

    lateinit var whiteDiffuseColor: ProgramData
    lateinit var vertexDiffuseColor: ProgramData

    lateinit var cylinderMesh: Mesh
    lateinit var planeMesh: Mesh

    var mat: Mat4? = null

    val projectionUniformBuffer = intBufferBig(1)

    val cameraToClipMatrix = Mat4(0.0f)

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

    val lightDirection = Vec4(0.866f, 0.5f, 0.0f, 0.0f)

    var drawColoredCyl = true

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        cylinderMesh = Mesh(gl, javaClass, "tut09/UnitCylinder.xml")
        planeMesh = Mesh(gl, javaClass, "tut09/LargePlane.xml")

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

    override fun display(gl: GL3) {

        with(gl) {

            glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
            glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

            val modelMatrix = MatrixStack(viewPole.calcMatrix())

            val lightDirCameraSpace = modelMatrix.top() * lightDirection

            lightDirCameraSpace to vecBuffer

            glUseProgram(whiteDiffuseColor.theProgram)
            glUniform3fv(whiteDiffuseColor.dirToLightUnif, 1, vecBuffer)
            glUseProgram(vertexDiffuseColor.theProgram)
            glUniform3fv(vertexDiffuseColor.dirToLightUnif, 1, vecBuffer)
            glUseProgram(0)

            modelMatrix run {

                //  Render the ground plane
                run {

                    top() to matBuffer

                    glUseProgram(whiteDiffuseColor.theProgram)
                    glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer)
                    val normalMatrix = top().toMat3()
                    glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix to matBuffer)
                    glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                    planeMesh.render(gl)
                    glUseProgram(0)
                }

                //  Render the Cylinder
                run {

                    applyMatrix(objectPole.calcMatrix())
                    top() to matBuffer

                    if (drawColoredCyl) {

                        glUseProgram(vertexDiffuseColor.theProgram)
                        glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer)
                        val normalMatrix = top().toMat3()
                        glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix to matBuffer)
                        glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                        cylinderMesh.render(gl, "lit-color")

                    } else {

                        glUseProgram(whiteDiffuseColor.theProgram)
                        glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer)
                        val normalMatrix = top().toMat3()
                        glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix to matBuffer)
                        glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                        cylinderMesh.render(gl, "lit")
                    }
                    glUseProgram(0)
                }
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

        glDeleteBuffers(1, projectionUniformBuffer)

        cylinderMesh.dispose(gl)
        planeMesh.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    class ProgramData(gl: GL3, vertex: String, fragment: String) {

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