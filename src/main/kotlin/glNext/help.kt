package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3
import com.jogamp.opengl.GL3
import glm.f
import uno.buffer.byteBufferBig
import uno.buffer.floatBufferBig
import uno.buffer.intBufferBig
import java.nio.IntBuffer

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

fun GL3.glDrawElementsBaseVertex(count: Int, type: Int, indices_buffer_offset: Long, basevertex: Int) =
        glDrawElementsBaseVertex(GL.GL_TRIANGLES, count, type, indices_buffer_offset, basevertex)

fun GL3.glClearBufferf(buffer: Int, f: Float) = when (buffer) {
    GL2ES3.GL_COLOR -> glClearBufferf(buffer, f, f, f, f)
    GL2ES3.GL_DEPTH -> glClearBufferfv(buffer, 0, matBuffer.put(0, f))
    else -> throw Error()
}

fun GL3.glClearBufferf(buffer: Int, r: Float, g: Float, b: Float, a: Float) = glClearBufferfv(buffer, 0, matBuffer.put(0, r).put(1, g).put(2, b).put(3, a))
fun GL3.glClearBufferf(buffer: Int, n: Number) = when (buffer) {
    GL2ES3.GL_COLOR -> glClearBufferf(buffer, n.f, n.f, n.f, n.f)
    GL2ES3.GL_DEPTH -> glClearBufferfv(buffer, 0, matBuffer.put(0, n.f))
    else -> throw Error()
}

fun GL3.glClearBufferf(buffer: Int) = when (buffer) {
    GL2ES3.GL_COLOR -> glClearBufferfv(buffer, 0, matBuffer.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f))
    GL2ES3.GL_DEPTH -> glClearBufferfv(buffer, 0, matBuffer.put(0, 1f))
    else -> throw Error()
}

fun GL3.glClearBufferf(buffer: Int, r: Number, g: Number, b: Number, a: Number) = glClearBufferf(buffer, r.f, g.f, b.f, a.f)


fun GL3.glViewport(width: Int, height: Int) = glViewport(0, 0, width, height)



inline fun GL3.clear(block: Clear.() -> Unit) {
    Clear.gl = this
    Clear.block()
}

object Clear {

    lateinit var gl: GL3


    fun color() = color(0f, 0f, 0f, 1f)
    fun color(f: Float) = color(f, f, f, f)
    fun color(r: Float, g: Float, b: Float, a: Float) =
            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, matBuffer.put(0, r).put(1, g).put(2, b).put(3, a))

    fun color(n: Number) = color(n, n, n, n)
    fun color(r: Number, g: Number, b: Number, a: Number)
            = gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, matBuffer.put(0, r.f).put(1, g.f).put(2, b.f).put(3, a.f))

    fun depth() = depth(1f)
    fun depth(depth: Float) = gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, matBuffer.put(0, depth))
    fun depth(depth: Number) = depth(depth.f)
}





fun GL3.glGetInteger(pname: Int): Int {
    glGetIntegerv(pname, int)
    return int[0]
}

infix fun GL3.glEnable(cap: Int) = glEnable(cap)
infix fun GL3.disable(cap: Int) = glDisable(cap)













