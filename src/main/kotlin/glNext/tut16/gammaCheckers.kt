package glNext.tut16

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.texture.spi.DDSImage
import glNext.*
import glm.f
import glm.glm
import glm.mat.Mat4
import glm.rad
import glm.vec._3.Vec3
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.time.Timer
import java.io.File

/**
 * Created by GBarbieri on 31.03.2017.
 */

fun main(args: Array<String>) {
    GammaCheckers_Next().setup("Tutorial 16 - Gamma Checkers")
}

class GammaCheckers_Next : Framework() {

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

        cullFace {
            enable()
            cullFace = back
            frontFace = cw
        }

        val depthZNear = 0f
        val depthZFar = 1f

        depth {
            test = true
            mask = true
            func = lEqual
            rangef = depthZNear..depthZFar
            clamp = true
        }

        //Setup our Uniform Buffers
        initUniformBuffer(projBufferName) {

            data(Mat4.SIZE, GL_DYNAMIC_DRAW)

            range(Semantic.Uniform.PROJECTION, 0, Mat4.SIZE)
        }

        loadCheckerTextures(gl)
        createSamplers(gl)
    }

    fun initializePrograms(gl: GL3) {

        progNoGamma = ProgramData(gl, "pt.vert", "texture-no-gamma.frag")
        progGamma = ProgramData(gl, "pt.vert", "texture-gamma.frag")
    }

    fun loadCheckerTextures(gl: GL3) = with(gl) {

        initTextures2d(textureName) {

            at(Texture.Linear) {

                val file = File(javaClass.getResource("/tut16/checker_linear.dds").toURI())
                val ddsImage = DDSImage.read(file)

                repeat(ddsImage.numMipMaps) { mipmapLevel ->

                    val mipmap = ddsImage.getMipMap(mipmapLevel)

                    image(mipmapLevel, GL_SRGB8, mipmap.width, mipmap.height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
                }
                levels = 0 until ddsImage.numMipMaps
            }
            at(Texture.Gamma) {

                val file = File(javaClass.getResource("/tut16/checker_gamma.dds").toURI())
                val ddsImage = DDSImage.read(file)

                repeat(ddsImage.numMipMaps) { mipmapLevel ->

                    val mipmap = ddsImage.getMipMap(mipmapLevel)

                    image(mipmapLevel, GL_SRGB8, mipmap.width, mipmap.height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
                }
                levels = 0 until ddsImage.numMipMaps
            }
        }
    }

    fun createSamplers(gl: GL3) = with(gl) {

        initSamplers(samplerName) {

            for (i in 0 until Samplers.MAX)
                at(i) {
                    wrapS = repeat
                    wrapT = repeat
                }
            at(Samplers.LinearMipmapLinear) {
                magFilter = linear
                minFilter = linear_mmLinear
            }

            val maxAniso = caps.limits.MAX_TEXTURE_MAX_ANISOTROPY_EXT

            at(Samplers.MaxAnisotropic) {
                magFilter = linear
                minFilter = linear_mmLinear
                maxAnisotropy = maxAniso.f // TODO f
            }
        }
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(.75f, .75f, 1f, 1f)
            depth()
        }

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

        usingProgram(prog.theProgram) {

            prog.modelToCameraMatrixUnif.mat4 = modelMatrix.top()

            val texture = textureName[if (drawGammaTexture) Texture.Gamma else Texture.Linear]
            withTexture2d(Semantic.Sampler.DIFFUSE, texture, samplerName[currSampler]) {

                if (drawCorridor)
                    corridor.render(gl, "tex")
                else
                    plane.render(gl, "tex")
            }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val cameraToClipMatrix = glm.perspective(90f.rad, w / h.f, 1f, 1000f)

        withUniformBuffer(projBufferName) { subData(cameraToClipMatrix) }

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