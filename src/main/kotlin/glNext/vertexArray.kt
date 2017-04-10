/**
 * Created by GBarbieri on 10.04.2017.
 */


package glNext

import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL3
import glm.L
import glm.vec._4.Vec4
import java.nio.IntBuffer
import kotlin.reflect.KClass


infix fun GL3.glGenVertexArray(vertexArray: IntBuffer) = glGenVertexArrays(1, vertexArray)
infix fun GL3.glGenVertexArrays(vertexArrays: IntBuffer) = glGenVertexArrays(vertexArrays.capacity(), vertexArrays)

fun GL3.glGenVertexArray(): Int {
    glGenVertexArrays(1, int)
    return int[0]
}


infix fun GL3.glBindVertexArray(vertexArray: IntBuffer) = glBindVertexArray(vertexArray[0])
fun GL3.glBindVertexArray() = glBindVertexArray(0)
infix fun GL3.glBindVertexArray(vertexArray: Int) = glBindVertexArray(vertexArray)


infix fun GL3.glDeleteVertexArray(vertexArray: IntBuffer) = glDeleteVertexArrays(vertexArray.capacity(), vertexArray)
infix fun GL3.glDeleteVertexArrays(vertexArrays: IntBuffer) = glDeleteVertexArrays(vertexArrays.capacity(), vertexArrays)
infix fun GL3.glDeleteVertexArray(vertexArray: Int) = glDeleteVertexArrays(1, int.put(0, vertexArray))


inline fun GL3.withVertexLayout(buffer: IntBuffer, layout: KClass<*>, vararg params: Pair<Int, Int>, block: () -> Unit) =
        withVertexLayout(buffer[0], layout, *params, block = block)

inline fun GL3.withVertexLayout(buffer: Int, layout: KClass<*>, vararg params: Pair<Int, Int>, block: () -> Unit) {

    when (layout) {
        Vec4::class -> {
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, buffer)
            for (p in params) {
                glEnableVertexAttribArray(p.first)
                glVertexAttribPointer(p.first, Vec4.length, GL2ES2.GL_FLOAT, false, Vec4.SIZE, p.second.L)
            }
            glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
            block()
            for (p in params)
                glDisableVertexAttribArray(p.first)
        }
    }
}

inline fun GL3.withVertexLayout(buffer: IntBuffer, layout: KClass<*>, index: Int, block: () -> Unit) =
        withVertexLayout(buffer[0], layout, index, block)

inline fun GL3.withVertexLayout(buffer: Int, layout: KClass<*>, index: Int, block: () -> Unit) {

    when (layout) {
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
