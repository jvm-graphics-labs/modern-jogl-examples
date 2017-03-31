package main.tut10

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glm.Glm
import glm.L
import glm.f
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.glm.MatrixStack
import uno.mousePole.*
import uno.time.Timer
import glm.glm
import uno.buffer.destroy
import uno.glsl.programOf

/**
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    FragmentPointLighting_().setup("Tutorial 10 - Fragment Point Lighting")
}

class FragmentPointLighting_() : Framework() {

    lateinit var whiteDiffuseColor: ProgramData
    lateinit var vertexDiffuseColor: ProgramData
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

    val projectionUniformBuffer = intBufferBig(1)

    var useFragmentLighting = true
    var drawColoredCyl = false
    var drawLight = false
    var scaleCyl = false
    var lightHeight = 1.5f
    var lightRadius = 1.0f

    val lightTimer = Timer(Timer.Type.Loop, 5.0f)

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

        glGenBuffers(1, projectionUniformBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer[0])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, null, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer[0], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER, 0)
    }

    fun initializePrograms(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "model-pos-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "model-pos-vertex-lighting-PCN.vert", "color-passthrough.frag")
        fragWhiteDiffuseColor = ProgramData(gl, "fragment-lighting-PN.vert", "fragment-lighting.frag")
        fragVertexDiffuseColor = ProgramData(gl, "fragment-lighting-PCN.vert", "fragment-lighting.frag")
        unlit = UnlitProgData(gl, "pos-transform.vert", "uniform-color.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        lightTimer.update()

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val modelMatrix = MatrixStack()
        modelMatrix.setMatrix(viewPole.calcMatrix())

        val worldLightPos = calcLightPosition()

        val lightPosCameraSpace = modelMatrix.top() * worldLightPos

        val whiteProgram = if (useFragmentLighting) fragWhiteDiffuseColor else whiteDiffuseColor
        val vertColorProgram = if (useFragmentLighting) fragVertexDiffuseColor else vertexDiffuseColor

        glUseProgram(whiteProgram.theProgram)
        glUniform4f(whiteProgram.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(whiteProgram.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUseProgram(vertColorProgram.theProgram)
        glUniform4f(vertColorProgram.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(vertColorProgram.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUseProgram(0)

        modelMatrix run {

            //Render the ground plane.
            run {

                glUseProgram(whiteProgram.theProgram)
                glUniformMatrix4fv(whiteProgram.modelToCameraMatrixUnif, 1, false, top() to matBuffer)

                val invTransform = top().inverse()
                val lightPosModelSpace = invTransform * lightPosCameraSpace
                glUniform3fv(whiteProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace to vecBuffer)

                plane.render(gl)
                glUseProgram(0)
            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCyl)
                    scale(1.0f, 1.0f, 0.2f)

                val invTransform = top().inverse()
                val lightPosModelSpace = invTransform * lightPosCameraSpace

                if (drawColoredCyl) {
                    glUseProgram(vertColorProgram.theProgram)
                    glUniformMatrix4fv(vertColorProgram.modelToCameraMatrixUnif, 1, false, top() to matBuffer)

                    glUniform3fv(vertColorProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace to vecBuffer)

                    cylinder.render(gl, "lit-color")
                } else {
                    glUseProgram(whiteProgram.theProgram)
                    glUniformMatrix4fv(whiteProgram.modelToCameraMatrixUnif, 1, false, top() to matBuffer)

                    glUniform3fv(whiteProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace to vecBuffer)

                    cylinder.render(gl, "lit")
                }
                glUseProgram(0)
            }

            if (drawLight)

                run {

                    translate(worldLightPos)
                    scale(0.1f, 0.1f, 0.1f)

                    glUseProgram(unlit.theProgram)
                    glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, top() to matBuffer)
                    glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f)
                    cube.render(gl, "flat")
                }
        }
    }

    fun calcLightPosition(): Vec4 {

        val currentTimeThroughLoop = lightTimer.getAlpha()

        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(currentTimeThroughLoop * (Glm.PIf * 2.0f)) * lightRadius
        ret.z = glm.sin(currentTimeThroughLoop * (Glm.PIf * 2.0f)) * lightRadius

        return ret
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){

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

            KeyEvent.VK_SPACE -> drawColoredCyl = !drawColoredCyl

            KeyEvent.VK_I -> lightHeight += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_K -> lightHeight -= if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_L -> lightRadius += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_J -> lightRadius -= if (e.isShiftDown) 0.05f else 0.2f

            KeyEvent.VK_Y -> drawLight = !drawLight
            KeyEvent.VK_T -> scaleCyl = !scaleCyl
            KeyEvent.VK_H -> useFragmentLighting = !useFragmentLighting
            KeyEvent.VK_B -> lightTimer.togglePause()
        }
        if (lightRadius < 0.2f)
            lightRadius = 0.2f
    }

    override fun end(gl: GL3) = with(gl){

        glDeleteProgram(vertexDiffuseColor.theProgram)
        glDeleteProgram(whiteDiffuseColor.theProgram)
        glDeleteProgram(fragVertexDiffuseColor.theProgram)
        glDeleteProgram(fragWhiteDiffuseColor.theProgram)
        glDeleteProgram(unlit.theProgram)

        glDeleteBuffers(1, projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut10", vertex, fragment)

        val modelSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "modelSpaceLightPos")
        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")
        val ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
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