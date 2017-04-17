package glNext.tut11

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.f
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import glm.quat.Quat
import glm.mat.Mat4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.intBufferBig
import uno.glm.MatrixStack
import uno.mousePole.*
import uno.time.Timer
import glm.glm
import uno.buffer.destroy
import uno.glsl.programOf

/**
 * Created by GBarbieri on 24.03.2017.
 */

fun main(args: Array<String>) {
    PhongLighting_Next().setup("Tutorial 11 - Fragment Attenuation")
}

class PhongLighting_Next : Framework() {

    lateinit var whiteNoPhong: ProgramData
    lateinit var colorNoPhong: ProgramData
    lateinit var whitePhong: ProgramData
    lateinit var colorPhong: ProgramData
    lateinit var whitePhongOnly: ProgramData
    lateinit var colorPhongOnly: ProgramData
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

    var lightModel = LightingModel.DiffuseAndSpecular

    var drawColoredCyl = false
    var drawLightSource = false
    var scaleCyl = false
    var drawDark = false

    var lightHeight = 1.5f
    var lightRadius = 1.0f
    val lightAttenuation = 1.2f
    var shininessFactor = 4.0f

    val darkColor = Vec4(0.2f, 0.2f, 0.2f, 1.0f)
    val lightColor = Vec4(1.0f)

    val lightTimer = Timer(Timer.Type.Loop, 5.0f)

    val projectionUniformBuffer = intBufferBig(1)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        cylinder = Mesh(gl, javaClass, "tut11/UnitCylinder.xml")
        plane = Mesh(gl, javaClass, "tut11/LargePlane.xml")
        cube = Mesh(gl, javaClass, "tut11/UnitCube.xml")

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

    fun initializePrograms(gl: GL3) {

        whiteNoPhong = ProgramData(gl, "pn.vert", "no-phong.frag")
        colorNoPhong = ProgramData(gl, "pcn.vert", "no-phong.frag")

        whitePhong = ProgramData(gl, "pn.vert", "phong-lighting.frag")
        colorPhong = ProgramData(gl, "pcn.vert", "phong-lighting.frag")

        whitePhongOnly = ProgramData(gl, "pn.vert", "phong-only.frag")
        colorPhongOnly = ProgramData(gl, "pcn.vert", "phong-only.frag")

        unlit = UnlitProgData(gl, "pos-transform.vert", "uniform-color.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        lightTimer.update()

        clear {
            color(0)
            depth()
        }

        val modelMatrix = MatrixStack()
        modelMatrix.setMatrix(viewPole.calcMatrix())

        val worldLightPos = calcLightPosition()
        val lightPosCameraSpace = modelMatrix.top() * worldLightPos

        val (whiteProg, colorProg) = when (lightModel) {

            LightingModel.PureDiffuse -> Pair(whiteNoPhong, colorNoPhong)

            LightingModel.DiffuseAndSpecular -> Pair(whitePhong, colorPhong)

            LightingModel.SpecularOnly -> Pair(whitePhongOnly, colorPhongOnly)
        }

        usingProgram(whiteProg.theProgram) {
            glUniform4f(whiteProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(whiteProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
            glUniform3f(whiteProg.cameraSpaceLightPosUnif, lightPosCameraSpace)
            glUniform1f(whiteProg.lightAttenuationUnif, lightAttenuation)
            glUniform1f(whiteProg.shininessFactorUnif, shininessFactor)
            glUniform4f(whiteProg.baseDiffuseColorUnif, if (drawDark) darkColor else lightColor)

            name = colorProg.theProgram
            glUniform4f(colorProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(colorProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
            glUniform3f(colorProg.cameraSpaceLightPosUnif, lightPosCameraSpace)
            glUniform1f(colorProg.lightAttenuationUnif, lightAttenuation)
            glUniform1f(colorProg.shininessFactorUnif, shininessFactor)
        }

        modelMatrix run {

            //Render the ground plane.
            run {

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                usingProgram(whiteProg.theProgram) {
                    whiteProg.modelToCameraMatrixUnif.mat4 = top()

                    glUniformMatrix3f(whiteProg.normalModelToCameraMatrixUnif, normMatrix)
                    plane.render(gl)
                }
            }

            //Render the Cylinder
            run {

                applyMatrix(objectPole.calcMatrix())

                if (scaleCyl)
                    scale(1.0f, 1.0f, 0.2f)

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                val prog = if (drawColoredCyl) colorProg else whiteProg
                usingProgram(prog.theProgram) {
                    prog.modelToCameraMatrixUnif.mat4 = top()

                    glUniformMatrix3f(prog.normalModelToCameraMatrixUnif, normMatrix)

                    cylinder.render(gl, if (drawColoredCyl) "lit-color" else "lit")
                }
            }

            //Render the light
            if (drawLightSource)

                run {
                    translate(worldLightPos)
                    scale(0.1f)

                    usingProgram(unlit.theProgram) {
                        unlit.modelToCameraMatrixUnif.mat4 = top()
                        glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f)
                        cube.render(gl, "flat")
                    }
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

        withUniformBuffer(projectionUniformBuffer) { subData(proj) }

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
                shininessFactor += if (e.isShiftDown) 0.1f else 0.5f
                changedShininess = true
            }
            KeyEvent.VK_U -> {
                shininessFactor -= if (e.isShiftDown) 0.1f else 0.5f
                changedShininess = true
            }

            KeyEvent.VK_Y -> drawLightSource = !drawLightSource
            KeyEvent.VK_T -> scaleCyl = !scaleCyl
            KeyEvent.VK_B -> lightTimer.togglePause()
            KeyEvent.VK_G -> drawDark = !drawDark

            KeyEvent.VK_H -> {
                lightModel += if (e.isShiftDown) -1 else +1
                println(lightModel)
            }
        }

        if (lightRadius < 0.2f)
            lightRadius = 0.2f

        if (shininessFactor < 0.0f)
            shininessFactor = 0.0001f

        if (changedShininess)
            println("Shiny: $shininessFactor")
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(whiteNoPhong.theProgram, colorNoPhong.theProgram, whitePhong.theProgram, colorPhong.theProgram,
                whitePhongOnly.theProgram, colorPhongOnly.theProgram, unlit.theProgram)

        glDeleteBuffer(projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    enum class LightingModel {

        PureDiffuse,
        DiffuseAndSpecular,
        SpecularOnly;

        operator fun plus(i: Int) = values()[(this.ordinal + i + values().size) % values().size]
    }

    inner class ProgramData(gl: GL3, vertex: String, fragment: String) {

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
}