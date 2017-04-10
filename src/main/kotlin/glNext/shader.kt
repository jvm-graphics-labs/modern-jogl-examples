package glNext

import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL3
import glm.set
import uno.buffer.byteBufferOf
import uno.buffer.destroy

/**
 * Created by GBarbieri on 10.04.2017.
 */

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