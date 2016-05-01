/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07.worldScene;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Framework;
import framework.component.Mesh;
import framework.glutil.MatrixStack_;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import java.nio.FloatBuffer;

/**
 *
 * @author gbarbieri
 */
public class WorldScene extends Framework {

    private final String SHADERS_ROOT = "src/tut07/worldScene/shaders";
    private final String[] VERT_SHADERS_SOURCE
            = new String[]{"pos-only-world-transform", "pos-color-world-transform", "pos-color-world-transform"};
    private final String[] FRAG_SHADERS_SOURCE
            = new String[]{"color-uniform", "color-passthrough", "color-mult-uniform"};
    private final String[] MESHES_SOURCE = new String[]{"UnitConeTint.xml", "UnitCylinderTint.xml", "UnitCubeTint.xml",
        "UnitCubeColor.xml", "UnitPlane.xml"};

    public static void main(String[] args) {
        WorldScene worldScene = new WorldScene("Tutorial 06 - World Scene");
    }

    public WorldScene(String title) {
        super(title);
    }

    public class Program {

        public final static int UNIFORM_COLOR = 0;
        public final static int OBJECT_COLOR = 1;
        public final static int UNIFORM_COLOR_TINT = 2;
        public final static int MAX = 3;
    }

    public class Uniform {

        public final static int MODEL_TO_WORLD = 0;
        public final static int WORLD_TO_CAMERA = 1;
        public final static int CAMERA_TO_CLIP = 2;
        public final static int BASE_COLOR = 3;
        public final static int MAX = 4;
    }

    private class Mesh_ {

        public final static int CONE = 0;
        public final static int CYLINDER = 1;
        public final static int CUBE_TINT = 2;
        public final static int CUBE_COLOR = 3;
        public final static int PLANE = 4;
        public final static int MAX = 5;
    }

    private int[] programName = new int[Program.MAX];
    private int[][] uniform = new int[Program.MAX][Uniform.MAX];
    private Mesh[] meshes = new Mesh[Mesh_.MAX];
    public static FloatBuffer matrixBuffer = GLBuffers.newDirectFloatBuffer(16);

    private Vec3 sphereCamRelPos = new Vec3(67.5f, -46.0f, 150.0f);
    private Vec3 camTarget = new Vec3(0.0f, 0.4f, 0.0f);
    boolean drawLookAtPoint = false;

    @Override
    public void init(GL3 gl3) {

        initializePrograms(gl3);

        for (int i = 0; i < Mesh_.MAX; i++) {
            meshes[i] = new Mesh(DATA_ROOT + MESHES_SOURCE[i], gl3);
        }

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);

        gl3.glEnable(GL_DEPTH_CLAMP);
    }

    private void initializePrograms(GL3 gl3) {

        for (int i = 0; i < Program.MAX; i++) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    VERT_SHADERS_SOURCE[i], "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    FRAG_SHADERS_SOURCE[i], "frag", null, true);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.link(gl3, System.out);

            programName[i] = shaderProgram.program();

            vertShaderCode.destroy(gl3);
            fragShaderCode.destroy(gl3);

            uniform[i][Uniform.MODEL_TO_WORLD] = gl3.glGetUniformLocation(programName[i], "modelToWorldMatrix");
            uniform[i][Uniform.WORLD_TO_CAMERA] = gl3.glGetUniformLocation(programName[i], "worldToCameraMatrix");
            uniform[i][Uniform.CAMERA_TO_CLIP] = gl3.glGetUniformLocation(programName[i], "cameraToClipMatrix");
            uniform[i][Uniform.BASE_COLOR] = gl3.glGetUniformLocation(programName[i], "baseColor");
        }
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1.0f).put(1, 1.0f).put(2, 1.0f).put(3, 1.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        final Vec3 camPos = resolveCamPosition();

        MatrixStack_ camMatrix = new MatrixStack_();

        camMatrix.setMatrix(calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)));

        for (int i = 0; i < Program.MAX; i++) {

            gl3.glUseProgram(programName[i]);
            gl3.glUniformMatrix4fv(uniform[i][Uniform.WORLD_TO_CAMERA], 1, false, camMatrix.top().toDfb(matrixBuffer));
        }

        MatrixStack_ modelMatrix = new MatrixStack_();

        //  Render the ground plane
        {
            modelMatrix.push();

            modelMatrix.scale(new Vec3(100.0f, 1.0f, 100.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR][Uniform.BASE_COLOR], 0.302f, 0.416f, 0.0589f, 1.0f);
            meshes[Mesh_.PLANE].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //  Draw the trees
        drawForest(gl3, modelMatrix);

        //  Draw the building
        {
            modelMatrix.push();
            modelMatrix.translate(new Vec3(20.0f, 0.0f, -10.0f));

            drawParthenon(gl3, modelMatrix);

            modelMatrix.pop();
        }

        if (drawLookAtPoint) {

            gl3.glDisable(GL3.GL_DEPTH_TEST);
            Mat4 identity = new Mat4(1.0f);

            modelMatrix.push();

            Vec3 cameraAimVec = camTarget.sub_(camPos.x);

            modelMatrix.translate(new Vec3(0.0f, 0.0f, -cameraAimVec.length()));
            modelMatrix.scale(new Vec3(1.0f));

            gl3.glUseProgram(programName[Program.OBJECT_COLOR]);
            gl3.glUniformMatrix4fv(uniform[Program.OBJECT_COLOR][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix.top().toDfb(matrixBuffer));
            gl3.glUniformMatrix4fv(uniform[Program.OBJECT_COLOR][Uniform.WORLD_TO_CAMERA], 1, false,
                    identity.toDfb(matrixBuffer));
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();

            gl3.glEnable(GL3.GL_DEPTH_TEST);
        }
    }

    private Vec3 resolveCamPosition() {

        float phi = (float) Math.toRadians(sphereCamRelPos.x);
        float theta = (float) Math.toRadians(sphereCamRelPos.y + 90.0f);

        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        return dirToCamera.mul(sphereCamRelPos.z).add(camTarget);
    }

    private void drawForest(GL3 gl3, MatrixStack_ modelMatrix_) {

        for (Forest.Tree tree : Forest.trees) {

            modelMatrix_.push();
            modelMatrix_.translate(new Vec3(tree.xPos, 0.0f, tree.zPos));
            drawTree(gl3, modelMatrix_, tree.trunkHeight, tree.coneHeight);
            modelMatrix_.pop();
        }
    }

    private void drawTree(GL3 gl3, MatrixStack_ modelStack_, float trunkHeight, float coneHeight) {

        //  Draw trunk
        {
            modelStack_.push();

            modelStack_.scale(new Vec3(1.0f, trunkHeight, 1.0f));
            modelStack_.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelStack_.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.694f, 0.4f, 0.106f, 1.0f);
            meshes[Mesh_.CYLINDER].render(gl3);
            gl3.glUseProgram(0);

            modelStack_.pop();
        }

        //  Draw the treetop
        {
            modelStack_.push();

            modelStack_.translate(new Vec3(0.0f, trunkHeight, 0.0f));
            modelStack_.scale(new Vec3(3.0f, coneHeight, 3.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelStack_.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.0f, 1.0f, 0.0f, 1.0f);
            meshes[Mesh_.CONE].render(gl3);
            gl3.glUseProgram(0);

            modelStack_.pop();
        }
    }

    private void drawParthenon(GL3 gl3, MatrixStack_ modelMatrix_) {

        final float parthenonWidth = 14.0f;
        final float parthenonLength = 20.0f;
        final float parthenonColumnHeight = 5.0f;
        final float parthenonBaseHeight = 1.0f;
        final float parthenonTopHeight = 2.0f;

        //  Draw base
        {
            modelMatrix_.push();

            modelMatrix_.scale(new Vec3(parthenonWidth, parthenonBaseHeight, parthenonLength));
            modelMatrix_.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix_.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }

        //  Draw top
        {
            modelMatrix_.push();
            {
                modelMatrix_.translate(new Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f));
                modelMatrix_.scale(new Vec3(parthenonWidth, parthenonTopHeight, parthenonLength));
                modelMatrix_.translate(new Vec3(0.0f, 0.5f, 0.0f));

                gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
                gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                        modelMatrix_.top().toDfb(matrixBuffer));
                gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.9f, 0.9f, 0.9f, 0.9f);
                meshes[Mesh_.CUBE_TINT].render(gl3);
                gl3.glUseProgram(0);
            }
            modelMatrix_.pop();
        }

        //  Draw columns
        final float frontZval = parthenonLength / 2.0f - 1.0f;
        final float rightXval = parthenonWidth / 2.0f - 1.0f;

        for (int iColumnNum = 0; iColumnNum < ((int) parthenonWidth / 2.0f); iColumnNum++) {
            {
                modelMatrix_.push();
                modelMatrix_.translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
            {
                modelMatrix_.push();
                modelMatrix_.translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
        }
        //Don't draw the first or last columns, since they've been drawn already.
        for (int iColumnNum = 1; iColumnNum < ((int) ((parthenonLength - 2.0f) / 2.0f)); iColumnNum++) {
            {
                modelMatrix_.push();
                modelMatrix_.translate(new Vec3(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
            {
                modelMatrix_.push();
                modelMatrix_.translate(new Vec3(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                drawColumn(gl3, modelMatrix_, parthenonColumnHeight);

                modelMatrix_.pop();
            }
        }

        //  Draw interior
        {
            modelMatrix_.push();

            modelMatrix_.translate(new Vec3(0.0f, 1.0f, 0.0f));
            modelMatrix_.scale(new Vec3(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f));
            modelMatrix_.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.OBJECT_COLOR]);
            gl3.glUniformMatrix4fv(uniform[Program.OBJECT_COLOR][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix_.top().toDfb(matrixBuffer));
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }

        //  Draw headpiece
        {
            modelMatrix_.push();

            modelMatrix_.translate(new Vec3(
                    0.0f,
                    parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f,
                    parthenonLength / 2.0f));
            modelMatrix_.rotateX(-135.0f);
            modelMatrix_.rotateY(45.0f);

            gl3.glUseProgram(programName[Program.OBJECT_COLOR]);
            gl3.glUniformMatrix4fv(uniform[Program.OBJECT_COLOR][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix_.top().toDfb(matrixBuffer));
            meshes[Mesh_.CUBE_COLOR].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix_.pop();
        }
    }

    //Columns are 1x1 in the X/Z, and fHieght units in the Y.
    private void drawColumn(GL3 gl3, MatrixStack_ modelMatrix, float parthenonColumnHeight) {

        final float columnBaseHeight = 0.25f;

        //Draw the bottom of the column.
        {
            modelMatrix.push();

            modelMatrix.scale(new Vec3(1.0f, columnBaseHeight, 1.0f));
            modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 1.0f, 1.0f, 1.0f, 1.0f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the top of the column.
        {
            modelMatrix.push();

            modelMatrix.translate(new Vec3(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f));
            modelMatrix.scale(new Vec3(1.0f, columnBaseHeight, 1.0f));
            modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CUBE_TINT].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }

        //Draw the main column.
        {
            modelMatrix.push();

            modelMatrix.translate(new Vec3(0.0f, columnBaseHeight, 0.0f));
            modelMatrix.scale(new Vec3(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f));
            modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

            gl3.glUseProgram(programName[Program.UNIFORM_COLOR_TINT]);
            gl3.glUniformMatrix4fv(uniform[Program.UNIFORM_COLOR_TINT][Uniform.MODEL_TO_WORLD], 1, false,
                    modelMatrix.top().toDfb(matrixBuffer));
            gl3.glUniform4f(uniform[Program.UNIFORM_COLOR_TINT][Uniform.BASE_COLOR], 0.9f, 0.9f, 0.9f, 0.9f);
            meshes[Mesh_.CYLINDER].render(gl3);
            gl3.glUseProgram(0);

            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1000.0f;

        MatrixStack_ perspectiveMatrix = new MatrixStack_();

        perspectiveMatrix.setMatrix(glm.perspective_(45.0f, w / (float) h, zNear, zFar));

        for (int i = 0; i < Program.MAX; i++) {

            gl3.glUseProgram(programName[i]);
            gl3.glUniformMatrix4fv(uniform[i][Uniform.CAMERA_TO_CLIP], 1, false,
                    perspectiveMatrix.top().toDfb(matrixBuffer));
        }
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    private Mat4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.sub_(cameraPt).normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 rightDir = lookDir.cross_(upDir).normalize();
        Vec3 perpUpDir = rightDir.cross_(lookDir);

        Mat4 rotationMat = new Mat4(1.0f);
        rotationMat.c0(rightDir, 0.0f);
        rotationMat.c1(perpUpDir, 0.0f);
        rotationMat.c2(lookDir.negate(), 0.0f);

        rotationMat = rotationMat.transpose();

        Mat4 translationMat = new Mat4(1.0f);
        translationMat.c3(cameraPt.negate(), 1.0f);

        return rotationMat.mul(translationMat);
    }

    @Override
    public void end(GL3 gl3) {

        for (int i = 0; i < Program.MAX; i++) {
            gl3.glDeleteProgram(programName[i]);
        }
    }

    @Override
    public void keyboard(KeyEvent e) {

        float factor;

        factor = e.isShiftDown() ? 10 : 1;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
                camTarget.z -= 4.0f / factor;
                break;

            case KeyEvent.VK_S:
                camTarget.z += 4.0f / factor;
                break;

            case KeyEvent.VK_D:
                camTarget.x += 4.0f / factor;
                break;

            case KeyEvent.VK_A:
                camTarget.x -= 4.0f / factor;
                break;

            case KeyEvent.VK_E:
                camTarget.y -= 4.0f / factor;
                break;

            case KeyEvent.VK_Q:
                camTarget.y += 4.0f / factor;
                break;

            case KeyEvent.VK_I:
                sphereCamRelPos.y -= 11.25f / factor;
                break;

            case KeyEvent.VK_K:
                sphereCamRelPos.y += 11.25f / factor;
                break;

            case KeyEvent.VK_J:
                sphereCamRelPos.x -= 11.25f / factor;
                break;

            case KeyEvent.VK_L:
                sphereCamRelPos.x += 11.25f / factor;
                break;

            case KeyEvent.VK_O:
                sphereCamRelPos.z -= 5.0f / factor;
                break;

            case KeyEvent.VK_U:
                sphereCamRelPos.z += 5.0f / factor;
                break;

            case KeyEvent.VK_SPACE:
                drawLookAtPoint = !drawLookAtPoint;
//                camTarget.print("Target:");
//                sphereCamRelPos.print("Position:");
                break;

            case KeyEvent.VK_ESCAPE:
                animator.stop();
                glWindow.destroy();
                break;
        }

//        sphereCamRelPos.y = Jglm.clamp(sphereCamRelPos.y, -78.75f, -1.0f);
//        camTarget.y = Jglm.clamp(camTarget.y, 0.0f, camTarget.y);
//        sphereCamRelPos.z = Jglm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z);
    }
}
