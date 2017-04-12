package main.tut14

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES2.GL_RED
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glNext.*
import glm.*
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.*
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.*
import uno.time.Timer
import java.nio.ByteBuffer

/**
 * Created by elect on 28/03/17.
 */

fun main(args: Array<String>) {
    BasicTexture_().setup("Tutorial 14 - Basic Texture")
}

class BasicTexture_() : Framework() {

    lateinit var litShaderProg: ProgramData
    lateinit var litTextureProg: ProgramData
    lateinit var unlit: UnlitProgData

    val initialObjectData = ObjectData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(1.0f, 0.0f, 0.0f, 0.0f))
    val initialViewData = ViewData(
            Vec3(initialObjectData.position),
            Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            10.0f,
            0.0f)
    val viewScale = ViewScale(
            1.5f, 70.0f,
            1.5f, 0.5f,
            0.0f, 0.0f, //No camera movement.
            90.0f / 250.0f)
    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)
    val objectPole = ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole)

    lateinit var objectMesh: Mesh
    lateinit var cube: Mesh

    object Buffer {
        val PROJECTION = 0
        val LIGHT = 1
        val MATERIAL = 2
        val MAX = 3
    }

    val gaussTextures = intBufferBig(NUM_GAUSSIAN_TEXTURES)
    val gaussSampler = intBufferBig(1)
    val bufferName = intBufferBig(Buffer.MAX)

    var drawCameraPos = false
    var drawLights = true
    var useTexture = false

    val specularShininess = 0.2f
    val lightHeight = 1.0f
    val lightRadius = 3.0f
    val halfLightDistance = 25.0f
    val lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance)

    val lightTimer = Timer(Timer.Type.Loop, 6.0f)

    val lightBuffer = byteBufferBig(LightBlock.SIZE)

    var currTexture = 0

    companion object {
        val NUMBER_OF_LIGHTS = 2
        val NUM_GAUSSIAN_TEXTURES = 4
    }

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        objectMesh = Mesh(gl, javaClass, "tut14/Infinity.xml")
        cube = Mesh(gl, javaClass, "tut14/UnitCube.xml")

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

        //Setup our Uniform Buffers
        val mtl = MaterialBlock
        mtl.diffuseColor = Vec4(1.0f, 0.673f, 0.043f, 1.0f)
        mtl.specularColor = Vec4(1.0f, 0.673f, 0.043f, 1.0f)
        mtl.specularShininess = specularShininess

        val mtlBuffer = mtl.to(byteBufferBig(MaterialBlock.SIZE))

        glGenBuffers(bufferName)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MATERIAL])
        glBufferData(GL_UNIFORM_BUFFER, mtlBuffer, GL_STATIC_DRAW)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, GL_DYNAMIC_DRAW)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName[Buffer.LIGHT], 0, LightBlock.SIZE.L)
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, Mat4.SIZE.L)
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL], 0, MaterialBlock.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER)

        createGaussianTextures(gl)

        mtlBuffer.destroy()
    }

    fun initializePrograms(gl: GL3) {

        litShaderProg = ProgramData(gl, "pn.vert", "shader-gaussian.frag")
        litTextureProg = ProgramData(gl, "pn.vert", "texture-gaussian.frag")

        unlit = UnlitProgData(gl, "unlit")
    }

    fun createGaussianTextures(gl: GL3) = with(gl) {
        glGenTextures(NUM_GAUSSIAN_TEXTURES, gaussTextures)
        repeat(NUM_GAUSSIAN_TEXTURES) {
            val cosAngleResolution = calcCosAngleResolution(it)
            createGaussianTexture(gl, it, cosAngleResolution)
        }
        glGenSampler(gaussSampler)
        glSamplerParameteri(gaussSampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glSamplerParameteri(gaussSampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glSamplerParameteri(gaussSampler, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    }

    fun calcCosAngleResolution(level: Int): Int {
        val cosAngleStart = 64
        return cosAngleStart * glm.pow(2f, level.f).i
    }

    fun createGaussianTexture(gl: GL3, index: Int, cosAngleResolution: Int) = with(gl) {

        val textureData = buildGaussianData(cosAngleResolution)

        glBindTexture(GL_TEXTURE_1D, gaussTextures[index])
        glTexImage1D(GL_TEXTURE_1D, 0, GL_R8, cosAngleResolution, 0, GL_RED, GL_UNSIGNED_BYTE, textureData)
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAX_LEVEL, 0)
        glBindTexture(GL_TEXTURE_1D)

        textureData.destroy()
    }

    fun buildGaussianData(cosAngleResolution: Int): ByteBuffer {

        val textureData = byteBufferBig(cosAngleResolution)

        repeat(cosAngleResolution) { iCosAng ->

            val cosAng = iCosAng / (cosAngleResolution - 1).f
            val angle = glm.acos(cosAng)
            var exponent = angle / specularShininess
            exponent = -(exponent * exponent)
            val gaussianTerm = glm.exp(exponent)

            textureData[iCosAng] = (gaussianTerm * 255f).b
        }
        return textureData
    }

    override fun display(gl: GL3) = with(gl) {

        lightTimer.update()

        glClearBufferf(GL_COLOR, 0.75f, 0.75f, 1.0f, 1.0f)
        glClearBufferf(GL_DEPTH)

        val modelMatrix = MatrixStack(viewPole.calcMatrix())
        val worldToCamMat = modelMatrix.top()

        val globalLightDirection = Vec3(0.707f, 0.707f, 0.0f)

        val lightData = LightBlock

        lightData.ambientIntensity = Vec4(0.2f, 0.2f, 0.2f, 1.0f)
        lightData.lightAttenuation = lightAttenuation

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat * Vec4(globalLightDirection, 0.0f)
        lightData.lights[0].lightIntensity = Vec4(0.6f, 0.6f, 0.6f, 1.0f)

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat * calcLightPosition()
        lightData.lights[1].lightIntensity = Vec4(0.4f, 0.4f, 0.4f, 1.0f)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferSubData(GL_UNIFORM_BUFFER, lightData to lightBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER)

        run {
            glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL], 0, MaterialBlock.SIZE.L)

            modelMatrix run {

                applyMatrix(objectPole.calcMatrix())
                scale(2.0f)

                val normMatrix = top().toMat3()
                normMatrix.inverse_().transpose_()

                val prog = if (useTexture) litTextureProg else litShaderProg

                glUseProgram(prog.theProgram)
                glUniformMatrix4f(prog.modelToCameraMatrixUnif, top())
                glUniformMatrix3f(prog.normalModelToCameraMatrixUnif, normMatrix)

                glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.GAUSSIAN_TEXTURE)
                glBindTexture(GL_TEXTURE_1D, gaussTextures[currTexture])
                glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, gaussSampler)

                objectMesh.render(gl, "lit")

                glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE)
                glBindTexture(GL_TEXTURE_1D)

                glUseProgram()
                glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL)
            }
        }

        if (drawLights)

            modelMatrix run {

                translate(calcLightPosition())
                scale(0.25f)

                glUseProgram(unlit.theProgram)
                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())

                val lightColor = Vec4(1.0f)
                glUniform4f(unlit.objectColorUnif, lightColor)
                cube.render(gl, "flat")

                reset()
                translate(globalLightDirection * 100.0f)
                scale(5.0f)

                glUniformMatrix4f(unlit.modelToCameraMatrixUnif, top())
                cube.render(gl, "flat")

                glUseProgram()
            }

        if (drawCameraPos)

            modelMatrix run {

                setIdentity()
                translate(0.0f, 0.0f, -viewPole.getView().radius)
                scale(0.25f)

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

        val timeThroughLoop = lightTimer.getAlpha()
        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(timeThroughLoop * scale) * lightRadius
        ret.z = glm.sin(timeThroughLoop * scale) * lightRadius

        return ret
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

            KeyEvent.VK_P -> lightTimer.togglePause()
            KeyEvent.VK_MINUS -> lightTimer.rewind(0.5f)
            KeyEvent.VK_PLUS -> lightTimer.fastForward(0.5f)
            KeyEvent.VK_T -> drawCameraPos = !drawCameraPos
            KeyEvent.VK_G -> drawLights = !drawLights

            KeyEvent.VK_SPACE -> useTexture = !useTexture
        }

        if (e.keyCode in KeyEvent.VK_1 .. KeyEvent.VK_9) {
            val number = e.keyCode - KeyEvent.VK_1
            if (number < NUM_GAUSSIAN_TEXTURES) {
                println("Angle Resolution: " + calcCosAngleResolution(number))
                currTexture = number
            }
        }

        viewPole.keyPressed(e)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(litShaderProg.theProgram, litTextureProg.theProgram, unlit.theProgram)

        glDeleteBuffers(bufferName)
        glDeleteSampler(gaussSampler)
        glDeleteTextures(gaussTextures)

        objectMesh.dispose(gl)
        cube.dispose(gl)

        destroyBuffers(bufferName, gaussSampler, gaussTextures, lightBuffer)
    }

    object MaterialBlock {

        lateinit var diffuseColor: Vec4
        lateinit var specularColor: Vec4
        var specularShininess = 0f
        var padding = FloatArray(3)

        fun to(buffer: ByteBuffer, offset: Int = 0): ByteBuffer {
            diffuseColor.to(buffer, offset)
            specularColor.to(buffer, offset + Vec4.SIZE)
            return buffer.putFloat(offset + 2 * Vec4.SIZE, specularShininess)
        }

        val SIZE = 3 * Vec4.SIZE
    }

    class PerLight {

        var cameraSpaceLightPos = Vec4()
        var lightIntensity = Vec4()

        fun to(buffer: ByteBuffer, offset: Int): ByteBuffer {
            cameraSpaceLightPos.to(buffer, offset)
            return lightIntensity.to(buffer, offset + Vec4.SIZE)
        }

        companion object {
            val SIZE = Vec4.SIZE * 2
        }
    }

    object LightBlock {

        lateinit var ambientIntensity: Vec4
        var lightAttenuation = 0f
        var padding = FloatArray(3)
        var lights = arrayOf(PerLight(), PerLight())

        infix fun to(buffer: ByteBuffer) = to(buffer, 0)

        fun to(buffer: ByteBuffer, offset: Int): ByteBuffer {
            ambientIntensity.to(buffer, offset)
            buffer.putFloat(offset + Vec4.SIZE, lightAttenuation)
            repeat(NUMBER_OF_LIGHTS) { lights[it].to(buffer, offset + 2 * Vec4.SIZE + it * PerLight.SIZE) }
            return buffer
        }

        val SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE
    }

    class ProgramData(gl: GL3, vertex: String, fragment: String) {

        var theProgram = programOf(gl, javaClass, "tut14", vertex, fragment)

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
                        glGetUniformBlockIndex(theProgram, "Material"),
                        Semantic.Uniform.MATERIAL)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Light"),
                        Semantic.Uniform.LIGHT)

                glUseProgram(theProgram)
                glUniform1i(
                        glGetUniformLocation(theProgram, "gaussianTexture"),
                        Semantic.Sampler.GAUSSIAN_TEXTURE)
                glUseProgram(theProgram)
            }
        }
    }

    inner class UnlitProgData(gl: GL3, shader: String) {

        var theProgram = programOf(gl, javaClass, "tut14", shader + ".vert", shader + ".frag")

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