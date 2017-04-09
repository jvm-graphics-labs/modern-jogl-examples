package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3
import glm.*
import glm.mat.Mat4
import glm.vec._2.Vec2
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import uno.buffer.*
import java.nio.*
import kotlin.reflect.KClass

/**
 * Created by elect on 09/04/17.
 */

val int = intBufferBig(1)
val byte = byteBufferBig(1)
val matBuffer = floatBufferBig(16)

fun GL3.glShaderSource(shader: Int, source: String) {
    val lines = arrayOf(source)
    int[0] = lines[0].length
    glShaderSource(shader, 1, lines, int)
}

fun GL3.glGetShader(shader: Int, pname: Int): Int {
    glGetShaderiv(shader, pname, int)
    return int[0]
}

fun GL3.glGetShaderInfoLog(shader: Int): String {

    glGetShaderiv(shader, GL2ES2.GL_INFO_LOG_LENGTH, int)
    val infoLogLength = int[0]

    val bufferInfoLog = byteBufferOf(infoLogLength)
    glGetShaderInfoLog(shader, infoLogLength, null, bufferInfoLog)

    val bytes = ByteArray(infoLogLength)
    bufferInfoLog.get(bytes).destroy()

    return String(bytes)
}

fun GL3.glGetProgram(program: Int, pname: Int): Int {
    glGetProgramiv(program, pname, int)
    return int[0]
}

fun GL3.glGetProgramInfoLog(program: Int): String {

    glGetProgramiv(program, GL2ES2.GL_INFO_LOG_LENGTH, int)
    val infoLogLength = int[0]

    val bufferInfoLog = byteBufferOf(infoLogLength)
    glGetProgramInfoLog(program, infoLogLength, null, bufferInfoLog)

    val bytes = ByteArray(infoLogLength)
    bufferInfoLog.get(bytes).destroy()

    return String(bytes)
}

fun GL3.glGenBuffers(buffers: IntBuffer) = glGenBuffers(buffers.capacity(), buffers)
fun GL3.glGenVertexArrays(VAOs: IntBuffer) = glGenVertexArrays(VAOs.capacity(), VAOs)
fun GL3.glDeleteBuffers(buffers: IntBuffer) = glDeleteBuffers(buffers.capacity(), buffers)
fun GL3.glDeleteVertexArrays(VAOs: IntBuffer) = glDeleteVertexArrays(VAOs.capacity(), VAOs)
fun GL3.glBufferData(target: Int, data: ByteBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: ShortBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: IntBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: LongBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: FloatBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferData(target: Int, data: DoubleBuffer, usage: Int) = glBufferData(target, data.size.L, data, usage)
fun GL3.glBufferSubData(target: Int, data: ByteBuffer) = glBufferSubData(target, 0, data.capacity().L, data)
fun GL3.glBufferSubData(target: Int, data: ShortBuffer) = glBufferSubData(target, 0, data.capacity() * Short.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: IntBuffer) = glBufferSubData(target, 0, data.capacity() * Int.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: LongBuffer) = glBufferSubData(target, 0, data.capacity() * Long.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: FloatBuffer) = glBufferSubData(target, 0, data.capacity() * Float.BYTES.L, data)
fun GL3.glBufferSubData(target: Int, data: DoubleBuffer) = glBufferSubData(target, 0, data.capacity() * Double.BYTES.L, data)
fun GL3.glBindBuffer(target: Int) = glBindBuffer(target, 0)
fun GL3.glBindBuffer(target: Int, buffer: IntBuffer) = glBindBuffer(target, buffer[0])
fun GL3.glBindVertexArray(vao: IntBuffer) = glBindVertexArray(vao[0])
fun GL3.glBindVertexArray() = glBindVertexArray(0)
fun GL3.glDrawArrays(mode: Int, count: Int) = glDrawArrays(mode, 0, count)
fun GL3.glDrawElements(mode: Int, count: Int, type: Int) = glDrawElements(mode, count, type, 0)
fun GL3.glClearBuffer(buffer: Int, f: Float) = when (buffer) {
    GL2ES3.GL_COLOR -> glClearBuffer(buffer, f, f, f, f)
    GL2ES3.GL_DEPTH -> glClearBufferfv(buffer, 0, matBuffer.put(0, f))
    else -> throw Error()
}

fun GL3.glClearBuffer(buffer: Int, r: Float, g: Float, b: Float, a: Float) = glClearBufferfv(buffer, 0, matBuffer.put(0, r).put(1, g).put(2, b).put(3, a))
fun GL3.glClearBuffer(buffer: Int, n: Number) = when (buffer) {
    GL2ES3.GL_COLOR -> glClearBuffer(buffer, n, n, n, n)
    GL2ES3.GL_DEPTH -> glClearBufferfv(buffer, 0, matBuffer.put(0, n.f))
    else -> throw Error()
}

fun GL3.glClearBuffer(buffer: Int, r: Number, g: Number, b: Number, a: Number) = glClearBuffer(buffer, r.f, g.f, b.f, a.f)
fun GL3.glVertexAttribPointer(index: Int, kClass: KClass<*>, offset: Int = 0) = when (kClass) {
    Vec4::class -> glVertexAttribPointer(index, Vec4.length, GL.GL_FLOAT, false, Vec4.SIZE, offset.L)
    Vec3::class -> glVertexAttribPointer(index, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE, offset.L)
    else -> throw Error()
}

fun GL3.glViewport(width: Int, height: Int) = glViewport(0, 0, width, height)
fun GL3.glUseProgram() = glUseProgram(0)
fun GL3.glUniform2f(location: Int) = glUniform2f(location, 0f)
fun GL3.glUniform2f(location: Int, f: Float) = glUniform2f(location, f, f)
fun GL3.glUniform2f(location: Int, vec2: Vec2) = glUniform2f(location, vec2.x, vec2.y)
fun GL3.glUniform3f(location: Int) = glUniform3f(location, 0f)
fun GL3.glUniform3f(location: Int, f: Float) = glUniform3f(location, f, f, f)
fun GL3.glUniform3f(location: Int, vec3: Vec3) = glUniform3f(location, vec3.x, vec3.y, vec3.z)
fun GL3.glUniformMatrix4(location: Int, value: FloatArray) {
    for (i in 0..15)
        matBuffer[i] = value[i]
    glUniformMatrix4fv(location, 1, false, matBuffer)
}
fun GL3.glUniformMatrix4(location: Int, value: Mat4) = glUniformMatrix4fv(location, 1, false, value to matBuffer)

fun GL3.glSetFaceCulling(enable: Boolean = false, frontFace: Int = GL.GL_BACK, cullFace: Int = GL.GL_CCW) {
    if (enable)
        glEnable(GL.GL_CULL_FACE)
    else
        glDisable(GL.GL_CULL_FACE)
    glFrontFace(frontFace)
    glCullFace(cullFace)
}

inline fun GL3.bindingBuffer(pair: Pair<Int, Int>, block: Buffer.(gl: GL3) -> Unit) {
    glBindBuffer(pair.second, pair.first)
    Buffer.target = pair.second
    Buffer.block(this)
    glBindBuffer(pair.second, 0)
}

object Buffer {

    var target = 0

    fun GL3.data(buffer: ByteBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: ShortBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: IntBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: LongBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: FloatBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
    fun GL3.data(buffer: DoubleBuffer, usage: Int) = glBufferData(target, buffer.size.L, buffer, usage)
}


inline fun <R> GL3.clear(block: Clear.() -> R) {
    Clear.reset()
    Clear.block()
    Clear.clear(this)
}

object Clear {

    private var clearColor = false
    private var clearDepth = false

    fun reset() {
        clearColor = false
        clearDepth = false
    }

    fun color(r: Number, g: Number, b: Number, a: Number) = color(r.f, g.f, b.f, a.f)
    fun color(r: Float, g: Float, b: Float, a: Float) {
        matBuffer.put(0, r).put(1, g).put(2, b).put(3, a)
        clearColor = true
    }

    var depth = 0f
        set(value) {
            field = value
            clearDepth = true
        }

    fun clear(gl: GL3) {
        if (clearColor)
            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, matBuffer)
        if (clearDepth)
            gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, matBuffer.put(0, depth))
    }
}

inline fun GL3.usingProgram(program: Int, block: Program.() -> Unit) {
    glUseProgram(program)
    withProgram(program) { block() }
    glUseProgram(0)
}

inline fun withProgram(program: Int, block: Program.() -> Unit) {
    Program.name = program
    return Program.block()
}

object Program {

    var name = 0
//
//    infix fun String.to(location: Int) = GL20.glBindAttribLocation(name, location, this)
//    val String.location
//        get() = GL20.glGetUniformLocation(name, this)
//
//    fun link() = glLinkProgram(name)
//
//    fun uniform4fv(location: Int, vec4: Vec4) = GL20.glUniform4fv(location, vec4 to vec4Buffer)
//    infix fun Vec4.to(location: Int) = GL20.glUniform4fv(location, this to vec4Buffer)
//    infix fun Mat4.to(location: Int) = GL20.glUniformMatrix4fv(location, false, this to mat4Buffer)
}


inline fun GL3.withVertexLayout(buffer: Int, clazz: KClass<*>, vararg params: Pair<Int, Int>, block: () -> Unit) {

    when (clazz) {
        Vec4::class -> {
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, buffer)
            glEnableVertexAttribArray(params[0].first)
            glVertexAttribPointer(params[0].first, Vec4.length, GL2ES2.GL_FLOAT, false, Vec4.SIZE, params[0].second.L)
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
            block()
            glDisableVertexAttribArray(params[0].first)
        }
    }
}

inline fun GL3.withVertexLayout(buffer: Int, clazz: KClass<*>, index: Int, block: () -> Unit) {

    when (clazz) {
        Vec4::class -> {
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, buffer)
            glEnableVertexAttribArray(index)
            glVertexAttribPointer(index, Vec4.length, GL2ES2.GL_FLOAT, false, Vec4.SIZE, 0)
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
            block()
            glDisableVertexAttribArray(index)
        }
    }
}






























