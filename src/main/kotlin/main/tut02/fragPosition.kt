package main.tut02

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.util.glsl.ShaderProgram
import glm.L
import glm.SIZE
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*
import uno.glsl.shaderCodeOf

/**
 * Created by GBarbieri on 21.02.2017.
 */

fun main(args: Array<String>) {
    FragPosition_()
}

class FragPosition_ : Framework("Tutorial 02 - Fragment Position") {

    var theProgram = 0
    val vertexBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)
    val vertexData = floatArrayOf(
            +0.75f, +0.75f, 0.0f, 1.0f,
            +0.75f, -0.75f, 0.0f, 1.0f,
            -0.75f, -0.75f, 0.0f, 1.0f)

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)
        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])
    }

    fun initializeProgram(gl: GL3) {
        theProgram = shaderProgramOf(gl, this::class.java, "tut02", "frag-position.vert", "frag-position.frag")
    }

    fun shaderProgramOf(gl: GL2ES2, context: Class<*>, vararg strings: String): Int {

        val shaders =
                if (strings[0].contains('.'))
                    strings.toList()
                else{
                    val root = if(strings[0].endsWith('/')) strings[0] else strings[0] + '/'
                    strings.drop(1).map { root + it }
                }

        val shaderProgram = ShaderProgram()

        val shaderCodes = shaders.map { shaderCodeOf(it, gl, context) }

        shaderCodes.forEach { shaderProgram.add(gl, it, System.err) }

        shaderProgram.link(gl, System.err)

        shaderCodes.forEach {
            gl.glDetachShader(shaderProgram.program(), it.id())
            gl.glDeleteShader(it.id())
        }

        return shaderProgram.program()
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl){

        val vertexBuffer = vertexData.toFloatBuffer()

        glGenBuffers(1, vertexBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    override fun display(gl: GL3) = with(gl){

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0f, 0f, 0f, 1f))

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4.length, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram(0)
    }

    public override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl){

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, vertexBufferObject)
        glDeleteVertexArrays(1, vao)

        destroyBuffers(vertexBufferObject, vao)
    }

    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}
