/**
 * Created by GBarbieri on 10.04.2017.
 */

package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER
import com.jogamp.opengl.GL3
import glm.BYTES
import glm.L
import glm.mat.Mat4
import glm.set
import glm.size
import java.nio.*


fun GL3.glGenBuffers(buffers: IntBuffer) = glGenBuffers(buffers.capacity(), buffers)
fun GL3.glGenBuffer(buffer: IntBuffer) = glGenBuffers(1, buffer)
fun GL3.glGenBuffer(): Int {
    glGenBuffers(1, int)
    return int[0]
}

fun GL3.glDeleteBuffers(buffers: IntBuffer) = glDeleteBuffers(buffers.capacity(), buffers)
fun GL3.glDeleteBuffer(buffer: IntBuffer) = glDeleteBuffers(1, buffer)
fun GL3.glDeleteBuffer(buffer: Int) = glDeleteBuffers(1, int.put(0, buffer))

fun GL3.glBufferData(target: Int, data: ByteBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: ShortBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: IntBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: LongBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: FloatBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: DoubleBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)

fun GL3.glBufferData(target: Int, size: Int, usage: Int) = glBufferData(target, size.L, null, usage)

fun GL3.glBufferSubData(target: Int, offset: Int, data: ByteBuffer) = glBufferSubData(target, offset.L, data.capacity().L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: ShortBuffer) = glBufferSubData(target, offset.L, data.capacity() * Short.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: IntBuffer) = glBufferSubData(target, offset.L, data.capacity() * Int.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: LongBuffer) = glBufferSubData(target, offset.L, data.capacity() * Long.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: FloatBuffer) = glBufferSubData(target, offset.L, data.capacity() * Float.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: DoubleBuffer) = glBufferSubData(target, offset.L, data.capacity() * Double.BYTES.L, data)

fun GL3.glBufferSubData(target: Int, data: ByteBuffer) = glBufferSubData(target, 0, data.capacity().L, data)
fun GL3.glBufferSubData(target: Int, data: ShortBuffer) = glBufferSubData(target, 0, data.capacity() * Short.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: IntBuffer) = glBufferSubData(target, 0, data.capacity() * Int.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: LongBuffer) = glBufferSubData(target, 0, data.capacity() * Long.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: FloatBuffer) = glBufferSubData(target, 0, data.capacity() * Float.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: DoubleBuffer) = glBufferSubData(target, 0, data.capacity() * Double.BYTES.L, data)


// ----- Mat4 -----
fun GL3.glBufferData(target: Int, mat: Mat4, usage: Int) = glBufferData(target, 16 * Float.BYTES.L, mat to matBuffer, usage)

fun GL3.glBufferSubData(target: Int, offset: Int, size: Long, mat4: Mat4) = glBufferSubData(target, offset.L, size, mat4 to matBuffer)
fun GL3.glBufferSubData(target: Int, offset: Int, mat: Mat4) = glBufferSubData(target, offset.L, 16 * Float.BYTES.L, mat to matBuffer)
fun GL3.glBufferSubData(target: Int, mat: Mat4) = glBufferSubData(target, 0, 16 * Float.BYTES.L, mat to matBuffer)

fun GL3.glBindBuffer(target: Int) = glBindBuffer(target, 0)
fun GL3.glBindBuffer(target: Int, buffer: IntBuffer) = glBindBuffer(target, buffer[0])

fun GL3.glBindBufferRange(target: Int, index: Int, buffer: IntBuffer, offset: Int, size: Int) = glBindBufferRange(target, index, buffer[0], offset.L, size.L)
fun GL3.glBindBufferBase(target: Int, index: Int) = glBindBufferBase(target, index, 0)

fun GL3.initArrayBuffer(buffer: IntBuffer, block: BufferA.() -> Unit) {
    buffer[0] = initBuffer(GL.GL_ARRAY_BUFFER, block)
}

fun GL3.initElementBuffer(buffer: IntBuffer, block: BufferA.() -> Unit) {
    buffer[0] = initBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, block)
}

fun GL3.initUniformBuffer(buffer: IntBuffer, block: BufferA.() -> Unit) {
    buffer[0] = initBuffer(GL3.GL_UNIFORM_BUFFER, block)
}

fun GL3.initUniformBuffers(buffers: IntBuffer, block: BufferB.() -> Unit) {
    BufferB.gl = this
    BufferB.target = GL_UNIFORM_BUFFER
    glGenBuffers(buffers.capacity(), buffers)
    BufferB.buffers = buffers
    BufferB.block()
    glBindBuffer(GL_UNIFORM_BUFFER, 0)
}
fun GL3.initBuffers(buffers: IntBuffer, block: BufferB.() -> Unit) {
    BufferB.gl = this
    glGenBuffers(buffers.capacity(), buffers)
    BufferB.buffers = buffers
    BufferB.block()
    glBindBuffer(BufferB.target, 0)
}

fun GL3.initBuffer(target: Int, block: BufferA.() -> Unit): Int {
    BufferA.gl = this
    BufferA.target = target
    glGenBuffers(1, int)
    val name = int[0]
    BufferA.name = name
    glBindBuffer(target, name)
    BufferA.block()
    glBindBuffer(target, 0)
    return name
}

fun GL3.withBuffer(target: Int, buffer: IntBuffer, block: BufferA.(GL3) -> Unit) = withBuffer(target, buffer[0], block)
fun GL3.withBuffer(target: Int, buffer: Int, block: BufferA.(gl: GL3) -> Unit) {
    BufferA.target = target
    BufferA.gl = this
    BufferA.name = buffer
    glBindBuffer(target, buffer)
    BufferA.block(this)
    glBindBuffer(target, 0)
}

fun GL3.withArrayBuffer(buffer: IntBuffer, block: BufferA.(GL3) -> Unit) = withBuffer(GL.GL_ARRAY_BUFFER, buffer[0], block)
fun GL3.withArrayBuffer(buffer: Int, block: BufferA.(gl: GL3) -> Unit) = withBuffer(GL.GL_ARRAY_BUFFER, buffer, block)
fun GL3.withElementBuffer(buffer: IntBuffer, block: BufferA.(gl: GL3) -> Unit) = withBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffer[0], block)
fun GL3.withElementBuffer(buffer: Int, block: BufferA.(gl: GL3) -> Unit) = withBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffer, block)
fun GL3.withUniformBuffer(buffer: IntBuffer, block: BufferA.(gl: GL3) -> Unit) = withBuffer(GL3.GL_UNIFORM_BUFFER, buffer[0], block)
fun GL3.withUniformBuffer(buffer: Int, block: BufferA.(gl: GL3) -> Unit) = withBuffer(GL3.GL_UNIFORM_BUFFER, buffer, block)

object BufferA {

    lateinit var gl: GL3
    var target = 0
    var name = 0

    fun data(data: ByteBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: ShortBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: IntBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: LongBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: FloatBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: DoubleBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)

    fun data(size: Int, usage: Int) = gl.glBufferData(target, size.L, null, usage)

    fun subData(offset: Int, data: ByteBuffer) = gl.glBufferSubData(target, offset.L, data.capacity().L, data)
    fun subData(offset: Int, data: ShortBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Short.BYTES.L, data)
    fun subData(offset: Int, data: IntBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Int.BYTES.L, data)
    fun subData(offset: Int, data: LongBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Long.BYTES.L, data)
    fun subData(offset: Int, data: FloatBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Float.BYTES.L, data)
    fun subData(offset: Int, data: DoubleBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Double.BYTES.L, data)

    fun subData(data: ByteBuffer) = gl.glBufferSubData(target, 0, data.capacity().L, data)
    fun subData(data: ShortBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Short.BYTES.L, data)
    fun subData(data: IntBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Int.BYTES.L, data)
    fun subData(data: LongBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Long.BYTES.L, data)
    fun subData(data: FloatBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Float.BYTES.L, data)
    fun subData(data: DoubleBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Double.BYTES.L, data)


    // ----- Mat4 -----
    fun data(mat: Mat4, usage: Int) = gl.glBufferData(target, 16 * Float.BYTES.L, mat to matBuffer, usage)

    fun subData(offset: Int, size: Long, mat4: Mat4) = gl.glBufferSubData(target, offset.L, size, mat4 to matBuffer)
    fun subData(offset: Int, mat: Mat4) = gl.glBufferSubData(target, offset.L, 16 * Float.BYTES.L, mat to matBuffer)
    fun subData(mat: Mat4) = gl.glBufferSubData(target, 0, 16 * Float.BYTES.L, mat to matBuffer)


    fun range(index: Int, offset: Int, size: Int) = gl.glBindBufferRange(target, index, name, offset.L, size.L)
    fun base(index: Int) = gl.glBindBufferBase(target, index, 0)
}

object BufferB {

    lateinit var gl: GL3
    lateinit var buffers: IntBuffer
    var target = 0
    var name = 0

    fun data(data: ByteBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: ShortBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: IntBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: LongBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: FloatBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)
    fun data(data: DoubleBuffer, usage: Int) = gl.glBufferData(target, data.size.L, data, usage)

    fun data(size: Int, usage: Int) = gl.glBufferData(target, size.L, null, usage)

    fun subData(offset: Int, data: ByteBuffer) = gl.glBufferSubData(target, offset.L, data.capacity().L, data)
    fun subData(offset: Int, data: ShortBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Short.BYTES.L, data)
    fun subData(offset: Int, data: IntBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Int.BYTES.L, data)
    fun subData(offset: Int, data: LongBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Long.BYTES.L, data)
    fun subData(offset: Int, data: FloatBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Float.BYTES.L, data)
    fun subData(offset: Int, data: DoubleBuffer) = gl.glBufferSubData(target, offset.L, data.capacity() * Double.BYTES.L, data)

    fun subData(data: ByteBuffer) = gl.glBufferSubData(target, 0, data.capacity().L, data)
    fun subData(data: ShortBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Short.BYTES.L, data)
    fun subData(data: IntBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Int.BYTES.L, data)
    fun subData(data: LongBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Long.BYTES.L, data)
    fun subData(data: FloatBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Float.BYTES.L, data)
    fun subData(data: DoubleBuffer) = gl.glBufferSubData(target, 0, data.capacity() * Double.BYTES.L, data)


    // ----- Mat4 -----
    fun data(mat: Mat4, usage: Int) = gl.glBufferData(target, 16 * Float.BYTES.L, mat to matBuffer, usage)

    fun subData(offset: Int, size: Long, mat4: Mat4) = gl.glBufferSubData(target, offset.L, size, mat4 to matBuffer)
    fun subData(offset: Int, mat: Mat4) = gl.glBufferSubData(target, offset.L, 16 * Float.BYTES.L, mat to matBuffer)
    fun subData(mat: Mat4) = gl.glBufferSubData(target, 0, 16 * Float.BYTES.L, mat to matBuffer)


    fun range(index: Int, offset: Int, size: Int) = gl.glBindBufferRange(target, index, name, offset.L, size.L)
    fun base(index: Int) = gl.glBindBufferBase(target, index, 0)


    fun at(bufferIndex: Int, block: BufferA.() -> Unit) {
        BufferA.gl = gl
        BufferA.name = buffers[bufferIndex]
        BufferA.target = target
        gl.glBindBuffer(target, buffers[bufferIndex])
        BufferA.block()
    }
}