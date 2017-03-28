package main.tut14

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
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
    PerspectiveInterpolation_()
}

class PerspectiveInterpolation_() : Framework("Tutorial 14 - Perspective Interpolation") {

    lateinit var smoothInterp: ProgramData
    lateinit var linearInterp: ProgramData

    lateinit var realHallway: Mesh
    lateinit var fauxHallway: Mesh

    var useFakeHallway = false
    var useSmoothInterpolation = true

    override fun init(gl: GL3) {

        initializePrograms(gl)

        realHallway = Mesh(gl, this::class.java, "tut14/RealHallway.xml")
        fauxHallway = Mesh(gl, this::class.java, "tut14/FauxHallway.xml")
    }

    fun initializePrograms(gl: GL3) = with(gl) {

        smoothInterp = ProgramData(gl, "smooth-vertex-colors")
        linearInterp = ProgramData(gl, "no-correct-vertex-colors")

        val zNear = 1.0f
        val zFar = 1_000f
        val persMatrix = MatrixStack()
        persMatrix.perspective(60.0f, 1.0f, zNear, zFar)

        glUseProgram(smoothInterp.theProgram)
        glUniformMatrix4fv(smoothInterp.cameraToClipMatrixUnif, 1, false, persMatrix.top() to matBuffer)
        glUseProgram(linearInterp.theProgram)
        glUniformMatrix4fv(linearInterp.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(0)
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.0f, 0.0f, 0.0f, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

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

    override fun reshape(gl: GL3, w: Int, h: Int) {
        gl.glViewport(0, 0, w, h)
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

                realHallway = Mesh(this, this::class.java, "tut14/RealHallway.xml")
                fauxHallway = Mesh(this, this::class.java, "tut14/FauxHallway.xml")
            }
        }
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(smoothInterp.theProgram)
        glDeleteProgram(linearInterp.theProgram)

        fauxHallway.dispose(gl)
        realHallway.dispose(gl)
    }

    class ProgramData(gl: GL3, shader: String,
                      val theProgram: Int = programOf(gl, PerspectiveInterpolation_::class.java, "tut14", shader + ".vert", shader + ".frag"),
                      val cameraToClipMatrixUnif: Int = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix"))
}