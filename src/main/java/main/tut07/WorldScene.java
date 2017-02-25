/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut07;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import glm.MatrixStack;
import glsl.ShaderProgramKt;
import main.framework.Framework;
import main.framework.component.Mesh;
import main.tut07.worldScene.Forest;
import mat.Mat4x4;
import one.util.streamex.StreamEx;
import org.xml.sax.SAXException;
import vec._3.Vec3;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static main.GlmKt.glm;

/**
 * @author gbarbieri
 */
public class WorldScene extends Framework {

    private final String[] MESHES_SOURCE = {"UnitConeTint.xml", "UnitCylinderTint.xml", "UnitCubeTint.xml", "UnitCubeColor.xml", "UnitPlane.xml"};

    public static void main(String[] args) {
        new WorldScene("Tutorial 07 - World Scene");
    }

    public WorldScene(String title) {
        super(title);
    }

    private interface MESH {

        int CONE = 0;
        int CYLINDER = 1;
        int CUBE_TINT = 2;
        int CUBE_COLOR = 3;
        int PLANE = 4;
        int MAX = 5;
    }

    private ProgramData uniformColor, objectColor, uniformColorTint;
    private Mesh[] meshes = new Mesh[MESH.MAX];
    private Vec3 sphereCamRelPos = new Vec3(67.5f, -46.0f, 150.0f), camTarget = new Vec3(0.0f, 0.4f, 0.0f);
    private boolean drawLookAtPoint = false;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        for (int i = 0; i < MESH.MAX; i++) {
            try {
                meshes[i] = new Mesh(gl, "tut07/" + MESHES_SOURCE[i]);
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                Logger.getLogger(WorldScene.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glEnable(GL_DEPTH_CLAMP);
    }

    private void initializeProgram(GL3 gl) {

        uniformColor = new ProgramData(gl, "pos-only-world-transform", "color-uniform");
        objectColor = new ProgramData(gl, "pos-color-world-transform", "color-passthrough");
        uniformColorTint = new ProgramData(gl, "pos-color-world-transform", "color-mult-uniform");
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        final Vec3 camPos = resolveCamPosition();

        Mat4x4 camMat = calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f));

        gl.glUseProgram(uniformColor.theProgram);
        gl.glUniformMatrix4fv(uniformColor.worldToCameraMatrixUnif, 1, false, camMat.to(matBuffer));
        gl.glUseProgram(objectColor.theProgram);
        gl.glUniformMatrix4fv(objectColor.worldToCameraMatrixUnif, 1, false, matBuffer);
        gl.glUseProgram(uniformColorTint.theProgram);
        gl.glUniformMatrix4fv(uniformColorTint.worldToCameraMatrixUnif, 1, false, matBuffer);
        gl.glUseProgram(0);

        MatrixStack modelMatrix = new MatrixStack();

        //  Render the ground plane
        {
            modelMatrix
                    .push()
                    .scale(new Vec3(100.0f, 1.0f, 100.0f));

            gl.glUseProgram(uniformColor.theProgram);
            gl.glUniformMatrix4fv(uniformColor.modelToWorldMatrixUnif, 1, false, modelMatrix.to(matBuffer));
            gl.glUniform4f(uniformColor.baseColorUnif, 0.302f, 0.416f, 0.0589f, 1.0f);
            meshes[MESH.PLANE].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw the trees
        drawForest(gl, modelMatrix);

        //  Draw the building
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(20.0f, 0.0f, -10.0f));

            drawParthenon(gl, modelMatrix);

            modelMatrix.pop();
        }

        if (drawLookAtPoint) {

            gl.glDisable(GL3.GL_DEPTH_TEST);

            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, 0.0f, -camTarget.sub_(camPos.x()).length()))
                    .scale(new Vec3(1.0f));

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, modelMatrix.to(matBuffer));
            gl.glUniformMatrix4fv(objectColor.worldToCameraMatrixUnif, 1, false, new Mat4x4(1.0f).to(matBuffer));
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
            gl.glEnable(GL3.GL_DEPTH_TEST);
        }
    }

    private Vec3 resolveCamPosition() {

        float phi = glm.toRad(sphereCamRelPos.x());
        float theta = glm.toRad(sphereCamRelPos.y() + 90.0f);

        float sinTheta = glm.sin(theta);
        float cosTheta = glm.cos(theta);
        float cosPhi = glm.cos(phi);
        float sinPhi = glm.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        return dirToCamera.mul_(sphereCamRelPos.z()).add_(camTarget);
    }

    private Mat4x4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.sub(cameraPt).normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 rightDir = lookDir.cross(upDir).normalize();
        Vec3 perpUpDir = rightDir.cross(lookDir);

        Mat4x4 rotationMat = new Mat4x4(1.0f);
        rotationMat.set(0, rightDir, 0.0f);
        rotationMat.set(1, perpUpDir, 0.0f);
        rotationMat.set(2, lookDir.negate(), 0.0f);

        rotationMat.transpose_();

        Mat4x4 translationMat = new Mat4x4(1.0f);
        translationMat.set(3, cameraPt.negate(), 1.0f);

        return rotationMat.mul_(translationMat);
    }

    private void drawForest(GL3 gl, MatrixStack modelMatrix) {

        for (Forest.Tree tree : Forest.trees) {
            modelMatrix
                    .push()
                    .translate(new Vec3(tree.xPos, 1.0f, tree.zPos));
            drawTree(gl, modelMatrix, tree.trunkHeight, tree.coneHeight);
            modelMatrix.pop();
        }
    }

    private void drawTree(GL3 gl, MatrixStack modelStack, float trunkHeight, float coneHeight) {

        //  Draw trunk
        {
            modelStack.push();

            modelStack
                    .scale(new Vec3(1.0f, trunkHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, modelStack.to(matBuffer));
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.694f, 0.4f, 0.106f, 1.0f);
            meshes[MESH.CYLINDER].render(gl);
            gl.glUseProgram(0);

            modelStack.pop();
        }

        //  Draw the treetop
        {
            modelStack.push()
                    .translate(new Vec3(0.0f, trunkHeight, 0.0f))
                    .scale(new Vec3(3.0f, coneHeight, 3.0f));

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, modelStack.to(matBuffer));
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.0f, 1.0f, 0.0f, 1.0f);
            meshes[MESH.CONE].render(gl);
            gl.glUseProgram(0);

            modelStack.pop();
        }
    }

    private void drawParthenon(GL3 gl, MatrixStack modelMatrix) {

        final float parthenonWidth = 14.0f;
        final float parthenonLength = 20.0f;
        final float parthenonColumnHeight = 5.0f;
        final float parthenonBaseHeight = 1.0f;
        final float parthenonTopHeight = 2.0f;

        //  Draw base
        {
            modelMatrix
                    .push()
                    .scale(new Vec3(parthenonWidth, parthenonBaseHeight, parthenonLength))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, modelMatrix.to(matBuffer));
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CUBE_TINT].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw top
        {
            modelMatrix.push()
                    .translate(new Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f))
                    .scale(new Vec3(parthenonWidth, parthenonTopHeight, parthenonLength))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, modelMatrix.to(matBuffer));
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CUBE_TINT].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw columns
        final float frontZval = parthenonLength / 2.0f - 1.0f;
        final float rightXval = parthenonWidth / 2.0f - 1.0f;

        for (int iColumnNum = 0; iColumnNum < ((int) parthenonWidth / 2.0f); iColumnNum++) {
            {
                modelMatrix
                        .push()
                        .translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval));

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
            {
                modelMatrix
                        .push()
                        .translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval));

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
        }
        //Don't draw the first or last columns, since they've been drawn already.
        for (int iColumnNum = 1; iColumnNum < ((int) ((parthenonLength - 2.0f) / 2.0f)); iColumnNum++) {
            {
                modelMatrix
                        .push()
                        .translate(new Vec3(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
            {
                modelMatrix
                        .push()
                        .translate(new Vec3(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
        }

        //  Draw interior
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, 1.0f, 0.0f))
                    .scale(new Vec3(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, modelMatrix.to(matBuffer));
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw headpiece
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(
                            0.0f,
                            parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                            parthenonLength / 2.0f))
                    .rotateX(-135.0f)
                    .rotateY(45.0f)
                    .top().toDfb(matBuffer);

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    //Columns are 1x1 in the X/Z, and fHieght units in the Y.
    private void drawColumn(GL3 gl3, MatrixStack modelMatrix, float parthenonColumnHeight) {

        final float columnBaseHeight = 0.25f;

        //Draw the bottom of the column.
        {
            modelMatrix
                    .push()
                    .scale(new Vec3(1.0f, columnBaseHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            meshes[MESH.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the top of the column.
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f))
                    .scale(new Vec3(1.0f, columnBaseHeight, 1.0f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the main column.
        {
            modelMatrix
                    .push()
                    .translate(new Vec3(0.0f, columnBaseHeight, 0.0f))
                    .scale(new Vec3(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f))
                    .translate(new Vec3(0.0f, 0.5f, 0.0f))
                    .top().toDfb(matBuffer);

            gl3.glUseProgram(uniformColorTint.theProgram);
            gl3.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl3.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CYLINDER].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1000.0f;

        new MatrixStack()
                .setMatrix(Glm.perspective_(45.0f, w / (float) h, zNear, zFar))
                .top().toDfb(matBuffer);

        gl3.glUseProgram(uniformColor.theProgram);
        gl3.glUniformMatrix4fv(uniformColor.cameraToClipMatrixUnif, 1, false, matBuffer);
        gl3.glUseProgram(objectColor.theProgram);
        gl3.glUniformMatrix4fv(objectColor.cameraToClipMatrixUnif, 1, false, matBuffer);
        gl3.glUseProgram(uniformColorTint.theProgram);
        gl3.glUniformMatrix4fv(uniformColorTint.cameraToClipMatrixUnif, 1, false, matBuffer);
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(uniformColor.theProgram);
        gl3.glDeleteProgram(objectColor.theProgram);
        gl3.glDeleteProgram(uniformColorTint.theProgram);

        StreamEx.of(meshes).forEach(mesh -> mesh.dispose(gl3));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                camTarget.z -= e.isShiftDown() ? 0.4f : 4.0f;
                break;
            case KeyEvent.VK_S:
                camTarget.z += e.isShiftDown() ? 0.4f : 4.0f;
                break;

            case KeyEvent.VK_D:
                camTarget.x += e.isShiftDown() ? 0.4f : 4.0f;
                break;
            case KeyEvent.VK_A:
                camTarget.x -= e.isShiftDown() ? 0.4f : 4.0f;
                break;

            case KeyEvent.VK_E:
                camTarget.y -= e.isShiftDown() ? 0.4f : 4.0f;
                break;
            case KeyEvent.VK_Q:
                camTarget.y += e.isShiftDown() ? 0.4f : 4.0f;
                break;

            case KeyEvent.VK_I:
                sphereCamRelPos.y -= e.isShiftDown() ? 1.125f : 11.25f;
                break;
            case KeyEvent.VK_K:
                sphereCamRelPos.y += e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_J:
                sphereCamRelPos.x -= e.isShiftDown() ? 1.125f : 11.25f;
                break;
            case KeyEvent.VK_L:
                sphereCamRelPos.x += e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_O:
                sphereCamRelPos.z -= e.isShiftDown() ? 1.125f : 11.25f;
                break;
            case KeyEvent.VK_U:
                sphereCamRelPos.z += e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_SPACE:
                drawLookAtPoint = !drawLookAtPoint;
                camTarget.print("Target");
                sphereCamRelPos.print("Position");
                break;

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;
        }

        sphereCamRelPos.y = Glm.clamp(sphereCamRelPos.y, -78.75f, -1.0f);
        camTarget.y = Glm.clamp(camTarget.y, 0.0f, camTarget.y);
        sphereCamRelPos.z = Glm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z);
    }

    class ProgramData {

        int theProgram, modelToWorldMatrixUnif, worldToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;

        public ProgramData(GL3 gl, String vert, String frag) {

            theProgram = ShaderProgramKt.programOf(gl, getClass(), "tut07", vert, frag);

            modelToWorldMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToWorldMatrix");
            worldToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "worldToCameraMatrix");
            cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix");
            baseColorUnif = gl.glGetUniformLocation(theProgram, "baseColor");
        }

    }
}
