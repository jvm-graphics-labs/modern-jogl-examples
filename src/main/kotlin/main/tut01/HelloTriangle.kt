/**
 * Created by elect on 21/02/17.
 */

package main.tut01


import com.jogamp.opengl.util.GLBuffers
import main.framework.Framework


fun main(args: Array<String>) {
    HelloTriangle_()
}

val SHADERS_ROOT = "tut01"
val VERT_SHADER_SOURCE = "vertex-shader"
val FRAG_SHADER_SOURCE = "fragment-shader"

class HelloTriangle_ : Framework("Tutorial 01 - Hello Triangle") {

    var theProgram = 0
    val positionBufferObject = GLBuffers.newDirectIntBuffer(1)
    val vao = GLBuffers.newDirectIntBuffer(1)
    val vertexPositions = floatArrayOf(
            +0.75f, +0.75f, 0.0f, 1.0f,
            +0.75f, -0.75f, 0.0f, 1.0f,
            -0.75f, -0.75f, 0.0f, 1.0f)
}