/**
 * Created by GBarbieri on 10.04.2017.
 */

package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL3
import glm.BYTES
import glm.L
import glm.mat.Mat4
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

fun GL3.glBufferSubData(target: Int, offset: Int, data: ByteBuffer) = glBufferSubData(target, offset.L,  data.capacity().L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: ShortBuffer) = glBufferSubData(target, offset.L,  data.capacity() * Short.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: IntBuffer) = glBufferSubData(target, offset.L,  data.capacity() * Int.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: LongBuffer) = glBufferSubData(target, offset.L,  data.capacity() * Long.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: FloatBuffer) = glBufferSubData(target, offset.L,  data.capacity() * Float.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, offset: Int, data: DoubleBuffer) = glBufferSubData(target, offset.L,  data.capacity() * Double.BYTES.L, data)

fun GL3.glBufferSubData(target: Int, data: ByteBuffer) = glBufferSubData(target, 0, data.capacity().L, data)
fun GL3.glBufferSubData(target: Int, data: ShortBuffer) = glBufferSubData(target, 0, data.capacity() * Short.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: IntBuffer) = glBufferSubData(target, 0, data.capacity() * Int.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: LongBuffer) = glBufferSubData(target, 0, data.capacity() * Long.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: FloatBuffer) = glBufferSubData(target, 0, data.capacity() * Float.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: DoubleBuffer) = glBufferSubData(target, 0, data.capacity() * Double.BYTES.L, data)


// ----- Mat4 -----
fun GL3.glBufferData(target: Int, mat: Mat4, usage: Int) = glBufferData(target, 16 * Float.BYTES.L, mat to matBuffer, usage)
fun GL3.glBufferSubData(target: Int, offset: Int, size: Long, mat4: Mat4) = glBufferSubData(target, offset.L, size, mat4 to matBuffer)
fun GL3.glBufferSubData(target: Int, offset: Int, mat: Mat4) = glBufferSubData(target, offset.L,  16 * Float.BYTES.L, mat to matBuffer)
fun GL3.glBufferSubData(target: Int, mat: Mat4) = glBufferSubData(target, 0, 16 * Float.BYTES.L, mat to matBuffer)

fun GL3.glBindBuffer(target: Int) = glBindBuffer(target, 0)
fun GL3.glBindBuffer(target: Int, buffer: IntBuffer) = glBindBuffer(target, buffer[0])

fun GL3.glBindBufferRange(target: Int, index: Int, buffer: IntBuffer, offset: Int, size: Int) = glBindBufferRange(target, index, buffer[0], offset.L, size.L)

fun GL3.withBuffer(target: Int, buffer: IntBuffer, block: Buffer.(GL3) -> Unit) = withBuffer(target, buffer[0], block)
fun GL3.withBuffer(target: Int, buffer: Int, block: Buffer.(gl: GL3) -> Unit) {
    Buffer.target = target
    glBindBuffer(target, buffer)
    Buffer.block(this)
    glBindBuffer(target, 0)
}

fun GL3.withArrayBuffer(buffer: IntBuffer, block: Buffer.(GL3) -> Unit) = withBuffer(GL.GL_ARRAY_BUFFER, buffer[0], block)
fun GL3.withArrayBuffer(buffer: Int, block: Buffer.(gl: GL3) -> Unit) = withBuffer(GL.GL_ARRAY_BUFFER, buffer, block)

object Buffer {

    var target = 0

    fun GL3.data(buffer: ByteBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: ShortBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: IntBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: LongBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: FloatBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: DoubleBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)

    fun GL3.subData(buffer: ByteBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)
    fun GL3.subData(buffer: ShortBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)
    fun GL3.subData(buffer: IntBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)
    fun GL3.subData(buffer: LongBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)
    fun GL3.subData(buffer: FloatBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)
    fun GL3.subData(buffer: DoubleBuffer) = glBufferSubData(target, 0, buffer.size.L, buffer)

    operator fun invoke(block: Buffer.(gl: GL3) -> Unit) {

    }
}