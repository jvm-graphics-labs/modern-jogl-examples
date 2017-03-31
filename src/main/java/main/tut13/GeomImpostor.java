
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

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static com.jogamp.opengl.GL3.GL_PROGRAM_POINT_SIZE;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * @author elect
 */
public class GeomImpostor extends Framework {

    public static void main(String[] args) {
        new GeomImpostor().setup("Tutorial 13 - Geometry Impostor");
    }

    private ProgramMeshData litMeshProg;
    private ProgramImposData litImpProg;
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

    private static final int NUMBER_OF_LIGHTS = 2, NUMBER_OF_SPHERES = 4;

    private interface Buffer {

        int PROJECTION = 0;
        int LIGHT = 1;
        int MATERIAL_ARRAY = 2;
        int MATERIAL_TERRAIN = 3;
        int IMPOSTER = 4;
        int MAX = 5;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            imposterVAO = GLBuffers.newDirectIntBuffer(1);

    private boolean drawCameraPos = false, drawLights = true;

    private Timer sphereTimer = new Timer(Timer.Type.Loop, 6.0f);

    private float lightHeight = 20.0f, halfLightDistance = 25.0f,
            lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance);

    private ByteBuffer impostorBuffer = GLBuffers.newDirectByteBuffer(NUMBER_OF_SPHERES * VertexData.SIZE);

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            sphere = new Mesh(gl, getClass(), "tut13/UnitSphere.xml");
            plane = new Mesh(gl, getClass(), "tut13/LargePlane.xml");
            cube = new Mesh(gl, getClass(), "tut13/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(GeomImpostor.class.getName()).log(Level.SEVERE, null, ex);
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

        //Setup our Uniform Buffers
        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT), 0, LightBlock.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.IMPOSTER));
        gl.glBufferData(GL_ARRAY_BUFFER, NUMBER_OF_SPHERES * VertexData.SIZE, null, GL_STREAM_DRAW);

        gl.glGenVertexArrays(1, imposterVAO);
        gl.glBindVertexArray(imposterVAO.get(0));
        gl.glEnableVertexAttribArray(Semantic.Attr.CAMERA_SPHERE_POS);
        gl.glVertexAttribPointer(Semantic.Attr.CAMERA_SPHERE_POS, 3, GL_FLOAT, false, VertexData.SIZE,
                VertexData.OFFSET_CAMERA_POSITION);
        gl.glEnableVertexAttribArray(Semantic.Attr.SPHERE_RADIUS);
        gl.glVertexAttribPointer(Semantic.Attr.SPHERE_RADIUS, 1, GL_FLOAT, false, VertexData.SIZE,
                VertexData.OFFSET_SPHERE_RADIUS);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glEnable(GL_PROGRAM_POINT_SIZE);

        createMaterials(gl);
    }

    private static class MaterialEntry {

        public static final int SIZE = 3 * Vec4.SIZE;

        public Vec4 diffuseColor;
        public Vec4 specularColor;
        public float specularShininess;
        public float[] padding = new float[3];

        public ByteBuffer to(ByteBuffer buffer) {
            return to(buffer, 0);
        }

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            diffuseColor.to(buffer, offset);
            specularColor.to(buffer, offset + Vec4.SIZE);
            return buffer.putFloat(offset + Vec4.SIZE * 2, specularShininess);
        }
    }

    private void createMaterials(GL3 gl) {

        MaterialEntry[] ubArray = new MaterialEntry[NUMBER_OF_SPHERES];

        ubArray[0] = new MaterialEntry();
        ubArray[0].diffuseColor = new Vec4(0.1f, 0.1f, 0.8f, 1.0f);
        ubArray[0].specularColor = new Vec4(0.8f, 0.8f, 0.8f, 1.0f);
        ubArray[0].specularShininess = 0.1f;

        ubArray[1] = new MaterialEntry();
        ubArray[1].diffuseColor = new Vec4(0.4f, 0.4f, 0.4f, 1.0f);
        ubArray[1].specularColor = new Vec4(0.1f, 0.1f, 0.1f, 1.0f);
        ubArray[1].specularShininess = 0.8f;

        ubArray[2] = new MaterialEntry();
        ubArray[2].diffuseColor = new Vec4(0.05f, 0.05f, 0.05f, 1.0f);
        ubArray[2].specularColor = new Vec4(0.95f, 0.95f, 0.95f, 1.0f);
        ubArray[2].specularShininess = 0.3f;

        ubArray[3] = new MaterialEntry();
        ubArray[3].diffuseColor = new Vec4(0.803f, 0.709f, 0.15f, 1.0f);
        ubArray[3].specularColor = new Vec4(0.803f, 0.709f, 0.15f, 1.0f).times(0.75f);
        ubArray[2].specularShininess = 0.18f;

        ByteBuffer arrayBuffer = GLBuffers.newDirectByteBuffer(ubArray.length * MaterialEntry.SIZE);
        for (int i = 0; i < ubArray.length; i++)
            ubArray[i].to(arrayBuffer, i * MaterialEntry.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL_ARRAY));
        gl.glBufferData(GL_UNIFORM_BUFFER, arrayBuffer.capacity(), arrayBuffer, GL_STATIC_DRAW);

        MaterialEntry mtl = new MaterialEntry();
        mtl.diffuseColor = new Vec4(0.5f, 0.5f, 0.5f, 1.0f);
        mtl.specularColor = new Vec4(0.5f, 0.5f, 0.5f, 1.0f);
        mtl.specularShininess = 0.6f;
        ByteBuffer terrainBuffer = GLBuffers.newDirectByteBuffer(MaterialEntry.SIZE);
        mtl.to(terrainBuffer);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL_TERRAIN));
        gl.glBufferData(GL_UNIFORM_BUFFER, MaterialEntry.SIZE, terrainBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        destroyBuffers(arrayBuffer, terrainBuffer);
    }

    private void initializePrograms(GL3 gl) {

        litMeshProg = new ProgramMeshData(gl, "pn.vert", "lighting.frag");

        litImpProg = new ProgramImposData(gl, "geom-impostor");

        unlit = new UnlitProgData(gl, "unlit");
    }

    @Override
    public void display(GL3 gl) {

        sphereTimer.update();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());

        Mat4 worldToCamMat = modelMatrix.top();

        LightBlock lightData = new LightBlock();

        lightData.ambientIntensity = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
        lightData.lightAttenuation = lightAttenuation;

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat.times(new Vec4(0.707f, 0.707f, 0.0f, 0.0f));
        lightData.lights[0].lightIntensity = new Vec4(0.6f, 0.6f, 0.6f, 1.0f);

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat.times(calcLightPosition());
        lightData.lights[1].lightIntensity = new Vec4(0.4f, 0.4f, 0.4f, 1.0f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.toBuffer());
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        {
            gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL_TERRAIN), 0, MaterialEntry.SIZE);

            Mat3 normMatrix = modelMatrix.top().toMat3();
            normMatrix.inverse_().transpose_();

            gl.glUseProgram(litMeshProg.theProgram);
            gl.glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

            plane.render(gl);

            gl.glUseProgram(0);
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
        }

        {
            VertexData[] posSizeArray = new VertexData[NUMBER_OF_SPHERES];

            posSizeArray[0] = new VertexData();
            posSizeArray[0].cameraPosition = worldToCamMat.times(new Vec4(0.0f, 10.0f, 0.0f, 1.0f)).toVec3();
            posSizeArray[0].sphereRadius = 4.0f;

            posSizeArray[1] = new VertexData();
            posSizeArray[1].cameraPosition = getSphereOrbitPos(modelMatrix, new Vec3(0.0f, 10.0f, 0.0f),
                    new Vec3(0.6, 0.8f, 0.0f), 20.0f, sphereTimer.getAlpha());
            posSizeArray[1].sphereRadius = 2.0f;

            posSizeArray[2] = new VertexData();
            posSizeArray[2].cameraPosition = getSphereOrbitPos(modelMatrix, new Vec3(-10.0f, 1.0f, 0.0f),
                    new Vec3(0.0, 1.0f, 0.0f), 10.0f, sphereTimer.getAlpha());
            posSizeArray[2].sphereRadius = 1.0f;

            posSizeArray[3] = new VertexData();
            posSizeArray[3].cameraPosition = getSphereOrbitPos(modelMatrix, new Vec3(10.0f, 1.0f, 0.0f),
                    new Vec3(0.0, 1.0f, 0.0f), 10.0f, sphereTimer.getAlpha() * 2.0f);
            posSizeArray[3].sphereRadius = 1.0f;

            for (int i = 0; i < posSizeArray.length; i++)
                posSizeArray[i].to(impostorBuffer, i * VertexData.SIZE);

            gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.IMPOSTER));
            gl.glBufferData(GL_ARRAY_BUFFER, NUMBER_OF_SPHERES * VertexData.SIZE, impostorBuffer, GL_STREAM_DRAW);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        {
            gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL_ARRAY), 0,
                    MaterialEntry.SIZE * NUMBER_OF_SPHERES);

            gl.glUseProgram(litImpProg.theProgram);
            gl.glBindVertexArray(imposterVAO.get(0));
            gl.glDrawArrays(GL_POINTS, 0, NUMBER_OF_SPHERES);
            gl.glBindVertexArray(0);
            gl.glUseProgram(0);

            gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
        }

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

    private Vec3 getSphereOrbitPos(MatrixStack modelMatrix, Vec3 orbitCenter, Vec3 orbitAxis, float orbitRadius, float orbitAlpha) {

        modelMatrix
                .push()
                .translate(orbitCenter)
                .rotate(orbitAxis, 360.0f * orbitAlpha);

        Vec3 offsetDir = orbitAxis.cross(new Vec3(0.0f, 1.0f, 0.0f));
        if (offsetDir.length() < 0.001f)
            offsetDir = orbitAxis.cross(new Vec3(1.0f, 0.0f, 0.0f));

        offsetDir.normalize_();

        modelMatrix.translate(offsetDir.times(orbitRadius));

        Vec3 result = modelMatrix.top().times(new Vec4(0.0f, 0.0f, 0.0f, 1.0f)).toVec3();

        modelMatrix.pop();

        return result;
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
        }

        viewPole.keyPressed(e);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(litImpProg.theProgram);
        gl.glDeleteProgram(litMeshProg.theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteVertexArrays(1, imposterVAO);

        sphere.dispose(gl);
        plane.dispose(gl);
        cube.dispose(gl);

        destroyBuffers(bufferName, imposterVAO, LightBlock.buffer, impostorBuffer);
    }

    private static class PerLight {

        public static final int SIZE = Vec4.SIZE * 2;

        public Vec4 cameraSpaceLightPos;
        public Vec4 lightIntensity;

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            cameraSpaceLightPos.to(buffer, offset);
            return lightIntensity.to(buffer, offset + Vec4.SIZE);
        }
    }

    private static class LightBlock {

        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;
        public static final ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public Vec4 ambientIntensity;
        public float lightAttenuation;
        public float[] padding = new float[3];
        public PerLight[] lights = {new PerLight(), new PerLight()};

        public ByteBuffer toBuffer() {
            ambientIntensity.to(buffer);
            buffer.putFloat(Vec4.SIZE, lightAttenuation);
            for (int i = 0; i < NUMBER_OF_LIGHTS; i++)
                lights[i].to(buffer, Vec4.SIZE * 2 + PerLight.SIZE * i);
            return buffer;
        }
    }

    private static class VertexData {

        public final static int SIZE = Vec3.SIZE + Float.BYTES;
        public final static int OFFSET_CAMERA_POSITION = 0;
        public final static int OFFSET_SPHERE_RADIUS = OFFSET_CAMERA_POSITION + Vec3.SIZE;

        public Vec3 cameraPosition;
        public float sphereRadius;

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            cameraPosition.to(buffer, offset);
            return buffer.putFloat(offset + Vec3.SIZE, sphereRadius);
        }
    }

    private class ProgramImposData {

        public int theProgram;

        public ProgramImposData(GL3 gl, String shader) {

            theProgram = programOf(gl, getClass(), "tut13", shader + ".vert", shader + ".geom", shader + ".frag");

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