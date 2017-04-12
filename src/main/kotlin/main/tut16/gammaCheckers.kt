package main.tut16

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import com.jogamp.opengl.util.texture.spi.DDSImage
import glNext.*
import glm.L
import glm.f
import glm.mat.Mat4
import glm.vec._3.Vec3
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.glm.MatrixStack
import uno.time.Timer
import java.io.File
import glm.glm
import glm.rad
import uno.buffer.destroyBuffers
import uno.glsl.programOf

/**
 * Created by GBarbieri on 31.03.2017.
 */

fun main(args: Array<String>) {
    GammaCheckers_().setup("Tutorial 16 - Gamma Checkers")
}

class GammaCheckers_ : Framework() {

    lateinit var progNoGamma: ProgramData
    lateinit var progGamma: ProgramData

    lateinit var plane: Mesh
    lateinit var corridor: Mesh

    val projBufferName = intBufferBig(1)

    object Texture {
        val Linear = 0
        val Gamma = 1
        val MAX = 2
    }

    val textureName = intBufferBig(Texture.MAX)

    object Samplers {
        val LinearMipmapLinear = 0
        val MaxAnisotropic = 1
        val MAX = 2
    }

    val samplerName = intBufferBig(Samplers.MAX)

    val camTimer = Timer(Timer.Type.Loop, 5f)

    var drawGammaProgram = false
    var drawGammaTexture = false
    var currSampler = 0
    var drawCorridor = false

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        corridor = Mesh(gl, javaClass, "tut16/Corridor.xml")
        plane = Mesh(gl, javaClass, "tut16/BigPlane.xml")

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        val depthZNear = 0f
        val depthZFar = 1f

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(depthZNear, depthZFar)
        glEnable(GL_DEPTH_CLAMP)

        //Setup our Uniform Buffers
        glGenBuffer(projBufferName)
        glBindBuffer(GL_UNIFORM_BUFFER, projBufferName)
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projBufferName, 0, Mat4.SIZE)

        glBindBuffer(GL_UNIFORM_BUFFER)

        loadCheckerTextures(gl)
        createSamplers(gl)
    }

    fun initializePrograms(gl: GL3) {

        progNoGamma = ProgramData(gl, "pt.vert", "texture-no-gamma.frag")
        progGamma = ProgramData(gl, "pt.vert", "texture-gamma.frag")
    }

    fun loadCheckerTextures(gl: GL3) = with(gl) {

        glGenTextures(Texture.MAX, textureName)

        glBindTexture(GL_TEXTURE_2D, textureName[Texture.Linear])


        var file = File(javaClass.getResource("/tut16/checker_linear.dds").toURI())
        var ddsImage = DDSImage.read(file)

        repeat(ddsImage.numMipMaps) { mipmapLevel ->

            val mipmap = ddsImage.getMipMap(mipmapLevel)

            glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_SRGB8, mipmap.width, mipmap.height, 0, GL_BGRA,
                    GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.numMipMaps - 1)

        glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.Gamma))

        file = File(javaClass.getResource("/tut16/checker_gamma.dds").toURI())
        ddsImage = DDSImage.read(file)

        repeat(ddsImage.numMipMaps) { mipmapLevel ->

            val mipmap = ddsImage.getMipMap(mipmapLevel)

            glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_SRGB8, mipmap.width, mipmap.height, 0, GL_BGRA,
                    GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.numMipMaps - 1)

        glBindTexture(GL_TEXTURE_2D)
    }

    fun createSamplers(gl: GL3) = with(gl) {

        glGenSamplers(Samplers.MAX, samplerName)

        repeat(Samplers.MAX) {

            glSamplerParameteri(samplerName[it], GL_TEXTURE_WRAP_S, GL_REPEAT)
            glSamplerParameteri(samplerName[it], GL_TEXTURE_WRAP_T, GL_REPEAT)
        }

        glSamplerParameteri(samplerName[Samplers.LinearMipmapLinear], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Samplers.LinearMipmapLinear], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)

        val maxAniso = caps.limits.MAX_TEXTURE_MAX_ANISOTROPY_EXT

        glSamplerParameteri(samplerName[Samplers.MaxAnisotropic], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Samplers.MaxAnisotropic], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glSamplerParameteri(samplerName[Samplers.MaxAnisotropic], GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, .75f, .75f, 1f, 1f)
        glClearBufferf(GL_DEPTH)

        camTimer.update()

        val cyclicAngle = camTimer.getAlpha() * 6.28f
        val hOffset = glm.cos(cyclicAngle) * .25f
        val vOffset = glm.sin(cyclicAngle) * .25f

        val modelMatrix = MatrixStack()
        val worldToCamMat = glm.lookAt(
                Vec3(hOffset, 1f, -64f),
                Vec3(hOffset, -5f + vOffset, -44f),
                Vec3(0f, 1f, 0f))

        modelMatrix.applyMatrix(worldToCamMat)

        val prog = if (drawGammaProgram) progGamma else progNoGamma

        glUseProgram(prog.theProgram)
        glUniformMatrix4f(prog.modelToCameraMatrixUnif, modelMatrix.top())

        glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE)
        glBindTexture(GL_TEXTURE_2D, textureName[if (drawGammaTexture) Texture.Gamma else Texture.Linear])
        glBindSampler(Semantic.Sampler.DIFFUSE, samplerName[currSampler])

        if (drawCorridor)
            corridor.render(gl, "tex")
        else
            plane.render(gl, "tex")

        glBindSampler(Semantic.Sampler.DIFFUSE)
        glBindTexture(GL_TEXTURE_2D)

        glUseProgram()
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val cameraToClipMatrix = glm.perspective(90f.rad, w / h.f, 1f, 1000f)

        glBindBuffer(GL_UNIFORM_BUFFER, projBufferName)
        glBufferSubData(GL_UNIFORM_BUFFER, cameraToClipMatrix)
        glBindBuffer(GL_UNIFORM_BUFFER)

        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        plane.dispose(gl)
        corridor.dispose(gl)

        glDeleteBuffer(projBufferName)
        glDeleteTextures(textureName)
        glDeleteSamplers(samplerName)

        destroyBuffers(projBufferName, textureName, samplerName)
    }

    override fun keyPressed(ke: KeyEvent) {

        when (ke.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_A -> drawGammaProgram = !drawGammaProgram

            KeyEvent.VK_G -> drawGammaTexture = !drawGammaTexture

            KeyEvent.VK_SPACE -> {
                drawGammaProgram = !drawGammaProgram
                drawGammaTexture = !drawGammaTexture
            }

            KeyEvent.VK_Y -> drawCorridor = !drawCorridor

            KeyEvent.VK_P -> camTimer.togglePause()

            KeyEvent.VK_1 -> currSampler = 0

            KeyEvent.VK_2 -> currSampler = 1
        }
        println("----")
        println("Rendering:\t\t\t" + if (drawGammaProgram) "Gamma" else "Linear")
        println("Mipmap Generation:\t" + if (drawGammaTexture) "Gamma" else "Linear")
    }


    class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut16", vertex, fragment)
        var modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)

                glUseProgram(theProgram)
                glUniform1i(
                        glGetUniformLocation(theProgram, "colorTexture"),
                        Semantic.Sampler.DIFFUSE)
                glUseProgram()
            }
        }
    }
}