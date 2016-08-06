/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.basicLighting;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.util.GLBuffers;
import framework.BufferUtils;
import framework.Framework;
import framework.Semantic;
import framework.component.Mesh;
import framework.glutil.MatrixStack_;
import framework.glutil.ObjectData;
import framework.glutil.ObjectPole;
import framework.glutil.ViewData;
import framework.glutil.ViewPole;
import framework.glutil.ViewScale;
import glm.mat._3.Mat3;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author gbarbieri
 */
public class BasicLighting extends Framework {

    private final String SHADERS_ROOT = "/tut09/basicLighting/shaders", DATA_ROOT = "/tut09/basicLighting/data/",
            WHITE_VERT_SHADER_SRC = "dir-vertex-lighting-pn", COLOR_VERT_SHADER_SRC = "dir-vertex-lighting-pcn",
            FRAG_SHADER_SRC = "color-passthrough", CYLINDER_SRC = "UnitCylinder.xml", PLANE_SRC = "LargePlane.xml";

    public static void main(String[] args) {
        BasicLighting basicLighting = new BasicLighting("Tutorial 09 - Basic Lighting");
    }

    public BasicLighting(String title) {
        super(title);
    }

    ProgramData whiteDiffuseColor, vertexDiffuseColor;
    private Mesh cylinderMesh, planeMesh;
    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(20.0f) / 2.0));
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private FloatBuffer matrixBuffer = GLBuffers.newDirectFloatBuffer(16),
            vecBuffer = GLBuffers.newDirectFloatBuffer(4);

    private ViewData initialViewData = new ViewData(
            new Vec3(0.0f, 0.5f, 0.0f),
            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            5.0f,
            0.0f);

    private ViewScale viewScale = new ViewScale(
            3.0f, 20.0f,
            1.5f, 0.5f,
            0.0f, 0.0f, //No camera movement.
            90.0f / 250.0f);

    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);

    private ObjectData initialObjectData = new ObjectData(
            new Vec3(0.0f, 0.5f, 0.0f),
            new Quat(1.0f, 0.0f, 0.0f, 0.0f));

    private ObjectPole objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole);

    private Vec4 lightDirection = new Vec4(0.866f, 0.5f, 0.0f, 0.0f);

    private boolean drawColoredCyl = true;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        try {
            cylinderMesh = new Mesh(DATA_ROOT + CYLINDER_SRC, gl3);
            planeMesh = new Mesh(DATA_ROOT + PLANE_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(BasicLighting.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
        gl3.glEnable(GL_DEPTH_CLAMP);

        gl3.glGenBuffers(1, projectionUniformBuffer);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer.get(0),
                0, Mat4.SIZE);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void initializeProgram(GL3 gl3) {
        whiteDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, WHITE_VERT_SHADER_SRC, FRAG_SHADER_SRC);
        vertexDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, COLOR_VERT_SHADER_SRC, FRAG_SHADER_SRC);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack_ modelMatrix = new MatrixStack_().setMatrix(viewPole.calcMatrix());

        Vec4 lightDirCameraSpace = modelMatrix.top().mul_(lightDirection);

        gl3.glUseProgram(whiteDiffuseColor.theProgram);
        gl3.glUniform3fv(whiteDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.toDfb(vecBuffer));
        gl3.glUseProgram(vertexDiffuseColor.theProgram);
        gl3.glUniform3fv(vertexDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.toDfb(vecBuffer));
        gl3.glUseProgram(0);

        {
            modelMatrix.push();

            //  Render the ground plane
            {
                modelMatrix
                        .push()
                        .top().toDfb(matrixBuffer);

                gl3.glUseProgram(whiteDiffuseColor.theProgram);
                gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matrixBuffer);
                Mat3 normalMatrix = new Mat3(modelMatrix.top());
                gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                        normalMatrix.toDfb(matrixBuffer));
                gl3.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                planeMesh.render(gl3);
                gl3.glUseProgram(0);

                modelMatrix.pop();
            }

            //  Render the Cylinder
            {
                modelMatrix
                        .push()
                        .applyMat(objectPole.calcMatrix())
                        .top().toDfb(matrixBuffer);

                if (drawColoredCyl) {

                    gl3.glUseProgram(vertexDiffuseColor.theProgram);
                    gl3.glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matrixBuffer);
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl3.glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                            normalMatrix.toDfb(matrixBuffer));
                    gl3.glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                    cylinderMesh.render(gl3, "lit-color");

                } else {

                    gl3.glUseProgram(whiteDiffuseColor.theProgram);
                    gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matrixBuffer);
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                            normalMatrix.toDfb(matrixBuffer));
                    gl3.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                    cylinderMesh.render(gl3, "lit");
                }
                gl3.glUseProgram(0);

                modelMatrix.pop();
            }
            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack_ perspMatrix = new MatrixStack_();

        perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().toDfb(matrixBuffer));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void keyboard(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;

            case KeyEvent.VK_SPACE:
                drawColoredCyl = !drawColoredCyl;
                break;
        }
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(vertexDiffuseColor.theProgram);
        gl3.glDeleteProgram(whiteDiffuseColor.theProgram);

        cylinderMesh.dispose(gl3);
        planeMesh.dispose(gl3);

        BufferUtils.destroyDirectBuffer(projectionUniformBuffer);
        BufferUtils.destroyDirectBuffer(matrixBuffer);
        BufferUtils.destroyDirectBuffer(vecBuffer);
    }
}
