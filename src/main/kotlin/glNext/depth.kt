package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL3
import glm.d

/**
 * Created by elect on 15/04/17.
 */


fun GL3.depth(block: ObjectDepth.() -> Unit) {
    ObjectDepth.gl = this
    ObjectDepth.block()
}

fun GL3.depth(enable: Boolean = false, mask: Boolean = true, func: Int = GL.GL_LESS, rangeNear: Float = 0f,
              rangeFar: Float = 1f, clamp: Boolean = false) = depth(enable, mask, func, rangeNear.d, rangeFar.d, clamp)

fun GL3.depth(enable: Boolean = false, mask: Boolean = true, func: Int = GL.GL_LESS, rangeNear: Double = 0.0,
              rangeFar: Double = 1.0, clamp: Boolean = false) {
    if (enable)
        glEnable(GL.GL_DEPTH_TEST)
    else
        glDisable(GL.GL_DEPTH_TEST)
    glDepthMask(mask)
    glDepthFunc(func)
    glDepthRange(rangeNear, rangeFar)
    if (clamp)
        glEnable(GL3.GL_DEPTH_CLAMP)
    else
        glDisable(GL3.GL_DEPTH_CLAMP)
}

object ObjectDepth {

    lateinit var gl: GL3

    val never = Func.never
    val less = Func.less
    val equal = Func.equal
    val lEqual = Func.lEqual
    val greater = Func.greater
    val notEqual = Func.notEqual
    val gEqual = Func.gEqual
    val always = Func.always

    var test = false
        set(value) {
            if (value)
                gl.glEnable(GL.GL_DEPTH_TEST)
            else
                gl.glDisable(GL.GL_DEPTH_TEST)
        }
    var mask = true
        set(value) {
            gl.glDepthMask(value)
            field = value
        }
    var func = Func.less
        set(value) {
            gl.glDepthFunc(func.i)
            field = value
        }
    var range = 0.0 .. 1.0
        set(value) {
            gl.glDepthRange(value.start, value.endInclusive)
            field = value
        }
    var rangef = 0f .. 1f
        set(value) {
            gl.glDepthRangef(value.start, value.endInclusive)
            field = value
        }
    var clamp = false
        set(value) {
            if (value)
                gl.glEnable(GL3.GL_DEPTH_CLAMP)
            else
                gl.glDisable(GL3.GL_DEPTH_CLAMP)
        }

    enum class Func(val i: Int) { never(GL.GL_NEVER), less(GL.GL_LESS), equal(GL.GL_EQUAL), lEqual(GL.GL_LEQUAL),
        greater(GL.GL_GREATER), notEqual(GL.GL_NOTEQUAL), gEqual(GL.GL_GEQUAL), always(GL.GL_ALWAYS)
    }
}