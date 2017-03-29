
package main.tut13;

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
import uno.gl.UniformBlockArray;
import uno.glm.MatrixStack;
import uno.mousePole.ViewData;
import uno.mousePole.ViewPole;
import uno.mousePole.ViewScale;
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
import static com.jogamp.opengl.GL.GL_TRIANGLE_STRIP;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * @author elect
 */
public class BasicImpostor extends Framework {

    public static void main(String[] args) {
        new BasicImpostor("Tutorial 13 - Basic Impostor");
    }

    private ProgramMeshData litMeshProg;
    private ProgramImposData[] litImpProgs = new ProgramImposData[Impostors.MAX];
    private UnlitProgData unlit;

    private ViewData initialViewData = new ViewData(
            new Vec3(0.0f, 30.0f, 25.0f),
            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            10.0f,
            0.0f);
    private ViewScale viewScale = new ViewScale(
            3.0f, 70.0f,
            3.5f, 1.5f,
            5.0f, 1.0f,
            90.0f / 250.0f);
    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);

    private Mesh sphere, plane, cube;

    private interface Buffer {

        int PROJECTION = 0;
        int LIGHT = 1;
        int MATERIAL = 2;
        int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX), imposterVAO = GLBuffers.newDirectIntBuffer(1);

    private static final int NUMBER_OF_LIGHTS = 2;

    private int currImpostor = Impostors.Basic;

    private boolean drawCameraPos = false, drawLights = true;

    private boolean[] drawImposter = {false, false, false, false};

    private float lightHeight = 20.0f, halfLightDistance = 25.0f, lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance);

    private Timer sphereTimer = new Timer(Timer.Type.Loop, 6.0f);

    private int materialBlockOffset = 0;

    public BasicImpostor(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            sphere = new Mesh(gl, getClass(), "tut13/UnitSphere.xml");
            plane = new Mesh(gl, getClass(), "tut13/LargePlane.xml");
            cube = new Mesh(gl, getClass(), "tut13/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(BasicImpostor.class.getName()).log(Level.SEVERE, null, ex);
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

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT), 0, LightBlock.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        //Empty Vertex Array Object.
        gl.glGenVertexArrays(1, imposterVAO);

        createMaterials(gl);
    }

    private void initializePrograms(GL3 gl) {

        String[] impShaderNames = {"basic-impostor", "persp-impostor", "depth-impostor"};

        litMeshProg = new ProgramMeshData(gl, "pn.vert", "lighting.frag");

        for (int loop = 0; loop < Impostors.MAX; loop++)
            litImpProgs[loop] = new ProgramImposData(gl, impShaderNames[loop]);

        unlit = new UnlitProgData(gl, "unlit");
    }

    private static class MaterialBlock {

        public static int SIZE = 3 * Vec4.SIZE;
        public static ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public Vec4 diffuseColor = new Vec4();
        public Vec4 specularColor = new Vec4();
        public float specularShininess;
        public float[] padding = new float[3];

        public ByteBuffer toBuffer() {
            diffuseColor.to(buffer);
            specularColor.to(buffer, Vec4.SIZE);
            return buffer.putFloat(Vec4.SIZE * 2, specularShininess);
        }
    }

    private void createMaterials(GL3 gl) {

        UniformBlockArray ubArray = new UniformBlockArray(gl, MaterialBlock.SIZE, Materials.MAX);
        materialBlockOffset = ubArray.getArrayOffset();

        MaterialBlock mtl = new MaterialBlock();
        mtl.diffuseColor.put(0.5f, 0.5f, 0.5f, 1.0f);
        mtl.specularColor.put(0.5f, 0.5f, 0.5f, 1.0f);
        mtl.specularShininess = 0.6f;
        ubArray.set(Materials.Terrain, mtl.toBuffer());

        mtl.diffuseColor.put(0.1f, 0.1f, 0.8f, 1.0f);
        mtl.specularColor.put(0.8f, 0.8f, 0.8f, 1.0f);
        mtl.specularShininess = 0.1f;
        ubArray.set(Materials.BlueShiny, mtl.toBuffer());

        mtl.diffuseColor.put(0.803f, 0.709f, 0.15f, 1.0f);
        mtl.specularColor.put(new Vec4(0.803f, 0.709f, 0.15f, 1.0f).times(0.75));
        mtl.specularShininess = 0.18f;
        ubArray.set(Materials.GoldMetal, mtl.toBuffer());

        mtl.diffuseColor.put(0.4f, 0.4f, 0.4f, 1.0f);
        mtl.specularColor.put(0.1f, 0.1f, 0.1f, 1.0f);
        mtl.specularShininess = 0.8f;
        ubArray.set(Materials.DullGrey, mtl.toBuffer());

        mtl.diffuseColor.put(0.05f, 0.05f, 0.05f, 1.0f);
        mtl.specularColor.put(0.95f, 0.95f, 0.95f, 1.0f);
        mtl.specularShininess = 0.3f;
        ubArray.set(Materials.BlackShiny, mtl.toBuffer());

        ubArray.uploadBufferObject(gl, bufferName.get(Buffer.MATERIAL));
        ubArray.dispose();
    }

    @Override
    public void display(GL3 gl) {

        sphereTimer.update();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());
        Mat4 worldToCamMat = modelMatrix.top();

        LightBlock lightData = new LightBlock();

        lightData.ambientIntensity.put(0.2f, 0.2f, 0.2f, 1.0f);
        lightData.lightAttenuation = lightAttenuation;

        lightData.lights[0].cameraSpaceLightPos.put(worldToCamMat.times(new Vec4(0.707f, 0.707f, 0.0f, 0.0f)));
        lightData.lights[0].lightIntensity.put(0.6f, 0.6f, 0.6f, 1.0f);

        lightData.lights[1].cameraSpaceLightPos.put(worldToCamMat.times(calcLightPosition()));
        lightData.lights[1].lightIntensity.put(0.4f, 0.4f, 0.4f, 1.0f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.toBuffer());
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        {
            gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
                    Materials.Terrain * materialBlockOffset, MaterialBlock.SIZE);

            Mat3 normMatrix = modelMatrix.top().toMat3();
            normMatrix.inverse_().transpose_();

            gl.glUseProgram(litMeshProg.theProgram);
            gl.glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

            plane.render(gl);

            gl.glUseProgram(0);
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
        }

        drawSphere(gl, modelMatrix, new Vec3(0.0f, 10.0f, 0.0f), 4.0f, Materials.BlueShiny, drawImposter[0]);

        drawSphereOrbit(gl, modelMatrix, new Vec3(0.0f, 10.0f, 0.0f), new Vec3(0.6f, 0.8f, 0.0f), 20.0f,
                sphereTimer.getAlpha(), 2.0f, Materials.DullGrey, drawImposter[1]);

        drawSphereOrbit(gl, modelMatrix, new Vec3(-10.0f, 1.0f, 0.0f), new Vec3(0.0f, 1.0f, 0.0f), 10.0f,
                sphereTimer.getAlpha(), 1.0f, Materials.BlackShiny, drawImposter[2]);

        drawSphereOrbit(gl, modelMatrix, new Vec3(10.0f, 1.0f, 0.0f), new Vec3(0.0f, 1.0f, 0.0f), 10.0f,
                sphereTimer.getAlpha() * 2.0f, 1.0f, Materials.GoldMetal, drawImposter[3]);

        if (drawLights) {

            modelMatrix
                    .push()
                    .translate(calcLightPosition())
                    .scale(0.5f);

            gl.glUseProgram(unlit.theProgram);
            gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

            Vec4 lightColor = new Vec4(1.0f);
            gl.glUniform4fv(unlit.objectColorUnif, 1, lightColor.to(vecBuffer));
            cube.render(gl, "flat");

            modelMatrix.pop();
        }

        if (drawCameraPos) {

            modelMatrix
                    .push()
                    .setIdentity()
                    .translate(0.0f, 0.0f, -viewPole.getView().radius);

            gl.glDisable(GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glUseProgram(unlit.theProgram);
            gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f);
            cube.render(gl, "flat");

            gl.glDepthMask(true);
            gl.glEnable(GL_DEPTH_TEST);
            gl.glUniform4f(unlit.objectColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            cube.render(gl, "flat");

            modelMatrix.pop();
        }
    }

    private Vec4 calcLightPosition() {

        float scale = Glm.PIf * 2.0f;

        float timeThroughLoop = sphereTimer.getAlpha();
        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = glm.cos(timeThroughLoop * scale) * 20.0f;
        ret.z = glm.sin(timeThroughLoop * scale) * 20.0f;

        return ret;
    }

    private void drawSphere(GL3 gl, MatrixStack modelMatrix, Vec3 position, float radius, int material) {
        drawSphere(gl, modelMatrix, position, radius, material, false);
    }

    private void drawSphere(GL3 gl, MatrixStack modelMatrix, Vec3 position, float radius, int material, boolean drawImposter) {

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
                material * materialBlockOffset, MaterialBlock.SIZE);

        if (drawImposter) {

            Vec4 cameraSpherePos = modelMatrix.top().times(new Vec4(position, 1.0f));
            gl.glUseProgram(litImpProgs[currImpostor].theProgram);
            gl.glUniform3fv(litImpProgs[currImpostor].cameraSpherePosUnif, 1, cameraSpherePos.to(vecBuffer));
            gl.glUniform1f(litImpProgs[currImpostor].sphereRadiusUnif, radius);

            gl.glBindVertexArray(imposterVAO.get(0));

            gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

            gl.glBindVertexArray(0);
            gl.glUseProgram(0);

        } else {

            modelMatrix
                    .push()
                    .translate(position)
                    .scale(radius * 2.0f); //The unit sphere has a radius 0.5f.

            Mat3 normMatrix = modelMatrix.top().toMat3();
            normMatrix.inverse_().transpose_();

            gl.glUseProgram(litMeshProg.theProgram);
            gl.glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

            sphere.render(gl, "lit");

            gl.glUseProgram(0);

            modelMatrix.pop();
        }
        gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
    }

    private void drawSphereOrbit(GL3 gl, MatrixStack modelMatrix, Vec3 orbitCenter, Vec3 orbitAxis, float orbitRadius,
                                 float orbitAlpha, float sphereRadius, int material) {
        drawSphereOrbit(gl, modelMatrix, orbitCenter, orbitAxis, orbitRadius, orbitAlpha, sphereRadius, material, false);
    }

    private void drawSphereOrbit(GL3 gl, MatrixStack modelMatrix, Vec3 orbitCenter, Vec3 orbitAxis, float orbitRadius,
                                 float orbitAlpha, float sphereRadius, int material, boolean drawImposter) {

        modelMatrix
                .push()
                .translate(orbitCenter)
                .rotate(orbitAxis, 360.0f * orbitAlpha);

        Vec3 offsetDir = orbitAxis.cross(new Vec3(0.0f, 1.0f, 0.0f));
        if (offsetDir.length() < 0.001f)
            offsetDir = orbitAxis.cross_(new Vec3(1.0f, 0.0f, 0.0f));

        offsetDir.normalize_();

        modelMatrix.translate(offsetDir.times(orbitRadius));

        drawSphere(gl, modelMatrix, new Vec3(0.0f), sphereRadius, material, drawImposter);

        modelMatrix.pop();
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack perspMatrix = new MatrixStack();

        Mat4 proj = perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar).top();

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, proj.to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        viewPole.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        viewPole.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        viewPole.mouseReleased(e);
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

            case KeyEvent.VK_P:
                sphereTimer.togglePause();
                break;
            case KeyEvent.VK_MINUS:
                sphereTimer.rewind(0.5f);
                break;
            case KeyEvent.VK_PLUS:
                sphereTimer.fastForward(0.5f);
                break;
            case KeyEvent.VK_T:
                drawCameraPos = !drawCameraPos;
                break;
            case KeyEvent.VK_G:
                drawLights = !drawLights;
                break;

            case KeyEvent.VK_1:
                drawImposter[0] = !drawImposter[0];
                break;
            case KeyEvent.VK_2:
                drawImposter[1] = !drawImposter[1];
                break;
            case KeyEvent.VK_3:
                drawImposter[2] = !drawImposter[2];
                break;
            case KeyEvent.VK_4:
                drawImposter[3] = !drawImposter[3];
                break;

            case KeyEvent.VK_L:
                currImpostor = Impostors.Basic;
                break;
            case KeyEvent.VK_J:
                currImpostor = Impostors.Perspective;
                break;
            case KeyEvent.VK_H:
                currImpostor = Impostors.Depth;
                break;
        }

        viewPole.keyPressed(e);
    }

    @Override
    public void end(GL3 gl) {

        for (int i = 0; i < NUMBER_OF_LIGHTS; i++)
            gl.glDeleteProgram(litImpProgs[i].theProgram);
        gl.glDeleteProgram(litMeshProg.theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteVertexArrays(1, imposterVAO);

        sphere.dispose(gl);
        plane.dispose(gl);
        cube.dispose(gl);

        destroyBuffers(bufferName, imposterVAO, LightBlock.buffer, MaterialBlock.buffer);
    }

    interface Materials {
        int Terrain = 0;
        int BlueShiny = 1;
        int GoldMetal = 2;
        int DullGrey = 3;
        int BlackShiny = 4;
        int MAX = 5;
    }

    interface Impostors {
        int Basic = 0;
        int Perspective = 1;
        int Depth = 2;
        int MAX = 3;
    }

    private static class PerLight {

        public static final int SIZE = Vec4.SIZE * 2;

        public Vec4 cameraSpaceLightPos = new Vec4();
        public Vec4 lightIntensity = new Vec4();

        public void to(ByteBuffer buffer, int offset) {
            cameraSpaceLightPos.to(buffer, offset);
            lightIntensity.to(buffer, offset + Vec4.SIZE);
        }
    }

    private static class LightBlock {

        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;

        public Vec4 ambientIntensity = new Vec4();
        public float lightAttenuation;
        public float[] padding = new float[3];
        public PerLight[] lights = {new PerLight(), new PerLight()};

        public static ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public ByteBuffer toBuffer() {
            ambientIntensity.to(buffer);
            buffer.putFloat(Vec4.SIZE, lightAttenuation);
            for (int i = 0; i < NUMBER_OF_LIGHTS; i++)
                lights[i].to(buffer, Vec4.SIZE * 2 + PerLight.SIZE * i);
            return buffer;
        }
    }

    private class ProgramImposData {

        public int theProgram;

        public int sphereRadiusUnif;
        public int cameraSpherePosUnif;

        public ProgramImposData(GL3 gl, String shader) {

            theProgram = programOf(gl, getClass(), "tut13", shader + ".vert", shader + ".frag");

            sphereRadiusUnif = gl.glGetUniformLocation(theProgram, "sphereRadius");
            cameraSpherePosUnif = gl.glGetUniformLocation(theProgram, "cameraSpherePos");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Light"),
                    Semantic.Uniform.LIGHT);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Material"),
                    Semantic.Uniform.MATERIAL);
        }
    }

    private class ProgramMeshData {

        public int theProgram;

        public int modelToCameraMatrixUnif;
        public int normalModelToCameraMatrixUnif;

        public ProgramMeshData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut13", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Light"),
                    Semantic.Uniform.LIGHT);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Material"),
                    Semantic.Uniform.MATERIAL);
        }
    }

    private class UnlitProgData {

        public int theProgram;

        public int objectColorUnif;
        public int modelToCameraMatrixUnif;

        public UnlitProgData(GL3 gl, String shader) {

            theProgram = programOf(gl, getClass(), "tut13", shader + ".vert", shader + ".frag");

            objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor");
            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
        }
    }
}