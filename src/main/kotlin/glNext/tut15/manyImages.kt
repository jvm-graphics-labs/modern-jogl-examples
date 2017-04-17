package glNext.tut15

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.texture.spi.DDSImage
import glNext.*
import glm.*
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.*
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.time.Timer
import java.io.File
import java.nio.ByteBuffer
import glm.mat.Mat4
import glm.vec._3.Vec3

/**
 * Created by GBarbieri on 31.03.2017.
 */

fun main(args: Array<String>) {
    ManyImages_Next().setup("Tutorial 15 - Many Images")
}

class ManyImages_Next : Framework() {

    lateinit var program: ProgramData

    lateinit var plane: Mesh
    lateinit var corridor: Mesh

    val projBufferName = intBufferBig(1)

    object Texture {
        val Checker = 0
        val MipmapTest = 1
        val MAX = 2
    }

    val textureName = intBufferBig(Texture.MAX)
    val samplerName = intBufferBig(Sampler.MAX)

    val camTimer = Timer(Timer.Type.Loop, 5f)
    var useMipmapTexture = false
    var currSampler = 0
    var drawCorridor = false

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        plane = Mesh(gl, javaClass, "tut15/BigPlane.xml")
        corridor = Mesh(gl, javaClass, "tut15/Corridor.xml")

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

        // Generate all the texture names
        glGenTextures(textureName)

        loadCheckerTexture(gl)
        loadMipmapTexture(gl)
        createSamplers(gl)
    }

    fun initializeProgram(gl: GL3) {
        program = ProgramData(gl, "pt.vert", "tex.frag")
    }

    fun loadCheckerTexture(gl: GL3) = with(gl) {

        val file = File(javaClass.getResource("/tut15/checker.dds").toURI())

        val ddsImage = DDSImage.read(file)

        withTexture2d(textureName[Texture.Checker]) {

            repeat(ddsImage.numMipMaps) { mipmapLevel ->

                val mipmap = ddsImage.getMipMap(mipmapLevel)

                image(mipmapLevel, GL_RGB8, mipmap.width, mipmap.height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
            }
            levels = 0 until ddsImage.numMipMaps
        }
    }

    fun loadMipmapTexture(gl: GL3) = with(gl) {

        withTexture2d(textureName[Texture.MipmapTest]) {

            val oldAlign = glGetInteger(GL_UNPACK_ALIGNMENT)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

            for (mipmapLevel in 0..7) {

                val width = 128 shr mipmapLevel
                val height = 128 shr mipmapLevel

                val currColor = mipmapColors[mipmapLevel]
                val buffer = fillWithColors(currColor, width, height)

                image(mipmapLevel, GL_RGB8, width, height, GL_RGB, GL_UNSIGNED_BYTE, buffer)

                buffer.destroy()
            }
            glPixelStorei(GL_UNPACK_ALIGNMENT, oldAlign)
            levels = 0..7
        }
    }

    val mipmapColors = arrayOf(
            byteArrayOf(0xFF.b, 0xFF.b, 0x00.b),
            byteArrayOf(0xFF.b, 0x00.b, 0xFF.b),
            byteArrayOf(0x00.b, 0xFF.b, 0xFF.b),
            byteArrayOf(0xFF.b, 0x00.b, 0x00.b),
            byteArrayOf(0x00.b, 0xFF.b, 0x00.b),
            byteArrayOf(0x00.b, 0x00.b, 0xFF.b),
            byteArrayOf(0x00.b, 0x00.b, 0x00.b),
            byteArrayOf(0xFF.b, 0xFF.b, 0xFF.b))

    fun fillWithColors(color: ByteArray, width: Int, height: Int): ByteBuffer {

        val numTexels = width * height
        val buffer = byteBufferBig(numTexels * 3)

        val (red, green, blue) = color

        while (buffer.hasRemaining())
            buffer
                    .put(red)
                    .put(green)
                    .put(blue)

        buffer.position(0)
        return buffer
    }

    fun createSamplers(gl: GL3) = with(gl) {

        initSamplers(samplerName) {

            for (i in 0 until Sampler.MAX)
                at(i) {
                    wrapS = repeat
                    wrapT = repeat
                }
            at(Sampler.Nearest) {
                magFilter = nearest
                minFilter = nearest
            }
            at(Sampler.Linear) {
                magFilter = linear
                minFilter = linear
            }
            at(Sampler.Linear_MipMap_Nearest) {
                magFilter = linear_mmNearest
                minFilter = linear_mmNearest
            }
            at(Sampler.Linear_MipMap_Linear) {
                magFilter = linear_mmLinear
                minFilter = linear_mmLinear
            }
            at(Sampler.LowAnisotropy) {
                magFilter = linear
                minFilter = linear_mmLinear
                maxAnisotropy = 4.0f
            }

            val maxAniso = caps.limits.MAX_TEXTURE_MAX_ANISOTROPY_EXT // TODO float?
            println("Maximum anisotropy: " + maxAniso)

            at(Sampler.MaxAnisotropy) {
                magFilter = linear
                minFilter = linear_mmLinear
                maxAnisotropy = maxAniso.f
            }
        }
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(0.75f, 0.75f, 1.0f, 1.0f)
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

        modelMatrix.applyMatrix(worldToCamMat) run {

            usingProgram(program.theProgram) {

                program.modelToCameraMatrixUL.mat4 = top()

                val texture = textureName[if (useMipmapTexture) Texture.MipmapTest else Texture.Checker]
                withTexture2d(Semantic.Sampler.DIFFUSE, texture, samplerName[currSampler]) {

                    if (drawCorridor)
                        corridor.render(gl, "tex")
                    else
                        plane.render(gl, "tex")
                }
            }
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val persMatrix = MatrixStack()
        persMatrix.perspective(90f, w / h.f, 1f, 1000f)

        withUniformBuffer(projBufferName) { subData(persMatrix.top()) }

        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        plane.dispose(gl)
        corridor.dispose(gl)

        glDeleteProgram(program.theProgram)

        glDeleteBuffer(projBufferName)
        glDeleteTextures(textureName)
        glDeleteSamplers(samplerName)

        destroyBuffers(projBufferName, textureName, samplerName)
    }

    override fun keyPressed(ke: KeyEvent) {

        when (ke.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> useMipmapTexture = !useMipmapTexture

            KeyEvent.VK_Y -> drawCorridor = !drawCorridor

            KeyEvent.VK_P -> camTimer.togglePause()
        }

        if (ke.keyCode in KeyEvent.VK_1..KeyEvent.VK_9) {
            val number = ke.keyCode - KeyEvent.VK_1
            if (number < Sampler.MAX) {
                println("Sampler: " + samplerNames[number])
                currSampler = number
            }
        }
    }

    object Sampler {
        val Nearest = 0
        val Linear = 1
        val Linear_MipMap_Nearest = 2
        val Linear_MipMap_Linear = 3
        val LowAnisotropy = 4
        val MaxAnisotropy = 5
        val MAX = 6
    }

    val samplerNames = arrayOf("Nearest", "Linear", "Linear with nearest mipmaps", "Linear with linear mipmaps", "Low anisotropic", "Max anisotropic")

    class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut15", vertex, fragment)
        val modelToCameraMatrixUL = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

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