
package main.tut14;

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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_R8;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL2ES2.GL_RED;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;
import static glm.GlmKt.glm;

/**
 * @author gbarbieri
 */
public class BasicTexture extends Framework {

    public static void main(String[] args) {
        new BasicTexture("Tutorial 14 - Basic Texture");
    }

    private ProgramData litShaderProg, litTextureProg;
    private UnlitProgData unlit;

    private ObjectData initialObjectData = new ObjectData(
            new Vec3(0.0f, 0.5f, 0.0f),
            new Quat(1.0f, 0.0f, 0.0f, 0.0f));
    private ViewData initialViewData = new ViewData(
            new Vec3(initialObjectData.position),
            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            10.0f,
            0.0f);
    private ViewScale viewScale = new ViewScale(
            1.5f, 70.0f,
            1.5f, 0.5f,
            0.0f, 0.0f, //No camera movement.
            90.0f / 250.0f);
    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);
    private ObjectPole objectPole = new ObjectPole(initialObjectData, 90.0f / 250.0f, MouseEvent.BUTTON3, viewPole);

    private Mesh objectMesh, cube;

    private static final int NUMBER_OF_LIGHTS = 2, NUM_GAUSSIAN_TEXTURES = 4;

    private interface Buffer {

        int PROJECTION = 0;
        int LIGHT = 1;
        int MATERIAL = 2;
        int MAX = 3;
    }

    private IntBuffer gaussTextures = GLBuffers.newDirectIntBuffer(NUM_GAUSSIAN_TEXTURES),
            gaussSampler = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);

    private boolean drawCameraPos = false, drawLights = true, useTexture = false;

    private float specularShininess = 0.2f, lightHeight = 1.0f, lightRadius = 3.0f, halfLightDistance = 25.0f,
            lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance);

    private Timer lightTimer = new Timer(Timer.Type.Loop, 6.0f);

    private ByteBuffer lightBuffer = GLBuffers.newDirectByteBuffer(LightBlock.SIZE);

    private int currTexture = 0;

    public BasicTexture(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            objectMesh = new Mesh(gl, getClass(), "tut14/Infinity.xml");
            cube = new Mesh(gl, getClass(), "tut14/UnitCube.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(BasicTexture.class.getName()).log(Level.SEVERE, null, ex);
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
        MaterialBlock mtl = new MaterialBlock();
        mtl.diffuseColor = new Vec4(1.0f, 0.673f, 0.043f, 1.0f);
        mtl.specularColor = new Vec4(1.0f, 0.673f, 0.043f, 1.0f);
        mtl.specularShininess = specularShininess;

        ByteBuffer mtlBuffer = mtl.to(GLBuffers.newDirectByteBuffer(MaterialBlock.SIZE));

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL));
        gl.glBufferData(GL_UNIFORM_BUFFER, MaterialBlock.SIZE, mtlBuffer, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT), 0, LightBlock.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0, MaterialBlock.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        createGaussianTextures(gl);

        destroyBuffer(mtlBuffer);
    }

    private void initializePrograms(GL3 gl) {

        litShaderProg = new ProgramData(gl, "pn.vert", "shader-gaussian.frag");
        litTextureProg = new ProgramData(gl, "pn.vert", "texture-gaussian.frag");

        unlit = new UnlitProgData(gl, "unlit");
    }

    private void createGaussianTextures(GL3 gl) {
        gl.glGenTextures(NUM_GAUSSIAN_TEXTURES, gaussTextures);
        for (int loop = 0; loop < NUM_GAUSSIAN_TEXTURES; loop++) {
            int cosAngleResolution = calcCosAngleResolution(loop);
            createGaussianTexture(gl, loop, cosAngleResolution);
        }
        gl.glGenSamplers(1, gaussSampler);
        gl.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    }

    private int calcCosAngleResolution(int level) {
        int cosAngleStart = 64;
        return cosAngleStart * (int) glm.pow(2f, level);
    }

    private void createGaussianTexture(GL3 gl, int index, int cosAngleResolution) {

        ByteBuffer textureData = buildGaussianData(cosAngleResolution);

        gl.glBindTexture(GL_TEXTURE_1D, gaussTextures.get(index));
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_R8, cosAngleResolution, 0, GL_RED, GL_UNSIGNED_BYTE, textureData);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAX_LEVEL, 0);
        gl.glBindTexture(GL_TEXTURE_1D, 0);

        destroyBuffer(textureData);
    }

    private ByteBuffer buildGaussianData(int cosAngleResolution) {

        ByteBuffer textureData = GLBuffers.newDirectByteBuffer(cosAngleResolution);

        for (int iCosAng = 0; iCosAng < cosAngleResolution; iCosAng++) {

            float cosAng = iCosAng / (float) (cosAngleResolution - 1);
            float angle = (float) Math.acos(cosAng);
            float exponent = angle / specularShininess;
            exponent = -(exponent * exponent);
            float gaussianTerm = (float) Math.exp(exponent);

            textureData.put(iCosAng, (byte) (gaussianTerm * 255f));
        }
        return textureData;
    }

    @Override
    public void display(GL3 gl) {

        lightTimer.update();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());
        Mat4 worldToCamMat = modelMatrix.top();

        Vec3 globalLightDirection = new Vec3(0.707f, 0.707f, 0.0f);

        LightBlock lightData = new LightBlock();

        lightData.ambientIntensity = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
        lightData.lightAttenuation = lightAttenuation;

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat.times(new Vec4(globalLightDirection, 0.0f));
        lightData.lights[0].lightIntensity = new Vec4(0.6f, 0.6f, 0.6f, 1.0f);

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat.times(calcLightPosition());
        lightData.lights[1].lightIntensity = new Vec4(0.4f, 0.4f, 0.4f, 1.0f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.to(lightBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        {
            gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0, MaterialBlock.SIZE);

            modelMatrix
                    .push()
                    .applyMatrix(objectPole.calcMatrix())
                    .scale(2.0f);

            Mat3 normMatrix = modelMatrix.top().toMat3();
            normMatrix.inverse_().transpose_();

            ProgramData prog = useTexture ? litTextureProg : litShaderProg;

            gl.glUseProgram(prog.theProgram);
            gl.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniformMatrix3fv(prog.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

            gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.GAUSSIAN_TEXTURE);
            gl.glBindTexture(GL_TEXTURE_1D, gaussTextures.get(currTexture));
            gl.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, gaussSampler.get(0));

            objectMesh.render(gl, "lit");

            gl.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, 0);
            gl.glBindTexture(GL_TEXTURE_1D, 0);

            gl.glUseProgram(0);
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);

            modelMatrix.pop();
        }

        if (drawLights) {

            modelMatrix
                    .push()
                    .translate(calcLightPosition())
                    .scale(0.25f);

            gl.glUseProgram(unlit.theProgram);
            gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

            Vec4 lightColor = new Vec4(1.0f);
            gl.glUniform4fv(unlit.objectColorUnif, 1, lightColor.to(vecBuffer));
            cube.render(gl, "flat");

            modelMatrix.push()
                    .reset()
                    .translate(globalLightDirection.times(100.0f))
                    .scale(5.0f);

            gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            cube.render(gl, "flat");

            gl.glUseProgram(0);
            modelMatrix.pop();
        }

        if (drawCameraPos) {

            modelMatrix
                    .push()
                    .setIdentity()
                    .translate(0.0f, 0.0f, -viewPole.getView().radius)
                    .scale(0.25f);

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

        float timeThroughLoop = lightTimer.getAlpha();
        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = glm.cos(timeThroughLoop * scale) * lightRadius;
        ret.z = glm.sin(timeThroughLoop * scale) * lightRadius;

        return ret;
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
                lightTimer.togglePause();
                break;
            case KeyEvent.VK_MINUS:
                lightTimer.rewind(0.5f);
                break;
            case KeyEvent.VK_PLUS:
                lightTimer.fastForward(0.5f);
                break;
            case KeyEvent.VK_T:
                drawCameraPos = !drawCameraPos;
                break;
            case KeyEvent.VK_G:
                drawLights = !drawLights;
                break;

            case KeyEvent.VK_SPACE:
                useTexture = !useTexture;
                break;
        }

        if (KeyEvent.VK_1 <= e.getKeyCode() && KeyEvent.VK_9 >= e.getKeyCode()) {
            int number = e.getKeyCode() - KeyEvent.VK_0 - 1;
            if (number < NUM_GAUSSIAN_TEXTURES) {
                System.out.println("Angle Resolution: " + calcCosAngleResolution(number));
                currTexture = number;
            }
        }

        viewPole.keyPressed(e);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(litShaderProg.theProgram);
        gl.glDeleteProgram(litTextureProg.theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteSamplers(1, gaussSampler);
        gl.glDeleteTextures(NUM_GAUSSIAN_TEXTURES, gaussTextures);

        objectMesh.dispose(gl);
        cube.dispose(gl);

        destroyBuffers(bufferName, gaussSampler, gaussTextures, lightBuffer);
    }

    private static class MaterialBlock {

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
            return buffer.putFloat(offset + 2 * Vec4.SIZE, specularShininess);
        }
    }

    private static class PerLight {

        public static final int SIZE = Vec4.SIZE * 2;

        public Vec4 cameraSpaceLightPos = new Vec4();
        public Vec4 lightIntensity = new Vec4();

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            cameraSpaceLightPos.to(buffer, offset);
            return lightIntensity.to(buffer, offset + Vec4.SIZE);
        }
    }

    private static class LightBlock {

        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;

        public Vec4 ambientIntensity = new Vec4();
        public float lightAttenuation;
        float[] padding = new float[3];
        public PerLight[] lights = {new PerLight(), new PerLight()};

        public ByteBuffer to(ByteBuffer buffer) {
            return to(buffer, 0);
        }

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            ambientIntensity.to(buffer, offset);
            buffer.putFloat(offset + Vec4.SIZE, lightAttenuation);
            for (int i = 0; i < NUMBER_OF_LIGHTS; i++)
                lights[i].to(buffer, offset + 2 * Vec4.SIZE + i * PerLight.SIZE);
            return buffer;
        }
    }

    private class ProgramData {

        public int theProgram;

        public int modelToCameraMatrixUnif;
        public int normalModelToCameraMatrixUnif;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut14", vertex, fragment);

            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
            normalModelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "normalModelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Material"),
                    Semantic.Uniform.MATERIAL);
            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Light"),
                    Semantic.Uniform.LIGHT);

            gl.glUseProgram(theProgram);
            gl.glUniform1i(
                    gl.glGetUniformLocation(theProgram, "gaussianTexture"),
                    Semantic.Sampler.GAUSSIAN_TEXTURE);
            gl.glUseProgram(theProgram);
        }
    }

    private class UnlitProgData {

        public int theProgram;

        public int objectColorUnif;
        public int modelToCameraMatrixUnif;

        public UnlitProgData(GL3 gl, String shader) {

            theProgram = programOf(gl, getClass(), "tut14", shader + ".vert", shader + ".frag");

            objectColorUnif = gl.glGetUniformLocation(theProgram, "objectColor");
            modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);
        }
    }

}
