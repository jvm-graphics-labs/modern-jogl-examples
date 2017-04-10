/**
 * Created by GBarbieri on 10.04.2017.
 */

package glNext

import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL3
import glm.mat.Mat4
import glm.set
import glm.vec._2.Vec2
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import uno.buffer.byteBufferOf
import uno.buffer.destroy


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

object Program {

    var name = 0
    lateinit var gl: GL3
    //
//    infix fun String.to(location: Int) = GL20.glBindAttribLocation(name, location, this)
    val String.location
        get() = gl.glGetUniformLocation(name, this)
    var String.uniform1f: Float
        get() = 0f
        set(value) = gl.glUniform1f(gl.glGetUniformLocation(name, this), value)
    var Int.float: Float
        get() = 0f
        set(value) = gl.glUniform1f(this, value)
    var Int.vec2: Vec2
        get() = Vec2()
        set(value) = gl.glUniform2f(this, value.x, value.y)
    var Int.mat4: Mat4
        get() = Mat4()
        set(value) = gl.glUniformMatrix4fv(this, 1, false, value to matBuffer)


    fun GL3.link() = glLinkProgram(name)

    infix fun Vec4.to(location: Int) = gl.glUniform4fv(location, 1, this to matBuffer)
    infix fun Mat4.to(location: Int) = gl.glUniformMatrix4fv(location, 1, false, this to matBuffer)
}


fun GL3.glUseProgram() = glUseProgram(0)
fun GL3.glUniform2f(location: Int) = glUniform2f(location, 0f, 0f)
fun GL3.glUniform2f(location: Int, f: Float) = glUniform2f(location, f, f)
fun GL3.glUniform2f(location: Int, vec2: Vec2) = glUniform2f(location, vec2.x, vec2.y)
fun GL3.glUniform3f(location: Int) = glUniform3f(location, 0f, 0f, 0f)
fun GL3.glUniform3f(location: Int, f: Float) = glUniform3f(location, f, f, f)
fun GL3.glUniform3f(location: Int, vec3: Vec3) = glUniform3f(location, vec3.x, vec3.y, vec3.z)
fun GL3.glUniformMatrix4(location: Int, value: FloatArray) {
    for (i in 0..15)
        matBuffer[i] = value[i]
    glUniformMatrix4fv(location, 1, false, matBuffer)
}

fun GL3.glUniformMatrix4(location: Int, value: Mat4) = glUniformMatrix4fv(location, 1, false, value to matBuffer)

inline fun GL3.usingProgram(program: Int, block: Program.(GL3) -> Unit) {
    Program.gl = this
    Program.name = program
    glUseProgram(program)
    Program.block(this)
    glUseProgram(0)
}

inline fun withProgram(program: Int, block: Program.() -> Unit) {
    Program.name = program
    return Program.block()
}