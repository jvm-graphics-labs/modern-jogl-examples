/**
 * Created by GBarbieri on 10.04.2017.
 */


package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL3
import glm.L
import glm.vec._3.Vec3
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


inline fun GL3.withVertexArray(vertexArray: IntBuffer, block: VertexArray.() -> Unit) {
    glBindVertexArray(vertexArray[0])
    VertexArray.block()
    glBindVertexArray(0)
}

object VertexArray {

    fun GL3.array(array: Int, format: VertexLayout) {
        glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, array)
        for (attr in format.attribute) {
            glEnableVertexAttribArray(attr.index)
            glVertexAttribPointer(attr.index, attr.size, attr.type, attr.normalized, attr.interleavedStride, attr.pointer)
        }
        glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
    }

    fun GL3.array(array: Int, format: VertexLayout, vararg offset: Int) {
        glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, array)
        for (i in format.attribute.indices) {
            val attr = format.attribute[i]
            glEnableVertexAttribArray(attr.index)
            glVertexAttribPointer(attr.index, attr.size, attr.type, attr.normalized, 0, offset[i].L)
        }
        glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
    }

    fun GL3.element(element: Int) = glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, element)
    fun GL3.element(element: IntBuffer) = glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, element[0])
}




inline fun GL3.withVertexLayout(buffer: IntBuffer, format: VertexLayout, block: () -> Unit) {
    glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, buffer[0])
    for (attr in format.attribute) {
        glEnableVertexAttribArray(attr.index)
        glVertexAttribPointer(attr.index, attr.size, attr.type, attr.normalized, attr.interleavedStride, attr.pointer)
    }
    glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
    block()
    for (attr in format.attribute)
        glDisableVertexAttribArray(attr.index)
}


/** For un-interleaved, that is not-interleaved */
inline fun GL3.withVertexLayout(buffer: IntBuffer, format: VertexLayout, vararg offset: Int, block: () -> Unit) {
    glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, buffer[0])
    for (i in format.attribute.indices) {
        val attr = format.attribute[i]
        glEnableVertexAttribArray(attr.index)
        glVertexAttribPointer(attr.index, attr.size, attr.type, attr.normalized, 0, offset[i].L)
    }
    glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, 0)
    block()
    for (attr in format.attribute)
        glDisableVertexAttribArray(attr.index)
}


fun GL3.glEnableVertexAttribArray(layout: VertexLayout) = glEnableVertexAttribArray(layout[0].index)
fun GL3.glEnableVertexAttribArray(attribute: VertexAttribute) = glEnableVertexAttribArray(attribute.index)

fun GL3.glDisableVertexAttribArray(layout: VertexLayout) = glDisableVertexAttribArray(layout[0].index)
fun GL3.glDisableVertexAttribArray(attribute: VertexAttribute) = glDisableVertexAttribArray(attribute.index)

fun GL3.glVertexAttribPointer(layout: VertexLayout) = glVertexAttribPointer(layout[0])
fun GL3.glVertexAttribPointer(attribute: VertexAttribute) =
        glVertexAttribPointer(
                attribute.index,
                attribute.size,
                attribute.type,
                attribute.normalized,
                attribute.interleavedStride,
                attribute.pointer)

fun GL3.glVertexAttribPointer(layout: VertexLayout, offset: Int) = glVertexAttribPointer(layout[0], offset)
fun GL3.glVertexAttribPointer(attribute: VertexAttribute, offset: Int) =
        glVertexAttribPointer(
                attribute.index,
                attribute.size,
                attribute.type,
                attribute.normalized,
                0, // tightly packed
                offset.L)

fun GL3.glVertexAttribPointer(index: Int, kClass: KClass<*>, offset: Int = 0) = when (kClass) {
    Vec4::class -> glVertexAttribPointer(index, Vec4.length, GL.GL_FLOAT, false, Vec4.SIZE, offset.L)
    Vec3::class -> glVertexAttribPointer(index, Vec3.length, GL.GL_FLOAT, false, Vec3.SIZE, offset.L)
    else -> throw Error()
}