/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.scaleAndLighting;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;
import framework.Framework;
import framework.Semantic;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import framework.component.Mesh;
import glm.mat._4.Mat4;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import tut09.basicLighting.BasicLighting;
import view.ObjectData;
import view.ObjectPole;
import view.ViewData;
import view.ViewPole;
import view.ViewScale;

/**
 *
 * @author gbarbieri
 */
public class ScaleAndLighting extends Framework {

    private final String SHADERS_ROOT = "/tut09/scaleAndLighting/shaders", DATA_ROOT = "/tut09/scaleAndLighting/data/",
            DIR_PN_SHADER_SRC = "dir-vertex-lighting-pn", DIR_PCN_SHADER_SRC = "dir-vertex-lighting-pcn",
            FRAG_SHADER_SRC = "color-passthrough", CYLINDER_SRC = "UnitCylinder.xml", PLANE_SRC = "LargePlane.xml";
    
    public static void main(String[] args) {
        new ScaleAndLighting("Tutorial 09 - Scale and Lighting");
    }
    
    private ProgramData whiteDiffuseColor;
    private ProgramData vertexDiffuseColor;
    private Mesh cylinder;
    private Mesh plane;
    
    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);
    
    private int[] projectionUBO;
    private ViewPole viewPole;
    private ViewData initialViewData;
    private ViewScale viewScale;
    private ObjectPole objectPole;
    private ObjectData initialObjectData;
    private Vec4 lightDirection;
    private boolean scaleCylinder;
    private boolean doInverseTranspose;

   

    @Override
    public void init(GL3 gl3) {
        
        initializeProgram(gl3);

        try {
            cylinder = new Mesh(DATA_ROOT + CYLINDER_SRC, gl3);
            plane = new Mesh(DATA_ROOT + PLANE_SRC, gl3);
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
        whiteDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_PN_SHADER_SRC, FRAG_SHADER_SRC);
        vertexDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, DIR_PCN_SHADER_SRC, FRAG_SHADER_SRC);
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

        MatrixStack modelMatrix = new MatrixStack();

        modelMatrix.setTop(viewPole.calcMatrix());

        Vec4 lightDirCameraSpace = modelMatrix.top().mult(lightDirection);

        whiteDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(whiteDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexDiffuseColor.bind(gl3);
        {
            gl3.glUniform3fv(vertexDiffuseColor.getDirToLightUnLoc(), 1, lightDirCameraSpace.toFloatArray(), 0);
        }
        vertexDiffuseColor.unbind(gl3);

        modelMatrix.push();
        {
            //  Render the ground plane
            modelMatrix.push();
            {
                whiteDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
                    gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    gl3.glUniform4f(whiteDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                    plane.render(gl3);
                }
                whiteDiffuseColor.unbind(gl3);
            }
            modelMatrix.pop();

            //  Render the Cylinder
            modelMatrix.push();
            {
                modelMatrix.applyMat(objectPole.calcMatrix());

                if (scaleCylinder) {

                    modelMatrix.scale(new Vec3(1.0f, 1.0f, 0.2f));
                }
                vertexDiffuseColor.bind(gl3);
                {
                    gl3.glUniformMatrix4fv(vertexDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                    Mat3 normalMatrix = new Mat3(modelMatrix.top());

                    if (doInverseTranspose) {

                        normalMatrix = normalMatrix.inverse();

                        normalMatrix = normalMatrix.transpose();
                    }
                    gl3.glUniformMatrix3fv(vertexDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                    gl3.glUniform4f(vertexDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                    cylinder.render(gl3, "lit-color");
                }
                vertexDiffuseColor.unbind(gl3);
            }
            modelMatrix.pop();
        }
        modelMatrix.pop();

        glad.swapBuffers();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        System.out.println("reshape() x: " + x + " y: " + y + " width: " + w + " height: " + h);

        GL3 gl3 = glad.getGL().getGL3();

        MatrixStack perspectiveMatrix = new MatrixStack();

        perspectiveMatrix.setTop(Jglm.perspective(45.0f, (float) w / (float) h, zNear, zFar));

        perspectiveMatrix.top().print("perspectiveMatrix.top()");

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, projectionUBO[0]);
        {
            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(perspectiveMatrix.top().toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(x, y, w, h);
    }

    private void initializeObjects(GL3 gl3) {
        System.out.println("initializeObjects");

//        cylinder = new Mesh(dataFilepath + "UnitCylinder.xml", gl3);
//        plane = new Mesh(dataFilepath + "LargePlane.xml", gl3);
    }

    private void initializePrograms(GL3 gl3) {

        System.out.println("initializePrograms...");

        whiteDiffuseColor = new GLSLProgramObject_1(gl3, shadersFilepath, "DirVertexLighting_PN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
        vertexDiffuseColor = new GLSLProgramObject_1(gl3, shadersFilepath, "DirVertexLighting_PCN_VS.glsl", "ColorPassthrough_FS.glsl", projectionBlockIndex);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(GLCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {

        viewPole.mousePressed(e);
        objectPole.mousePressed(e);

        canvas.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        viewPole.mouseReleased(e);
        objectPole.mouseReleased(e);

        canvas.display();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_SPACE:
                scaleCylinder = !scaleCylinder;
                break;

            case KeyEvent.VK_T:
                doInverseTranspose = !doInverseTranspose;
                System.out.println("doInverseTranspose: " + doInverseTranspose);
        }

        canvas.display();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        viewPole.mouseMove(e);
        objectPole.mouseMove(e);

        canvas.display();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        viewPole.mouseWheel(e);

        canvas.display();
    }
}
