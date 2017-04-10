/**
 * Created by elect on 21/02/17.
 */

package main.tut01


import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES2.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3ES3.GL_GEOMETRY_SHADER
import glNext.*
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import uno.buffer.*


fun main(args: Array<String>) {
    HelloTriangle_().setup("Tutorial 01 - Hello Triangle")
}

class HelloTriangle_ : Framework() {

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

    val vertexPositions = floatBufferOf(
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

        glGenVertexArray(vao)
        glBindVertexArray(vao)
    }

    fun initializeProgram(gl: GL3) {

        val shaderList = listOf(createShader(gl, GL_VERTEX_SHADER, strVertexShader), createShader(gl, GL_FRAGMENT_SHADER, strFragmentShader))

        theProgram = createProgram(gl, shaderList)

        shaderList.forEach(gl::glDeleteShader)
    }

    fun createShader(gl: GL3, shaderType: Int, shaderFile: String): Int = with(gl) {

        val shader = glCreateShader(shaderType)
        glShaderSource(shader, shaderFile)

        glCompileShader(shader)

        val status = glGetShader(shader, GL_COMPILE_STATUS)
        if (status == GL_FALSE) {

            val strInfoLog = glGetShaderInfoLog(shader)

            var strShaderType = ""
            when (shaderType) {
                GL_VERTEX_SHADER -> strShaderType = "vertex"
                GL_GEOMETRY_SHADER -> strShaderType = "geometry"
                GL_FRAGMENT_SHADER -> strShaderType = "fragment"
            }
            System.err.println("Compiler failure in $strShaderType shader: $strInfoLog")
        }

        return shader
    }

    fun createProgram(gl: GL3, shaderList: List<Int>): Int = with(gl) {

        val program = glCreateProgram()

        shaderList.forEach { glAttachShader(program, it) }

        glLinkProgram(program)

        val status = glGetProgram(program, GL_LINK_STATUS)
        if (status == GL_FALSE) {

            val strInfoLog = glGetProgramInfoLog(program)

            System.err.println("Linker failure: $strInfoLog")
        }

        shaderList.forEach { glDetachShader(program, it) }

        return program
    }

    fun initializeVertexBuffer(gl: GL3) = with(gl) {

        glGenBuffer(positionBufferObject)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glBufferData(GL_ARRAY_BUFFER, vertexPositions, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER)
    }

    /**
     * Called to update the display. You don't need to swap the buffers after all of your rendering to display what you rendered,
     * it is done automatically.
     * @param gl
     */
    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR)

        glUseProgram(theProgram)

        glBindBuffer(GL_ARRAY_BUFFER, positionBufferObject)
        glEnableVertexAttribArray(Semantic.Attr.POSITION)
        glVertexAttribPointer(Semantic.Attr.POSITION, Vec4::class)

        glDrawArrays(3)

        glDisableVertexAttribArray(Semantic.Attr.POSITION)
        glUseProgram()
    }

    /**
     * Called whenever the window is resized. The new window size is given, in pixels. This is an opportunity to call glViewport or
     * glScissor to keep up with the change in size.
     * @param gl
     * @param w
     * @param h
     */
    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl){
        glViewport(w, h)
    }

    /**
     * Called at the end, here you want to clean all the resources.
     * @param gl
     */
    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(theProgram)
        glDeleteBuffer(positionBufferObject)
        glDeleteVertexArray(vao)

        destroyBuffers(positionBufferObject, vao, vertexPositions)
    }

    /**
     * Called whenever a key on the keyboard was pressed. The key is given by the KeyCode(). It's often a good idea to have the escape
     * key to exit the program.
     * @param keyEvent
     */
    override fun keyPressed(keyEvent: KeyEvent) {

        when (keyEvent.keyCode) {
            KeyEvent.VK_ESCAPE -> quit()
        }
    }
}