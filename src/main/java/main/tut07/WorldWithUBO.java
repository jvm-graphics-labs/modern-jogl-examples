
package main.tut07;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat.Mat4;
import glm.vec._3.Vec3;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import one.util.streamex.StreamEx;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.glsl.Program;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static glm.GlmKt.glm;

/**
 * @author gbarbieri
 */
public class WorldWithUBO extends Framework {

    private final String[] MESHES_SOURCE = {"UnitConeTint.xml", "UnitCylinderTint.xml", "UnitCubeTint.xml", "UnitCubeColor.xml", "UnitPlane.xml"};

    public static void main(String[] args) {
        new WorldWithUBO("Tutorial 07 - World Scene");
    }

    public WorldWithUBO(String title) {
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

    private IntBuffer globalMatricesBufferName = GLBuffers.newDirectIntBuffer(1);

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        for (int i = 0; i < MESH.MAX; i++) {
            try {
                meshes[i] = new Mesh(gl, getClass(), "tut07/" + MESHES_SOURCE[i]);
            } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
                Logger.getLogger(WorldWithUBO.class.getName()).log(Level.SEVERE, null, ex);
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

        uniformColor = new ProgramData(gl, "pos-only-world-transform-ubo.vert", "color-uniform.frag");
        objectColor = new ProgramData(gl, "pos-color-world-transform-ubo.vert", "color-passthrough.frag");
        uniformColorTint = new ProgramData(gl, "pos-color-world-transform-ubo.vert", "color-mult-uniform.frag");

        gl.glGenBuffers(1, globalMatricesBufferName);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesBufferName.get(0));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * 2, null, GL_STREAM_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, globalMatricesBufferName.get(0), 0, Mat4.SIZE * 2);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        final Vec3 camPos = resolveCamPosition();

        // camMat
        calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)).to(matBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesBufferName.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, Mat4.SIZE, Mat4.SIZE, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        MatrixStack modelMatrix = new MatrixStack();

        //  Render the ground plane
        {
            modelMatrix
                    .push()
                    .scale(100.0f, 1.0f, 100.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColor.theProgram);
            gl.glUniformMatrix4fv(uniformColor.modelToWorldMatrixUnif, 1, false, matBuffer);
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
                    .translate(20.0f, 0.0f, -10.0f);

            drawParthenon(gl, modelMatrix);

            modelMatrix.pop();
        }

        if (drawLookAtPoint) {

            gl.glDisable(GL3.GL_DEPTH_TEST);

            modelMatrix
                    .push()
                    .translate(camTarget)
                    .scale(1.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
            gl.glEnable(GL3.GL_DEPTH_TEST);
        }
    }

    private Vec3 resolveCamPosition() {

        float phi = glm.toRad(sphereCamRelPos.x);
        float theta = glm.toRad(sphereCamRelPos.y + 90.0f);

        float sinTheta = glm.sin(theta);
        float cosTheta = glm.cos(theta);
        float cosPhi = glm.cos(phi);
        float sinPhi = glm.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        return dirToCamera.times_(sphereCamRelPos.z).plus_(camTarget);
    }

    private Mat4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.minus(cameraPt).normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 rightDir = lookDir.cross(upDir).normalize();
        Vec3 perpUpDir = rightDir.cross(lookDir);

        Mat4 rotMat = new Mat4(1.0f);
        rotMat.set(0, rightDir, 0.0f);
        rotMat.set(1, perpUpDir, 0.0f);
        rotMat.set(2, lookDir.negate(), 0.0f);

        rotMat.transpose_();

        Mat4 transMat = new Mat4(1.0f);
        transMat.set(3, cameraPt.negate(), 1.0f);

        return rotMat.times_(transMat);
    }

    private void drawForest(GL3 gl, MatrixStack modelMatrix) {

        for (TreeData tree : forest) {
            modelMatrix
                    .push()
                    .translate(tree.xPos, 1.0f, tree.zPos);
            drawTree(gl, modelMatrix, tree.trunkHeight, tree.coneHeight);
            modelMatrix.pop();
        }
    }

    private void drawTree(GL3 gl, MatrixStack modelStack, float trunkHeight, float coneHeight) {

        //  Draw trunk
        {
            modelStack.push();

            modelStack
                    .scale(1.0f, trunkHeight, 1.0f)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.694f, 0.4f, 0.106f, 1.0f);
            meshes[MESH.CYLINDER].render(gl);
            gl.glUseProgram(0);

            modelStack.pop();
        }

        //  Draw the treetop
        {
            modelStack.push()
                    .translate(0.0f, trunkHeight, 0.0f)
                    .scale(3.0f, coneHeight, 3.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
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
                    .scale(parthenonWidth, parthenonBaseHeight, parthenonLength)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CUBE_TINT].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw top
        {
            modelMatrix.push()
                    .translate(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f)
                    .scale(parthenonWidth, parthenonTopHeight, parthenonLength)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
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
                        .translate(2.0f * iColumnNum - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval);

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
            {
                modelMatrix
                        .push()
                        .translate(2.0f * iColumnNum - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval);

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
        }
        //Don't draw the first or last columns, since they've been drawn already.
        for (int iColumnNum = 1; iColumnNum < ((int) ((parthenonLength - 2.0f) / 2.0f)); iColumnNum++) {
            {
                modelMatrix
                        .push()
                        .translate(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f);

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
            {
                modelMatrix
                        .push()
                        .translate(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f);

                drawColumn(gl, modelMatrix, parthenonColumnHeight);

                modelMatrix.pop();
            }
        }

        //  Draw interior
        {
            modelMatrix
                    .push()
                    .translate(0.0f, 1.0f, 0.0f)
                    .scale(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw headpiece
        {
            modelMatrix
                    .push()
                    .translate(
                            0.0f,
                            parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                            parthenonLength / 2.0f)
                    .rotateX(-135.0f)
                    .rotateY(45.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(objectColor.theProgram);
            gl.glUniformMatrix4fv(objectColor.modelToWorldMatrixUnif, 1, false, matBuffer);
            meshes[MESH.CUBE_COLOR].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    //Columns are 1x1 in the X/Z, and fHieght units in the Y.
    private void drawColumn(GL3 gl, MatrixStack modelMatrix, float parthenonColumnHeight) {

        final float columnBaseHeight = 0.25f;

        //Draw the bottom of the column.
        {
            modelMatrix
                    .push()
                    .scale(1.0f, columnBaseHeight, 1.0f)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl.glUniform4f(uniformColorTint.baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            meshes[MESH.CUBE_TINT].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the top of the column.
        {
            modelMatrix
                    .push()
                    .translate(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f)
                    .scale(1.0f, columnBaseHeight, 1.0f)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CUBE_TINT].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the main column.
        {
            modelMatrix
                    .push()
                    .translate(0.0f, columnBaseHeight, 0.0f)
                    .scale(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f)
                    .translate(0.0f, 0.5f, 0.0f)
                    .top().to(matBuffer);

            gl.glUseProgram(uniformColorTint.theProgram);
            gl.glUniformMatrix4fv(uniformColorTint.modelToWorldMatrixUnif, 1, false, matBuffer);
            gl.glUniform4f(uniformColorTint.baseColorUnif, 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[MESH.CYLINDER].render(gl);
            gl.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        float zNear = 1.0f, zFar = 1000.0f;

        new MatrixStack()
                .perspective(45.0f, w / (float) h, zNear, zFar)
                .top().to(matBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, globalMatricesBufferName.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(uniformColor.theProgram);
        gl.glDeleteProgram(objectColor.theProgram);
        gl.glDeleteProgram(uniformColorTint.theProgram);

        StreamEx.of(meshes).forEach(mesh -> mesh.dispose(gl));
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
//                camTarget.print("Target"); TODO
//                sphereCamRelPos.print("Position");
                break;

            case KeyEvent.VK_ESCAPE:
                quit();
                break;
        }

        sphereCamRelPos.y = glm.clamp(sphereCamRelPos.y, -78.75f, -1.0f);
        camTarget.y = glm.clamp(camTarget.y, 0.0f, camTarget.y);
        sphereCamRelPos.z = glm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z);
    }

    private class ProgramData {

        int theProgram;

        int modelToWorldMatrixUnif;
        int baseColorUnif;

        public ProgramData(GL3 gl, String vert, String frag) {

            theProgram = new Program(gl, getClass(), "tut07", vert, frag).name;

            modelToWorldMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToWorldMatrix");
            baseColorUnif = gl.glGetUniformLocation(theProgram, "baseColor");

            int globalUniformBlockIndex = gl.glGetUniformBlockIndex(theProgram, "GlobalMatrices");

            gl.glUniformBlockBinding(theProgram, globalUniformBlockIndex, Semantic.Uniform.GLOBAL_MATRICES);
        }
    }

    TreeData[] forest = {
            new TreeData(-45.0f, -40.0f, 2.0f, 3.0f),
            new TreeData(-42.0f, -35.0f, 2.0f, 3.0f),
            new TreeData(-39.0f, -29.0f, 2.0f, 4.0f),
            new TreeData(-44.0f, -26.0f, 3.0f, 3.0f),
            new TreeData(-40.0f, -22.0f, 2.0f, 4.0f),
            new TreeData(-36.0f, -15.0f, 3.0f, 3.0f),
            new TreeData(-41.0f, -11.0f, 2.0f, 3.0f),
            new TreeData(-37.0f, -6.0f, 3.0f, 3.0f),
            new TreeData(-45.0f, 0.0f, 2.0f, 3.0f),
            new TreeData(-39.0f, 4.0f, 3.0f, 4.0f),
            new TreeData(-36.0f, 8.0f, 2.0f, 3.0f),
            new TreeData(-44.0f, 13.0f, 3.0f, 3.0f),
            new TreeData(-42.0f, 17.0f, 2.0f, 3.0f),
            new TreeData(-38.0f, 23.0f, 3.0f, 4.0f),
            new TreeData(-41.0f, 27.0f, 2.0f, 3.0f),
            new TreeData(-39.0f, 32.0f, 3.0f, 3.0f),
            new TreeData(-44.0f, 37.0f, 3.0f, 4.0f),
            new TreeData(-36.0f, 42.0f, 2.0f, 3.0f),
            //
            new TreeData(-32.0f, -45.0f, 2.0f, 3.0f),
            new TreeData(-30.0f, -42.0f, 2.0f, 4.0f),
            new TreeData(-34.0f, -38.0f, 3.0f, 5.0f),
            new TreeData(-33.0f, -35.0f, 3.0f, 4.0f),
            new TreeData(-29.0f, -28.0f, 2.0f, 3.0f),
            new TreeData(-26.0f, -25.0f, 3.0f, 5.0f),
            new TreeData(-35.0f, -21.0f, 3.0f, 4.0f),
            new TreeData(-31.0f, -17.0f, 3.0f, 3.0f),
            new TreeData(-28.0f, -12.0f, 2.0f, 4.0f),
            new TreeData(-29.0f, -7.0f, 3.0f, 3.0f),
            new TreeData(-26.0f, -1.0f, 2.0f, 4.0f),
            new TreeData(-32.0f, 6.0f, 2.0f, 3.0f),
            new TreeData(-30.0f, 10.0f, 3.0f, 5.0f),
            new TreeData(-33.0f, 14.0f, 2.0f, 4.0f),
            new TreeData(-35.0f, 19.0f, 3.0f, 4.0f),
            new TreeData(-28.0f, 22.0f, 2.0f, 3.0f),
            new TreeData(-33.0f, 26.0f, 3.0f, 3.0f),
            new TreeData(-29.0f, 31.0f, 3.0f, 4.0f),
            new TreeData(-32.0f, 38.0f, 2.0f, 3.0f),
            new TreeData(-27.0f, 41.0f, 3.0f, 4.0f),
            new TreeData(-31.0f, 45.0f, 2.0f, 4.0f),
            new TreeData(-28.0f, 48.0f, 3.0f, 5.0f),
            //
            new TreeData(-25.0f, -48.0f, 2.0f, 3.0f),
            new TreeData(-20.0f, -42.0f, 3.0f, 4.0f),
            new TreeData(-22.0f, -39.0f, 2.0f, 3.0f),
            new TreeData(-19.0f, -34.0f, 2.0f, 3.0f),
            new TreeData(-23.0f, -30.0f, 3.0f, 4.0f),
            new TreeData(-24.0f, -24.0f, 2.0f, 3.0f),
            new TreeData(-16.0f, -21.0f, 2.0f, 3.0f),
            new TreeData(-17.0f, -17.0f, 3.0f, 3.0f),
            new TreeData(-25.0f, -13.0f, 2.0f, 4.0f),
            new TreeData(-23.0f, -8.0f, 2.0f, 3.0f),
            new TreeData(-17.0f, -2.0f, 3.0f, 3.0f),
            new TreeData(-16.0f, 1.0f, 2.0f, 3.0f),
            new TreeData(-19.0f, 4.0f, 3.0f, 3.0f),
            new TreeData(-22.0f, 8.0f, 2.0f, 4.0f),
            new TreeData(-21.0f, 14.0f, 2.0f, 3.0f),
            new TreeData(-16.0f, 19.0f, 2.0f, 3.0f),
            new TreeData(-23.0f, 24.0f, 3.0f, 3.0f),
            new TreeData(-18.0f, 28.0f, 2.0f, 4.0f),
            new TreeData(-24.0f, 31.0f, 2.0f, 3.0f),
            new TreeData(-20.0f, 36.0f, 2.0f, 3.0f),
            new TreeData(-22.0f, 41.0f, 3.0f, 3.0f),
            new TreeData(-21.0f, 45.0f, 2.0f, 3.0f),
            //
            new TreeData(-12.0f, -40.0f, 2.0f, 4.0f),
            new TreeData(-11.0f, -35.0f, 3.0f, 3.0f),
            new TreeData(-10.0f, -29.0f, 1.0f, 3.0f),
            new TreeData(-9.0f, -26.0f, 2.0f, 2.0f),
            new TreeData(-6.0f, -22.0f, 2.0f, 3.0f),
            new TreeData(-15.0f, -15.0f, 1.0f, 3.0f),
            new TreeData(-8.0f, -11.0f, 2.0f, 3.0f),
            new TreeData(-14.0f, -6.0f, 2.0f, 4.0f),
            new TreeData(-12.0f, 0.0f, 2.0f, 3.0f),
            new TreeData(-7.0f, 4.0f, 2.0f, 2.0f),
            new TreeData(-13.0f, 8.0f, 2.0f, 2.0f),
            new TreeData(-9.0f, 13.0f, 1.0f, 3.0f),
            new TreeData(-13.0f, 17.0f, 3.0f, 4.0f),
            new TreeData(-6.0f, 23.0f, 2.0f, 3.0f),
            new TreeData(-12.0f, 27.0f, 1.0f, 2.0f),
            new TreeData(-8.0f, 32.0f, 2.0f, 3.0f),
            new TreeData(-10.0f, 37.0f, 3.0f, 3.0f),
            new TreeData(-11.0f, 42.0f, 2.0f, 2.0f),
            //
            new TreeData(15.0f, 5.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 10.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 15.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 20.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 25.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 30.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 35.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 40.0f, 2.0f, 3.0f),
            new TreeData(15.0f, 45.0f, 2.0f, 3.0f),
            //
            new TreeData(25.0f, 5.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 10.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 15.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 20.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 25.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 30.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 35.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 40.0f, 2.0f, 3.0f),
            new TreeData(25.0f, 45.0f, 2.0f, 3.0f)};

    class TreeData {

        float xPos;
        float zPos;
        float trunkHeight;
        float coneHeight;

        TreeData(float xPos, float zPos, float trunkHeight, float coneHeight) {
            this.xPos = xPos;
            this.zPos = zPos;
            this.trunkHeight = trunkHeight;
            this.coneHeight = coneHeight;
        }
    }
}