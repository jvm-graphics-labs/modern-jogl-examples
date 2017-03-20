//
//package main.tut09;
//
//import com.jogamp.newt.event.KeyEvent;
//import com.jogamp.newt.event.MouseEvent;
//
//import static com.jogamp.opengl.GL.GL_BACK;
//import static com.jogamp.opengl.GL.GL_CULL_FACE;
//import static com.jogamp.opengl.GL.GL_CW;
//import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
//import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
//import static com.jogamp.opengl.GL.GL_LEQUAL;
//import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
//import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
//import static com.jogamp.opengl.GL2ES3.GL_COLOR;
//import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
//import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
//
//import com.jogamp.opengl.GL3;
//
//import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
//import static uno.glsl.UtilKt.programOf;
//
//import com.jogamp.opengl.util.GLBuffers;
//import com.jogamp.opengl.util.glsl.ShaderCode;
//import com.jogamp.opengl.util.glsl.ShaderProgram;
//import glm.mat.Mat3x3;
//import glm.mat.Mat4x4;
//import main.framework.Framework;
//import main.framework.Semantic;
//import main.framework.component.Mesh;
//import glm.quat.Quat;
//import glm.vec._3.Vec3;
//import glm.vec._4.Vec4;
//
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.nio.IntBuffer;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.ParserConfigurationException;
//
//import org.xml.sax.SAXException;
//import uno.glm.MatrixStack;
//import uno.mousePole.*;
//
///**
// * @author gbarbieri
// */
//public class BasicLighting extends Framework {
//
//    public static void main(String[] args) {
//        new BasicLighting("Tutorial 09 - Basic Lighting");
//    }
//
//    public BasicLighting(String title) {
//        super(title);
//    }
//
//    private ProgramData whiteDiffuseColor, vertexDiffuseColor;
//
//    private Mesh cylinderMesh, planeMesh;
//
//    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);
//
//    private Mat4x4 cameraToClipMatrix = new Mat4x4(0.0f);
//
//    private ViewData initialViewData = new ViewData(
//            new Vec3(0.0f, 0.5f, 0.0f),
//            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
//            5.0f,
//            0.0f);
//
//    private ViewScale viewScale = new ViewScale(
//            3.0f, 20.0f,
//            1.5f, 0.5f,
//            0.0f, 0.0f, //No camera movement.
//            90.0f / 250.0f);
//
//    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);
//
//    private ObjectData initialObjectData = new ObjectData(
//            new Vec3(0.0f, 0.5f, 0.0f),
//            new Quat(1.0f, 0.0f, 0.0f, 0.0f));
//
//    private ObjectPole objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole);
//
//    private Vec4 lightDirection = new Vec4(0.866f, 0.5f, 0.0f, 0.0f);
//
//    private boolean drawColoredCyl = true;
//
//    @Override
//    public void init(GL3 gl) {
//
//        initializeProgram(gl);
//
//        try {
//            cylinderMesh = new Mesh(gl, getClass(), "tut08/UnitCylinder.xml");
//            planeMesh = new Mesh(gl, getClass(), "tut08/UnitPlane.xml");
//        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
//            Logger.getLogger(BasicLighting.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        gl.glEnable(GL_CULL_FACE);
//        gl.glCullFace(GL_BACK);
//        gl.glFrontFace(GL_CW);
//
//        gl.glEnable(GL_DEPTH_TEST);
//        gl.glDepthMask(true);
//        gl.glDepthFunc(GL_LEQUAL);
//        gl.glDepthRangef(0.0f, 1.0f);
//        gl.glEnable(GL_DEPTH_CLAMP);
//
//        gl.glGenBuffers(1, projectionUniformBuffer);
//        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
//        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4x4.SIZE, null, GL_DYNAMIC_DRAW);
//
//        //Bind the static buffers.
//        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer.get(0), 0, Mat4x4.SIZE);
//
//        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//    }
//
//    private void initializeProgram(GL3 gl) {
//        whiteDiffuseColor = new ProgramData(gl, "tut08", "dir-vertex-lighting-PN,vert", "color-passthrough.frag");
//        vertexDiffuseColor = new ProgramData(gl, "tut08", "dir-vertex-lighting-PCN,vert", "color-passthrough.frag");
//    }
//
//    @Override
//    public void display(GL3 gl3) {
//
//        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
//        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));
//
//        MatrixStack modelMatrix = new MatrixStack().setMatrix(viewPole.calcMatrix());
//
//        Vec4 lightDirCameraSpace = modelMatrix.top().times(lightDirection);
//
//        gl3.glUseProgram(whiteDiffuseColor.theProgram);
//        gl3.glUniform3fv(whiteDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.to(vecBuffer));
//        gl3.glUseProgram(vertexDiffuseColor.theProgram);
//        gl3.glUniform3fv(vertexDiffuseColor.dirToLightUnif, 1, lightDirCameraSpace.to(vecBuffer));
//        gl3.glUseProgram(0);
//
//        {
//            modelMatrix.push();
//
//            //  Render the ground plane
//            {
//                modelMatrix
//                        .push()
//                        .top().to(matBuffer);
//
//                gl3.glUseProgram(whiteDiffuseColor.theProgram);
//                gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                Mat3x3 normalMatrix = new Mat3x3(modelMatrix.top());
//                gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
//                        normalMatrix.to(matBuffer));
//                gl3.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
//                planeMesh.render(gl3);
//                gl3.glUseProgram(0);
//
//                modelMatrix.pop();
//            }
//
//            //  Render the Cylinder
//            {
//                modelMatrix
//                        .push()
//                        .applyMatrix(objectPole.calcMatrix())
//                        .top().to(matBuffer);
//
//                if (drawColoredCyl) {
//
//                    gl3.glUseProgram(vertexDiffuseColor.theProgram);
//                    gl3.glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
//                    gl3.glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
//                            normalMatrix.toDfb(matBuffer));
//                    gl3.glUniform4f(vertexDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
//                    cylinderMesh.render(gl3, "lit-color");
//
//                } else {
//
//                    gl3.glUseProgram(whiteDiffuseColor.theProgram);
//                    gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                    Mat3 normalMatrix = new Mat3(modelMatrix.top());
//                    gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
//                            normalMatrix.toDfb(matBuffer));
//                    gl3.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 1.0f, 1.0f, 1.0f, 1.0f);
//                    cylinderMesh.render(gl3, "lit");
//                }
//                gl3.glUseProgram(0);
//
//                modelMatrix.pop();
//            }
//            modelMatrix.pop();
//        }
//    }
//
//    @Override
//    public void reshape(GL3 gl3, int w, int h) {
//
//        float zNear = 1.0f, zFar = 1_000f;
//        MatrixStack perspMatrix = new MatrixStack();
//
//        perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar);
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
//        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().toDfb(matBuffer));
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//
//        gl3.glViewport(0, 0, w, h);
//    }
//
//    @Override
//    public void keyPressed(KeyEvent e) {
//
//        switch (e.getKeyCode()) {
//
//            case KeyEvent.VK_ESCAPE:
//                animator.remove(glWindow);
//                glWindow.destroy();
//                break;
//
//            case KeyEvent.VK_SPACE:
//                drawColoredCyl = !drawColoredCyl;
//                break;
//        }
//    }
//
//    @Override
//    public void mousePressed(MouseEvent e) {
//        viewPole.mousePressed(e);
//        objectPole.mousePressed(e);
//    }
//
//    @Override
//    public void mouseDragged(MouseEvent e) {
//        viewPole.mouseMove(e);
//        objectPole.mouseMove(e);
//    }
//
//    @Override
//    public void mouseReleased(MouseEvent e) {
//        viewPole.mouseReleased(e);
//        objectPole.mouseReleased(e);
//    }
//
//    @Override
//    public void mouseWheelMoved(MouseEvent e) {
//        viewPole.mouseWheel(e);
//    }
//
//    @Override
//    public void end(GL3 gl3) {
//
//        gl3.glDeleteProgram(vertexDiffuseColor.theProgram);
//        gl3.glDeleteProgram(whiteDiffuseColor.theProgram);
//
//        gl3.glDeleteBuffers(1, projectionUniformBuffer);
//
//        cylinderMesh.dispose(gl3);
//        planeMesh.dispose(gl3);
//
//        BufferUtils.destroyDirectBuffer(projectionUniformBuffer);
//    }
//
//    class ProgramData {
//
//        public int theProgram;
//
//        public int dirToLightUnif;
//        public int lightIntensityUnif;
//
//        public int modelToCameraMatrixUnif;
//        public int normalModelToCameraMatrixUnif;
//
//        public ProgramData(GL3 gl, String root, String vertex, String fragment) {
//
//            theProgram = programOf(gl, getClass(), root, vertex, fragment);
//
//            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
//            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");
//            dirToLightUnif = gl.glGetUniformLocation(theProgram, "dirToLight");
//            lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity");
//
//            gl.glUniformBlockBinding(theProgram,
//                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
//                    Semantic.Uniform.PROJECTION);
//        }
//    }
//}
