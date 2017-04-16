package glNext.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL
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
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    ScaleAndLighting_Next().setup("Tutorial 09 - Scale and Lighting")
}

class ScaleAndLighting_Next() : Framework() {

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
            data(Mat4.SIZE, GL.GL_DYNAMIC_DRAW)
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

        val modelMatrix = MatrixStack().setMatrix(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        usingProgram(whiteDiffuseColor.theProgram) {
            glUniform3f(whiteDiffuseColor.dirToLightUnif, lightDirCameraSpace)
            name = vertexDiffuseColor.theProgram
            glUniform3f(vertexDiffuseColor.dirToLightUnif, lightDirCameraSpace)
        }

        modelMatrix run {

            //Render the ground plane.
            run {
                usingProgram(whiteDiffuseColor.theProgram) {
                    whiteDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                    whiteDiffuseColor.normalModelToCameraMatrixUnif.mat3 = top().toMat3()
                    glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f)
                    plane.render(gl)
                }
            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCylinder)
                    scale(1.0f, 1.0f, 0.2f)

                usingProgram(vertexDiffuseColor.theProgram) {
                    vertexDiffuseColor.modelToCameraMatrixUnif.mat4= top()

                    val normMatrix = top().toMat3()
                    if (doInverseTranspose)
                        normMatrix.inverse_().transpose_()

                    vertexDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normMatrix
                    glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f)
                    cylinder.render(gl, "lit-color")
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