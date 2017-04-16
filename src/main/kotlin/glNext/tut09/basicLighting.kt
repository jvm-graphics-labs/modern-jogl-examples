package glNext.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import glNext.*
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
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*

/**
 * Created by elect on 20/03/17.
 */

fun main(args: Array<String>) {
    BasicLighting_Next().setup("Tutorial 09 - Basic Lighting")
}

class BasicLighting_Next() : Framework() {

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
            clamp = true
        }

        initUniformBuffer(projectionUniformBuffer) {

            data(Mat4.SIZE, GL_DYNAMIC_DRAW)

            //Bind the static buffers.
            range(Semantic.Uniform.PROJECTION, 0, Mat4.SIZE)
        }
    }

    fun initializeProgram(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(0)
            depth()
        }

        val modelMatrix = MatrixStack(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        usingProgram(whiteDiffuseColor.theProgram) {
            glUniform3f(whiteDiffuseColor.dirToLightUnif, lightDirCameraSpace)
            name = vertexDiffuseColor.theProgram
            glUniform3f(vertexDiffuseColor.dirToLightUnif, lightDirCameraSpace)
        }

        modelMatrix run {

            //  Render the ground plane
            run {

                usingProgram(whiteDiffuseColor.theProgram) {
                    whiteDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                    val normalMatrix = top().toMat3()
                    whiteDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normalMatrix
                    glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f)
                    planeMesh.render(gl)
                }
            }

            //  Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                usingProgram {
                    if (drawColoredCyl) {

                        name = vertexDiffuseColor.theProgram
                        vertexDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                        val normalMatrix = top().toMat3()
                        vertexDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normalMatrix
                        glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f)
                        cylinderMesh.render(gl, "lit-color")

                    } else {

                        name = whiteDiffuseColor.theProgram
                        whiteDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                        val normalMatrix = top().toMat3()
                        whiteDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normalMatrix
                        glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f)
                        cylinderMesh.render(gl, "lit")
                    }
                }
            }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        perspMatrix.perspective(45.0f, w.f / h, zNear, zFar)

        withUniformBuffer(projectionUniformBuffer) { subData(perspMatrix.top()) }

        glViewport(w, h)
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

        glDeletePrograms(vertexDiffuseColor.theProgram, whiteDiffuseColor.theProgram)

        glDeleteBuffer(projectionUniformBuffer)

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