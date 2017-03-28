package main.tut13

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import com.jogamp.opengl.GL3.GL_PROGRAM_POINT_SIZE
import com.jogamp.opengl.util.GLBuffers
import glm.*
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._3.Vec3
import glm.vec._4.Vec4
import main.framework.Framework
import main.framework.Semantic
import main.framework.component.Mesh
import uno.buffer.byteBufferBig
import uno.buffer.destroyBuffers
import uno.buffer.intBufferBig
import uno.buffer.put
import uno.glm.MatrixStack
import uno.glsl.programOf
import uno.mousePole.ViewData
import uno.mousePole.ViewPole
import uno.mousePole.ViewScale
import uno.time.Timer
import java.nio.ByteBuffer

/**
 * Created by elect on 26/03/17.
 */

fun main(args: Array<String>) {
    GeomImpostor_()
}

private val NUMBER_OF_LIGHTS = 2
private val NUMBER_OF_SPHERES = 4

class GeomImpostor_() : Framework("Tutorial 13 - Geometry Impostor") {

    lateinit var litMeshProg: ProgramMeshData
    lateinit var litImpProg: ProgramImposData
    lateinit var unlit: UnlitProgData

    val initialViewData = ViewData(
            Vec3(0.0f, 30.0f, 25.0f),
            Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            10.0f,
            0.0f)
    val viewScale = ViewScale(
            3.0f, 70.0f,
            3.5f, 1.5f,
            5.0f, 1.0f,
            90.0f / 250.0f)
    val viewPole = ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1)

    lateinit var sphere: Mesh
    lateinit var plane: Mesh
    lateinit var cube: Mesh

    object Buffer {
        val PROJECTION = 0
        val LIGHT = 1
        val MATERIAL_ARRAY = 2
        val MATERIAL_TERRAIN = 3
        val IMPOSTER = 4
        val MAX = 5
    }

    val bufferName = intBufferBig(Buffer.MAX)
    val imposterVAO = intBufferBig(1)

    var drawCameraPos = false
    var drawLights = true

    val sphereTimer = Timer(Timer.Type.Loop, 6.0f)

    val lightHeight = 20.0f
    val halfLightDistance = 25.0f
    val lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance)

    val impostorBuffer = byteBufferBig(NUMBER_OF_SPHERES * VertexData.SIZE)

    override fun init(gl: GL3) = with(gl) {

        initializePrograms(gl)

        sphere = Mesh(gl, this::class.java, "tut13/UnitSphere.xml")
        plane = Mesh(gl, this::class.java, "tut13/LargePlane.xml")
        cube = Mesh(gl, this::class.java, "tut13/UnitCube.xml")

        val depthZNear = 0.0f
        val depthZFar = 1.0f

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(depthZNear, depthZFar)
        glEnable(GL_DEPTH_CLAMP)

        //Setup our Uniform Buffers
        glGenBuffers(Buffer.MAX, bufferName)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE.L, null, GL_DYNAMIC_DRAW)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE.L, null, GL_DYNAMIC_DRAW)

        //Bind the static buffers.
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName[Buffer.LIGHT], 0, LightBlock.SIZE.L)
        glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName[Buffer.PROJECTION], 0, Mat4.SIZE.L)

        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.IMPOSTER])
        glBufferData(GL_ARRAY_BUFFER, NUMBER_OF_SPHERES * VertexData.SIZE.L, null, GL_STREAM_DRAW)

        glGenVertexArrays(1, imposterVAO)
        glBindVertexArray(imposterVAO[0])
        glEnableVertexAttribArray(Semantic.Attr.CAMERA_SPHERE_POS)
        glVertexAttribPointer(Semantic.Attr.CAMERA_SPHERE_POS, 3, GL_FLOAT, false, VertexData.SIZE, VertexData.OFFSET_CAMERA_POSITION.L)
        glEnableVertexAttribArray(Semantic.Attr.SPHERE_RADIUS)
        glVertexAttribPointer(Semantic.Attr.SPHERE_RADIUS, 1, GL_FLOAT, false, VertexData.SIZE, VertexData.OFFSET_SPHERE_RADIUS.L)

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glEnable(GL_PROGRAM_POINT_SIZE)

        createMaterials(gl)
    }

    fun createMaterials(gl: GL3) = with(gl) {

        val ubArray = arrayOf(
                MaterialEntry(
                        diffuseColor = Vec4(0.1f, 0.1f, 0.8f, 1.0f),
                        specularColor = Vec4(0.8f, 0.8f, 0.8f, 1.0f),
                        specularShininess = 0.1f),
                MaterialEntry(
                        diffuseColor = Vec4(0.4f, 0.4f, 0.4f, 1.0f),
                        specularColor = Vec4(0.1f, 0.1f, 0.1f, 1.0f),
                        specularShininess = 0.8f),
                MaterialEntry(
                        diffuseColor = Vec4(0.05f, 0.05f, 0.05f, 1.0f),
                        specularColor = Vec4(0.95f, 0.95f, 0.95f, 1.0f),
                        specularShininess = 0.3f),
                MaterialEntry(
                        diffuseColor = Vec4(0.803f, 0.709f, 0.15f, 1.0f),
                        specularColor = Vec4(0.803f, 0.709f, 0.15f, 1.0f) * 0.75f,
                        specularShininess = 0.18f))

        val arrayBuffer = byteBufferBig(ubArray.size * MaterialEntry.SIZE)
        ubArray.forEachIndexed { i, it -> it.to(arrayBuffer, i * MaterialEntry.SIZE) }

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MATERIAL_ARRAY])
        glBufferData(GL_UNIFORM_BUFFER, arrayBuffer.capacity().L, arrayBuffer, GL_STATIC_DRAW)

        val terrainBuffer = byteBufferBig(MaterialEntry.SIZE)
        MaterialEntry(
                diffuseColor = Vec4(0.5f, 0.5f, 0.5f, 1.0f),
                specularColor = Vec4(0.5f, 0.5f, 0.5f, 1.0f),
                specularShininess = 0.6f)
                .to(terrainBuffer)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MATERIAL_TERRAIN])
        glBufferData(GL_UNIFORM_BUFFER, MaterialEntry.SIZE.L, terrainBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        destroyBuffers(arrayBuffer, terrainBuffer)
    }

    class MaterialEntry(
            var diffuseColor: Vec4,
            var specularColor: Vec4,
            var specularShininess: Float,
            var padding: FloatArray = FloatArray(3)) {

        fun to(buffer: ByteBuffer, offset: Int = 0): ByteBuffer {
            diffuseColor.to(buffer, offset)
            specularColor.to(buffer, offset + Vec4.SIZE)
            return buffer.putFloat(offset + Vec4.SIZE * 2, specularShininess)
        }

        companion object {
            val SIZE = 3 * Vec4.SIZE
        }
    }

    fun initializePrograms(gl: GL3) {

        litMeshProg = ProgramMeshData(gl, "pn.vert", "lighting.frag")

        litImpProg = ProgramImposData(gl, "geom-impostor")

        unlit = UnlitProgData(gl, "unlit")
    }

    override fun display(gl: GL3) = with(gl) {

        sphereTimer.update()

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0.75f, 0.75f, 1.0f, 1.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val modelMatrix = MatrixStack(viewPole.calcMatrix())

        val worldToCamMat = modelMatrix.top()

        val lightData = LightBlock

        lightData.ambientIntensity = Vec4(0.2f, 0.2f, 0.2f, 1.0f)
        lightData.lightAttenuation = lightAttenuation

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat * Vec4(0.707f, 0.707f, 0.0f, 0.0f)
        lightData.lights[0].lightIntensity = Vec4(0.6f, 0.6f, 0.6f, 1.0f)

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat * calcLightPosition()
        lightData.lights[1].lightIntensity = Vec4(0.4f, 0.4f, 0.4f, 1.0f)

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.LIGHT])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE.L, lightData.toBuffer())
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        run {
            glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL_TERRAIN], 0, MaterialEntry.SIZE.L)

            val normMatrix = modelMatrix.top().toMat3()
            normMatrix.inverse_().transpose_()

            glUseProgram(litMeshProg.theProgram)
            glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top() to matBuffer)
            glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix to matBuffer)

            plane.render(gl)

            glUseProgram(0)
            glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0)
        }

        run {
            val posSizeArray = arrayOf(
                    VertexData(
                            cameraPosition = (worldToCamMat * Vec4(0.0f, 10.0f, 0.0f, 1.0f)).toVec3(),
                            sphereRadius = 4.0f),
                    VertexData(
                            cameraPosition = getSphereOrbitPos(modelMatrix, Vec3(0.0f, 10.0f, 0.0f), Vec3(0.6, 0.8f, 0.0f),
                                    20.0f, sphereTimer.getAlpha()),
                            sphereRadius = 2.0f),
                    VertexData(
                            cameraPosition = getSphereOrbitPos(modelMatrix, Vec3(-10.0f, 1.0f, 0.0f), Vec3(0.0, 1.0f, 0.0f),
                                    10.0f, sphereTimer.getAlpha()),
                            sphereRadius = 1.0f),
                    VertexData(
                            cameraPosition = getSphereOrbitPos(modelMatrix, Vec3(10.0f, 1.0f, 0.0f), Vec3(0.0, 1.0f, 0.0f),
                                    10.0f, sphereTimer.getAlpha() * 2.0f),
                            sphereRadius = 1.0f))

            posSizeArray.forEachIndexed { i, it -> it.to(impostorBuffer, VertexData.SIZE * i) }

            glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.IMPOSTER])
            glBufferData(GL_ARRAY_BUFFER, NUMBER_OF_SPHERES * VertexData.SIZE.L, impostorBuffer, GL_STREAM_DRAW)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
        }

        run {
            glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL_ARRAY], 0,
                    MaterialEntry.SIZE * NUMBER_OF_SPHERES.L)

            glUseProgram(litImpProg.theProgram)
            glBindVertexArray(imposterVAO[0])
            glDrawArrays(GL_POINTS, 0, NUMBER_OF_SPHERES)
            glBindVertexArray(0)
            glUseProgram(0)

            glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0)
        }

        if (drawLights)

            modelMatrix run {

                translate(calcLightPosition())
                scale(0.5f)

                glUseProgram(unlit.theProgram)
                glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, top() to matBuffer)

                val lightColor = Vec4(1.0f)
                glUniform4fv(unlit.objectColorUnif, 1, lightColor to vecBuffer)
                cube.render(gl, "flat")
            }

        if (drawCameraPos)

            modelMatrix run {

                setIdentity()
                translate(0.0f, 0.0f, -viewPole.getView().radius)

                glDisable(GL_DEPTH_TEST)
                glDepthMask(false)
                glUseProgram(unlit.theProgram)
                glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, top() to matBuffer)
                glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f)
                cube.render(gl, "flat")

                glDepthMask(true)
                glEnable(GL_DEPTH_TEST)
                glUniform4f(unlit.objectColorUnif, 1.0f, 1.0f, 1.0f, 1.0f)
                cube.render(gl, "flat")
            }
    }

    fun calcLightPosition(): Vec4 {

        val scale = glm.PIf * 2.0f

        val timeThroughLoop = sphereTimer.getAlpha()
        val ret = Vec4(0.0f, lightHeight, 0.0f, 1.0f)

        ret.x = glm.cos(timeThroughLoop * scale) * 20.0f
        ret.z = glm.sin(timeThroughLoop * scale) * 20.0f

        return ret
    }

    fun getSphereOrbitPos(modelMatrix: MatrixStack, orbitCenter: Vec3, orbitAxis: Vec3, orbitRadius: Float, orbitAlpha: Float) =

            modelMatrix.run {

                translate(orbitCenter)
                rotate(orbitAxis, 360.0f * orbitAlpha)

                var offsetDir = orbitAxis cross Vec3(0.0f, 1.0f, 0.0f)
                if (offsetDir.length() < 0.001f)
                    offsetDir = orbitAxis cross Vec3(1.0f, 0.0f, 0.0f)

                offsetDir.normalize_()

                translate(offsetDir * orbitRadius)

                (top() * Vec4(0.0f, 0.0f, 0.0f, 1.0f)).toVec3()
            }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1_000f
        val perspMatrix = MatrixStack()

        val proj = perspMatrix.perspective(45.0f, w.f / h, zNear, zFar).top()

        glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.PROJECTION])
        glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE.L, proj to matBuffer)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        glViewport(0, 0, w, h)
    }

    override fun mousePressed(e: MouseEvent) {
        viewPole.mousePressed(e)
    }

    override fun mouseDragged(e: MouseEvent) {
        viewPole.mouseDragged(e)
    }

    override fun mouseReleased(e: MouseEvent) {
        viewPole.mouseReleased(e)
    }

    override fun mouseWheelMoved(e: MouseEvent) {
        viewPole.mouseWheel(e)
    }

    override fun keyPressed(e: KeyEvent) {

        when (e.keyCode) {

            KeyEvent.VK_ESCAPE -> quit()

            KeyEvent.VK_P -> sphereTimer.togglePause()
            KeyEvent.VK_MINUS -> sphereTimer.rewind(0.5f)
            KeyEvent.VK_PLUS -> sphereTimer.fastForward(0.5f)
            KeyEvent.VK_T -> drawCameraPos = !drawCameraPos
            KeyEvent.VK_G -> drawLights = !drawLights
        }

        viewPole.keyPressed(e)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(litImpProg.theProgram)
        glDeleteProgram(litMeshProg.theProgram)
        glDeleteProgram(unlit.theProgram)

        glDeleteBuffers(Buffer.MAX, bufferName)
        glDeleteVertexArrays(1, imposterVAO)

        sphere.dispose(gl)
        plane.dispose(gl)
        cube.dispose(gl)

        destroyBuffers(bufferName, imposterVAO, LightBlock.buffer, impostorBuffer)
    }

    class PerLight {

        lateinit var cameraSpaceLightPos: Vec4
        lateinit var lightIntensity: Vec4

        fun to(buffer: ByteBuffer, offset: Int): ByteBuffer {
            cameraSpaceLightPos.to(buffer, offset)
            return lightIntensity.to(buffer, offset + Vec4.SIZE)
        }

        companion object {
            val SIZE = Vec4.SIZE * 2
        }
    }

    object LightBlock {

        lateinit var ambientIntensity: Vec4
        var lightAttenuation = 0.f
        var padding = FloatArray(3)
        var lights = arrayOf(PerLight(), PerLight())

        fun toBuffer(): ByteBuffer {
            ambientIntensity to buffer
            buffer.putFloat(Vec4.SIZE, lightAttenuation)
            repeat(NUMBER_OF_LIGHTS) { lights[it].to(buffer, Vec4.SIZE * 2 + PerLight.SIZE * it) }
            return buffer
        }

        val SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE
        val buffer = byteBufferBig(SIZE)
    }

    class VertexData(
            var cameraPosition: Vec3,
            var sphereRadius: Float) {

        fun to(buffer: ByteBuffer, offset: Int): ByteBuffer {
            cameraPosition.to(buffer, offset)
            return buffer.putFloat(offset + Vec3.SIZE, sphereRadius)
        }

        companion object {
            val SIZE = Vec3.SIZE + Float.BYTES
            val OFFSET_CAMERA_POSITION = 0
            val OFFSET_SPHERE_RADIUS = OFFSET_CAMERA_POSITION + Vec3.SIZE
        }
    }

    class ProgramImposData(gl: GL3, shader: String) {

        val theProgram = programOf(gl, this::class.java, "tut13", shader + ".vert", shader + ".geom", shader + ".frag")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Light"),
                        Semantic.Uniform.LIGHT)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Material"),
                        Semantic.Uniform.MATERIAL)
            }
        }
    }

    class ProgramMeshData(gl: GL3, vertex: String, fragment: String) {

        val theProgram = programOf(gl, this::class.java, "tut13", vertex, fragment)

        val modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix")
        val normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix")

        init {
            with(gl) {
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Projection"),
                        Semantic.Uniform.PROJECTION)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Light"),
                        Semantic.Uniform.LIGHT)
                glUniformBlockBinding(
                        theProgram,
                        glGetUniformBlockIndex(theProgram, "Material"),
                        Semantic.Uniform.MATERIAL)
            }
        }
    }

    class UnlitProgData(gl: GL3, shader: String) {

        val theProgram = programOf(gl, this::class.java, "tut13", shader + ".vert", shader + ".frag")

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