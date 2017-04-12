package main.tut16

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.texture.TextureIO
import glNext.*
import glm.L
import glm.mat.Mat4
import glm.size
import glm.vec._2.Vec2s
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.buffer.shortBufferOf
import uno.glm.MatrixStack
import uno.glsl.programOf
import java.io.File

/**
 * Created by GBarbieri on 31.03.2017.
 */

fun main(args: Array<String>) {
    GammaRamp_().setup("Tutorial 14 - Material Texture")
}

class GammaRamp_ : Framework() {

    var noGammaProgram = 0
    var gammaProgram = 0

    object Buffer {
        val VERTEX = 0
        val PROJECTION = 1
        val MAX = 2
    }

    object Texture {
        val NO_GAMMA = 0
        val GAMMA = 1
        val MAX = 2
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val vao = intBufferBig(1)
    val textureName = intBufferBig(Texture.MAX)
    val samplerName = intBufferBig(1)

    val useGammaCorrect = booleanArrayOf(false, false)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        glGenBuffers(bufferName)
        initializeVertexData(gl)

        loadTextures(gl)

        //Setup our Uniform Buffers
        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, GL_DYNAMIC_DRAW)

        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER)
    }

    fun initializePrograms(gl: GL3) = with(gl) {

        noGammaProgram = programOf(gl, javaClass, "tut16", "screen-coords.vert", "texture-no-gamma.frag")
        gammaProgram = programOf(gl, javaClass, "tut16", "screen-coords.vert", "texture-gamma.frag")

        var projectionBlock = gl.glGetUniformBlockIndex(noGammaProgram, "Projection")
        glUniformBlockBinding(noGammaProgram, projectionBlock, Semantic.Uniform.PROJECTION)

        var colorTextureUnif = glGetUniformLocation(noGammaProgram, "colorTexture")
        glUseProgram(noGammaProgram)
        glUniform1i(colorTextureUnif, Semantic.Sampler.DIFFUSE)
        glUseProgram()

        projectionBlock = glGetUniformBlockIndex(gammaProgram, "Projection")
        glUniformBlockBinding(gammaProgram, projectionBlock, Semantic.Uniform.PROJECTION)

        colorTextureUnif = glGetUniformLocation(gammaProgram, "colorTexture")
        glUseProgram(gammaProgram)
        glUniform1i(colorTextureUnif, Semantic.Sampler.DIFFUSE)
        glUseProgram()
    }

    fun initializeVertexData(gl: GL3) = with(gl) {

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)

        glGenVertexArray(vao)

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
        glEnableVertexAttribArray(glf.pos2us_tc2us)
        glVertexAttribPointer(glf.pos2us_tc2us)
        glEnableVertexAttribArray(glf.pos2us_tc2us[1])
        // cant use glf.pos2_tc2[1] because of normalized is true
        glVertexAttribPointer(Semantic.Attr.TEX_COORD, Vec2s.length, GL_UNSIGNED_SHORT, true, Vec2s.SIZE * 2, Vec2s.SIZE.L)

        glBindVertexArray()
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    val vertexData = shortBufferOf(
            90, 80, 0, 0,
            90, 16, 0, 65535,
            410, 80, 65535, 0,
            410, 16, 65535, 65535,
            90, 176, 0, 0,
            90, 112, 0, 65535,
            410, 176, 65535, 0,
            410, 112, 65535, 65535)

    fun loadTextures(gl: GL3) = with(gl) {

        glGenTextures(Texture.MAX, textureName)

        val file = File(javaClass.getResource("/tut16/gamma_ramp.png").toURI())

        val textureData = TextureIO.newTextureData(glProfile, file, false, TextureIO.PNG)

        glBindTexture(GL_TEXTURE_2D, textureName[Texture.NO_GAMMA])
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, textureData.width, textureData.height,
                0, textureData.pixelFormat, textureData.pixelType, textureData.buffer)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)

        glBindTexture(GL_TEXTURE_2D, textureName[Texture.GAMMA])
        glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8, textureData.width, textureData.height,
                0, textureData.pixelFormat, textureData.pixelType, textureData.buffer)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)

        glBindTexture(GL_TEXTURE_2D)

        glGenSampler(samplerName)
        glSamplerParameteri(samplerName, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glSamplerParameteri(samplerName, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glSamplerParameteri(samplerName, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glSamplerParameteri(samplerName, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0.0f, 0.5f, 0.3f, 1.0f)

        glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE)
        glBindTexture(GL_TEXTURE_2D, textureName[if (useGammaCorrect[0]) Texture.GAMMA else Texture.NO_GAMMA])
        glBindSampler(Semantic.Sampler.DIFFUSE, samplerName)

        glBindVertexArray(vao)

        glUseProgram(noGammaProgram)
        glDrawArrays(GL_TRIANGLE_STRIP, 4)

        glBindTexture(GL_TEXTURE_2D, textureName[if (useGammaCorrect[1]) Texture.GAMMA else Texture.NO_GAMMA])

        glUseProgram(gammaProgram)
        glDrawArrays(GL_TRIANGLE_STRIP, 4, 4)

        glBindVertexArray()
        glUseProgram()

        glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE)
        glBindTexture(GL_TEXTURE_2D)
        glBindSampler(Semantic.Sampler.DIFFUSE)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val persMatrix = MatrixStack()
        persMatrix
                .translate(-1f, 1f, 0f)
                .scale(2f / w, -2f / h, 1f)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferSubData(GL_UNIFORM_BUFFER, persMatrix.top())
        glBindBuffer(GL_UNIFORM_BUFFER)

        glViewport(w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(noGammaProgram, gammaProgram)

        glDeleteBuffers(bufferName)
        glDeleteVertexArray(vao)
        glDeleteTextures(textureName)
        glDeleteSampler(samplerName)

        destroyBuffers(bufferName, vao, textureName, samplerName, vertexData)
    }

    override fun keyPressed(ke: KeyEvent) {

        when (ke.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_1 -> {
                useGammaCorrect[0] = !useGammaCorrect[0]
                if (useGammaCorrect[0])
                    println("Top:\tsRGB texture.")
                else
                    println("Top:\tlinear texture.")
            }

            KeyEvent.VK_2 -> {
                useGammaCorrect[1] = !useGammaCorrect[1]
                if (useGammaCorrect[1])
                    println("Bottom:\tsRGB texture.")
                else
                    println("Bottom:\tlinear texture.")
            }
        }
    }

    class ProgramData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, "tut16", vertex, fragment)
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