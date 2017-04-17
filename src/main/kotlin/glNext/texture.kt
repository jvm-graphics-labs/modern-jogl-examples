package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL2GL3
import com.jogamp.opengl.GL3
import glm.set
import java.nio.IntBuffer

/**
 * Created by GBarbieri on 12.04.2017.
 */

fun GL3.glGenTextures(textures: IntBuffer) = glGenTextures(textures.capacity(), textures)

fun GL3.glBindTexture(target: Int) = glBindTexture(target, 0)

// TODO change capacity() with remaining()?
fun GL3.glDeleteTextures(textures: IntBuffer) = glDeleteTextures(textures.capacity(), textures)

inline fun GL3.withTexture1d(texture: Int, block: Texture1d.() -> Unit) {
    Texture1d.gl = this
    glBindTexture(GL2GL3.GL_TEXTURE_1D, texture)
    Texture1d.block()
    glBindTexture(GL2GL3.GL_TEXTURE_1D, 0)
}

inline fun GL3.withTexture2d(texture: Int, block: Texture2d.() -> Unit) {
    Texture2d.gl = this
    glBindTexture(GL2GL3.GL_TEXTURE_2D, texture)
    Texture2d.block()
    glBindTexture(GL2GL3.GL_TEXTURE_2D, 0)
}

inline fun GL3.withTexture(target: Int, texture: Int, block: Texture.() -> Unit) {
    Texture.gl = this
    Texture.target = target
    glBindTexture(target, texture)
    Texture.block()
    glBindTexture(target, 0)
}

inline fun GL3.withTexture1d(unit: Int, texture: Int, sampler: IntBuffer, block: Texture1d.() -> Unit) {
    Texture1d.gl = this
    glActiveTexture(GL.GL_TEXTURE0 + unit)
    Texture1d.name = texture  // bind
    glBindSampler(unit, sampler)
    Texture1d.block()
    glBindTexture(GL2GL3.GL_TEXTURE_1D, 0)
    glBindSampler(0, sampler)
}

inline fun GL3.withTexture2d(unit: Int, texture: Int, sampler: IntBuffer, block: Texture2d.() -> Unit) =
        withTexture2d(unit, texture, sampler[0], block)

inline fun GL3.withTexture2d(unit: Int, texture: Int, sampler: Int, block: Texture2d.() -> Unit) {
    Texture2d.gl = this
    glActiveTexture(GL.GL_TEXTURE0 + unit)
    Texture2d.name = texture  // bind
    glBindSampler(unit, sampler)
    Texture2d.block()
    glBindTexture(GL2GL3.GL_TEXTURE_2D, 0)
    glBindSampler(0, sampler)
}

inline fun GL3.withTexture(unit: Int, target: Int, texture: Int, sampler: Int, block: Texture.() -> Unit) {
    Texture.gl = this
    Texture.target = target
    glActiveTexture(GL.GL_TEXTURE0 + unit)
    Texture.name = texture  // bind
    glBindSampler(unit, sampler)
    Texture.block()
    glBindTexture(target, 0)
    glBindSampler(0, sampler)
}

fun GL3.initTexture1d(texture: IntBuffer, block: Texture1d.() -> Unit) {
    texture[0] = initTexture1d(block)
}

fun GL3.initTexture1d(block: Texture1d.() -> Unit): Int {
    glGenTextures(1, int)
    val name = int[0]
    Texture1d.gl = this
    Texture1d.name = name   // bind
    Texture1d.block()
    return name
}

fun GL3.initTexture2d(texture: IntBuffer, block: Texture2d.() -> Unit) {
    texture[0] = initTexture2d(block)
}

fun GL3.initTexture2d(block: Texture2d.() -> Unit): Int {
    glGenTextures(1, int)
    val name = int[0]
    Texture2d.gl = this
    Texture2d.name = name   // bind
    Texture2d.block()
    return name
}

fun GL3.initTexture(target: Int, texture: IntBuffer, block: Texture.() -> Unit) {
    texture[0] = initTexture(target, block)
}

fun GL3.initTexture(target: Int, block: Texture.() -> Unit): Int {
    glGenTextures(1, int)
    val name = int[0]
    Texture.gl = this
    Texture.target = target
    Texture.name = name   // bind
    Texture.block()
    return name
}


fun GL3.initTextures2d(textures: IntBuffer, block: Textures2d.() -> Unit) {
    glGenTextures(textures.capacity(), textures)
    Textures2d.gl = this
    Textures2d.names = textures
    Textures2d.block()
}

fun GL3.initTextures(target: Int, textures: IntBuffer, block: Textures.() -> Unit) {
    glGenTextures(textures.capacity(), textures)
    Textures.gl = this
    Textures.target = target
    Textures.names = textures
    Textures.block()
}

object Texture {

    lateinit var gl: GL3
    var target = 0
    var name = 0
        set(value) {
            gl.glBindTexture(target, value)
            field = name
        }

    fun image1d(internalFormat: Int, width: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage1D(GL2GL3.GL_TEXTURE_1D, 0, internalFormat, width, 0, format, type, pixels)

    fun image1d(level: Int, internalFormat: Int, width: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage1D(GL2GL3.GL_TEXTURE_1D, level, internalFormat, width, 0, format, type, pixels)

    var baseLevel = 0
        set(value) {
            gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_BASE_LEVEL, value)
            field = value
        }
    var maxLevel = 1_000
        set(value) {
            gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_MAX_LEVEL, value)
            field = value
        }
    var levels = 0..1_000
        set(value) {
            gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_BASE_LEVEL, value.first)
            gl.glTexParameteri(target, GL2ES3.GL_TEXTURE_MAX_LEVEL, value.endInclusive)
            field = value
        }
}

object Texture1d {

    lateinit var gl: GL3
    var name = 0
        set(value) {
            gl.glBindTexture(GL2GL3.GL_TEXTURE_1D, value)
            field = name
        }

    fun image(internalFormat: Int, width: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage1D(GL2GL3.GL_TEXTURE_1D, 0, internalFormat, width, 0, format, type, pixels)

    fun image(level: Int, internalFormat: Int, width: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage1D(GL2GL3.GL_TEXTURE_1D, level, internalFormat, width, 0, format, type, pixels)

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
    var levels = 0..1_000
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_BASE_LEVEL, value.first)
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_MAX_LEVEL, value.endInclusive)
            field = value
        }
}

object Texture2d {

    lateinit var gl: GL3
    var name = 0
        set(value) {
            gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, value)
            field = name
        }

    fun image(internalFormat: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, pixels)

    fun image(level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, level, internalFormat, width, height, 0, format, type, pixels)

    var baseLevel = 0
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2ES3.GL_TEXTURE_BASE_LEVEL, value)
            field = value
        }
    var maxLevel = 1_000
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2ES3.GL_TEXTURE_MAX_LEVEL, value)
            field = value
        }
    var levels = 0..1_000
        set(value) {
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_BASE_LEVEL, value.first)
            gl.glTexParameteri(GL2GL3.GL_TEXTURE_1D, GL2ES3.GL_TEXTURE_MAX_LEVEL, value.endInclusive)
            field = value
        }
}

object Textures {

    lateinit var gl: GL3
    var target = 0
    lateinit var names: IntBuffer

    fun image(level: Int, internalFormat: Int, width: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage1D(target, level, internalFormat, width, 0, format, type, pixels)

    fun image(level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, pixels)

    fun at1d(index: Int, block: Texture1d.() -> Unit) {
        Texture1d.gl = gl
        Texture1d.name = names[index] // bind
        Texture1d.block()
    }

    fun at2d(index: Int, block: Texture2d.() -> Unit) {
        Texture2d.gl = gl
        Texture2d.name = names[index] // bind
        Texture2d.block()
    }
}

object Textures2d {

    lateinit var gl: GL3
    lateinit var names: IntBuffer

    fun image(level: Int, internalFormat: Int, width: Int, height: Int, format: Int, type: Int, pixels: java.nio.Buffer) =
            gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, level, internalFormat, width, height, 0, format, type, pixels)

    fun at(index: Int, block: Texture2d.() -> Unit) {
        Texture2d.gl = gl
        Texture2d.name = names[index] // bind
        Texture2d.block()
    }
}