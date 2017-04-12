package main.tut14

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import glNext.*
import main.framework.Framework
import main.framework.component.Mesh
import uno.buffer.put
import uno.gl.gl3
import uno.glm.MatrixStack
import uno.glsl.programOf

/**
 * Created by elect on 28/03/17.
 */

fun main(args: Array<String>) {
    PerspectiveInterpolation_().setup("Tutorial 14 - Perspective Interpolation")
}

class PerspectiveInterpolation_() : Framework() {

    lateinit var smoothInterp: ProgramData
    lateinit var linearInterp: ProgramData

    lateinit var realHallway: Mesh
    lateinit var fauxHallway: Mesh

    var useFakeHallway = false
    var useSmoothInterpolation = true

    override fun init(gl: GL3) {

        initializePrograms(gl)

        realHallway = Mesh(gl, javaClass, "tut14/RealHallway.xml")
        fauxHallway = Mesh(gl, javaClass, "tut14/FauxHallway.xml")
    }

    fun initializePrograms(gl: GL3) = with(gl) {

        smoothInterp = ProgramData(gl, "smooth-vertex-colors")
        linearInterp = ProgramData(gl, "no-correct-vertex-colors")

        val zNear = 1.0f
        val zFar = 1_000f
        val persMatrix = MatrixStack()
        persMatrix.perspective(60.0f, 1.0f, zNear, zFar)

        glUseProgram(smoothInterp.theProgram)
        glUniformMatrix4f(smoothInterp.cameraToClipMatrixUnif, persMatrix.top())
        glUseProgram(linearInterp.theProgram)
        glUniformMatrix4f(linearInterp.cameraToClipMatrixUnif, persMatrix.top())
        glUseProgram()
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferf(GL_COLOR, 0.0f)
        glClearBufferf(GL_DEPTH)

        if (useSmoothInterpolation)
            glUseProgram(smoothInterp.theProgram)
        else
            glUseProgram(linearInterp.theProgram)

        if (useFakeHallway)
            fauxHallway.render(gl)
        else
            realHallway.render(gl)

        glUseProgram(0)
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {
        glViewport(w, h)
    }

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_S -> {
                useFakeHallway = !useFakeHallway
                println(if (useFakeHallway) "Fake Hallway." else "Real Hallway.")
            }
            KeyEvent.VK_P -> {
                useSmoothInterpolation = !useSmoothInterpolation
                println(if (useSmoothInterpolation) "Perspective correct interpolation." else "Just linear interpolation")
            }
            KeyEvent.VK_SPACE -> window.gl3 {

                realHallway.dispose(this)
                fauxHallway.dispose(this)

                realHallway = Mesh(this, javaClass, "tut14/RealHallway.xml")
                fauxHallway = Mesh(this, javaClass, "tut14/FauxHallway.xml")
            }
        }
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(smoothInterp.theProgram, linearInterp.theProgram)

        fauxHallway.dispose(gl)
        realHallway.dispose(gl)
    }

    class ProgramData(gl: GL3, shader: String) {
        val theProgram = programOf(gl, PerspectiveInterpolation_::class.java, "tut14", shader + ".vert", shader + ".frag")
        val cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix")
    }
}