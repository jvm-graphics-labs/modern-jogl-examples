package main.framework

import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GLAutoDrawable

/**
 * Created by GBarbieri on 22.03.2017.
 */

inline infix fun GLWindow.gl3(crossinline inject: GL3.() -> Unit) {
    invoke(false) { glAutoDrawable ->
        glAutoDrawable.gl.gL3.inject()
        false
    }
}