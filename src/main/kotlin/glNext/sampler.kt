package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3
import java.nio.IntBuffer

/**
 * Created by GBarbieri on 12.04.2017.
 */


fun GL3.glGenSampler(sampler: IntBuffer) = glGenSamplers(1, sampler)

fun GL3.glGenSamplers(samplers: IntBuffer) = glGenSamplers(samplers.capacity(), samplers)

fun GL3.glDeleteSampler(sampler: IntBuffer) = glDeleteSamplers(1, sampler)
fun GL3.glDeleteSamplers(samplers: IntBuffer) = glDeleteSamplers(samplers.capacity(), samplers)

fun GL3.glSamplerParameteri(sampler: IntBuffer, pname: Int, param: Int) = glSamplerParameteri(sampler[0], pname, param)

fun GL3.glBindSampler(unit: Int, sampler: IntBuffer) = glBindSampler(unit, sampler[0])
fun GL3.glBindSampler(unit: Int) = glBindSampler(unit, 0)

fun GL3.initSampler(sampler: IntBuffer, block: Sampler.() -> Unit) {
    glGenSamplers(1, sampler)
    Sampler.gl = this
    Sampler.name = sampler[0]
    Sampler.block()
}

object Sampler {

    lateinit var gl: GL3
    var name = 0

    val linear = Filer.linear
    val nearest = Filer.nearest

    val nearest_mmNearest = Filer.nearest_mmNearest
    val linear_mmNearest = Filer.linear_mmNearest
    val nearest_mmLinear = Filer.nearest_mmLinear
    val linear_mmLinear = Filer.linear_mmLinear

    val clampToEdge = Wrap.clampToEdge
    val mirroredRepeat = Wrap.mirroredRepeat
    val repeat = Wrap.repeat

    var magFilter = linear
        set(value) {
            gl.glSamplerParameteri(name, GL2ES3.GL_TEXTURE_MAG_FILTER, value.i)
            field = value
        }
    var minFilter = nearest_mmLinear
        set(value) {
            gl.glSamplerParameteri(name, GL2ES3.GL_TEXTURE_MIN_FILTER, value.i)
            field = value
        }
    var maxAnisotropy = 1.0f
        set(value) {
            gl.glSamplerParameterf(name, GL2ES3.GL_TEXTURE_MAX_ANISOTROPY_EXT, value)
            field = value
        }
    var wrapS = repeat
        set(value) {
            gl.glSamplerParameteri(name, GL.GL_TEXTURE_WRAP_S, value.i)
            field = value
        }
    var wrapT = repeat
        set(value) {
            gl.glSamplerParameteri(name, GL.GL_TEXTURE_WRAP_T, value.i)
            field = value
        }

    enum class Filer(val i: Int) {nearest(GL.GL_NEAREST), linear(GL.GL_LINEAR),
        nearest_mmNearest(GL.GL_NEAREST_MIPMAP_NEAREST), linear_mmNearest(GL.GL_LINEAR_MIPMAP_NEAREST),
        nearest_mmLinear(GL.GL_NEAREST_MIPMAP_LINEAR), linear_mmLinear(GL.GL_LINEAR_MIPMAP_LINEAR)
    }

    enum class Wrap(val i: Int) {clampToEdge(GL.GL_CLAMP_TO_EDGE), mirroredRepeat(GL.GL_MIRRORED_REPEAT), repeat(GL.GL_REPEAT) }
}

fun GL3.initSamplers(samplers: IntBuffer, block: Samplers.() -> Unit) {
    glGenSamplers(samplers.capacity(), samplers)
    Samplers.gl = this
    Samplers.names = samplers
    Samplers.block()
}

object Samplers {

    lateinit var gl: GL3
    lateinit var names: IntBuffer

    fun at(index: Int, block: Sampler.() -> Unit) {
        Sampler.gl = gl
        Sampler.name = names[index] // bind
        Sampler.block()
    }
}