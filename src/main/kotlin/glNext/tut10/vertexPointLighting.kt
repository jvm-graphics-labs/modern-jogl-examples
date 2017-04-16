package glNext.tut10

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import glNext.*
import glm.f
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import glm.quat.Quat
import glm.mat.Mat4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.intBufferBig
import uno.glm.MatrixStack
import uno.mousePole.*
import uno.time.Timer
import glm.glm
import uno.buffer.destroy
import uno.glsl.programOf

/**
 * Created by GBarbieri on 23.03.2017.
 */

fun main(args: Array<String>) {
    VertexPointLighting_Next().setup("Tutorial 10 - Vertex Point Lighting")
}

class VertexPointLighting_Next() : Framework() {

    lateinit var whiteDiffuseColor: ProgramData
    lateinit var vertexDiffuseColor: ProgramData
    lateinit var unlit: UnlitProgData

    lateinit var cylinder: Mesh
    lateinit var plane: Mesh
    lateinit var cube: Mesh

    val initialViewData = ViewData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            5.0f,
            0.0f)
    val viewScale = ViewScale(
            3.0f, 20.0f,
            1.5f, 0.5f,
            0.0f, 0.0f, //No camera movement.
            90.0f / 250.0f)
    val initialObjectData = ObjectData(
            Vec3(0.0f, 0.5f, 0.0f),
            Quat(1.0f, 0.0f, 0.0f, 0.0f))

    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)
    val objectPole = ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole)

    val projectionUniformBuffer = intBufferBig(1)

    var drawColoredCyl = false
    var drawLight = false
    var lightHeight = 1.5f
    var lightRadius = 1.0f
    val lightTimer = Timer(Timer.Type.Loop, 5.0f)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        cylinder = Mesh(gl, javaClass, "tut10/UnitCylinder.xml")
        plane = Mesh(gl, javaClass, "tut10/LargePlane.xml")
        cube = Mesh(gl, javaClass, "tut10/UnitCube.xml")

        cullFace {
            enable()
            cullFace = back
            frontFace = cw
        }

        depth {
            test = true
            mask = true
            func = lEqual
            range = 0.0 .. 1.0
            clamp = true
        }

        initUniformBuffer(projectionUniformBuffer) {
            data(Mat4.SIZE, GL.GL_DYNAMIC_DRAW)
            //Bind the static buffers.
            range(Semantic.Uniform.PROJECTION, 0, Mat4.SIZE)
        }
    }

    fun initializePrograms(gl: GL3) {
        whiteDiffuseColor = ProgramData(gl, "tut10", "pos-vertex-lighting-PN.vert", "color-passthrough.frag")
        vertexDiffuseColor = ProgramData(gl, "tut10", "pos-vertex-lighting-PCN.vert", "color-passthrough.frag")
        unlit = UnlitProgData(gl, "tut10", "pos-transform.vert", "uniform-color.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        clear {
            color(0)
            depth()
        }

        lightTimer.update()

        val modelMatrix = MatrixStack()
        modelMatrix.setMatrix(viewPole.calcMatrix())

        val worldLightPosition = calcLightPosition()

        val lightPosCameraSpace = modelMatrix.top() * worldLightPosition

        usingProgram(whiteDiffuseColor.theProgram) {
            glUniform3f(whiteDiffuseColor.lightPosUnif, lightPosCameraSpace)
            name = vertexDiffuseColor.theProgram
            glUniform3f(vertexDiffuseColor.lightPosUnif, lightPosCameraSpace)

            name = whiteDiffuseColor.theProgram
            glUniform4f(whiteDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(whiteDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
            name = vertexDiffuseColor.theProgram
            glUniform4f(vertexDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f)
            glUniform4f(vertexDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f)
        }

        modelMatrix run {

            //Render the ground plane.
            run {
                usingProgram(whiteDiffuseColor.theProgram) {
                    whiteDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                    val normMatrix = top().toMat3()
                    whiteDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normMatrix
                    plane.render(gl)
                }
            }

            //Render the Cylinder
            run {
                applyMatrix(objectPole.calcMatrix())

                usingProgram {
                    if (drawColoredCyl) {
                        name = vertexDiffuseColor.theProgram
                        vertexDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                        val normMatrix = top().toMat3()
                        vertexDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normMatrix
                        cylinder.render(gl, "lit-color")
                    } else {
                        name = whiteDiffuseColor.theProgram
                        whiteDiffuseColor.modelToCameraMatrixUnif.mat4 = top()
                        val normMatrix = top().toMat3()
                        whiteDiffuseColor.normalModelToCameraMatrixUnif.mat3 = normMatrix
                        cylinder.render(gl, "lit")
                    }
                }
            }

            //Render the light
            if (drawLight)

                run {
                    translate(worldLightPosition).scale(0.1f)

                    usingProgram(unlit.theProgram) {
                        unlit.modelToCameraMatrixUnif.mat4 = top()
                        glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f)
                        cube.render(gl, "flat")
                    }
                }
        }
    }

    fun calcLightPosition(): Vec4 {

        val currentTimeThroughLoop = lightTimer.getAlpha()

        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(currentTimeThroughLoop * (glm.PIf * 2.0f)) * lightRadius
        ret.z = glm.sin(currentTimeThroughLoop * (glm.PIf * 2.0f)) * lightRadius

        return ret
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        perspMatrix.perspective(45.0f, w.f / h, zNear, zFar)

        glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer)
        glBufferSubData(GL_UNIFORM_BUFFER, perspMatrix.top())
        glBindBuffer(GL_UNIFORM_BUFFER)

        glViewport(w, h)
    }

    override fun mousePressed(e: MouseEvent) {
        viewPole.mousePressed(e)
        objectPole.mousePressed(e)
    }

    override fun mouseDragged(e: MouseEvent) {
        viewPole.mouseDragged(e)
        objectPole.mouseDragged(e)
    }

    override fun mouseReleased(e: MouseEvent) {
        viewPole.mouseReleased(e)
        objectPole.mouseReleased(e)
    }

    override fun mouseWheelMoved(e: MouseEvent) {
        viewPole.mouseWheel(e)
    }

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_SPACE -> drawColoredCyl = !drawColoredCyl

            KeyEvent.VK_I -> lightHeight += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_K -> lightHeight -= if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_L -> lightRadius += if (e.isShiftDown) 0.05f else 0.2f
            KeyEvent.VK_J -> lightRadius -= if (e.isShiftDown) 0.05f else 0.2f

            KeyEvent.VK_Y -> drawLight = !drawLight

            KeyEvent.VK_B -> lightTimer.togglePause()
        }
        if (lightRadius < 0.2f)
            lightRadius = 0.2f
    }

    override fun end(gl: GL3) = with(gl) {

        glDeletePrograms(vertexDiffuseColor.theProgram, whiteDiffuseColor.theProgram, unlit.theProgram)

        glDeleteBuffer(projectionUniformBuffer)

        cylinder.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        projectionUniformBuffer.destroy()
    }

    inner class ProgramData(gl: GL3, root: String, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, root, vertex, fragment)

        val lightPosUnif = gl.glGetUniformLocation(theProgram, "lightPos")
        val lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity")
        val ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }

    inner class UnlitProgData(gl: GL3, root: String, vertex: String, fragment: String) {

        val theProgram = programOf(gl, javaClass, root, vertex, fragment)

        val objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor")

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")

        init {
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION)
        }
    }
}