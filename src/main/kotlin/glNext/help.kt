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
val float = floatBufferBig(1)
val byte = byteBufferBig(1)
val matBuffer = floatBufferBig(16)


fun GL3.glDrawArrays(count: Int) = glDrawArrays(GL.GL_TRIANGLES, 0, count)
fun GL3.glDrawArrays(mode: Int, count: Int) = glDrawArrays(mode, 0, count)

fun GL3.glDrawElements(count: Int, type: Int) = glDrawElements(GL.GL_TRIANGLES, count, type, 0)
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


fun GL3.faceCulling(enable: Boolean = false, frontFace: Int = GL.GL_BACK, cullFace: Int = GL.GL_CCW) {
    if (enable)
        glEnable(GL.GL_CULL_FACE)
    else
        glDisable(GL.GL_CULL_FACE)
    glFrontFace(frontFace)
    glCullFace(cullFace)
}

infix fun Int.bind(buffer: IntBuffer): Buffer {

    return Buffer
}


inline fun GL3.clear(block: Clear.() -> Unit) {
    Clear.gl = this
    Clear.block()
}

object Clear {

    lateinit var gl: GL3

    fun color(n: Number) = color(n, n, n, n)
    fun color(r: Number, g: Number, b: Number, a: Number)
            = gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, matBuffer.put(0, r.f).put(1, g.f).put(2, b.f).put(3, a.f))

    fun color(f: Float) = color(f, f, f, f)
    fun color(r: Float, g: Float, b: Float, a: Float) =
            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, matBuffer.put(0, r).put(1, g).put(2, b).put(3, a))

    fun depth(depth: Number) = depth(depth.f)
    fun depth(depth: Float) = gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, matBuffer.put(0, depth))
}





























