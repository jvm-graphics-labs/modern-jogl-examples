/**
 * Created by GBarbieri on 10.04.2017.
 */

package glNext

import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL3
import glm.mat.Mat3
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

inline fun GL3.usingProgram(program: Int = 0, block: ProgramA.(GL3) -> Unit) {
    ProgramA.gl = this
    ProgramA.name = program //glUse
    ProgramA.block(this)
    glUseProgram(0)
}

inline fun GL3.withProgram(program: Int = 0, block: ProgramB.() -> Unit) {
    ProgramB.gl = this
    ProgramB.name = program
    ProgramB.block()
}

object ProgramA {

    lateinit var gl: GL3
    var name = 0
        set(value) {
            gl.glUseProgram(value)
            field = value
        }

    val String.location: Int
        get() = gl.glGetUniformLocation(name, this)

    var Int.int: Int
        get() = 0
        set(value) = gl.glUniform1i(this, value)
    var Int.float: Float
        get() = 0f
        set(value) = gl.glUniform1f(this, value)
    var Int.vec2: Vec2
        get() = Vec2()
        set(value) = gl.glUniform2f(this, value.x, value.y)
    var Int.vec3: Vec3
        get() = Vec3()
        set(value) = gl.glUniform3f(this, value.x, value.y, value.z)
    var Int.vec4: Vec4
        get() = Vec4()
        set(value) = gl.glUniform4f(this, value.x, value.y, value.z, value.w)
    var Int.mat4: Mat4
        get() = Mat4()
        set(value) = gl.glUniformMatrix4fv(this, 1, false, value to matBuffer)
    var Int.mat3: Mat3
        get() = Mat3()
        set(value) = gl.glUniformMatrix3fv(this, 1, false, value to matBuffer)


    fun GL3.link() = glLinkProgram(name)

    infix fun Vec4.to(location: Int) = gl.glUniform4fv(location, 1, this to matBuffer)
    infix fun Mat4.to(location: Int) = gl.glUniformMatrix4fv(location, 1, false, this to matBuffer)
}

object ProgramB {

    lateinit var gl: GL3
    var name = 0

    val String.location
        get() = gl.glGetUniformLocation(name, this)
    val String.blockIndex
        get() = gl.glGetUniformBlockIndex(name, this)

    inline fun use(block: ProgramA.(GL3) -> Unit) {
        ProgramA.gl = gl
        ProgramA.name = name
        ProgramA.block(gl)
        gl.glUseProgram(0)
    }

    infix fun Int.blockBinding(uniformBlockBinding: Int) = gl.glUniformBlockBinding(name, this, uniformBlockBinding)
}


fun GL3.glUseProgram() = glUseProgram(0)


fun GL3.glUniform2f(location: Int) = glUniform2f(location, 0f, 0f)
fun GL3.glUniform2f(location: Int, f: Float) = glUniform2f(location, f, f)
// TODO vec1
fun GL3.glUniform2f(location: Int, vec2: Vec2) = glUniform2f(location, vec2.x, vec2.y)

fun GL3.glUniform2f(location: Int, vec3: Vec3) = glUniform2f(location, vec3.x, vec3.y)
fun GL3.glUniform2f(location: Int, vec4: Vec4) = glUniform2f(location, vec4.x, vec4.y)

fun GL3.glUniform3f(location: Int) = glUniform3f(location, 0f, 0f, 0f)
fun GL3.glUniform3f(location: Int, f: Float) = glUniform3f(location, f, f, f)
fun GL3.glUniform3f(location: Int, vec2: Vec2) = glUniform3f(location, vec2.x, vec2.y, 0f)
fun GL3.glUniform3f(location: Int, vec3: Vec3) = glUniform3f(location, vec3.x, vec3.y, vec3.z)
fun GL3.glUniform3f(location: Int, vec4: Vec4) = glUniform3f(location, vec4.x, vec4.y, vec4.z)

fun GL3.glUniform4f(location: Int) = glUniform4f(location, 0f, 0f, 0f, 1f)
fun GL3.glUniform4f(location: Int, f: Float) = glUniform4f(location, f, f, f, f)
fun GL3.glUniform4f(location: Int, vec2: Vec2) = glUniform4f(location, vec2.x, vec2.y, 0f, 1f)
fun GL3.glUniform4f(location: Int, vec3: Vec3) = glUniform4f(location, vec3.x, vec3.y, vec3.z, 1f)
fun GL3.glUniform4f(location: Int, vec4: Vec4) = glUniform4f(location, vec4.x, vec4.y, vec4.z, vec4.w)

fun GL3.glUniformMatrix4f(location: Int, value: FloatArray) {
    for (i in 0..15)
        matBuffer[i] = value[i]
    glUniformMatrix4fv(location, 1, false, matBuffer)
}

fun GL3.glUniformMatrix4f(location: Int, value: Mat4) = glUniformMatrix4fv(location, 1, false, value to matBuffer)
fun GL3.glUniformMatrix3f(location: Int, value: Mat3) = glUniformMatrix3fv(location, 1, false, value to matBuffer)
fun GL3.glUniformMatrix3f(location: Int, value: Mat4) {
    matBuffer[0] = value[0][0]
    matBuffer[1] = value[0][1]
    matBuffer[2] = value[0][2]
    matBuffer[3] = value[1][0]
    matBuffer[4] = value[1][1]
    matBuffer[5] = value[1][2]
    matBuffer[6] = value[2][0]
    matBuffer[7] = value[2][1]
    matBuffer[8] = value[2][2]
    glUniformMatrix3fv(location, 1, false, value to matBuffer)
}


fun GL3.glDeletePrograms(vararg programs: Int) = programs.forEach { glDeleteProgram(it) }