/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut07;

import com.jogamp.opengl.util.GLBuffers;
import glsl.GLSLProgramObject;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec3;
import jglm.Vec4;
import mesh.Mesh;
import stack.MatrixStack;
import tut07.glsl.GLSLProgramObject_2;

/**
 *
 * @author gbarbieri
 */
public class WorldSceneUBO implements GLEventListener, KeyListener {

    private int imageWidth = 800;
    private int imageHeight = 600;
    private GLCanvas canvas;
    private GLSLProgramObject_2 uniformColor;
    private GLSLProgramObject_2 objectColor;
    private GLSLProgramObject_2 uniformColorTint;
    private int[] VBO = new int[1];
    private int[] IBO = new int[1];
    private int[] VAO = new int[1];
    private String shadersFilepath = "/tut07/shaders/";
    private String dataFilepath = "/tut07/data/";
    private Mat4 cameraToClipMatrix = new Mat4();
    private Mesh cone;
    private Mesh cylinder;
    private Mesh cubeTint;
    private Mesh cubeColor;
    private Mesh plane;
    private Vec3 sphereCamRelPos;
    private Vec3 camTarget;
    private float zNear = 1.0f;
    private float zFar = 1000.0f;
    private Forest forest;
    private boolean drawLookAtPoint;
    private int[] globalMatricesUBO;
    private int globalMatricesBindingIndex;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        WorldSceneUBO worldSceneUBO = new WorldSceneUBO();

        Frame frame = new Frame("Tutorial 07 - World Scene UBO");

        frame.add(worldSceneUBO.getCanvas());

        frame.setSize(worldSceneUBO.getCanvas().getWidth(), worldSceneUBO.getCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    public WorldSceneUBO() {
        initGL();
    }

    private void initGL() {
        GLProfile profile = GLProfile.getDefault();

        GLCapabilities capabilities = new GLCapabilities(profile);

        canvas = new GLCanvas(capabilities);

        canvas.setSize(imageWidth, imageHeight);

        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        canvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();

        globalMatricesBindingIndex = 0;

        initializePrograms(gl3);

        initializeObjects(gl3);

        forest = new Forest();

        sphereCamRelPos = new Vec3(67.5f, -46.0f, 150.0f);

        camTarget = new Vec3(0.0f, 0.4f, 0.0f);

        drawLookAtPoint = false;

        gl3.glEnable(GL3.GL_CULL_FACE);
        gl3.glCullFace(GL3.GL_BACK);
        gl3.glFrontFace(GL3.GL_CW);

        gl3.glEnable(GL3.GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL3.GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);

        gl3.glEnable(GL3.GL_DEPTH_CLAMP);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        gl3.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glClearDepthf(1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        Vec3 camPos = resolveCamPosition();

        MatrixStack camMatrix = new MatrixStack();

        camMatrix.setTop(CalcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)));

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, globalMatricesUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 16 * 4, 16 * 4, GLBuffers.newDirectFloatBuffer(camMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        MatrixStack modelMatrix = new MatrixStack();

        //  Render the ground plane
        modelMatrix.push();
        {
            modelMatrix.scale(new Vec3(100.0f, 1.0f, 100.0f));

            uniformColor.bind(gl3);
            {
                gl3.glUniformMatrix4fv(uniformColor.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                gl3.glUniform4f(uniformColor.getBaseColorUnLoc(), 0.302f, 0.416f, 0.0589f, 1.0f);
                plane.render(gl3);
            }
            uniformColor.unbind(gl3);
        }
        modelMatrix.pop();

        //  Draw the trees
        drawForest(gl3, modelMatrix);

        //  Draw the building
        modelMatrix.push();
        {
            modelMatrix.translate(new Vec3(20.0f, 0.0f, -10.0f));

            drawParthenon(gl3, modelMatrix);
        }
        modelMatrix.pop();

        if (drawLookAtPoint) {

            gl3.glDisable(GL3.GL_DEPTH_TEST);
            {
                modelMatrix.push();
                {
                    modelMatrix.translate(camTarget);
                    modelMatrix.scale(new Vec3(1.0f, 1.0f, 1.0f));
                    
                    objectColor.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(objectColor.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                        
                        cubeColor.render(gl3);
                    }
                    objectColor.unbind(gl3);
                }
                modelMatrix.pop();
            }
            gl3.glEnable(GL3.GL_DEPTH_TEST);
        }

        glad.swapBuffers();
    }

    private void drawParthenon(GL3 gl3, MatrixStack modelMatrix) {

        float parthenonWidth = 14.0f;
        float parthenonLength = 20.0f;
        float parthenonColumnHeight = 5.0f;
        float parthenonBaseHeight = 1.0f;
        float parthenonTopHeight = 2.0f;

        //  Draw base
        {
            modelMatrix.push();
            {
                modelMatrix.scale(new Vec3(parthenonWidth, parthenonBaseHeight, parthenonLength));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.9f, 0.9f, 0.9f, 0.9f);

                    cubeTint.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelMatrix.pop();
        }

        //  Draw top
        {
            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight, 0.0f));
                modelMatrix.scale(new Vec3(parthenonWidth, parthenonTopHeight, parthenonLength));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.9f, 0.9f, 0.9f, 0.9f);

                    cubeTint.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelMatrix.pop();
        }

        //  Draw columns
        float frontZval = parthenonLength / 2.0f - 1.0f;
        float rightXval = parthenonWidth / 2.0f - 1.0f;

        for (int iColumnNum = 0; iColumnNum < ((int) parthenonWidth / 2.0f); iColumnNum++) {
            {
                modelMatrix.push();
                {
                    modelMatrix.translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2 + 1.0f, parthenonBaseHeight, frontZval));

                    drawColumn(gl3, modelMatrix, parthenonColumnHeight);
                }
                modelMatrix.pop();
            }
            {
                modelMatrix.push();
                {
                    modelMatrix.translate(new Vec3(2.0f * iColumnNum - parthenonWidth / 2.0f + 1.0f, parthenonBaseHeight, -frontZval));

                    drawColumn(gl3, modelMatrix, parthenonColumnHeight);
                }
                modelMatrix.pop();
            }
        }
        for (int iColumnNum = 1; iColumnNum < ((int) ((parthenonLength - 2.0f) / 2.0f)); iColumnNum++) {
            {
                modelMatrix.push();
                {
                    modelMatrix.translate(new Vec3(rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                    drawColumn(gl3, modelMatrix, parthenonColumnHeight);
                }
                modelMatrix.pop();
            }
            {
                modelMatrix.push();
                {
                    modelMatrix.translate(new Vec3(-rightXval, parthenonBaseHeight, 2.0f * iColumnNum - parthenonLength / 2.0f + 1.0f));

                    drawColumn(gl3, modelMatrix, parthenonColumnHeight);
                }
                modelMatrix.pop();
            }
        }

        //  Draw interior
        {
            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(0.0f, 1.0f, 0.0f));
                modelMatrix.scale(new Vec3(parthenonWidth - 6.0f, parthenonColumnHeight, parthenonLength - 6.0f));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                objectColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(objectColor.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    cubeColor.render(gl3);
                }
                objectColor.unbind(gl3);
            }
            modelMatrix.pop();
        }

        //  Draw headpiece
        {
            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(0.0f, parthenonColumnHeight + parthenonBaseHeight + parthenonTopHeight / 2.0f, parthenonLength / 2.0f));
                modelMatrix.rotateX(-135.0f);
                modelMatrix.rotateY(45.0f);

                objectColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(objectColor.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    cubeColor.render(gl3);
                }
                objectColor.unbind(gl3);
            }
            modelMatrix.pop();
        }
    }

    private void drawColumn(GL3 gl3, MatrixStack modelMatrix, float parthenonColumnHeight) {

        float columnBaseHeight = 0.25f;

        //  Draw the column bottom
        {
            modelMatrix.push();
            {
                modelMatrix.scale(new Vec3(1.0f, columnBaseHeight, 1.0f));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                    cubeTint.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelMatrix.pop();
        }

        //  Draw the column top
        {
            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(0.0f, parthenonColumnHeight - columnBaseHeight, 0.0f));
                modelMatrix.scale(new Vec3(1.0f, columnBaseHeight, 1.0f));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.9f, 0.9f, 0.9f, 0.9f);

                    cubeTint.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelMatrix.pop();
        }

        //  Draw the main column
        {
            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(0.0f, columnBaseHeight, 0.0f));
                modelMatrix.scale(new Vec3(0.8f, parthenonColumnHeight - columnBaseHeight * 2.0f, 0.8f));
                modelMatrix.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.9f, 0.9f, 0.9f, 0.9f);

                    cylinder.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelMatrix.pop();
        }
    }

    private void drawForest(GL3 gl3, MatrixStack modelMatrix) {

        for (Tree tree : forest.forest) {

            modelMatrix.push();
            {
                modelMatrix.translate(new Vec3(tree.xPos, 0.0f, tree.zPos));

                drawTree(gl3, modelMatrix, tree.trunkHeight, tree.coneHeight);
            }
            modelMatrix.pop();
        }
    }

    private void drawTree(GL3 gl3, MatrixStack modelStack, float trunkHeight, float coneHeight) {

        //  Draw trunk
        {
            modelStack.push();
            {
                modelStack.scale(new Vec3(1.0f, trunkHeight, 1.0f));
                modelStack.translate(new Vec3(0.0f, 0.5f, 0.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelStack.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.694f, 0.4f, 0.106f, 1.0f);

                    cylinder.render(gl3);
                }
                uniformColorTint.unbind(gl3);
            }
            modelStack.pop();
        }

        //  Draw the treetop
        {
            modelStack.push();
            {
                modelStack.translate(new Vec3(0.0f, trunkHeight, 0.0f));
                modelStack.scale(new Vec3(3.0f, coneHeight, 3.0f));

                uniformColorTint.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(uniformColorTint.getModelToWorldMatUnLoc(), 1, false, modelStack.top().toFloatArray(), 0);
                    gl3.glUniform4f(uniformColorTint.getBaseColorUnLoc(), 0.0f, 1.0f, 0.0f, 1.0f);

                    cone.render(gl3);
                }
                uniformColor.unbind(gl3);
            }
            modelStack.pop();
        }
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        MatrixStack perspectiveMatrix = new MatrixStack();

        perspectiveMatrix.perspective(45.0f, w / (float) h, zNear, zFar);

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, globalMatricesUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    private void initializeObjects(GL3 gl3) {
        System.out.println("initializeObjects");

        cone = new Mesh(dataFilepath + "UnitConeTint.xml", gl3);
        cylinder = new Mesh(dataFilepath + "UnitCylinderTint.xml", gl3);
        cubeTint = new Mesh(dataFilepath + "UnitCubeTint.xml", gl3);
        cubeColor = new Mesh(dataFilepath + "UnitCubeColor.xml", gl3);
        plane = new Mesh(dataFilepath + "UnitPlane.xml", gl3);
    }

    private void initializePrograms(GL3 gl3) {
        System.out.println("initializePrograms...");

        uniformColor = new GLSLProgramObject_2(gl3, shadersFilepath, "PosOnlyWorldTransformUBO_VS.glsl", "ColorUniform_FS.glsl", globalMatricesBindingIndex);
        objectColor = new GLSLProgramObject_2(gl3, shadersFilepath, "PosColorWorldTransformUBO_VS.glsl", "ColorPassthrough_FS.glsl", globalMatricesBindingIndex);
        uniformColorTint = new GLSLProgramObject_2(gl3, shadersFilepath, "PosColorWorldTransformUBO_VS.glsl", "ColorMultUniform_FS.glsl", globalMatricesBindingIndex);

        globalMatricesUBO = new int[1];

        gl3.glGenBuffers(1, IntBuffer.wrap(globalMatricesUBO));
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, globalMatricesUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, 2 * 16 * 4, null, GL3.GL_STREAM_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, globalMatricesBindingIndex, globalMatricesUBO[0], 0, 2 * 16 * 4);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }

    private Vec3 resolveCamPosition() {

        float phi = (float) Math.toRadians(sphereCamRelPos.x);
        float theta = (float) Math.toRadians(sphereCamRelPos.y + 90.0f);

        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float cosPhi = (float) Math.cos(phi);
        float sinPhi = (float) Math.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        dirToCamera = dirToCamera.times(sphereCamRelPos.z);

        return dirToCamera.plus(camTarget);
    }

    private Mat4 CalcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.minus(cameraPt);
        lookDir = lookDir.normalize();

        Vec3 upDir = upPt.normalize();

        Vec3 crossProduct = lookDir.crossProduct(upDir);
        Vec3 rightDir = crossProduct.normalize();

        Vec3 perpUpDir = rightDir.crossProduct(lookDir);

        Mat4 rotationMat = new Mat4(1.0f);
        rotationMat.c0 = new Vec4(rightDir, 0.0f);
        rotationMat.c1 = new Vec4(perpUpDir, 0.0f);
        rotationMat.c2 = new Vec4(lookDir.negated(), 0.0f);

        rotationMat = rotationMat.transpose();

        Mat4 translationMat = new Mat4(1.0f);
        translationMat.c3 = new Vec4(cameraPt.negated(), 1.0f);

        return rotationMat.times(translationMat);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

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
                camTarget.print("Target:");
                sphereCamRelPos.print("Position:");
                break;
        }

        sphereCamRelPos.y = Jglm.clamp(sphereCamRelPos.y, -78.75f, -1.0f);
        camTarget.y = Jglm.clamp(camTarget.y, 0.0f, camTarget.y);
        sphereCamRelPos.z = Jglm.clamp(sphereCamRelPos.z, 5.0f, sphereCamRelPos.z);

        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    class Tree {

        private float xPos;
        private float zPos;
        private float trunkHeight;
        private float coneHeight;

        public Tree(float xPos, float zpos, float trunkHeight, float coneHeight) {
            this.xPos = xPos;
            this.zPos = zpos;
            this.trunkHeight = trunkHeight;
            this.coneHeight = coneHeight;
        }
    }

    class Forest {

        private ArrayList<Tree> forest;

        public Forest() {

            forest = new ArrayList<>();

            forest.add(new Tree(-45.0f, -40.0f, 2.0f, 3.0f));
            forest.add(new Tree(-42.0f, -35.0f, 2.0f, 3.0f));
            forest.add(new Tree(-39.0f, -29.0f, 2.0f, 4.0f));
            forest.add(new Tree(-44.0f, -26.0f, 3.0f, 3.0f));
            forest.add(new Tree(-40.0f, -22.0f, 2.0f, 4.0f));
            forest.add(new Tree(-36.0f, -15.0f, 3.0f, 3.0f));
            forest.add(new Tree(-41.0f, -11.0f, 2.0f, 3.0f));
            forest.add(new Tree(-37.0f, -6.0f, 3.0f, 3.0f));
            forest.add(new Tree(-45.0f, 0.0f, 2.0f, 3.0f));
            forest.add(new Tree(-39.0f, 4.0f, 3.0f, 4.0f));
            forest.add(new Tree(-36.0f, 8.0f, 2.0f, 3.0f));
            forest.add(new Tree(-44.0f, 13.0f, 3.0f, 3.0f));
            forest.add(new Tree(-42.0f, 17.0f, 2.0f, 3.0f));
            forest.add(new Tree(-38.0f, 23.0f, 3.0f, 4.0f));
            forest.add(new Tree(-41.0f, 27.0f, 2.0f, 3.0f));
            forest.add(new Tree(-39.0f, 32.0f, 3.0f, 3.0f));
            forest.add(new Tree(-44.0f, 37.0f, 3.0f, 4.0f));
            forest.add(new Tree(-36.0f, 42.0f, 2.0f, 3.0f));
//            
            forest.add(new Tree(-32.0f, -45.0f, 2.0f, 3.0f));
            forest.add(new Tree(-30.0f, -42.0f, 2.0f, 4.0f));
            forest.add(new Tree(-34.0f, -38.0f, 3.0f, 5.0f));
            forest.add(new Tree(-33.0f, -35.0f, 3.0f, 4.0f));
            forest.add(new Tree(-29.0f, -28.0f, 2.0f, 3.0f));
            forest.add(new Tree(-26.0f, -25.0f, 3.0f, 5.0f));
            forest.add(new Tree(-35.0f, -21.0f, 3.0f, 4.0f));
            forest.add(new Tree(-31.0f, -17.0f, 3.0f, 3.0f));
            forest.add(new Tree(-28.0f, -12.0f, 2.0f, 4.0f));
            forest.add(new Tree(-29.0f, -7.0f, 3.0f, 3.0f));
            forest.add(new Tree(-26.0f, -1.0f, 2.0f, 4.0f));
            forest.add(new Tree(-32.0f, 6.0f, 2.0f, 3.0f));
            forest.add(new Tree(-30.0f, 10.0f, 3.0f, 5.0f));
            forest.add(new Tree(-33.0f, 14.0f, 2.0f, 4.0f));
            forest.add(new Tree(-35.0f, 19.0f, 3.0f, 4.0f));
            forest.add(new Tree(-28.0f, 22.0f, 2.0f, 3.0f));
            forest.add(new Tree(-33.0f, 26.0f, 3.0f, 3.0f));
            forest.add(new Tree(-29.0f, 31.0f, 3.0f, 4.0f));
            forest.add(new Tree(-32.0f, 38.0f, 2.0f, 3.0f));
            forest.add(new Tree(-27.0f, 41.0f, 3.0f, 4.0f));
            forest.add(new Tree(-31.0f, 45.0f, 2.0f, 4.0f));
            forest.add(new Tree(-28.0f, 48.0f, 3.0f, 5.0f));
//            
            forest.add(new Tree(-25.0f, -48.0f, 2.0f, 3.0f));
            forest.add(new Tree(-20.0f, -42.0f, 3.0f, 4.0f));
            forest.add(new Tree(-22.0f, -39.0f, 2.0f, 3.0f));
            forest.add(new Tree(-19.0f, -34.0f, 2.0f, 3.0f));
            forest.add(new Tree(-23.0f, -30.0f, 3.0f, 4.0f));
            forest.add(new Tree(-24.0f, -24.0f, 2.0f, 3.0f));
            forest.add(new Tree(-16.0f, -21.0f, 2.0f, 3.0f));
            forest.add(new Tree(-17.0f, -17.0f, 3.0f, 3.0f));
            forest.add(new Tree(-25.0f, -13.0f, 2.0f, 4.0f));
            forest.add(new Tree(-23.0f, -8.0f, 2.0f, 3.0f));
            forest.add(new Tree(-17.0f, -2.0f, 3.0f, 3.0f));
            forest.add(new Tree(-16.0f, 1.0f, 2.0f, 3.0f));
            forest.add(new Tree(-19.0f, 4.0f, 3.0f, 3.0f));
            forest.add(new Tree(-22.0f, 8.0f, 2.0f, 4.0f));
            forest.add(new Tree(-21.0f, 14.0f, 2.0f, 3.0f));
            forest.add(new Tree(-16.0f, 19.0f, 2.0f, 3.0f));
            forest.add(new Tree(-23.0f, 24.0f, 3.0f, 3.0f));
            forest.add(new Tree(-18.0f, 28.0f, 2.0f, 4.0f));
            forest.add(new Tree(-24.0f, 31.0f, 2.0f, 3.0f));
            forest.add(new Tree(-20.0f, 36.0f, 2.0f, 3.0f));
            forest.add(new Tree(-22.0f, 41.0f, 3.0f, 3.0f));
            forest.add(new Tree(-21.0f, 45.0f, 2.0f, 3.0f));
//            
            forest.add(new Tree(-12.0f, -40.0f, 2.0f, 4.0f));
            forest.add(new Tree(-11.0f, -35.0f, 3.0f, 3.0f));
            forest.add(new Tree(-10.0f, -29.0f, 1.0f, 3.0f));
            forest.add(new Tree(-9.0f, -26.0f, 2.0f, 2.0f));
            forest.add(new Tree(-6.0f, -22.0f, 2.0f, 3.0f));
            forest.add(new Tree(-15.0f, -15.0f, 1.0f, 3.0f));
            forest.add(new Tree(-8.0f, -11.0f, 2.0f, 3.0f));
            forest.add(new Tree(-14.0f, -6.0f, 2.0f, 4.0f));
            forest.add(new Tree(-12.0f, 0.0f, 2.0f, 3.0f));
            forest.add(new Tree(-7.0f, 4.0f, 2.0f, 2.0f));
            forest.add(new Tree(-13.0f, 8.0f, 2.0f, 2.0f));
            forest.add(new Tree(-9.0f, 13.0f, 1.0f, 3.0f));
            forest.add(new Tree(-13.0f, 17.0f, 3.0f, 4.0f));
            forest.add(new Tree(-6.0f, 23.0f, 2.0f, 3.0f));
            forest.add(new Tree(-12.0f, 27.0f, 1.0f, 2.0f));
            forest.add(new Tree(-8.0f, 32.0f, 2.0f, 3.0f));
            forest.add(new Tree(-10.0f, 37.0f, 3.0f, 3.0f));
            forest.add(new Tree(-11.0f, 42.0f, 2.0f, 2.0f));
//            
            forest.add(new Tree(15.0f, 5.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 10.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 15.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 20.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 25.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 30.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 35.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 40.0f, 2.0f, 3.0f));
            forest.add(new Tree(15.0f, 45.0f, 2.0f, 3.0f));
//            
            forest.add(new Tree(25.0f, 5.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 10.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 15.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 20.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 25.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 30.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 35.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 40.0f, 2.0f, 3.0f));
            forest.add(new Tree(25.0f, 45.0f, 2.0f, 3.0f));
        }
    }
}