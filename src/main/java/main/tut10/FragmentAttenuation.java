
package main.tut10;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.Glm;
import glm.mat.Mat3;
import glm.mat.Mat4;
import glm.quat.Quat;
import glm.vec._2.Vec2i;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.mousePole.*;
import uno.time.Timer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
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
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;
import static glm.GlmKt.glm;

/**
 * @author gbarbieri
 */
public class FragmentAttenuation extends Framework {

    public static void main(String[] args) {
        new FragmentAttenuation().setup("Tutorial 10 - Fragment Attenuation");
    }

    private ProgramData fragWhiteDiffuseColor, fragVertexDiffuseColor;
    private UnlitProgData unlit;

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
    private ObjectData initialObjectData = new ObjectData(
            new Vec3(0.0f, 0.5f, 0.0f),
            new Quat(1.0f, 0.0f, 0.0f, 0.0f));

    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);
    private ObjectPole objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole);

    private Mesh cylinder, plane, cube;

    private boolean drawColoredCyl = false, drawLight = false, scaleCyl = false, useRSquare = false;

    private float lightHeight = 1.5f, lightRadius = 1.0f, lightAttenuation = 1.0f;

    private Timer lightTimer = new Timer(Timer.Type.Loop, 5.0f);

    private interface Buffer {

        int PROJECTION = 0;
        int UNPROJECTION = 1;
        int MAX = 2;
    }

    private static class UnProjectionBlock {

        public final static int SIZE = Mat4.SIZE + Vec2i.SIZE;

        public static Mat4 clipToCameraMatrix;
        public static Vec2i windowSize;

        public static ByteBuffer to(ByteBuffer buffer) {
            clipToCameraMatrix.to(buffer);
            windowSize.to(buffer, Mat4.SIZE);
            return buffer;
        }
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);

    private ByteBuffer unprojectBuffer = GLBuffers.newDirectByteBuffer(UnProjectionBlock.SIZE);

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            cylinder = new Mesh(gl, getClass(), "tut10/UnitCylinder.xml");
            plane = new Mesh(gl, getClass(), "tut10/LargePlane.xml");
            cube = new Mesh(gl, getClass(), "tut10/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(FragmentAttenuation.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(0.0f, 1.0f);
        gl.glEnable(GL_DEPTH_CLAMP);

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.UNPROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, UnProjectionBlock.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.UNPROJECTION, bufferName.get(Buffer.UNPROJECTION),
                0, UnProjectionBlock.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void initializePrograms(GL3 gl) {
        fragWhiteDiffuseColor = new ProgramData(gl, "frag-light-atten-PN.vert", "frag-light-atten.frag");
        fragVertexDiffuseColor = new ProgramData(gl, "frag-light-atten-PCN.vert", "frag-light-atten.frag");
        unlit = new UnlitProgData(gl, "pos-transform.vert", "uniform-color.frag");
    }

    @Override
    public void display(GL3 gl) {

        lightTimer.update();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack();
        modelMatrix.setMatrix(viewPole.calcMatrix());

        Vec4 worldLightPos = calcLightPosition();
        Vec4 lightPosCameraSpace = modelMatrix.top().times(worldLightPos);

        gl.glUseProgram(fragWhiteDiffuseColor.theProgram);
        gl.glUniform4f(fragWhiteDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(fragWhiteDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUniform3fv(fragWhiteDiffuseColor.cameraSpaceLightPosUnif, 1, lightPosCameraSpace.to(vecBuffer));
        gl.glUniform1f(fragWhiteDiffuseColor.lightAttenuationUnif, lightAttenuation);
        gl.glUniform1i(fragWhiteDiffuseColor.bUseRSquareUnif, useRSquare ? 1 : 0);

        gl.glUseProgram(fragVertexDiffuseColor.theProgram);
        gl.glUniform4f(fragVertexDiffuseColor.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(fragVertexDiffuseColor.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUniform3fv(fragVertexDiffuseColor.cameraSpaceLightPosUnif, 1, lightPosCameraSpace.to(vecBuffer));
        gl.glUniform1f(fragVertexDiffuseColor.lightAttenuationUnif, lightAttenuation);
        gl.glUniform1i(fragVertexDiffuseColor.bUseRSquareUnif, useRSquare ? 1 : 0);
        gl.glUseProgram(0);

        {
            modelMatrix.push();

            //Render the ground plane.
            {
                modelMatrix.push();

                Mat3 normMatrix = modelMatrix.top().toMat3();
                normMatrix.inverse_().transpose_();

                gl.glUseProgram(fragWhiteDiffuseColor.theProgram);
                gl.glUniformMatrix4fv(fragWhiteDiffuseColor.modelToCameraMatrixUnif, 1, false,
                        modelMatrix.top().to(matBuffer));

                gl.glUniformMatrix3fv(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                        normMatrix.to(matBuffer));
                plane.render(gl);
                gl.glUseProgram(0);

                modelMatrix.pop();
            }

            //Render the Cylinder
            {
                modelMatrix
                        .push()
                        .applyMatrix(objectPole.calcMatrix());

                if (scaleCyl)
                    modelMatrix.scale(1.0f, 1.0f, 0.2f);

                Mat3 normMatrix = modelMatrix.top().toMat3();
                normMatrix.inverse_().transpose_();

                if (drawColoredCyl) {
                    gl.glUseProgram(fragVertexDiffuseColor.theProgram);
                    gl.glUniformMatrix4fv(fragVertexDiffuseColor.modelToCameraMatrixUnif, 1, false,
                            modelMatrix.top().to(matBuffer));

                    gl.glUniformMatrix3fv(fragVertexDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                            normMatrix.to(matBuffer));
                    cylinder.render(gl, "lit-color");
                } else {
                    gl.glUseProgram(fragWhiteDiffuseColor.theProgram);
                    gl.glUniformMatrix4fv(fragWhiteDiffuseColor.modelToCameraMatrixUnif, 1, false,
                            modelMatrix.top().to(matBuffer));

                    gl.glUniformMatrix3fv(fragWhiteDiffuseColor.normalModelToCameraMatrixUnif, 1, false,
                            normMatrix.to(matBuffer));
                    cylinder.render(gl, "lit");
                }
                gl.glUseProgram(0);

                modelMatrix.pop();
            }

            //Render the light
            if (drawLight) {

                modelMatrix
                        .push()
                        .translate(worldLightPos)
                        .scale(0.1f);

                gl.glUseProgram(unlit.theProgram);
                gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
                gl.glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f);
                cube.render(gl, "flat");

                modelMatrix.pop();
            }
            modelMatrix.pop();
        }
    }

    private Vec4 calcLightPosition() {

        float currentTimeThroughLoop = lightTimer.getAlpha();

        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = glm.cos(currentTimeThroughLoop * (Glm.PIf * 2.0f)) * lightRadius;
        ret.z = glm.sin(currentTimeThroughLoop * (Glm.PIf * 2.0f)) * lightRadius;

        return ret;
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack perspMatrix = new MatrixStack();

        Mat4 proj = perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar).top();

        UnProjectionBlock.clipToCameraMatrix = perspMatrix.top().inverse();
        UnProjectionBlock.windowSize = new Vec2i(w, h);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, proj.to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.UNPROJECTION));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, unprojectBuffer.capacity(), UnProjectionBlock.to(unprojectBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
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
    public void keyPressed(KeyEvent e) {

        boolean changedAtten = false;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_SPACE:
                drawColoredCyl = !drawColoredCyl;
                break;

            case KeyEvent.VK_I:
                lightHeight += e.isShiftDown() ? 0.05f : 0.2f;
                break;
            case KeyEvent.VK_K:
                lightHeight -= e.isShiftDown() ? 0.05f : 0.2f;
                break;
            case KeyEvent.VK_L:
                lightRadius += e.isShiftDown() ? 0.05f : 0.2f;
                break;
            case KeyEvent.VK_J:
                lightRadius -= e.isShiftDown() ? 0.05f : 0.2f;
                break;

            case KeyEvent.VK_O:
                lightAttenuation *= e.isShiftDown()? 1.1f : 1.5f;
                changedAtten = true;
                break;
            case KeyEvent.VK_U:
                lightAttenuation /= e.isShiftDown()? 1.1f : 1.5f;
                changedAtten = true;
                break;

            case KeyEvent.VK_Y:
                drawLight = !drawLight;
                break;
            case KeyEvent.VK_T:
                scaleCyl = !scaleCyl;
                break;
            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;

            case KeyEvent.VK_H:
                useRSquare = !useRSquare;
                System.out.println((useRSquare ? "Inverse Squared" : "Plain Inverse") + " Attenuation");
                break;
        }

        if (lightRadius < 0.2f)
            lightRadius = 0.2f;

        if (lightAttenuation < 0.1f)
            lightAttenuation = 0.1f;

        if (changedAtten)
            System.out.println("Atten: " + lightAttenuation);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(fragVertexDiffuseColor.theProgram);
        gl.glDeleteProgram(fragWhiteDiffuseColor.theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);

        cylinder.dispose(gl);
        plane.dispose(gl);
        cube.dispose(gl);

        destroyBuffers(bufferName, unprojectBuffer);
    }

    private class ProgramData {

        public int theProgram;

        public int modelToCameraMatrixUnif;

        public int lightIntensityUnif;
        public int ambientIntensityUnif;

        public int normalModelToCameraMatrixUnif;
        public int cameraSpaceLightPosUnif;

        public int lightAttenuationUnif;
        public int bUseRSquareUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut10", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
            lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity");
            ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity");

            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");
            cameraSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "cameraSpaceLightPos");

            lightAttenuationUnif = gl.glGetUniformLocation(theProgram, "lightAttenuation");
            bUseRSquareUnif = gl.glGetUniformLocation(theProgram, "bUseRSquare");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "UnProjection"),
                    Semantic.Uniform.UNPROJECTION);
        }
    }

    private class UnlitProgData {

        public int theProgram;

        public int objectColorUnif;

        public int modelToCameraMatrixUnif;

        public UnlitProgData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut10", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
        }
    }
}
