/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.ambientLighting;

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
import glutil.BufferUtils;
import framework.Framework;
import framework.Semantic;
import framework.component.Mesh;
import glm.mat._3.Mat3;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import glutil.MatrixStack;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import view.ObjectData;
import view.ObjectPole;
import view.ViewData;
import view.ViewPole;
import view.ViewScale;

/**
 *
 * @author gbarbieri
 */
public class AmbientLighting extends Framework {

    private final String SHADERS_ROOT = "/tut09/ambientLighting/shaders", MESHES_ROOT = "/tut09/data/",
            DIR_PN_SHADER_SRC = "dir-vertex-lighting-pn", DIR_PCN_SHADER_SRC = "dir-vertex-lighting-pcn",
            DIR_AMB_PN_SHADER_SRC = "dir-amb-vertex-lighting-pn", DIR_AMB_PCN_SHADER_SRC = "dir-amb-vertex-lighting-pcn",
            FRAG_SHADER_SRC = "color-passthrough", CYLINDER_SRC = "UnitCylinder.xml", PLANE_SRC = "LargePlane.xml";

    public static void main(String[] args) {
        new AmbientLighting("Tutorial 09 - Ambient Lighting");
    }

    private ProgramData whiteDiffuseColor;
    private ProgramData vertexDiffuseColor;
    private ProgramData whiteAmbDiffuseColor;
    private ProgramData vertexAmbDiffuseColor;

    private Mesh cylinder, plane;

    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    private Vec4 lightDirection = new Vec4(0.866f, 0.5f, 0.0f, 0.0f);

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

    private boolean drawColoredCyl = true, showAmbient = false;

    public AmbientLighting(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        try {
            cylinder = new Mesh(MESHES_ROOT + CYLINDER_SRC, gl3);
            plane = new Mesh(MESHES_ROOT + PLANE_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(AmbientLighting.class.getName()).log(Level.SEVERE, null, ex);
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
        whiteDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_PN_SHADER_SRC, FRAG_SHADER_SRC);
        vertexDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_PCN_SHADER_SRC, FRAG_SHADER_SRC);
        whiteAmbDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_AMB_PN_SHADER_SRC, FRAG_SHADER_SRC);
        vertexAmbDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_AMB_PCN_SHADER_SRC, FRAG_SHADER_SRC);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack().setMatrix(viewPole.calcMatrix());

        Vec4 lightDirCameraSpace = modelMatrix.top().mul_(lightDirection);

        ProgramData whiteDiffuse = showAmbient ? whiteAmbDiffuseColor : whiteDiffuseColor;
        ProgramData vertexDiffuse = showAmbient ? vertexAmbDiffuseColor : vertexDiffuseColor;

        if (showAmbient) {

            gl3.glUseProgram(whiteDiffuse.theProgram);
            gl3.glUniform4f(whiteDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
            gl3.glUniform4f(whiteDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
            gl3.glUseProgram(vertexDiffuse.theProgram);
            gl3.glUniform4f(vertexDiffuse.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
            gl3.glUniform4f(vertexDiffuse.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);

        } else {

            gl3.glUseProgram(whiteDiffuse.theProgram);
            gl3.glUniform4f(whiteDiffuse.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            gl3.glUseProgram(vertexDiffuse.theProgram);
            gl3.glUniform4f(vertexDiffuse.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        gl3.glUseProgram(whiteDiffuse.theProgram);
        gl3.glUniform3fv(whiteDiffuse.dirToLightUnif, 1, lightDirCameraSpace.toDfb(vecBuffer));
        gl3.glUseProgram(vertexDiffuse.theProgram);
        gl3.glUniform3fv(vertexDiffuse.dirToLightUnif, 1, lightDirCameraSpace.toDfb(vecBuffer));
        gl3.glUseProgram(0);

        //Render the ground plane.
        {
            gl3.glUseProgram(whiteDiffuse.theProgram);
            gl3.glUniformMatrix4fv(whiteDiffuse.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
            Mat3 normMatrix = new Mat3(modelMatrix.top());
            gl3.glUniformMatrix3fv(whiteDiffuse.normalModelToCameraMatrixUnif, 1, false, normMatrix.toDfb(matBuffer));
            plane.render(gl3);
            gl3.glUseProgram(0);
        }
        //Render the Cylinder
        {
            modelMatrix.push()
                    .applyMatrix(objectPole.calcMatrix());

            if (drawColoredCyl) {
                gl3.glUseProgram(vertexDiffuse.theProgram);
                gl3.glUniformMatrix4fv(vertexDiffuse.modelToCameraMatrixUnif, 1, false,
                        modelMatrix.top().toDfb(matBuffer));
                Mat3 normMatrix = new Mat3(modelMatrix.top());
                gl3.glUniformMatrix3fv(vertexDiffuse.normalModelToCameraMatrixUnif, 1, false,
                        normMatrix.toDfb(matBuffer));
                cylinder.render(gl3, "lit-color");
            } else {
                gl3.glUseProgram(whiteDiffuse.theProgram);
                gl3.glUniformMatrix4fv(whiteDiffuse.modelToCameraMatrixUnif, 1, false,
                        modelMatrix.top().toDfb(matBuffer));
                Mat3 normMatrix = new Mat3(modelMatrix.top());
                gl3.glUniformMatrix3fv(whiteDiffuse.normalModelToCameraMatrixUnif, 1, false,
                        normMatrix.toDfb(matBuffer));
                cylinder.render(gl3, "lit");
            }
            modelMatrix.pop();

            gl3.glUseProgram(0);
        }
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack perspMatrix = new MatrixStack();

        perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().toDfb(matBuffer));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;

            case KeyEvent.VK_SPACE:
                drawColoredCyl = !drawColoredCyl;
                break;

            case KeyEvent.VK_T:
                showAmbient = !showAmbient;
                System.out.println("Ambient Lighting " + (showAmbient ? "On." : "Off"));
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
        viewPole.mouseMove(e);
        objectPole.mouseMove(e);
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
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(vertexDiffuseColor.theProgram);
        gl3.glDeleteProgram(whiteDiffuseColor.theProgram);
        gl3.glDeleteProgram(vertexAmbDiffuseColor.theProgram);
        gl3.glDeleteProgram(whiteAmbDiffuseColor.theProgram);

        gl3.glDeleteBuffers(1, projectionUniformBuffer);

        cylinder.dispose(gl3);
        plane.dispose(gl3);

        BufferUtils.destroyDirectBuffer(projectionUniformBuffer);
    }
}
