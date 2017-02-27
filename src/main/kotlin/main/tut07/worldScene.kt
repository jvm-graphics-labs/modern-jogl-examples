package main.tut07

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import com.jogamp.opengl.GL3ES3
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import glm.MatrixStack
import glsl.Program
import glsl.programOf
import glsl.shaderCodeOf
import main.f
import main.framework.Framework
import main.framework.component.Mesh
import main.glm
import main.i
import main.rad
import main.tut07.worldScene.Forest
import mat.Mat4
import mat.Mat4x4
import vec._3.Vec3
import vec._4.Vec4
import kotlin.properties.Delegates

/**
 * Created by elect on 26/02/17.
 */

fun main(args: Array<String>) {
    WorldScene_()
}

class WorldScene_ : Framework("Tutorial 07 - World Scene") {

    val MESHES_SOURCE = arrayOf("UnitConeTint.xml", "UnitCylinderTint.xml", "UnitCubeTint.xml", "UnitCubeColor.xml", "UnitPlane.xml")

    object MESH {
        val CONE = 0
        val CYLINDER = 1
        val CUBE_TINT = 2
        val CUBE_COLOR = 3
        val PLANE = 4
        val MAX = 5
    }

    var uniformColor by Delegates.notNull<Program>()
    var objectColor by Delegates.notNull<ProgramData>()
    var uniformColorTint by Delegates.notNull<ProgramData>()
    var meshes by Delegates.notNull<Array<Mesh>>()
    val sphereCamRelPos = Vec3(67.5f, -46.0f, 150.0f)
    val camTarget = Vec3(0.0f, 0.4f, 0.0f)
    var drawLookAtPoint = false

    override fun init(gl: GL3) = with(gl) {

        initializeProgram(gl)

        meshes = Array<Mesh>(MESH.MAX, { Mesh(gl, this::class.java, "tut07/${MESHES_SOURCE[it]}") })

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CW)

        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDepthFunc(GL_LEQUAL)
        glDepthRangef(0.0f, 1.0f)
        glEnable(GL_DEPTH_CLAMP)
    }

    fun initializeProgram(gl: GL3) {

//        uniformColor = ProgramData(gl, "pos-only-world-transform.vert", "color-uniform.frag")
        uniformColor = Program(gl, this::class.java, arrayOf("tut07/pos-only-world-transform.vert", "tut07/color-uniform.frag"),
                arrayOf("modelToWorldMatrix", "worldToCameraMatrix", "cameraToClipMatrix", "baseColor"))
        objectColor = ProgramData(gl, "pos-color-world-transform.vert", "color-passthrough.frag")
        uniformColorTint = ProgramData(gl, "pos-color-world-transform.vert", "color-mult-uniform.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val camPos = resolveCamPosition()

        val camMat = calcLookAtMatrix(camPos, camTarget, Vec3(0.0f, 1.0f, 0.0f))
        camMat to Framework.matBuffer

        glUseProgram(uniformColor.name)
        glUniformMatrix4fv(uniformColor["worldToCameraMatrix"], 1, false, Framework.matBuffer)
        glUseProgram(objectColor.theProgram)
        glUniformMatrix4fv(objectColor.worldToCameraMatrixUnif, 1, false, Framework.matBuffer)
        glUseProgram(uniformColorTint.theProgram)
        glUniformMatrix4fv(uniformColorTint.worldToCameraMatrixUnif, 1, false, Framework.matBuffer)
        glUseProgram(0)

        val modelMatrix = MatrixStack()

        //  Render the ground plane
        modelMatrix run {

            scale(Vec3(100.0f, 1.0f, 100.0f))

            glUseProgram(uniformColor.name)
            glUniformMatrix4fv(uniformColor["modelToWorldMatrix"], 1, false, top() to matBuffer)
            glUniform4f(uniformColor["baseColor"], 0.302f, 0.416f, 0.0589f, 1.0f)
            meshes[MESH.PLANE].render(gl)
            glUseProgram(0)
        }

        //  Draw the trees
        drawForest(gl, modelMatrix)

        //  Draw the building
        modelMatrix run {

            translate(Vec3(20.0f, 0.0f, -10.0f))

            drawParthenon(gl, modelMatrix)
        }

        if (drawLookAtPoint) {

            glDisable(GL3.GL_DEPTH_TEST)

            modelMatrix run {

                translate(Vec3(0.0f, 0.0f, (-camTarget - camPos.x).length()))
                scale(Vec3(1.0f))

                glUseProgram(objectColor.theProgram)
                glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
                glUniformMatrix4fv(objectColor.worldToCameraMatrixUnif, 1, false, Mat4(1.0f) to matBuffer)
                meshes[MESH.CUBE_COLOR].render(gl)
                glUseProgram(0)
            }
            glEnable(GL3.GL_DEPTH_TEST)
        }
    }

    fun resolveCamPosition(): Vec3 {

        val phi = sphereCamRelPos.x.rad
        val theta = (sphereCamRelPos.y + 90.0f).rad

        val sinTheta = glm.sin(theta)
        val cosTheta = glm.cos(theta)
        val cosPhi = glm.cos(phi)
        val sinPhi = glm.sin(phi)

        val dirToCamera = Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi)

        return (dirToCamera * sphereCamRelPos.z) + camTarget
    }

    fun calcLookAtMatrix(cameraPt: Vec3, lookPt: Vec3, upPt: Vec3): Mat4x4 {

        val lookDir = (lookPt - cameraPt).normalize()
        val upDir = upPt.normalize()

        val rightDir = (lookDir cross upDir).normalize()
        val perpUpDir = rightDir cross lookDir

        val rotMat = Mat4(1.0f)
        rotMat[0] = Vec4(rightDir, 0.0f)
        rotMat[1] = Vec4(perpUpDir, 0.0f)
        rotMat[2] = Vec4(lookDir.negate(), 0.0f)

        rotMat.transpose_()

        val transMat = Mat4x4(1.0f)
        transMat[3] = Vec4(cameraPt.negate(), 1.0f)

        return rotMat * transMat
    }

    fun drawForest(gl: GL3, modelMatrix: MatrixStack) = Forest.trees.forEach {
        modelMatrix run {
            translate(Vec3(it.xPos, 1.0f, it.zPos))
            drawTree(gl, modelMatrix, it.trunkHeight, it.coneHeight)
        }
    }

    fun drawTree(gl: GL3, modelStack: MatrixStack, trunkHeight: Float, coneHeight: Float) = with(gl) {

        //  Draw trunk
        modelStack run {

            scale(Vec3(1.0f, trunkHeight, 1.0f))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.694f, 0.4f, 0.106f, 1.0f)
            meshes[MESH.CYLINDER].render(gl)
            glUseProgram(0)

        } run {
            //  Draw the treetop

            translate(Vec3(0.0f, trunkHeight, 0.0f))
            scale(Vec3(3.0f, coneHeight, 3.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.0f, 1.0f, 0.0f, 1.0f)
            meshes[MESH.CONE].render(gl)
            glUseProgram(0)
        }
    }

    fun drawParthenon(gl: GL3, modelMatrix: MatrixStack) = with(gl) {

        val parthenonWidth = 14.0f
        val parthenonLength = 20.0f
        val parthenonColumnHeight = 5.0f
        val parthenonBaseHeight = 1.0f
        val parthenonTopHeight = 2.0f

        //  Draw base
        modelMatrix run {

            scale(Vec3(parthenonWidth, parthenonBaseHeight, parthenonLength))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {
            //  Draw top

            translate(Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f))
            scale(Vec3(parthenonWidth, parthenonTopHeight, parthenonLength))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)
        }

        //  Draw columns
        val frontZval = parthenonLength / 2.0f - 1.0f
        val rightXval = parthenonWidth / 2.0f - 1.0f

        repeat((parthenonWidth / 2.0f).i) {

            modelMatrix run {

                translate(Vec3(2.0f * it - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval))

                drawColumn(gl, modelMatrix, parthenonColumnHeight)

            } run {

                translate(Vec3(2.0f * it - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval))

                drawColumn(gl, modelMatrix, parthenonColumnHeight)
            }
        }

        //Don't draw the first or last columns, since they've been drawn already.
        for (iColumnNum in 1..((parthenonLength - 2.0f) / 2.0f).i - 1) {

            modelMatrix run {

                translate(Vec3(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f))

                drawColumn(gl, modelMatrix, parthenonColumnHeight)

            } run {

                translate(Vec3(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f))

                drawColumn(gl, modelMatrix, parthenonColumnHeight)
            }
        }

        //  Draw interior
        modelMatrix run {

            translate(Vec3(0.0f, 1.0f, 0.0f))
            scale(Vec3(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(objectColor.theProgram)
            glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            meshes[MESH.CUBE_COLOR].render(gl)
            glUseProgram(0)
        }

        //  Draw headpiece
        modelMatrix run {

            translate(Vec3(
                    0.0f,
                    parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                    parthenonLength / 2.0f))
            rotateX(-135.0f)
            rotateY(45.0f)

            glUseProgram(objectColor.theProgram)
            glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            meshes[MESH.CUBE_COLOR].render(gl)
            glUseProgram(0)
        }
    }

    //Columns are 1x1 in the X/Z, and fHieght units in the Y.
    fun drawColumn(gl: GL3, modelMatrix: MatrixStack, parthenonColumnHeight: Float) = with(gl) {

        val columnBaseHeight = 0.25f

        //Draw the bottom of the column.
        modelMatrix run {

            scale(Vec3(1.0f, columnBaseHeight, 1.0f))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {

            //Draw the top of the column.

            translate(Vec3(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f))
            scale(Vec3(1.0f, columnBaseHeight, 1.0f))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {

            //Draw the main column.

            translate(vec._3.Vec3(0.0f, columnBaseHeight, 0.0f))
            scale(Vec3(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f))
            translate(Vec3(0.0f, 0.5f, 0.0f))

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CYLINDER].render(gl)
            glUseProgram(0)
        }
    }

    override fun reshape(gl: GL3, w: Int, h: Int) = with(gl) {

        val zNear = 1.0f
        val zFar = 1000.0f

        glm.perspective(45.0f, w / h.f, zNear, zFar) to matBuffer

        glUseProgram(uniformColor.name)
        glUniformMatrix4fv(uniformColor.uniforms["cameraToClipMatrix"]!!, 1, false, matBuffer)
        glUseProgram(objectColor.theProgram)
        glUniformMatrix4fv(objectColor.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(uniformColorTint.theProgram)
        glUniformMatrix4fv(uniformColorTint.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(uniformColor.name)
        glDeleteProgram(objectColor.theProgram)
        glDeleteProgram(uniformColorTint.theProgram)

        meshes.forEach { it.dispose(gl) }
    }

    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {

            KeyEvent.VK_W -> camTarget.z = camTarget.z - if (e.isShiftDown) 0.4f else 4.0f
            KeyEvent.VK_S -> camTarget.z = camTarget.z + if (e.isShiftDown) 0.4f else 4.0f

            KeyEvent.VK_D -> camTarget.x = camTarget.x + if (e.isShiftDown) 0.4f else 4.0f
            KeyEvent.VK_A -> camTarget.x = camTarget.x - if (e.isShiftDown) 0.4f else 4.0f

            KeyEvent.VK_E -> camTarget.y = camTarget.y - if (e.isShiftDown) 0.4f else 4.0f
            KeyEvent.VK_Q -> camTarget.y = camTarget.y + if (e.isShiftDown) 0.4f else 4.0f

            KeyEvent.VK_I -> sphereCamRelPos.y = sphereCamRelPos.y - if (e.isShiftDown) 1.125f else 11.25f
            KeyEvent.VK_K -> sphereCamRelPos.y = sphereCamRelPos.y + if (e.isShiftDown) 1.125f else 11.25f

            KeyEvent.VK_J -> sphereCamRelPos.x = sphereCamRelPos.x - if (e.isShiftDown) 1.125f else 11.25f
            KeyEvent.VK_L -> sphereCamRelPos.x = sphereCamRelPos.x + if (e.isShiftDown) 1.125f else 11.25f

            KeyEvent.VK_O -> sphereCamRelPos.z = sphereCamRelPos.z - if (e.isShiftDown) 1.125f else 11.25f
            KeyEvent.VK_U -> sphereCamRelPos.z = sphereCamRelPos.z + if (e.isShiftDown) 1.125f else 11.25f

            KeyEvent.VK_SPACE -> drawLookAtPoint = !drawLookAtPoint

            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
        //                camTarget.print("Target");
        //                sphereCamRelPos.print("Position");

        sphereCamRelPos.y = glm.clamp(sphereCamRelPos.y, -78.75f, -1.0f)
        camTarget.y = glm.clamp(camTarget.y, 0.0f, camTarget.y)
        sphereCamRelPos.z = glm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z)
    }

    inner class ProgramData(gl: GL3, vert: String, frag: String) {

        var theProgram = 0
        var modelToWorldMatrixUnif = 0
        var worldToCameraMatrixUnif = 0
        var cameraToClipMatrixUnif = 0
        var baseColorUnif = 0

        init {
            
            with(gl) {

                theProgram = programOf(gl, this::class.java, "tut07", vert, frag)

                modelToWorldMatrixUnif = glGetUniformLocation(theProgram, "modelToWorldMatrix")
                worldToCameraMatrixUnif = glGetUniformLocation(theProgram, "worldToCameraMatrix")
                cameraToClipMatrixUnif = glGetUniformLocation(theProgram, "cameraToClipMatrix")
                baseColorUnif = glGetUniformLocation(theProgram, "baseColor")
            }
        }
    }
}