package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL2GL3
import com.jogamp.opengl.GL3
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Created by GBarbieri on 12.04.2017.
 */

fun GL3.glGenTextures(textures: IntBuffer) = glGenTextures(textures.capacity(), textures)

fun GL3.glBindTexture(target: Int) = glBindTexture(target, 0)

// TODO change capacity() with remaining()?
fun GL3.glDeleteTextures(textures: IntBuffer) = glDeleteTextures(textures.capacity(), textures)

inline fun GL3.withTexture1d(texture: Int, block: Texture.() -> Unit) = withTexture(GL2GL3.GL_TEXTURE_1D, texture, block)
inline fun GL3.withTexture(target: Int, texture: Int, block: Texture.() -> Unit) {
    Texture.gl = this
    Texture.target = target
    glBindTexture(target, texture)
    Texture.block()
    glBindTexture(target, 0)
}

inline fun GL3.withTexture1d(unit: Int, texture: Int, sampler: IntBuffer, block: Texture.() -> Unit) =
        withTexture(unit, GL2GL3.GL_TEXTURE_1D, texture, sampler[0], block)
inline fun GL3.withTexture(unit: Int, target: Int, texture: Int, sampler: Int, block: Texture.() -> Unit) {
    Texture.gl = this
    Texture.target = target
    glActiveTexture(GL.GL_TEXTURE0 + unit)
    glBindTexture(target, texture)
    glBindSampler(unit, sampler)
    Texture.block()
    glBindTexture(target, 0)
    glBindSampler(0, sampler)
}

object Texture {

    var target = 0
    lateinit var gl: GL3

    fun image(level: Int, internalFormat: Int, width: Int, format: Int, type: Int, pixels: ByteBuffer) =
            gl.glTexImage1D(target, level, internalFormat, width, 0, format, type, pixels)

    var baseLevel = 0
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_BASE_LEVEL, value)
            field = value
        }
    var maxLevel = 1_000
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_MAX_LEVEL, value)
            field = value
        }
    var active = 0
        set(value) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + value)
            field = value
        }
}