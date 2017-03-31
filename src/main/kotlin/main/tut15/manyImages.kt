package main.tut15

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import com.jogamp.opengl.util.texture.spi.DDSImage
import glm.*
import glm.mat.Mat4
import glm.vec._3.Vec3
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.*
import uno.gl.getFloat
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.time.Timer
import java.io.File
import java.nio.ByteBuffer

/**
 * Created by GBarbieri on 31.03.2017.
 */

fun main(args: Array<String>) {
    ManyImages_().setup("Tutorial 15 - Many Images")
}

class ManyImages_ : Framework() {

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
        glGenBuffers(1, projBufferName)
        glBindBuffer(GL_UNIFORM_BUFFER, projBufferName[0])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, null, GL_DYNAMIC_DRAW)

        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projBufferName[0], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        // Generate all the texture names
        glGenTextures(Texture.MAX, textureName)

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

        glBindTexture(GL_TEXTURE_2D, textureName[Texture.Checker])

        repeat(ddsImage.numMipMaps) { mipmapLevel ->

            val mipmap = ddsImage.getMipMap(mipmapLevel)

            glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_RGB8, mipmap.width, mipmap.height, 0,
                    GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, mipmap.data)
        }

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, ddsImage.numMipMaps - 1)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    fun loadMipmapTexture(gl: GL3) = with(gl) {

        glBindTexture(GL_TEXTURE_2D, textureName[Texture.MipmapTest])

        val oldAlign = intBufferBig(1)

        glGetIntegerv(GL_UNPACK_ALIGNMENT, oldAlign)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        for (mipmapLevel in 0..7) {

            val width = 128 shr mipmapLevel
            val height = 128 shr mipmapLevel

            val currColor = mipmapColors[mipmapLevel]
            val buffer = fillWithColors(currColor, width, height)

            glTexImage2D(GL_TEXTURE_2D, mipmapLevel, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)

            buffer.destroy()
        }

        glPixelStorei(GL_UNPACK_ALIGNMENT, oldAlign[0])

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 7)

        glBindTexture(GL_TEXTURE_2D, 0)

        oldAlign.destroy()
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

        glGenSamplers(Sampler.MAX, samplerName)

        repeat(Sampler.MAX) {

            glSamplerParameteri(samplerName[it], GL_TEXTURE_WRAP_S, GL_REPEAT)
            glSamplerParameteri(samplerName[it], GL_TEXTURE_WRAP_T, GL_REPEAT)
        }

        glSamplerParameteri(samplerName[Sampler.Nearest], GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glSamplerParameteri(samplerName[Sampler.Nearest], GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        glSamplerParameteri(samplerName[Sampler.Linear], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Sampler.Linear], GL_TEXTURE_MIN_FILTER, GL_LINEAR)

        glSamplerParameteri(samplerName[Sampler.Linear_MipMap_Nearest], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Sampler.Linear_MipMap_Nearest], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST)

        glSamplerParameteri(samplerName[Sampler.Linear_MipMap_Linear], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Sampler.Linear_MipMap_Linear], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)

        glSamplerParameteri(samplerName[Sampler.LowAnysotropic], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Sampler.LowAnysotropic], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glSamplerParameterf(samplerName[Sampler.LowAnysotropic], GL_TEXTURE_MAX_ANISOTROPY_EXT, 4.0f)


        val maxAniso = getFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)

        println("Maximum anisotropy: " + maxAniso)

        glSamplerParameteri(samplerName[Sampler.MaxAnysotropic], GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerName[Sampler.MaxAnysotropic], GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glSamplerParameteri(samplerName[Sampler.MaxAnysotropic], GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAniso.i)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.75f, 0.75f, 1.0f, 1.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))


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

            glUseProgram(program.theProgram)

            glUniformMatrix4fv(program.modelToCameraMatrixUL, 1, false, top() to matBuffer)

            glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE)
            glBindTexture(GL_TEXTURE_2D, textureName[if (useMipmapTexture) Texture.MipmapTest else Texture.Checker])
            glBindSampler(Semantic.Sampler.DIFFUSE, samplerName[currSampler])

            if (drawCorridor)
                corridor.render(gl, "tex")
            else
                plane.render(gl, "tex")

            glBindSampler(Semantic.Sampler.DIFFUSE, 0)
            glBindTexture(GL_TEXTURE_2D, 0)

            glUseProgram(program.theProgram)
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val persMatrix = MatrixStack()
        persMatrix.perspective(90f, w / h.f, 1f, 1000f)

        glBindBuffer(GL_UNIFORM_BUFFER, projBufferName[0])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE.L, persMatrix.top() to matBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        plane.dispose(gl)
        corridor.dispose(gl)

        glDeleteProgram(program.theProgram)

        glDeleteBuffers(1, projBufferName)
        glDeleteTextures(Texture.MAX, textureName)
        glDeleteSamplers(Sampler.MAX, samplerName)

        destroyBuffers(projBufferName, textureName, samplerName)
    }

    override fun keyPressed(ke: KeyEvent) {

        when (ke.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> useMipmapTexture = !useMipmapTexture

            KeyEvent.VK_Y -> drawCorridor = !drawCorridor

            KeyEvent.VK_P -> camTimer.togglePause()
        }

        if (ke.keyCode in KeyEvent.VK_1 .. KeyEvent.VK_9) {
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
        val LowAnysotropic = 4
        val MaxAnysotropic = 5
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
                glUseProgram(0)
            }
        }
    }
}