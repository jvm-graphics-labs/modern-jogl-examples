/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut09.basicLighting;

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
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;
import framework.Framework;
import framework.Semantic;
import framework.component.Mesh;
import framework.glutil.MatrixStack_;
import framework.glutil.ObjectData;
import framework.glutil.ObjectPole;
import framework.glutil.ViewData;
import framework.glutil.ViewPole;
import framework.glutil.ViewScale;
import glm.mat._4.Mat4;
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

    private final String SHADERS_ROOT = "/tut08/basicLighting/shaders", DATA_ROOT = "/tut08/basicLighting/data/",
            VERT_SHADER_SRC = "pos-color-local-transform", FRAG_SHADER_SRC = "color-mult-uniform",
            CYLINDER_SRC = "UnitCylinder.xml", PLANE_SRC = "LargePlane.xml";

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
    private FloatBuffer matrixBuffer = GLBuffers.newDirectFloatBuffer(16);

    private ViewPole viewPole;
    private ViewData initialViewData;
    private ViewScale viewScale;
    private ObjectPole objectPole;
    private ObjectData initialObjectData;
    private Vec4 lightDirection;
    private boolean drawColoredCyl;

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

        whiteDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, VERT_SHADER_SRC, FRAG_SHADER_SRC);
        vertexDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, VERT_SHADER_SRC, FRAG_SHADER_SRC);
    }


//        initialViewData = new ViewData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.3826834f, 0.0f, 0.0f, 0.92387953f), 5.0f, 0.0f);
//
//        viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.5f, 0.0f, 0.0f, 90.0f / 250.0f);
//
//        viewPole = new ViewPole(initialViewData, viewScale);
//
//        initialObjectData = new ObjectData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.0f, 0.0f, 0.0f, 1.0f));
//
//        objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, viewPole);
//
//        lightDirection = new Vec4(0.866f, 0.5f, 0.0f, 0.0f);
//
//        zNear = 1.0f;
//        zFar = 1000.0f;
//
//        drawColoredCyl = true;

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack_ modelMatrix = new MatrixStack_()
                .setMatrix(viewPole.calcMatrix());

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

                if (drawColoredCyl) {

                    vertexDiffuseColor.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(vertexDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                        Mat3 normalMatrix = new Mat3(modelMatrix.top());
                        gl3.glUniformMatrix3fv(vertexDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                        gl3.glUniform4f(vertexDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                        cylinder.render(gl3, "lit-color");
                    }
                    vertexDiffuseColor.unbind(gl3);

                } else {

                    whiteDiffuseColor.bind(gl3);
                    {
                        gl3.glUniformMatrix4fv(whiteDiffuseColor.getModelToCameraMatUnLoc(), 1, false, modelMatrix.top().toFloatArray(), 0);

                        Mat3 normalMatrix = new Mat3(modelMatrix.top());
                        gl3.glUniformMatrix3fv(whiteDiffuseColor.getNormalModelToCameraMatUnLoc(), 1, false, normalMatrix.toFloatArray(), 0);

                        gl3.glUniform4f(whiteDiffuseColor.getLightIntensityUnLoc(), 1.0f, 1.0f, 1.0f, 1.0f);

                        cylinder.render(gl3, "lit");
                    }
                    whiteDiffuseColor.unbind(gl3);
                }
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
                drawColoredCyl = !drawColoredCyl;
                break;
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
