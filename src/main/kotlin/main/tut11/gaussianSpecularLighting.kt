package main.tut11

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glNext.*
import glm.glm
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import main.tut11.GaussianSpecularLighting_.LightingModel.BlinnOnly
import main.tut11.GaussianSpecularLighting_.LightingModel.BlinnSpecular
import main.tut11.GaussianSpecularLighting_.LightingModel.GaussianSpecular
import main.tut11.GaussianSpecularLighting_.LightingModel.PhongOnly
import main.tut11.GaussianSpecularLighting_.LightingModel.PhongSpecular
import uno.buffer.destroy
import uno.buffer.intBufferBig
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*
import uno.time.Timer

/**
 * Created by GBarbieri on 24.03.2017.
 */

fun main(args: Array<String>) {
    GaussianSpecularLighting_().setup("Tutorial 11 - Gaussian Specular Lighting")
}

class GaussianSpecularLighting_ : Framework() {

    lateinit var programs: Array<ProgramPairs>
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

    var lightModel = LightingModel.BlinnSpecular

    var drawColoredCyl = false
    var drawLightSource = false
    var scaleCyl = false
    var drawDark = false

    var lightHeight = 1.5f
    var lightRadius = 1.0f
    val lightAttenuation = 1.2f

    val darkColor = Vec4(0.2f, 0.2f, 0.2f, 1.0f)
    val lightColor = Vec4(1.0f)

    val lightTimer = Timer(Timer.Type.Loop, 5.0f)

    val projectionUniformBuffer = intBufferBig(1)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        cylinder = Mesh(gl, javaClass, "tut11/UnitCylinder.xml")
        plane = Mesh(gl, javaClass, "tut11/LargePlane.xml")
        cube = Mesh(gl, javaClass, "tut11/UnitCube.xml")

        val depthZNear = 0.0f
        val depthZFar = 1.0f

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(depthZNear, depthZFar)
        glEnable(GL_DEPTH_CLAMP)

        glGenBuffer(projectionUniformBuffer)

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer)
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer, 0, Mat4.SIZE)

        glBindBuffer(GL_UNIFORM_BUFFER)
    }

    fun initializePrograms(gl: GL3) {
        val FRAGMENTS = arrayOf("phong-lighting", "phong-only", "blinn-lighting", "blinn-only", "gaussian-lighting", "gaussian-only")
        programs = Array(LightingModel.MAX, {
            ProgramPairs(ProgramData(gl, "pn.vert", "${FRAGMENTS[it]}.frag"), ProgramData(gl, "pcn.vert", "${FRAGMENTS[it]}.frag"))
        })
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

        val whiteProg = programs[lightModel].whiteProgram
        val colorProg = programs[lightModel].colorProgram

        glUseProgram(whiteProg.theProgram)
        glUniform4f(whiteProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(whiteProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUniform3f(whiteProg.cameraSpaceLightPosUnif, lightPosCameraSpace)
        glUniform1f(whiteProg.lightAttenuationUnif, lightAttenuation)
        glUniform1f(whiteProg.shininessFactorUnif, MaterialParameters.getSpecularValue(lightModel))
        glUniform4f(whiteProg.baseDiffuseColorUnif, if (drawDark) darkColor else lightColor)

        glUseProgram(colorProg.theProgram)
        glUniform4f(colorProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
        glUniform4f(colorProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        glUniform3f(colorProg.cameraSpaceLightPosUnif, lightPosCameraSpace)
        glUniform1f(colorProg.lightAttenuationUnif, lightAttenuation)
        glUniform1f(colorProg.shininessFactorUnif, MaterialParameters.getSpecularValue(lightModel))
        glUseProgram()

        modelMatrix run {

            //Render the ground plane.
            run {

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                glUseProgram(whiteProg.theProgram)
                glUniformMatrix4f(whiteProg.modelToCameraMatrixUnif, top())

                glUniformMatrix3f(whiteProg.normalModelToCameraMatrixUnif, normMatrix)
                plane.render(gl)
                glUseProgram()
            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCyl)
                    scale(1.0f, 1.0f, 0.2f)

                val normMatrix = modelMatrix.top().toMat3()
                normMatrix.inverse_().transpose_()

                val prog = if (drawColoredCyl) colorProg else whiteProg
                glUseProgram(prog.theProgram)
                glUniformMatrix4f(prog.modelToCameraMatrixUnif, top())

                glUniformMatrix3f(prog.normalModelToCameraMatrixUnif, normMatrix)

                cylinder.render(gl, if (drawColoredCyl) "lit-color" else "lit")

                glUseProgram()
            }

            //Render the light
            if (drawLightSource)

                run {

                    translate(worldLightPos)
                    scale(0.1f)

                    glUseProgram(unlit.theProgram)
                    glUniformMatrix4f(unlit.modelToCameraMatrixUnif, modelMatrix.top())
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

        val proj = perspMatrix.perspective(45.0f, w.toFloat() / h, zNear, zFar).top()

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer)
        glBufferSubData(GL_UNIFORM_BUFFER, proj)
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

        var changedShininess = false

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> drawColoredCyl = !drawColoredCyl

            KeyEvent.VK_I -> lightHeight += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_K -> lightHeight -= if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_L -> lightRadius += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_J -> lightRadius -= if (e.isShiftDown) 0.05f else 0.2f

            KeyEvent.VK_O -> {
                MaterialParameters.increment(lightModel, !e.isShiftDown)
                changedShininess = true
            }
            KeyEvent.VK_U -> {
                MaterialParameters.decrement(lightModel, !e.isShiftDown)
                changedShininess = true
            }

            KeyEvent.VK_Y -> drawLightSource = !drawLightSource
            KeyEvent.VK_T -> scaleCyl = !scaleCyl
            KeyEvent.VK_B -> lightTimer.togglePause()
            KeyEvent.VK_G -> drawDark = !drawDark

            KeyEvent.VK_H -> {
                if (e.isShiftDown)
                    if (lightModel % 2 != 0)
                        lightModel -= 1
                    else
                        lightModel += 1
                else
                    lightModel = (lightModel + 2) % LightingModel.MAX
                println(when (lightModel) {
                    PhongSpecular -> "PhongSpecular"
                    PhongOnly -> "PhongOnly"
                    BlinnSpecular -> "BlinnSpecular"
                    BlinnOnly -> "BlinnOnly"
                    GaussianSpecular -> "GaussianSpecular"
                    else -> "GaussianOnly"
                })
            }
        }

        if (lightRadius < 0.2f)
            lightRadius = 0.2f

        if (changedShininess)
            println("Shiny: " + MaterialParameters.getSpecularValue(lightModel))
    }

    override fun end(gl: GL3) = with(gl) {

        programs.forEach { glDeletePrograms(it.whiteProgram.theProgram, it.colorProgram.theProgram) }
        glDeleteProgram(unlit.theProgram)

        glDeleteBuffer(projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    object LightingModel {
        val PhongSpecular = 0
        val PhongOnly = 1
        val BlinnSpecular = 2
        val BlinnOnly = 3
        val GaussianSpecular = 4
        val GaussianOnly = 5
        val MAX = 6
    }

    class ProgramPairs(val whiteProgram: ProgramData, val colorProgram: ProgramData)

    class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut11", vertex, fragment)

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")
        val ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity")

        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")
        val cameraSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "cameraSpaceLightPos")

        val lightAttenuationUnif = gl.glGetUniformLocation(theProgram, "lightAttenuation")
        val shininessFactorUnif = gl.glGetUniformLocation(theProgram, "shininessFactor")
        val baseDiffuseColorUnif = gl.glGetUniformLocation(theProgram, "baseDiffuseColor")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }

    inner class UnlitProgData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut11", vertex, fragment)

        val objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }

    object MaterialParameters {

        var phongExponent = 4.0f
        var blinnExponent = 4.0f
        var gaussianRoughness = 0.5f

        fun getSpecularValue(model: Int) = when (model) {
            PhongSpecular, PhongOnly -> phongExponent
            BlinnSpecular, BlinnOnly -> blinnExponent
            else -> gaussianRoughness
        }

        fun increment(model: Int, isLarge: Boolean) {
            when (model) {
                PhongSpecular, PhongOnly -> phongExponent += if (isLarge) 0.5f else 0.1f
                BlinnSpecular, BlinnOnly -> blinnExponent += if (isLarge) 0.5f else 0.1f
                else -> gaussianRoughness += if (isLarge) 0.1f else 0.01f
            }
            clampParam(model)
        }

        fun decrement(model: Int, isLarge: Boolean) {
            when (model) {
                PhongSpecular, PhongOnly -> phongExponent -= if (isLarge) 0.5f else 0.1f
                BlinnSpecular, BlinnOnly -> blinnExponent -= if (isLarge) 0.5f else 0.1f
                else -> gaussianRoughness -= if (isLarge) 0.1f else 0.01f
            }
            clampParam(model)
        }

        fun clampParam(model: Int) {
            when (model) {
                PhongSpecular, PhongOnly -> if (phongExponent <= 0.0f) phongExponent = 0.0001f
                BlinnSpecular, BlinnOnly -> if (blinnExponent <= 0.0f) blinnExponent = 0.0001f
                else -> gaussianRoughness = glm.clamp(gaussianRoughness, 0.00001f, 1.0f)
            }
        }
    }
}