package main.tut13

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
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.byteBufferBig
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.gl.UniformBlockArray
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.ViewData
import uno.mousePole.ViewPole
import uno.mousePole.ViewScale
import uno.time.Timer
import java.nio.ByteBuffer

/**
 * Created by elect on 26/03/17.
 */

fun main(args: Array<String>) {
    BasicImpostor_().setup("Tutorial 13 - Basic Impostor")
}

class BasicImpostor_ : Framework() {

    lateinit var litMeshProg: ProgramMeshData
    lateinit var litImpProgs: Array<ProgramImposData>
    lateinit var unlit: UnlitProgData

    val initialViewData = ViewData(
            Vec3(0.0f, 30.0f, 25.0f),
            Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            10.0f,
            0.0f)
    val viewScale = ViewScale(
            3.0f, 70.0f,
            3.5f, 1.5f,
            5.0f, 1.0f,
            90.0f / 250.0f)
    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)

    lateinit var sphere: Mesh
    lateinit var plane: Mesh
    lateinit var cube: Mesh

    object Buffer {
        val PROJECTION = 0
        val LIGHT = 1
        val MATERIAL = 2
        val MAX = 3
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val imposterVAO = intBufferBig(1)

    var currImpostor = Impostors.Basic

    var drawCameraPos = false
    var drawLights = true

    val drawImposter = booleanArrayOf(false, false, false, false)

    val lightHeight = 20.0f
    val halfLightDistance = 25.0f
    val lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance)

    val sphereTimer = Timer(Timer.Type.Loop, 6.0f)

    var materialBlockOffset = 0

    companion object {
        val NUMBER_OF_LIGHTS = 2
    }

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        sphere = Mesh(gl, javaClass, "tut13/UnitSphere.xml")
        plane = Mesh(gl, javaClass, "tut13/LargePlane.xml")
        cube = Mesh(gl, javaClass, "tut13/UnitCube.xml")

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

        glGenBuffers(bufferName)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, GL_DYNAMIC_DRAW)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName[Buffer.LIGHT], 0, LightBlock.SIZE.L)
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER)

        //Empty Vertex Array Object.
        glGenVertexArray(imposterVAO)

        createMaterials(gl)
    }

    fun initializePrograms(gl: GL3) {

        val impShaderNames = arrayOf("basic-impostor", "persp-impostor", "depth-impostor")

        litMeshProg = ProgramMeshData(gl, "pn.vert", "lighting.frag")

        litImpProgs = Array(Impostors.MAX, { ProgramImposData(gl, impShaderNames[it]) })

        unlit = UnlitProgData(gl, "unlit")
    }

    fun createMaterials(gl: GL3) {

        val ubArray = UniformBlockArray(gl, MaterialBlock.SIZE, Materials.MAX)
        materialBlockOffset = ubArray.arrayOffset

        val mtl = MaterialBlock
        mtl.diffuseColor.put(0.5f, 0.5f, 0.5f, 1.0f)
        mtl.specularColor.put(0.5f, 0.5f, 0.5f, 1.0f)
        mtl.specularShininess = 0.6f
        ubArray[Materials.Terrain] = mtl.toBuffer()

        mtl.diffuseColor.put(0.1f, 0.1f, 0.8f, 1.0f)
        mtl.specularColor.put(0.8f, 0.8f, 0.8f, 1.0f)
        mtl.specularShininess = 0.1f
        ubArray[Materials.BlueShiny] = mtl.toBuffer()

        mtl.diffuseColor.put(0.803f, 0.709f, 0.15f, 1.0f)
        mtl.specularColor = Vec4(0.803f, 0.709f, 0.15f, 1.0f) * 0.75
        mtl.specularShininess = 0.18f
        ubArray[Materials.GoldMetal] = mtl.toBuffer()

        mtl.diffuseColor.put(0.4f, 0.4f, 0.4f, 1.0f)
        mtl.specularColor.put(0.1f, 0.1f, 0.1f, 1.0f)
        mtl.specularShininess = 0.8f
        ubArray[Materials.DullGrey] = mtl.toBuffer()

        mtl.diffuseColor.put(0.05f, 0.05f, 0.05f, 1.0f)
        mtl.specularColor.put(0.95f, 0.95f, 0.95f, 1.0f)
        mtl.specularShininess = 0.3f
        ubArray[Materials.BlackShiny] = mtl.toBuffer()

        ubArray.uploadBufferObject(gl, bufferName[Buffer.MATERIAL])
        ubArray.dispose()
    }

    object MaterialBlock {

        var diffuseColor = Vec4()
        var specularColor = Vec4()
        var specularShininess = 0.f
        var padding = FloatArray(3)

        fun toBuffer(): ByteBuffer {
            diffuseColor to buffer
            specularColor.to(buffer, Vec4.SIZE)
            return buffer.putFloat(Vec4.SIZE * 2, specularShininess)
        }

        var SIZE = 3 * Vec4.SIZE
        val buffer = byteBufferBig(SIZE)
    }

    override fun display(gl: GL3) = with(gl) {

        sphereTimer.update()

        glClearBufferf(GL_COLOR, 0.75f, 0.75f, 1.0f, 1.0f)
        glClearBufferf(GL_DEPTH)

        val modelMatrix = MatrixStack(viewPole.calcMatrix())
        val worldToCamMat = modelMatrix.top()

        val lightData = LightBlock

        lightData.ambientIntensity = Vec4(0.2f, 0.2f, 0.2f, 1.0f)
        lightData.lightAttenuation = lightAttenuation

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat * Vec4(0.707f, 0.707f, 0.0f, 0.0f)
        lightData.lights[0].lightIntensity = Vec4(0.6f, 0.6f, 0.6f, 1.0f)

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat * calcLightPosition()
        lightData.lights[1].lightIntensity = Vec4(0.4f, 0.4f, 0.4f, 1.0f)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferSubData(GL_UNIFORM_BUFFER, lightData.toBuffer())
        glBindBuffer(GL_UNIFORM_BUFFER)

        run {
            glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL],
                    Materials.Terrain * materialBlockOffset.L, MaterialBlock.SIZE.L)

            val normMatrix = modelMatrix.top().toMat3()
            normMatrix.inverse_().transpose_()

            glUseProgram(litMeshProg.theProgram)
            glUniformMatrix4f(litMeshProg.modelToCameraMatrixUnif, modelMatrix.top())
            glUniformMatrix3f(litMeshProg.normalModelToCameraMatrixUnif, normMatrix)

            plane.render(gl)

            glUseProgram()
            glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL)
        }

        drawSphere(gl, modelMatrix, Vec3(0.0f, 10.0f, 0.0f), 4.0f, Materials.BlueShiny, drawImposter[0])

        drawSphereOrbit(gl, modelMatrix, Vec3(0.0f, 10.0f, 0.0f), Vec3(0.6f, 0.8f, 0.0f), 20.0f, sphereTimer.getAlpha(),
                2.0f, Materials.DullGrey, drawImposter[1])

        drawSphereOrbit(gl, modelMatrix, Vec3(-10.0f, 1.0f, 0.0f), Vec3(0.0f, 1.0f, 0.0f), 10.0f, sphereTimer.getAlpha(),
                1.0f, Materials.BlackShiny, drawImposter[2])

        drawSphereOrbit(gl, modelMatrix, Vec3(10.0f, 1.0f, 0.0f), Vec3(0.0f, 1.0f, 0.0f), 10.0f, sphereTimer.getAlpha() * 2.0f,
                1.0f, Materials.GoldMetal, drawImposter[3])

        if (drawLights)

            modelMatrix run {

                translate(calcLightPosition())
                scale(0.5f)

                glUseProgram(unlit.theProgram)
                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())

                val lightColor = Vec4(1.0f)
                glUniform4f(unlit.objectColorUnif, lightColor)
                cube.render(gl, "flat")
            }

        if (drawCameraPos)

            modelMatrix run {

                setIdentity()
                translate(0.0f, 0.0f, -viewPole.getView().radius)

                glDisable(GL_DEPTH_TEST)
                glDepthMask(false)
                glUseProgram(unlit.theProgram)
                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
                glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f)
                cube.render(gl, "flat")

                glDepthMask(true)
                glEnable(GL_DEPTH_TEST)
                glUniform4f(unlit.objectColorUnif, 1.0f)
                cube.render(gl, "flat")
            }
    }

    fun calcLightPosition(): Vec4 {

        val scale = glm.PIf * 2.0f

        val timeThroughLoop = sphereTimer.getAlpha()
        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(timeThroughLoop * scale) * 20.0f
        ret.z = glm.sin(timeThroughLoop * scale) * 20.0f

        return ret
    }

    fun drawSphere(gl: GL3, modelMatrix: MatrixStack, position: Vec3, radius: Float, material: Int,
                   drawImposter: Boolean = false) = with(gl) {

        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL],
                material * materialBlockOffset.L, MaterialBlock.SIZE.L)

        if (drawImposter) {

            val cameraSpherePos = modelMatrix.top() * Vec4(position, 1.0f)
            glUseProgram(litImpProgs[currImpostor].theProgram)
            glUniform3f(litImpProgs[currImpostor].cameraSpherePosUnif, cameraSpherePos)
            glUniform1f(litImpProgs[currImpostor].sphereRadiusUnif, radius)

            glBindVertexArray(imposterVAO)

            glDrawArrays(GL_TRIANGLE_STRIP, 4)

            glBindVertexArray()
            glUseProgram()

        } else

            modelMatrix run {

                translate(position)
                scale(radius * 2.0f) //The unit sphere has a radius 0.5f.

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                glUseProgram(litMeshProg.theProgram)
                glUniformMatrix4f(litMeshProg.modelToCameraMatrixUnif, top())
                glUniformMatrix3f(litMeshProg.normalModelToCameraMatrixUnif, normMatrix)

                sphere.render(gl, "lit")

                glUseProgram()
            }
        glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL)
    }

    fun drawSphereOrbit(gl: GL3, modelMatrix: MatrixStack, orbitCenter: Vec3, orbitAxis: Vec3, orbitRadius: Float,
                        orbitAlpha: Float, sphereRadius: Float, material: Int, drawImposter: Boolean = false) {

        modelMatrix run {

            translate(orbitCenter)
            rotate(orbitAxis, 360.0f * orbitAlpha)

            var offsetDir = orbitAxis cross Vec3(0.0f, 1.0f, 0.0f)
            if (offsetDir.length() < 0.001f)
                offsetDir = orbitAxis cross_ Vec3(1.0f, 0.0f, 0.0f)

            offsetDir.normalize_()

            translate(offsetDir * orbitRadius)

            drawSphere(gl, this, Vec3(0.0f), sphereRadius, material, drawImposter)
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        val proj = perspMatrix.perspective(45.0f, w.f / h, zNear, zFar).top()

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferSubData(GL_UNIFORM_BUFFER, proj)
        glBindBuffer(GL_UNIFORM_BUFFER)

        glViewport(w, h)
    }

    override fun mousePressed(e: MouseEvent) {
        viewPole.mousePressed(e)
    }

    override fun mouseDragged(e: MouseEvent) {
        viewPole.mouseDragged(e)
    }

    override fun mouseReleased(e: MouseEvent) {
        viewPole.mouseReleased(e)
    }

    override fun mouseWheelMoved(e: MouseEvent) {
        viewPole.mouseWheel(e)
    }

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_P -> sphereTimer.togglePause()
            KeyEvent.VK_MINUS -> sphereTimer.rewind(0.5f)
            KeyEvent.VK_PLUS -> sphereTimer.fastForward(0.5f)
            KeyEvent.VK_T -> drawCameraPos = !drawCameraPos
            KeyEvent.VK_G -> drawLights = !drawLights

            KeyEvent.VK_1 -> drawImposter[0] = !drawImposter[0]
            KeyEvent.VK_2 -> drawImposter[1] = !drawImposter[1]
            KeyEvent.VK_3 -> drawImposter[2] = !drawImposter[2]
            KeyEvent.VK_4 -> drawImposter[3] = !drawImposter[3]

            KeyEvent.VK_L -> currImpostor = Impostors.Basic
            KeyEvent.VK_J -> currImpostor = Impostors.Perspective
            KeyEvent.VK_H -> currImpostor = Impostors.Depth
        }
        viewPole.keyPressed(e)
    }

    override fun end(gl: GL3) = with(gl) {

        repeat(NUMBER_OF_LIGHTS) { glDeleteProgram(litImpProgs[it].theProgram) }
        glDeletePrograms(litMeshProg.theProgram, unlit.theProgram)

        glDeleteBuffers(Buffer.MAX, bufferName)
        glDeleteVertexArray(imposterVAO)

        sphere.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        destroyBuffers(bufferName, imposterVAO, LightBlock.buffer, MaterialBlock.buffer)
    }

    object Materials {
        val Terrain = 0
        val BlueShiny = 1
        val GoldMetal = 2
        val DullGrey = 3
        val BlackShiny = 4
        val MAX = 5
    }

    object Impostors {
        val Basic = 0
        val Perspective = 1
        val Depth = 2
        val MAX = 3
    }

    class PerLight {

        lateinit var cameraSpaceLightPos: Vec4
        lateinit var lightIntensity: Vec4

        fun to(buffer: ByteBuffer, offset: Int) {
            cameraSpaceLightPos.to(buffer, offset)
            lightIntensity.to(buffer, offset + Vec4.SIZE)
        }

        companion object {
            val SIZE = Vec4.SIZE * 2
        }
    }

    object LightBlock {

        val SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE
        var buffer = byteBufferBig(SIZE)

        lateinit var ambientIntensity: Vec4
        var lightAttenuation = 0.f
        var padding = FloatArray(3)
        var lights = Array(NUMBER_OF_LIGHTS, { PerLight() })

        fun toBuffer(): ByteBuffer {
            ambientIntensity to buffer
            buffer.putFloat(Vec4.SIZE, lightAttenuation)
            lights.forEachIndexed { i, it -> it.to(buffer, Vec4.SIZE * 2 + PerLight.SIZE * i) }
            return buffer
        }
    }

    class ProgramImposData(gl: GL3, shader: String) {

        var theProgram = programOf(gl, javaClass, "tut13", shader + ".vert", shader + ".frag")

        var sphereRadiusUnif = gl.glGetUniformLocation(theProgram, "sphereRadius")
        var cameraSpherePosUnif = gl.glGetUniformLocation(theProgram, "cameraSpherePos")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Light"),
                        Semantic.Uniform.LIGHT)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Material"),
                        Semantic.Uniform.MATERIAL)
            }
        }
    }

    class ProgramMeshData(gl: GL3, vertex: String, fragment: String) {

        var theProgram = programOf(gl, javaClass, "tut13", vertex, fragment)

        var modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
        var normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Light"),
                        Semantic.Uniform.LIGHT)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Material"),
                        Semantic.Uniform.MATERIAL)
            }
        }
    }

    class UnlitProgData(gl: GL3, shader: String) {

        var theProgram = programOf(gl, javaClass, "tut13", shader + ".vert", shader + ".frag")

        var objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor")
        var modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }
}