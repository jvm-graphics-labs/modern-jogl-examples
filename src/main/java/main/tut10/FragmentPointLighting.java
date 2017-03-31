
package main.tut10;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.Glm;
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
import uno.time.Timer;

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
import static glm.GlmKt.glm;

/**
 * @author gbarbieri
 */
public class FragmentPointLighting extends Framework {

    public static void main(String[] args) {
        new FragmentPointLighting().setup("Tutorial 10 - Fragment Point Lighting");
    }

    private ProgramData whiteDiffuseColor, vertexDiffuseColor, fragWhiteDiffuseColor, fragVertexDiffuseColor;
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

    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    private boolean useFragmentLighting = true, drawColoredCyl = false, drawLight = false, scaleCyl = false;
    private float lightHeight = 1.5f, lightRadius = 1.0f;

    private Timer lightTimer = new Timer(Timer.Type.Loop, 5.0f);

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            cylinder = new Mesh(gl, getClass(), "tut10/UnitCylinder.xml");
            plane = new Mesh(gl, getClass(), "tut10/LargePlane.xml");
            cube = new Mesh(gl, getClass(), "tut10/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(FragmentPointLighting.class.getName()).log(Level.SEVERE, null, ex);
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

    private void initializePrograms(GL3 gl) {
        whiteDiffuseColor = new ProgramData(gl, "model-pos-vertex-lighting-PN.vert", "color-passthrough.frag");
        vertexDiffuseColor = new ProgramData(gl, "model-pos-vertex-lighting-PCN.vert", "color-passthrough.frag");
        fragWhiteDiffuseColor = new ProgramData(gl, "fragment-lighting-PN.vert", "fragment-lighting.frag");
        fragVertexDiffuseColor = new ProgramData(gl, "fragment-lighting-PCN.vert", "fragment-lighting.frag");
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

        ProgramData whiteProgram = useFragmentLighting ? fragWhiteDiffuseColor : whiteDiffuseColor;
        ProgramData vertColorProgram = useFragmentLighting ? fragVertexDiffuseColor : vertexDiffuseColor;

        gl.glUseProgram(whiteProgram.theProgram);
        gl.glUniform4f(whiteProgram.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(whiteProgram.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUseProgram(vertColorProgram.theProgram);
        gl.glUniform4f(vertColorProgram.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(vertColorProgram.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUseProgram(0);

        {
            modelMatrix.push();

            //Render the ground plane.
            {
                modelMatrix.push();

                gl.glUseProgram(whiteProgram.theProgram);
                gl.glUniformMatrix4fv(whiteProgram.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

                Mat4 invTransform = modelMatrix.top().inverse();
                Vec4 lightPosModelSpace = invTransform.times(lightPosCameraSpace);
                gl.glUniform3fv(whiteProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace.to(vecBuffer));

                plane.render(gl);
                gl.glUseProgram(0);

                modelMatrix.pop();
            }

            //Render the Cylinder
            {
                modelMatrix.push();

                modelMatrix.applyMatrix(objectPole.calcMatrix());

                if (scaleCyl)
                    modelMatrix.scale(1.0f, 1.0f, 0.2f);

                Mat4 invTransform = modelMatrix.top().inverse();
                Vec4 lightPosModelSpace = invTransform.times(lightPosCameraSpace);

                if (drawColoredCyl) {
                    gl.glUseProgram(vertColorProgram.theProgram);
                    gl.glUniformMatrix4fv(vertColorProgram.modelToCameraMatrixUnif, 1, false,
                            modelMatrix.top().to(matBuffer));

                    gl.glUniform3fv(vertColorProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace.to(vecBuffer));

                    cylinder.render(gl, "lit-color");
                } else {
                    gl.glUseProgram(whiteProgram.theProgram);
                    gl.glUniformMatrix4fv(whiteProgram.modelToCameraMatrixUnif, 1, false,
                            modelMatrix.top().to(matBuffer));

                    gl.glUniform3fv(whiteProgram.modelSpaceLightPosUnif, 1, lightPosModelSpace.to(vecBuffer));

                    cylinder.render(gl, "lit");
                }
                gl.glUseProgram(0);

                modelMatrix.pop();
            }

            if (drawLight) {

                modelMatrix
                        .push()
                        .translate(worldLightPos)
                        .scale(0.1f, 0.1f, 0.1f);

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

        perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, perspMatrix.top().to(matBuffer));
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

            case KeyEvent.VK_Y:
                drawLight = !drawLight;
                break;
            case KeyEvent.VK_T:
                scaleCyl = !scaleCyl;
                break;
            case KeyEvent.VK_H:
                useFragmentLighting = !useFragmentLighting;
                break;
            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;
        }
        if (lightRadius < 0.2f)
            lightRadius = 0.2f;
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(vertexDiffuseColor.theProgram);
        gl.glDeleteProgram(whiteDiffuseColor.theProgram);
        gl.glDeleteProgram(fragVertexDiffuseColor.theProgram);
        gl.glDeleteProgram(fragWhiteDiffuseColor.theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(1, projectionUniformBuffer);

        cylinder.dispose(gl);
        plane.dispose(gl);
        cube.dispose(gl);

        destroyBuffer(projectionUniformBuffer);
    }

    private class ProgramData {

        public int theProgram;

        public int modelSpaceLightPosUnif;
        public int lightIntensityUnif;
        public int ambientIntensityUnif;

        public int modelToCameraMatrixUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut10", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            modelSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "modelSpaceLightPos");
            lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity");
            ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
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