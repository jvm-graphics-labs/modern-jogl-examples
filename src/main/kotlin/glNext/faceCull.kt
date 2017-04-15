package glNext

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL3

/**
 * Created by elect on 15/04/17.
 */


fun GL3.faceCull(block: ObjectCullFace.() -> Unit) {
    ObjectCullFace.gl = this
    ObjectCullFace.block()
}

object ObjectCullFace {

    lateinit var gl: GL3

    val front = CullFace.front
    val back = CullFace.back
    val frontAndBack = CullFace.frontAndBack

    val cw = FrontFace.cw
    val ccw = FrontFace.ccw

    fun enable() = gl.glEnable(GL.GL_CULL_FACE)
    fun disable() = gl.glDisable(GL.GL_CULL_FACE)
    var cullFace = CullFace.back
        set(value) {
            gl.glCullFace(value.i)
            field = value
        }

    var frontFace = FrontFace.ccw
        set(value) {
            gl.glFrontFace(value.i)
            field = value
        }

    enum class CullFace(val i: Int) {front(GL.GL_FRONT), back(GL.GL_BACK), frontAndBack(GL.GL_FRONT_AND_BACK) }
    enum class FrontFace(val i: Int) {cw(GL.GL_CW), ccw(GL.GL_CCW) }
}

fun GL3.faceCull(enable: Boolean = false, cullFace: Int = GL.GL_BACK, frontFace: Int = GL.GL_CCW) {
    if (enable)
        glEnable(GL.GL_CULL_FACE)
    else
        glDisable(GL.GL_CULL_FACE)
    glCullFace(cullFace)
    glFrontFace(frontFace)
}