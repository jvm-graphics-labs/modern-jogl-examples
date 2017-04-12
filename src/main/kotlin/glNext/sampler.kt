package glNext

import com.jogamp.opengl.GL3
import java.nio.IntBuffer

/**
 * Created by GBarbieri on 12.04.2017.
 */


fun GL3.glGenSampler(sampler: IntBuffer) = glGenSamplers(1, sampler)
fun GL3.glGenSamplers(samplers: IntBuffer) = glGenSamplers(samplers.capacity(), samplers)

fun GL3.glDeleteSampler(sampler: IntBuffer) = glDeleteSamplers(1, sampler)
fun GL3.glDeleteSamplers(samplers: IntBuffer) = glDeleteSamplers(samplers.capacity(), samplers)

fun GL3.glSamplerParameteri(sampler:IntBuffer, pname:Int, param:Int) = glSamplerParameteri(sampler[0], pname, param)

fun GL3.glBindSampler(unit: Int, sampler: IntBuffer) = glBindSampler(unit, sampler[0])
fun GL3.glBindSampler(unit: Int) = glBindSampler(unit, 0)