package main.tut10

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glNext.*
import glm.L
import glm.f
import glm.glm
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._2.Vec2i
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.byteBufferBig
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*
import uno.time.Timer
import java.nio.ByteBuffer

/**
 * Created by GBarbieri on 24.03.2017.
 */

fun main(args: Array<String>) {
    FragmentAttenuation_().setup("Tutorial 10 - Fragment Attenuation")
}

class FragmentAttenuation_() : Framework() {

    lateinit var fragWhiteDiffuseColor: ProgramData
    lateinit var fragVertexDiffuseColor: ProgramData
    lateinit var unlit: UnlitProgData

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
    val initialObjectData = ObjectData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(1.0f, 0.0f, 0.0f, 0.0f))

    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)
    val objectPole = ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole)

    lateinit var cylinder: Mesh
    lateinit var plane: Mesh
    lateinit var cube: Mesh

    var drawColoredCyl = false
    var drawLight = false
    var scaleCyl = false
    var useRSquare = false

    var lightHeight = 1.5f
    var lightRadius = 1.0f
    var lightAttenuation = 1.0f

    val lightTimer = Timer(Timer.Type.Loop, 5.0f)

    object Buffer {
        val PROJECTION = 0
        val UNPROJECTION = 1
        val MAX = 2
    }

    object UnProjectionBlock {

        val SIZE = Mat4.SIZE + Vec2i.SIZE

        lateinit var clipToCameraMatrix: Mat4
        lateinit var windowSize: Vec2i

        infix fun to(buffer: ByteBuffer): ByteBuffer {
            clipToCameraMatrix to buffer
            windowSize.to(buffer, Mat4.SIZE)
            return buffer
        }
    }

    val bufferName = intBufferBig(Buffer.MAX)

    val unprojectBuffer = byteBufferBig(UnProjectionBlock.SIZE)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        cylinder = Mesh(gl, javaClass, "tut10/UnitCylinder.xml")
        plane = Mesh(gl, javaClass, "tut10/LargePlane.xml")
        cube = Mesh(gl, javaClass, "tut10/UnitCube.xml")

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(0.0f, 1.0f)
        glEnable(GL_DEPTH_CLAMP)

        glGenBuffers(bufferName)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.UNPROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, UnProjectionBlock.SIZE, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, Mat4.SIZE.L)
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.UNPROJECTION, bufferName[Buffer.UNPROJECTION], 0, UnProjectionBlock.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER)
    }

    fun initializePrograms(gl: GL3) {
        fragWhiteDiffuseColor = ProgramData(gl, "frag-light-atten-PN.vert", "frag-light-atten.frag")
        fragVertexDiffuseColor = ProgramData(gl, "frag-light-atten-PCN.vert", "frag-light-atten.frag")
        unlit = UnlitProgData(gl, "pos-transform.vert", "uniform-color.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        lightTimer.update()

        glClearBufferf(GL_COLOR, 0)
        glClearBufferf(GL_DEPTH)

        val modelMatrix = MatrixStack()
        modelMatrix.setMatrix(viewPole.calcMatrix())

        val worldLightPos = calcLightPosition()
        val lightPosCameraSpace = modelMatrix.top() * worldLightPos

        glUseProgram(fragWhiteDiffuseColor.theProgram)
        glUniform4f(fragWhiteDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(fragWhiteDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUniform3f(fragWhiteDiffuseColor.cameraSpaceLightPosUnif, lightPosCameraSpace)
        glUniform1f(fragWhiteDiffuseColor.lightAttenuationUnif, lightAttenuation)
        glUniform1i(fragWhiteDiffuseColor.bUseRSquareUnif, if (useRSquare) 1 else 0)

        glUseProgram(fragVertexDiffuseColor.theProgram)
        glUniform4f(fragVertexDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(fragVertexDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUniform3f(fragVertexDiffuseColor.cameraSpaceLightPosUnif, lightPosCameraSpace )
        glUniform1f(fragVertexDiffuseColor.lightAttenuationUnif, lightAttenuation)
        glUniform1i(fragVertexDiffuseColor.bUseRSquareUnif, if (useRSquare) 1 else 0)
        glUseProgram()

        modelMatrix run {

            //Render the ground plane.
            run {

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                glUseProgram(fragWhiteDiffuseColor.theProgram)
                glUniformMatrix4f(fragWhiteDiffuseColor.modelToCameraMatrixUnif, top())

                glUniformMatrix3f(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, normMatrix)
                plane.render(gl)
                glUseProgram()
            }

            //Render the Cylinder
            run {
                applyMatrix(objectPole.calcMatrix())

                if (scaleCyl)
                    modelMatrix.scale(1.0f, 1.0f, 0.2f)

                val normMatrix = modelMatrix.top().toMat3()
                normMatrix.inverse_().transpose_()

                if (drawColoredCyl) {
                    glUseProgram(fragVertexDiffuseColor.theProgram)
                    glUniformMatrix4f(fragVertexDiffuseColor.modelToCameraMatrixUnif, top())

                    glUniformMatrix3f(fragVertexDiffuseColor.normalModelToCameraMatrixUnif, normMatrix)
                    cylinder.render(gl, "lit-color")
                } else {
                    glUseProgram(fragWhiteDiffuseColor.theProgram)
                    glUniformMatrix4f(fragWhiteDiffuseColor.modelToCameraMatrixUnif, top())

                    glUniformMatrix3f(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, normMatrix)
                    cylinder.render(gl, "lit")
                }
                glUseProgram()
            }

            //Render the light
            if (drawLight)

                run {
                    translate(worldLightPos)
                    scale(0.1f)

                    glUseProgram(unlit.theProgram)
                    glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
                    glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f)
                    cube.render(gl, "flat")
                }
        }
    }

    fun calcLightPosition(): Vec4 {

        val currentTimeThroughLoop = lightTimer.getAlpha()

        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(currentTimeThroughLoop * (glm.PIf * 2.0f)) * lightRadius
        ret.z = glm.sin(currentTimeThroughLoop * (glm.PIf * 2.0f)) * lightRadius

        return ret
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        val proj = perspMatrix.perspective(45.0f, w.f / h, zNear, zFar).top()

        UnProjectionBlock.clipToCameraMatrix = perspMatrix.top().inverse()
        UnProjectionBlock.windowSize = Vec2i(w, h)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferSubData(GL_UNIFORM_BUFFER, proj)
        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.UNPROJECTION])
        glBufferSubData(GL_UNIFORM_BUFFER, UnProjectionBlock to unprojectBuffer)
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

        var changedAtten = false

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> drawColoredCyl = !drawColoredCyl

            KeyEvent.VK_I -> lightHeight += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_K -> lightHeight -= if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_L -> lightRadius += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_J -> lightRadius -= if (e.isShiftDown) 0.05f else 0.2f

            KeyEvent.VK_O -> {
                lightAttenuation *= if (e.isShiftDown) 1.1f else 1.5f
                changedAtten = true
            }
            KeyEvent.VK_U -> {
                lightAttenuation /= if (e.isShiftDown) 1.1f else 1.5f
                changedAtten = true
            }

            KeyEvent.VK_Y -> drawLight = !drawLight
            KeyEvent.VK_T -> scaleCyl = !scaleCyl
            KeyEvent.VK_B -> lightTimer.togglePause()

            KeyEvent.VK_H -> {
                useRSquare = !useRSquare
                println(if (useRSquare) "Inverse Squared" else "Plain Inverse" + " Attenuation")
            }
        }

        if (lightRadius < 0.2f)
            lightRadius = 0.2f

        if (lightAttenuation < 0.1f)
            lightAttenuation = 0.1f

        if (changedAtten)
            println("Atten: $lightAttenuation")
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(fragVertexDiffuseColor.theProgram, fragWhiteDiffuseColor.theProgram, unlit.theProgram)

        glDeleteBuffers(bufferName)

        cylinder.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        destroyBuffers(bufferName, unprojectBuffer)
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut10", vertex, fragment)

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")
        val ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity")

        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")
        val cameraSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "cameraSpaceLightPos")

        val lightAttenuationUnif = gl.glGetUniformLocation(theProgram, "lightAttenuation")
        val bUseRSquareUnif = gl.glGetUniformLocation(theProgram, "bUseRSquare")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)

                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "UnProjection"),
                        Semantic.Uniform.UNPROJECTION)
            }
        }
    }

    inner class UnlitProgData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut10", vertex, fragment)

        val objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }
}