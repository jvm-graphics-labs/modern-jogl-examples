package main.tut07

import com.jogamp.newt.event.KeyEvent
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES3.GL_COLOR
import com.jogamp.opengl.GL2ES3.GL_DEPTH
import com.jogamp.opengl.GL3
import com.jogamp.opengl.GL3.GL_DEPTH_CLAMP
import glm.MatrixStack
import glsl.Program
import main.f
import main.framework.Framework
import main.framework.component.Mesh
import main.glm
import main.i
import main.rad
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

    var uniformColor by Delegates.notNull<ProgramData>()
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

        uniformColor = ProgramData(gl, "pos-only-world-transform.vert", "color-uniform.frag")
        objectColor = ProgramData(gl,"pos-color-world-transform.vert", "color-passthrough.frag")
        uniformColorTint = ProgramData(gl, "pos-color-world-transform.vert", "color-mult-uniform.frag")
    }

    override fun display(gl: GL3) = with(gl) {

        glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f))
        glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f))

        val camPos = resolveCamPosition()

        // camMat
        calcLookAtMatrix(camPos, camTarget, Vec3(0.0f, 1.0f, 0.0f)) to matBuffer

        glUseProgram(uniformColor.theProgram)
        glUniformMatrix4fv(uniformColor.worldToCameraMatrixUnif, 1, false, matBuffer)
        glUseProgram(objectColor.theProgram)
        glUniformMatrix4fv(objectColor.worldToCameraMatrixUnif, 1, false, matBuffer)
        glUseProgram(uniformColorTint.theProgram)
        glUniformMatrix4fv(uniformColorTint.worldToCameraMatrixUnif, 1, false, matBuffer)
        glUseProgram(0)

        val modelMatrix = MatrixStack()

        //  Render the ground plane
        modelMatrix run {

            scale(100.0f, 1.0f, 100.0f)

            glUseProgram(uniformColor.theProgram)
            glUniformMatrix4fv(uniformColor.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColor.baseColorUnif, 0.302f, 0.416f, 0.0589f, 1.0f)
            meshes[MESH.PLANE].render(gl)
            glUseProgram(0)
        }

        //  Draw the trees
        drawForest(gl, modelMatrix)

        //  Draw the building
        modelMatrix run {

            translate(20.0f, 0.0f, -10.0f)

            drawParthenon(gl, modelMatrix)
        }

        if (drawLookAtPoint) {

            glDisable(GL3.GL_DEPTH_TEST)

            modelMatrix run {

                val cameraAimVec = camTarget - camPos

                translate(0.0f, 0.0f, -glm.length(cameraAimVec))
                scale(1.0f)

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

    fun drawForest(gl: GL3, modelMatrix: MatrixStack) = forest.forEach {
        modelMatrix run {
            translate(it.xPos, 1.0f, it.zPos)
            drawTree(gl, modelMatrix, it.trunkHeight, it.coneHeight)
        }
    }

    fun drawTree(gl: GL3, modelStack: MatrixStack, trunkHeight: Float, coneHeight: Float) = with(gl) {

        //  Draw trunk
        modelStack run {

            scale(1.0f, trunkHeight, 1.0f)
            translate(0.0f, 0.5f, 0.0f)

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.694f, 0.4f, 0.106f, 1.0f)
            meshes[MESH.CYLINDER].render(gl)
            glUseProgram(0)

        } run {
            //  Draw the treetop

            translate(0.0f, trunkHeight, 0.0f)
            scale(3.0f, coneHeight, 3.0f)

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

            scale(parthenonWidth, parthenonBaseHeight, parthenonLength)
            translate(0.0f, 0.5f, 0.0f)

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {
            //  Draw top

            translate(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f)
            scale(parthenonWidth, parthenonTopHeight, parthenonLength)
            translate(0.0f, 0.5f, 0.0f)

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

                translate(2.0f * it - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval)

                drawColumn(gl, modelMatrix, parthenonColumnHeight)

            } run {

                translate(2.0f * it - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval)

                drawColumn(gl, modelMatrix, parthenonColumnHeight)
            }
        }

        //Don't draw the first or last columns, since they've been drawn already.
        for (iColumnNum in 1 until ((parthenonLength - 2.0f) / 2.0f).i - 1)

            modelMatrix run {

                translate(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f)

                drawColumn(gl, modelMatrix, parthenonColumnHeight)

            } run {

                translate(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f)

                drawColumn(gl, modelMatrix, parthenonColumnHeight)
            }

        //  Draw interior
        modelMatrix run {

            translate(0.0f, 1.0f, 0.0f)
            scale(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f)
            translate(0.0f, 0.5f, 0.0f)

            glUseProgram(objectColor.theProgram)
            glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            meshes[MESH.CUBE_COLOR].render(gl)
            glUseProgram(0)
        }

        //  Draw headpiece
        modelMatrix run {

            translate(
                    0.0f,
                    parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                    parthenonLength / 2.0f)
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

            scale(1.0f, columnBaseHeight, 1.0f)
            translate(0.0f, 0.5f, 0.0f)

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {

            //Draw the top of the column.

            translate(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f)
            scale(1.0f, columnBaseHeight, 1.0f)
            translate(0.0f, 0.5f, 0.0f)

            glUseProgram(uniformColorTint.theProgram)
            glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, top() to matBuffer)
            glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f)
            meshes[MESH.CUBE_TINT].render(gl)
            glUseProgram(0)

        } run {

            //Draw the main column.

            translate(0.0f, columnBaseHeight, 0.0f)
            scale(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f)
            translate(0.0f, 0.5f, 0.0f)

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

        glUseProgram(uniformColor.theProgram)
        glUniformMatrix4fv(uniformColor.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(objectColor.theProgram)
        glUniformMatrix4fv(objectColor.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(uniformColorTint.theProgram)
        glUniformMatrix4fv(uniformColorTint.cameraToClipMatrixUnif, 1, false, matBuffer)
        glUseProgram(0)

        glViewport(0, 0, w, h)
    }

    override fun end(gl: GL3) = with(gl) {

        glDeleteProgram(uniformColor.theProgram)
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
        //                camTarget.print("Target"); TODO
        //                sphereCamRelPos.print("Position");

        sphereCamRelPos.y = glm.clamp(sphereCamRelPos.y, -78.75f, -1.0f)
        camTarget.y = glm.clamp(camTarget.y, 0.0f, camTarget.y)
        sphereCamRelPos.z = glm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z)
    }

    class ProgramData(gl: GL3, vert: String, frag: String) {
        
        val theProgram = Program(gl, this::class.java, "tut07", vert, frag).name
        val modelToWorldMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToWorldMatrix")
        val worldToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "worldToCameraMatrix")
        val cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix")
        val baseColorUnif = gl.glGetUniformLocation(theProgram, "baseColor")
    }

    val forest = arrayOf(
            TreeData(-45.0f, -40.0f, 2.0f, 3.0f),
            TreeData(-42.0f, -35.0f, 2.0f, 3.0f),
            TreeData(-39.0f, -29.0f, 2.0f, 4.0f),
            TreeData(-44.0f, -26.0f, 3.0f, 3.0f),
            TreeData(-40.0f, -22.0f, 2.0f, 4.0f),
            TreeData(-36.0f, -15.0f, 3.0f, 3.0f),
            TreeData(-41.0f, -11.0f, 2.0f, 3.0f),
            TreeData(-37.0f, -6.0f, 3.0f, 3.0f),
            TreeData(-45.0f, 0.0f, 2.0f, 3.0f),
            TreeData(-39.0f, 4.0f, 3.0f, 4.0f),
            TreeData(-36.0f, 8.0f, 2.0f, 3.0f),
            TreeData(-44.0f, 13.0f, 3.0f, 3.0f),
            TreeData(-42.0f, 17.0f, 2.0f, 3.0f),
            TreeData(-38.0f, 23.0f, 3.0f, 4.0f),
            TreeData(-41.0f, 27.0f, 2.0f, 3.0f),
            TreeData(-39.0f, 32.0f, 3.0f, 3.0f),
            TreeData(-44.0f, 37.0f, 3.0f, 4.0f),
            TreeData(-36.0f, 42.0f, 2.0f, 3.0f),

            TreeData(-32.0f, -45.0f, 2.0f, 3.0f),
            TreeData(-30.0f, -42.0f, 2.0f, 4.0f),
            TreeData(-34.0f, -38.0f, 3.0f, 5.0f),
            TreeData(-33.0f, -35.0f, 3.0f, 4.0f),
            TreeData(-29.0f, -28.0f, 2.0f, 3.0f),
            TreeData(-26.0f, -25.0f, 3.0f, 5.0f),
            TreeData(-35.0f, -21.0f, 3.0f, 4.0f),
            TreeData(-31.0f, -17.0f, 3.0f, 3.0f),
            TreeData(-28.0f, -12.0f, 2.0f, 4.0f),
            TreeData(-29.0f, -7.0f, 3.0f, 3.0f),
            TreeData(-26.0f, -1.0f, 2.0f, 4.0f),
            TreeData(-32.0f, 6.0f, 2.0f, 3.0f),
            TreeData(-30.0f, 10.0f, 3.0f, 5.0f),
            TreeData(-33.0f, 14.0f, 2.0f, 4.0f),
            TreeData(-35.0f, 19.0f, 3.0f, 4.0f),
            TreeData(-28.0f, 22.0f, 2.0f, 3.0f),
            TreeData(-33.0f, 26.0f, 3.0f, 3.0f),
            TreeData(-29.0f, 31.0f, 3.0f, 4.0f),
            TreeData(-32.0f, 38.0f, 2.0f, 3.0f),
            TreeData(-27.0f, 41.0f, 3.0f, 4.0f),
            TreeData(-31.0f, 45.0f, 2.0f, 4.0f),
            TreeData(-28.0f, 48.0f, 3.0f, 5.0f),

            TreeData(-25.0f, -48.0f, 2.0f, 3.0f),
            TreeData(-20.0f, -42.0f, 3.0f, 4.0f),
            TreeData(-22.0f, -39.0f, 2.0f, 3.0f),
            TreeData(-19.0f, -34.0f, 2.0f, 3.0f),
            TreeData(-23.0f, -30.0f, 3.0f, 4.0f),
            TreeData(-24.0f, -24.0f, 2.0f, 3.0f),
            TreeData(-16.0f, -21.0f, 2.0f, 3.0f),
            TreeData(-17.0f, -17.0f, 3.0f, 3.0f),
            TreeData(-25.0f, -13.0f, 2.0f, 4.0f),
            TreeData(-23.0f, -8.0f, 2.0f, 3.0f),
            TreeData(-17.0f, -2.0f, 3.0f, 3.0f),
            TreeData(-16.0f, 1.0f, 2.0f, 3.0f),
            TreeData(-19.0f, 4.0f, 3.0f, 3.0f),
            TreeData(-22.0f, 8.0f, 2.0f, 4.0f),
            TreeData(-21.0f, 14.0f, 2.0f, 3.0f),
            TreeData(-16.0f, 19.0f, 2.0f, 3.0f),
            TreeData(-23.0f, 24.0f, 3.0f, 3.0f),
            TreeData(-18.0f, 28.0f, 2.0f, 4.0f),
            TreeData(-24.0f, 31.0f, 2.0f, 3.0f),
            TreeData(-20.0f, 36.0f, 2.0f, 3.0f),
            TreeData(-22.0f, 41.0f, 3.0f, 3.0f),
            TreeData(-21.0f, 45.0f, 2.0f, 3.0f),

            TreeData(-12.0f, -40.0f, 2.0f, 4.0f),
            TreeData(-11.0f, -35.0f, 3.0f, 3.0f),
            TreeData(-10.0f, -29.0f, 1.0f, 3.0f),
            TreeData(-9.0f, -26.0f, 2.0f, 2.0f),
            TreeData(-6.0f, -22.0f, 2.0f, 3.0f),
            TreeData(-15.0f, -15.0f, 1.0f, 3.0f),
            TreeData(-8.0f, -11.0f, 2.0f, 3.0f),
            TreeData(-14.0f, -6.0f, 2.0f, 4.0f),
            TreeData(-12.0f, 0.0f, 2.0f, 3.0f),
            TreeData(-7.0f, 4.0f, 2.0f, 2.0f),
            TreeData(-13.0f, 8.0f, 2.0f, 2.0f),
            TreeData(-9.0f, 13.0f, 1.0f, 3.0f),
            TreeData(-13.0f, 17.0f, 3.0f, 4.0f),
            TreeData(-6.0f, 23.0f, 2.0f, 3.0f),
            TreeData(-12.0f, 27.0f, 1.0f, 2.0f),
            TreeData(-8.0f, 32.0f, 2.0f, 3.0f),
            TreeData(-10.0f, 37.0f, 3.0f, 3.0f),
            TreeData(-11.0f, 42.0f, 2.0f, 2.0f),

            TreeData(15.0f, 5.0f, 2.0f, 3.0f),
            TreeData(15.0f, 10.0f, 2.0f, 3.0f),
            TreeData(15.0f, 15.0f, 2.0f, 3.0f),
            TreeData(15.0f, 20.0f, 2.0f, 3.0f),
            TreeData(15.0f, 25.0f, 2.0f, 3.0f),
            TreeData(15.0f, 30.0f, 2.0f, 3.0f),
            TreeData(15.0f, 35.0f, 2.0f, 3.0f),
            TreeData(15.0f, 40.0f, 2.0f, 3.0f),
            TreeData(15.0f, 45.0f, 2.0f, 3.0f),

            TreeData(25.0f, 5.0f, 2.0f, 3.0f),
            TreeData(25.0f, 10.0f, 2.0f, 3.0f),
            TreeData(25.0f, 15.0f, 2.0f, 3.0f),
            TreeData(25.0f, 20.0f, 2.0f, 3.0f),
            TreeData(25.0f, 25.0f, 2.0f, 3.0f),
            TreeData(25.0f, 30.0f, 2.0f, 3.0f),
            TreeData(25.0f, 35.0f, 2.0f, 3.0f),
            TreeData(25.0f, 40.0f, 2.0f, 3.0f),
            TreeData(25.0f, 45.0f, 2.0f, 3.0f))

    class TreeData(val xPos: Float, val zPos: Float, val trunkHeight: Float, val coneHeight: Float)
}