
package main.tut11;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import glm.Glm;
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
import static glm.GlmKt.glm;
import static main.tut11.GaussianSpecularLighting.LightingModel.*;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class GaussianSpecularLighting extends Framework {

    public static void main(String[] args) {
        new GaussianSpecularLighting("Tutorial 11 - Gaussian Specular Lighting");
    }

    private ProgramPairs[] programs = new ProgramPairs[LightingModel.MAX];
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

    private int lightModel = LightingModel.BlinnSpecular;

    private boolean drawColoredCyl = false, drawLightSource = false, scaleCyl = false, drawDark = false;

    private float lightHeight = 1.5f, lightRadius = 1.0f, lightAttenuation = 1.2f;

    private Vec4 darkColor = new Vec4(0.2f, 0.2f, 0.2f, 1.0f), lightColor = new Vec4(1.0f);

    private Timer lightTimer = new Timer(Timer.Type.Loop, 5.0f);

    private IntBuffer projectionUniformBuffer = GLBuffers.newDirectIntBuffer(1);

    public GaussianSpecularLighting(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            cylinder = new Mesh(gl, getClass(), "tut11/UnitCylinder.xml");
            plane = new Mesh(gl, getClass(), "tut11/LargePlane.xml");
            cube = new Mesh(gl, getClass(), "tut11/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(GaussianSpecularLighting.class.getName()).log(Level.SEVERE, null, ex);
        }

        float depthZNear = 0.0f, depthZFar = 1.0f;

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(depthZNear, depthZFar);
        gl.glEnable(GL_DEPTH_CLAMP);

        gl.glGenBuffers(1, projectionUniformBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, projectionUniformBuffer.get(0), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void initializePrograms(GL3 gl) {
        String[] FRAGMENTS = {"phong-lighting", "phong-only", "blinn-lighting", "blinn-only", "gaussian-lighting", "gaussian-only"};
        for (int i = 0; i < programs.length; i++) {
            programs[i] = new ProgramPairs();
            programs[i].whiteProgram = new ProgramData(gl, "pn.vert", FRAGMENTS[i] + ".frag");
            programs[i].colorProgram = new ProgramData(gl, "pcn.vert", FRAGMENTS[i] + ".frag");
        }
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

        ProgramData whiteProg = programs[lightModel].whiteProgram;
        ProgramData colorProg = programs[lightModel].colorProgram;

        gl.glUseProgram(whiteProg.theProgram);
        gl.glUniform4f(whiteProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(whiteProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUniform3fv(whiteProg.cameraSpaceLightPosUnif, 1, lightPosCameraSpace.to(vecBuffer));
        gl.glUniform1f(whiteProg.lightAttenuationUnif, lightAttenuation);
        gl.glUniform1f(whiteProg.shininessFactorUnif, MaterialParameters.getSpecularValue(lightModel));
        gl.glUniform4fv(whiteProg.baseDiffuseColorUnif, 1, (drawDark ? darkColor : lightColor).to(vecBuffer));

        gl.glUseProgram(colorProg.theProgram);
        gl.glUniform4f(colorProg.lightIntensityUnif, 0.8f, 0.8f, 0.8f, 1.0f);
        gl.glUniform4f(colorProg.ambientIntensityUnif, 0.2f, 0.2f, 0.2f, 1.0f);
        gl.glUniform3fv(colorProg.cameraSpaceLightPosUnif, 1, lightPosCameraSpace.to(vecBuffer));
        gl.glUniform1f(colorProg.lightAttenuationUnif, lightAttenuation);
        gl.glUniform1f(colorProg.shininessFactorUnif, MaterialParameters.getSpecularValue(lightModel));
        gl.glUseProgram(0);

        {
            modelMatrix.push();

            //Render the ground plane.
            {
                modelMatrix.push();

                Mat3 normMatrix = modelMatrix.top().toMat3();
                normMatrix.inverse_().transpose_();

                gl.glUseProgram(whiteProg.theProgram);
                gl.glUniformMatrix4fv(whiteProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

                gl.glUniformMatrix3fv(whiteProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));
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

                Mat3 normMatrix = modelMatrix.top().toMat3();
                normMatrix.inverse_().transpose_();

                ProgramData prog = drawColoredCyl ? colorProg : whiteProg;
                gl.glUseProgram(prog.theProgram);
                gl.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

                gl.glUniformMatrix3fv(prog.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

                if (drawColoredCyl)
                    cylinder.render(gl, "lit-color");
                else
                    cylinder.render(gl, "lit");

                gl.glUseProgram(0);
                modelMatrix.pop();
            }

            //Render the light
            if (drawLightSource) {

                modelMatrix
                        .push()
                        .translate(worldLightPos)
                        .scale(0.1f);

                gl.glUseProgram(unlit.theProgram);
                gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
                gl.glUniform4f(unlit.objectColorUnif, 0.8078f, 0.8706f, 0.9922f, 1.0f);
                cube.render(gl, "flat");
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

        gl.glBindBuffer(GL_UNIFORM_BUFFER, projectionUniformBuffer.get(0));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, proj.to(matBuffer));
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

        boolean changedShininess = false;

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
                MaterialParameters.increment(lightModel, !e.isShiftDown());
                changedShininess = true;
                break;
            case KeyEvent.VK_U:
                MaterialParameters.decrement(lightModel, !e.isShiftDown());
                changedShininess = true;
                break;

            case KeyEvent.VK_Y:
                drawLightSource = !drawLightSource;
                break;
            case KeyEvent.VK_T:
                scaleCyl = !scaleCyl;
                break;
            case KeyEvent.VK_B:
                lightTimer.togglePause();
                break;
            case KeyEvent.VK_G:
                drawDark = !drawDark;
                break;

            case KeyEvent.VK_H:
                if (e.isShiftDown())
                    if ((lightModel % 2) != 0)
                        lightModel -= 1;
                    else
                        lightModel += 1;
                else
                    lightModel = (lightModel + 2) % LightingModel.MAX;
                System.out.println(LightingModel.getName(lightModel));
                break;
        }

        if (lightRadius < 0.2f)
            lightRadius = 0.2f;

        if (changedShininess)
            System.out.println("Shiny: " + MaterialParameters.getSpecularValue(lightModel));
    }

    @Override
    public void end(GL3 gl) {

        for (ProgramPairs programPair : programs) {
            gl.glDeleteProgram(programPair.whiteProgram.theProgram);
            gl.glDeleteProgram(programPair.colorProgram.theProgram);
        }
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(1, projectionUniformBuffer);

        cylinder.dispose(gl);
        plane.dispose(gl);
        cube.dispose(gl);

        destroyBuffer(projectionUniformBuffer);
    }

    interface LightingModel {

        int PhongSpecular = 0;
        int PhongOnly = 1;
        int BlinnSpecular = 2;
        int BlinnOnly = 3;
        int GaussianSpecular = 4;
        int GaussianOnly = 5;
        int MAX = 6;

        static String getName(int model) {
            switch (model) {
                case PhongSpecular:
                    return "PhongSpecular";
                case PhongOnly:
                    return "PhongOnly";
                case BlinnSpecular:
                    return "BlinnSpecular";
                case BlinnOnly:
                    return "BlinnOnly";
                case GaussianSpecular:
                    return "GaussianSpecular";
            }
            return "GaussianOnly";
        }
    }

    private class ProgramPairs {

        public ProgramData whiteProgram;
        public ProgramData colorProgram;
    }

    private class ProgramData {

        public int theProgram;

        public int modelToCameraMatrixUnif;

        public int lightIntensityUnif;
        public int ambientIntensityUnif;

        public int normalModelToCameraMatrixUnif;
        public int cameraSpaceLightPosUnif;

        public int lightAttenuationUnif;
        public int shininessFactorUnif;
        public int baseDiffuseColorUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut11", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
            lightIntensityUnif = gl.glGetUniformLocation(theProgram, "lightIntensity");
            ambientIntensityUnif = gl.glGetUniformLocation(theProgram, "ambientIntensity");

            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");
            cameraSpaceLightPosUnif = gl.glGetUniformLocation(theProgram, "cameraSpaceLightPos");

            lightAttenuationUnif = gl.glGetUniformLocation(theProgram, "lightAttenuation");
            shininessFactorUnif = gl.glGetUniformLocation(theProgram, "shininessFactor");
            baseDiffuseColorUnif = gl.glGetUniformLocation(theProgram, "baseDiffuseColor");

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

        public UnlitProgData(GL3  gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut11", vertex, fragment);

            objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor");
            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
        }
    }

    public static class MaterialParameters {

        private static float phongExponent = 4.0f;
        private static float blinnExponent = 4.0f;
        private static float gaussianRoughness = 0.5f;

        static float getSpecularValue(int model) {

            switch (model) {

                case PhongSpecular:
                case PhongOnly:
                    return phongExponent;

                case BlinnSpecular:
                case BlinnOnly:
                    return blinnExponent;

                default:
                    return gaussianRoughness;
            }
        }

        static void increment(int model, boolean isLarge) {

            switch (model) {

                case PhongSpecular:
                case PhongOnly:
                    phongExponent += isLarge ? 0.5f : 0.1f;
                    break;

                case BlinnSpecular:
                case BlinnOnly:
                    blinnExponent += isLarge ? 0.5f : 0.1f;
                    break;

                default:
                    gaussianRoughness += isLarge ? 0.1f : 0.01f;
                    break;
            }
            clampParam(model);
        }

        static void decrement(int model, boolean isLarge) {

            switch (model) {

                case PhongSpecular:
                case PhongOnly:
                    phongExponent -= isLarge ? 0.5f : 0.1f;
                    break;

                case BlinnSpecular:
                case BlinnOnly:
                    blinnExponent -= isLarge ? 0.5f : 0.1f;
                    break;

                default:
                    gaussianRoughness -= isLarge ? 0.1f : 0.01f;
                    break;
            }
            clampParam(model);
        }

        private static void clampParam(int model) {

            switch (model) {

                case PhongSpecular:
                case PhongOnly:
                    if (phongExponent <= 0.0f) {
                        phongExponent = 0.0001f;
                    }
                    break;

                case BlinnSpecular:
                case BlinnOnly:
                    if (blinnExponent <= 0.0f) {
                        blinnExponent = 0.0001f;
                    }
                    break;

                default:
                    gaussianRoughness = glm.clamp(gaussianRoughness, 0.00001f, 1.0f);
                    break;
            }
        }
    }
}