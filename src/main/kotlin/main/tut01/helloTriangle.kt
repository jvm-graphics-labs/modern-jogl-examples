/**
 * Created by elect on 21/02/17.
 */

package main.tut01


import buffer.destroy
import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES2.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3ES3.GL_GEOMETRY_SHADER
import extensions.byteBufferOf
import extensions.intBufferBig
import extensions.intBufferOf
import extensions.toFloatBuffer
import main.L
import main.SIZE
import main.framework.Framework
import main.framework.Semantic
import vec._4.Vec4


fun main(args: Array<String>) {
    HelloTriangle_()
}

class HelloTriangle_ : Framework("Tutorial 01 - Hello Triangle") {

    val strVertexShader =
            "#version 330\n" +
                    "#define POSITION 0\n" +
                    "layout(location = POSITION) in vec4 position;\n" +
                    "void main()\n" +
                    "{\n" +
                    "   gl_Position = position;\n" +
                    "}\n"

    val strFragmentShader =
            "#version 330\n" +
                    "out vec4 outputColor;\n" +
                    "void main()\n" +
                    "{\n" +
                    "   outputColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);\n" +
                    "}\n"

    val vertexPositions = floatArrayOf(
            +0.75f, +0.75f, 0.0f, 1.0f,
            +0.75f, -0.75f, 0.0f, 1.0f,
            -0.75f, -0.75f, 0.0f, 1.0f)

    var theProgram = 0
    val positionBufferObject = intBufferBig(1)
    val vao = intBufferBig(1)


    /**
     * Called after the window and OpenGL are initialized. Called exactly once, before the main loop.
     * @param gl
     */
    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        initializeVertexBuffer(gl)

        glGenVertexArrays(1, vao)
        glBindVertexArray(vao[0])
    }

    fun initializeProgram(gl: GL3) {

        val shaderList = listOf(createShader(gl, GL_VERTEX_SHADER, strVertexShader), createShader(gl, GL_FRAGMENT_SHADER, strFragmentShader))

        theProgram = createProgram(gl, shaderList)

        shaderList.forEach(gl::glDeleteShader)
    }

    fun createShader(gl: GL3, shaderType: Int, shaderFile: String): Int = with(gl) {

        val shader = glCreateShader(shaderType)
        val lines = arrayOf(shaderFile)
        val length = intBufferOf(lines[0].length)
        glShaderSource(shader, 1, lines, length)

        glCompileShader(shader)

        val status = intBufferBig(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, status)
        if (status[0] == GL_FALSE) {

            val infoLogLength = intBufferBig(1)
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, infoLogLength)

            val bufferInfoLog = byteBufferOf(infoLogLength[0])
            glGetShaderInfoLog(shader, infoLogLength[0], null, bufferInfoLog)
            val bytes = ByteArray(infoLogLength[0])
            bufferInfoLog.get(bytes)

            var strShaderType = ""
            when (shaderType) {
                GL_VERTEX_SHADER -> strShaderType = "vertex"
                GL_GEOMETRY_SHADER -> strShaderType = "geometry"
                GL_FRAGMENT_SHADER -> strShaderType = "fragment"
            }
            System.err.println("Compiler failure in $strShaderType shader: ${bytes.toString()}")

            infoLogLength.destroy()
            bufferInfoLog.destroy()
        }
        length.destroy()
        status.destroy()

        return shader
    }

    fun createProgram(gl: GL3, shaderList: List<Int>): Int = with(gl){

        val program = glCreateProgram()

        shaderList.forEach { glAttachShader(program, it) }

        glLinkProgram(program)

        val status = intBufferBig(1)
        glGetProgramiv(program, GL_LINK_STATUS, status)
        if (status[0] == GL_FALSE) {

            val infoLogLength = intBufferBig(1)
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, infoLogLength)

            val bufferInfoLog = byteBufferOf(infoLogLength[0])
            glGetProgramInfoLog(program, infoLogLength[0], null, bufferInfoLog)
            val bytes = ByteArray(infoLogLength[0])
            bufferInfoLog.get(bytes)

            System.err.println("Linker failure: " + bytes.toString())

            infoLogLength.destroy()
            bufferInfoLog.destroy()
        }

        shaderList.forEach { glDetachShader(program, it) }

        status.destroy()

        return program
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        val vertexBuffer = vertexPositions.toFloatBuffer()

        glGenBuffers(1, positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        vertexBuffer.destroy()
    }

    /**
     * Called to update the display. You don't need to swap the buffers after all of your rendering to display what you rendered,
     * it is done automatically.
     * @param gl
     */
    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 1f))

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject[0])
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, 4, GL_FLOAT, false, Vec4.SIZE, 0)

        glDrawArrays(GL_TRIANGLES, 0, 3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram(0)
    }

    /**
     * Called whenever the window is resized. The new window size is given, in pixels. This is an opportunity to call glViewport or
     * glScissor to keep up with the change in size.
     * @param gl
     * @param w
     * @param h
     */
    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
    }

    /**
     * Called at the end, here you want to clean all the resources.
     * @param gl
     */
    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffers(1, positionBufferObject)
        glDeleteVertexArrays(1, vao)

        positionBufferObject.destroy()
        vao.destroy()
    }

    /**
     * Called whenever a key on the keyboard was pressed. The key is given by the KeyCode(). It's often a good idea to have the escape
     * key to exit the program.
     * @param keyEvent
     */
    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
    }
}