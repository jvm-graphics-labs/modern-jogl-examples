
package main.tut09;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat.Mat3;
import glm.mat.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.mousePole.*;

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
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class BasicLighting extends Framework {

    public static void main(String[] args) {
        new BasicLighting().setup("Tutorial 09 - Basic Lighting");
    }

    private ProgramData whiteDiffuseColor, vertexDiffuseColor;

    private Mesh cylinder, plane;

    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    private Mat4 cameraToClipMatrix = new Mat4(0.0f);

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
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            cylinder = new Mesh(gl, getClass(), "tut09/UnitCylinder.xml");
            plane = new Mesh(gl, getClass(), "tut09/LargePlane.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(BasicLighting.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glEnable(GL_DEPTH_CLAMP);

        gl.glGenBuffers(1, projectionUniformBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer.get(0), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void initializeProgram(GL3 gl) {
        whiteDiffuseColor = new ProgramData(gl, "dir-vertex-lighting-PN.vert", "color-passthrough.frag");
        vertexDiffuseColor = new ProgramData(gl, "dir-vertex-lighting-PCN.vert", "color-passthrough.frag");
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack().setMatrix(viewPole.calcMatrix());

        Vec4 lightDirCameraSpace = modelMatrix.top().times(lightDirection);

        gl.glUseProgram(whiteDiffuseColor.theProgram);
        gl.glUniform3fv(whiteDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.to(vecBuffer));
        gl.glUseProgram(vertexDiffuseColor.theProgram);
        gl.glUniform3fv(vertexDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.to(vecBuffer));
        gl.glUseProgram(0);

        {
            modelMatrix.push();

            //  Render the ground plane
            {
                modelMatrix
                        .push()
                        .top().to(matBuffer);

                gl.glUseProgram(whiteDiffuseColor.theProgram);
                gl.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
                Mat3 normalMatrix = new Mat3(modelMatrix.top());
                gl.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix.to(matBuffer));
                gl.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                plane.render(gl);
                gl.glUseProgram(0);

                modelMatrix.pop();
            }

            //  Render the Cylinder
            {
                modelMatrix
                        .push()
                        .applyMatrix(objectPole.calcMatrix())
                        .top().to(matBuffer);

                if (drawColoredCyl) {

                    gl.glUseProgram(vertexDiffuseColor.theProgram);
                    gl.glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl.glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix.to(matBuffer));
                    gl.glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                    cylinder.render(gl, "lit-color");

                } else {

                    gl.glUseProgram(whiteDiffuseColor.theProgram);
                    gl.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, normalMatrix.to(matBuffer));
                    gl.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
                    cylinder.render(gl, "lit");
                }
                gl.glUseProgram(0);

                modelMatrix.pop();
            }
            modelMatrix.pop();
        }
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack perspMatrix = new MatrixStack();

        perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_SPACE:
                drawColoredCyl = !drawColoredCyl;
                break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        viewPole.mousePressed(e);
        objectPole.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        viewPole.mouseDragged(e);
        objectPole.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        viewPole.mouseReleased(e);
        objectPole.mouseReleased(e);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        viewPole.mouseWheel(e);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(vertexDiffuseColor.theProgram);
        gl.glDeleteProgram(whiteDiffuseColor.theProgram);

        gl.glDeleteBuffers(1, projectionUniformBuffer);

        cylinder.dispose(gl);
        plane.dispose(gl);

        destroyBuffer(projectionUniformBuffer);
    }

    class ProgramData {

        public int theProgram;

        public int dirToLightUnif;
        public int lightIntensityUnif;

        public int modelToCameraMatrixUnif;
        public int normalModelToCameraMatrixUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut09", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");
            dirToLightUnif = gl.glGetUniformLocation(theProgram, "dirToLight");
            lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
        }
    }
}
