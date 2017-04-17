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
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    AmbientLighting_Next().setup("Tutorial 09 - Ambient Lighting")
}

class AmbientLighting_Next : Framework() {

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

            data(Mat4.SIZE, GL_DYNAMIC_DRAW)

            //Bind the static buffers.
            range(Semantic.Uniform.PROJECTION, 0, Mat4.SIZE)
        }
    }

    fun initializeProgram(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag")
        whiteAmbDiffuseColor = ProgramData(gl, "dir-amb-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexAmbDiffuseColor = ProgramData(gl, "dir-amb-vertex-lighting-PCN.vert", "color-passthrough.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(0)
            depth()
        }

        val modelMatrix = MatrixStack().setMatrix(viewPole.calcMatrix())

        val lightDirCameraSpace = modelMatrix.top() * lightDirection

        val whiteDiffuse = if (showAmbient) whiteAmbDiffuseColor else whiteDiffuseColor
        val vertexDiffuse = if (showAmbient) vertexAmbDiffuseColor else vertexDiffuseColor

        usingProgram {

            if (showAmbient) {

                name = whiteDiffuse.theProgram
                glUniform4f(whiteDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
                glUniform4f(whiteDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
                name = vertexDiffuse.theProgram
                glUniform4f(vertexDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
                glUniform4f(vertexDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)

            } else {

                name = whiteDiffuse.theProgram
                glUniform4f(whiteDiffuse.lightIntensityUnif, 1.0f)
                name = vertexDiffuse.theProgram
                glUniform4f(vertexDiffuse.lightIntensityUnif, 1.0f)
            }

            name = whiteDiffuse.theProgram
            glUniform3f(whiteDiffuse.dirToLightUnif, lightDirCameraSpace)
            name = vertexDiffuse.theProgram
            glUniform3f(vertexDiffuse.dirToLightUnif, lightDirCameraSpace)

            modelMatrix run {

                //Render the ground plane.
                run {
                    name = whiteDiffuse.theProgram
                    whiteDiffuse.modelToCameraMatrixUnif.mat4 = top()
                    val normMatrix = top().toMat3()
                    whiteDiffuse.normalModelToCameraMatrixUnif.mat3 = normMatrix
                    plane.render(gl)
                }

                //Render the Cylinder
                run {

                    applyMatrix(objectPole.calcMatrix())

                    if (drawColoredCyl) {
                        name = vertexDiffuse.theProgram
                        vertexDiffuse.modelToCameraMatrixUnif.mat4 = top()
                        val normMatrix = top().toMat3()
                        vertexDiffuse.normalModelToCameraMatrixUnif.mat3 = normMatrix
                        cylinder.render(gl, "lit-color")
                    } else {
                        name = whiteDiffuse.theProgram
                        whiteDiffuse.modelToCameraMatrixUnif.mat4 = top()
                        val normMatrix = top().toMat3()
                        whiteDiffuse.normalModelToCameraMatrixUnif.mat3 = normMatrix
                        cylinder.render(gl, "lit")
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

        glDeletePrograms(vertexDiffuseColor.theProgram, whiteDiffuseColor.theProgram, vertexAmbDiffuseColor.theProgram, whiteAmbDiffuseColor.theProgram)

        glDeleteBuffer(projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut09", vertex, fragment)

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