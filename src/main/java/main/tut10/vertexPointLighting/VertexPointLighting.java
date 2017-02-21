///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut10.vertexPointLighting;
//
//import com.jogamp.newt.event.KeyEvent;
//import com.jogamp.newt.event.MouseEvent;
//import static com.jogamp.opengl.GL.GL_BACK;
//import static com.jogamp.opengl.GL.GL_CULL_FACE;
//import static com.jogamp.opengl.GL.GL_CW;
//import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
//import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
//import static com.jogamp.opengl.GL.GL_LEQUAL;
//import static com.jogamp.opengl.GL2ES3.GL_COLOR;
//import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
//import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
//import com.jogamp.opengl.GL3;
//import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
//import com.jogamp.opengl.util.GLBuffers;
//import glutil.BufferUtils;
//import main.framework.Framework;
//import main.framework.Semantic;
//import main.framework.component.Mesh;
//import glm.mat._4.Mat4;
//import glm.quat.Quat;
//import glm.vec._3.Vec3;
//import glm.vec._4.Vec4;
//import glutil.MatrixStack;
//import glutil.Timer;
//import java.io.IOException;
//import java.nio.IntBuffer;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.ParserConfigurationException;
//import org.xml.sax.SAXException;
//import view.ObjectData;
//import view.ObjectPole;
//import view.ViewData;
//import view.ViewPole;
//import view.ViewScale;
//
///**
// *
// * @author gbarbieri
// */
//public class VertexPointLighting extends Framework {
//
//    private final String SHADERS_ROOT = "/tut10/vertexPointLighting/shaders",
//            POS_VERTEX_LIGHTING_PN_SHADER_SRC = "pos-vertex-lighting-pn",
//            POS_VERTEX_LIGHTING_PCN_SHADER_SRC = "pos-vertex-lighting-pcn",
//            POS_TRANSFORM_SHADER_SRC = "pos-transform", COLOR_PASSTHROUGH_SHADER_SRC = "color-passthrough",
//            UNIFORM_COLOR_SHADER_SRC = "uniform-color", MESHES_ROOT = "/tut10/data/",
//            CYLINDER_MESH_SRC = "UnitCylinder.xml", PLANE_MESH_SRC = "LargePlane.xml", CUBE_MESH_SRC = "UnitCube.xml";
//
//    public static void main(String[] args) {
//        new VertexPointLighting("Tutorial 10 - Vertex Point Lighting");
//    }
//
//    private ProgramData whiteDiffuseColor, vertexDiffuseColor;
//    private UnlitProgData unlit;
//
//    private Mesh cylinder, plane, cube;
//
//    private ViewData initialViewData = new ViewData(
//            new Vec3(0.0f, 0.5f, 0.0f),
//            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
//            5.0f,
//            0.0f);
//    private ViewScale viewScale = new ViewScale(
//            3.0f, 20.0f,
//            1.5f, 0.5f,
//            0.0f, 0.0f, //No camera movement.
//            90.0f / 250.0f);
//    private ObjectData initialObjectData = new ObjectData(
//            new Vec3(0.0f, 0.5f, 0.0f),
//            new Quat(1.0f, 0.0f, 0.0f, 0.0f));
//
//    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);
//    private ObjectPole objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole);
//
//    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);
//
//    private boolean drawColoredCyl = false, drawLight = false;
//    private float lightHeight = 1.5f, lightRadius = 1.0f;
//    private Timer lightTimer = new Timer(Timer.Type.LOOP, 5.0f);
//
//    public VertexPointLighting(String title) {
//        super(title);
//    }
//
//    @Override
//    public void init(GL3 gl3) {
//
//        initializePrograms(gl3);
//
//        try {
//            cylinder = new Mesh(MESHES_ROOT + CYLINDER_MESH_SRC, gl3);
//            plane = new Mesh(MESHES_ROOT + PLANE_MESH_SRC, gl3);
//            cube = new Mesh(MESHES_ROOT + CUBE_MESH_SRC, gl3);
//        } catch (ParserConfigurationException | SAXException | IOException ex) {
//            Logger.getLogger(VertexPointLighting.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        gl3.glEnable(GL_CULL_FACE);
//        gl3.glCullFace(GL_BACK);
//        gl3.glFrontFace(GL_CW);
//
//        gl3.glEnable(GL_DEPTH_TEST);
//        gl3.glDepthMask(true);
//        gl3.glDepthFunc(GL_LEQUAL);
//        gl3.glDepthRangef(0.0f, 1.0f);
//        gl3.glEnable(GL_DEPTH_CLAMP);
//
//        gl3.glGenBuffers(1, projectionUniformBuffer);
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
//        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);
//
//        //Bind the static buffers.
//        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer.get(0),
//                0, Mat4.SIZE);
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//    }
//
//    private void initializePrograms(GL3 gl3) {
//        whiteDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, POS_VERTEX_LIGHTING_PN_SHADER_SRC, COLOR_PASSTHROUGH_SHADER_SRC);
//        vertexDiffuseColor = new ProgramData(gl3, SHADERS_ROOT, POS_VERTEX_LIGHTING_PCN_SHADER_SRC, COLOR_PASSTHROUGH_SHADER_SRC);
//        unlit = new UnlitProgData(gl3, SHADERS_ROOT, POS_TRANSFORM_SHADER_SRC, UNIFORM_COLOR_SHADER_SRC);
//    }
//
//    @Override
//    public void display(GL3 gl3) {
//
//        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
//        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));
//
//        lightTimer.update();
//
//        MatrixStack modelMatrix = new MatrixStack();
//        modelMatrix.setMatrix(viewPole.calcMatrix());
//
//        Vec4 worldLightPosition = calcLightPosition();
//
//        Vec4 lightPosCameraSpace = modelMatrix.top().mul_(worldLightPosition);
//
//        gl3.glUseProgram(whiteDiffuseColor.theProgram);
//        gl3.glUniform3fv(whiteDiffuseColor.lightPosUnif, 1, lightPosCameraSpace.toDfb(vecBuffer));
//        gl3.glUseProgram(vertexDiffuseColor.theProgram);
//        gl3.glUniform3fv(vertexDiffuseColor.lightPosUnif, 1, lightPosCameraSpace.toDfb(vecBuffer));
//
//        gl3.glUseProgram(whiteDiffuseColor.theProgram);
//        gl3.glUniform4f(whiteDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
//        gl3.glUniform4f(whiteDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
//        gl3.glUseProgram(vertexDiffuseColor.theProgram);
//        gl3.glUniform4f(vertexDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
//        gl3.glUniform4f(vertexDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
//        gl3.glUseProgram(0);
//
//        {
//            modelMatrix.push();
//
//            //Render the ground plane.
//            {
//                modelMatrix.push().top().toDfb(matBuffer);
//
//                gl3.glUseProgram(whiteDiffuseColor.theProgram);
//                gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                modelMatrix.top().toMat3_().toDfb(matBuffer);
//                gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, matBuffer);
//                plane.render(gl3);
//                gl3.glUseProgram(0);
//
//                modelMatrix.pop();
//            }
//            modelMatrix.pop();
//
//            //Render the Cylinder
//            {
//                modelMatrix.push().applyMatrix(objectPole.calcMatrix()).top().toDfb(matBuffer);
//
//                if (drawColoredCyl) {
//                    gl3.glUseProgram(vertexDiffuseColor.theProgram);
//                    gl3.glUniformMatrix4fv(vertexDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                    modelMatrix.top().toMat3_().toDfb(matBuffer);
//                    gl3.glUniformMatrix3fv(vertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false, matBuffer);
//                    cylinder.render(gl3, "lit-color");
//                } else {
//                    gl3.glUseProgram(whiteDiffuseColor.theProgram);
//                    gl3.glUniformMatrix4fv(whiteDiffuseColor.modelToCameraMatrixUnif, 1, false, matBuffer);
//                    modelMatrix.top().toMat3_().toDfb(matBuffer);
//                    gl3.glUniformMatrix3fv(whiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false, matBuffer);
//                    cylinder.render(gl3, "lit");
//                }
//                gl3.glUseProgram(0);
//
//                modelMatrix.pop();
//            }
//
//            //Render the light
//            if (drawLight) {
//
//                modelMatrix.push().translate(worldLightPosition).scale(0.1f, 0.1f, 0.1f);
//
//                gl3.glUseProgram(unlit.theProgram);
//                gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
//                gl3.glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f);
//                cube.render(gl3, "flat");
//
//                modelMatrix.pop();
//            }
//        }
//    }
//
//    private Vec4 calcLightPosition() {
//
//        float currentTimeThroughLoop = lightTimer.getAlpha();
//
//        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);
//
//        ret.x = (float) (Math.cos(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);
//        ret.z = (float) (Math.sin(currentTimeThroughLoop * (Math.PI * 2.0f)) * lightRadius);
//
//        return ret;
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
//
//            case KeyEvent.VK_I:
//                lightHeight += e.isShiftDown() ? 0.05f : 0.2f;
//                break;
//            case KeyEvent.VK_K:
//                lightHeight -= e.isShiftDown() ? 0.05f : 0.2f;
//                break;
//            case KeyEvent.VK_L:
//                lightRadius += e.isShiftDown() ? 0.05f : 0.2f;
//                break;
//            case KeyEvent.VK_J:
//                lightRadius -= e.isShiftDown() ? 0.05f : 0.2f;
//                break;
//
//            case KeyEvent.VK_Y:
//                drawLight = !drawLight;
//                break;
//
//            case KeyEvent.VK_B:
//                lightTimer.togglePause();
//                break;
//        }
//        if (lightRadius < 0.2f) {
//            lightRadius = 0.2f;
//        }
//    }
//
//    @Override
//    public void end(GL3 gl3) {
//
//        gl3.glDeleteProgram(vertexDiffuseColor.theProgram);
//        gl3.glDeleteProgram(whiteDiffuseColor.theProgram);
//        gl3.glDeleteProgram(unlit.theProgram);
//
//        gl3.glDeleteBuffers(1, projectionUniformBuffer);
//
//        cylinder.dispose(gl3);
//        plane.dispose(gl3);
//        cube.dispose(gl3);
//
//        BufferUtils.destroyDirectBuffer(projectionUniformBuffer);
//    }
//}
