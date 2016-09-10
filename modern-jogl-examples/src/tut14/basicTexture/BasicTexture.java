/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tut14.basicTexture;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
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
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_BASE_LEVEL;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MAX_LEVEL;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.util.GLBuffers;
import framework.Framework;
import static framework.Framework.matBuffer;
import framework.Semantic;
import framework.component.Mesh;
import glm.mat._3.Mat3;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import glutil.BufferUtils;
import glutil.MatrixStack;
import glutil.Timer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import one.util.streamex.IntStreamEx;
import org.xml.sax.SAXException;
import view.ObjectData;
import view.ObjectPole;
import view.ViewData;
import view.ViewPole;
import view.ViewScale;

/**
 *
 * @author gbarbieri
 */
public class BasicTexture extends Framework {

    private final String SHADERS_ROOT = "/tut14/basicTexture/shaders", MESHES_ROOT = "/tut14/data/",
            PN_SHADER_SRC = "pn", SHADER_GAUSSIAN_SHADER_SRC = "shader-gaussian", UNLIT_SHADER_SRC = "unlit",
            TEXTURE_GAUSSIAN_SHADER_SRC = "texture-gaussian", OBJECT_MESH_SRC = "Infinity.xml",
            CUBE_MESH_SRC = "UnitCube.xml";

    public static void main(String[] args) {
        new BasicTexture("Tutorial 14 - Basic Texture");
    }

    private ProgramData litShaderProg, litTextureProg;
    private UnlitProgData unlit;

    private ObjectData initialObjectData = new ObjectData(
            new Vec3(0.0f, 0.5f, 0.0f),
            new Quat(1.0f, 0.0f, 0.0f, 0.0f));
    private ViewData initialViewData = new ViewData(
            new Vec3(initialObjectData.position()),
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

    private class PerLight {

        public static final int SIZE = Vec4.SIZE * 2;

        public Vec4 cameraSpaceLightPos;
        public Vec4 lightIntensity;

        public PerLight(Vec4 cameraSpaceLightPos, Vec4 lightIntensity) {
            this.cameraSpaceLightPos = cameraSpaceLightPos;
            this.lightIntensity = lightIntensity;
        }

        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
            cameraSpaceLightPos.toDbb(bb, offset + 0);
            lightIntensity.toDbb(bb, offset + Vec4.SIZE);
            return bb;
        }
    }

    private class LightBlock {

        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;

        public Vec4 ambientIntensity;
        float lightAttenuation;
        float[] padding = new float[3];
        private PerLight[] lights = new PerLight[NUMBER_OF_LIGHTS];

        public LightBlock(Vec4 ambientIntensity, float lightAttenuation, PerLight[] lights) {
            this.ambientIntensity = ambientIntensity;
            this.lightAttenuation = lightAttenuation;
            this.lights = lights;
        }

        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
            ambientIntensity.toDbb(bb, offset + 0);
            bb.putFloat(offset + Vec4.SIZE, lightAttenuation);
            IntStreamEx.range(lights.length).forEach(i -> lights[i].toDbb(bb, offset + 2 * Vec4.SIZE + i * PerLight.SIZE));
            return bb;
        }
    }

    private interface Buffer {

        public static final int PROJECTION = 0;
        public static final int LIGHT = 1;
        public static final int MATERIAL = 2;
        public static final int MAX = 3;
    }

    private IntBuffer gaussTextures = GLBuffers.newDirectIntBuffer(NUM_GAUSSIAN_TEXTURES),
            gaussSampler = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);

    private boolean drawCameraPos = false, drawLights = true, useTexture = false;

    private float specularShininess = 0.2f, lightHeight = 1.0f, lightRadius = 3.0f, halfLightDistance = 25.0f,
            lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance);

    private Timer lightTimer = new Timer(Timer.Type.LOOP, 6.0f);

    private ByteBuffer lightBuffer = GLBuffers.newDirectByteBuffer(LightBlock.SIZE);

    private int currTexture = 0;

    private class MaterialBlock {

        public static final int SIZE = 3 * Vec4.SIZE;

        public Vec4 diffuseColor;
        public Vec4 specularColor;
        public float specularShininess;
        public float[] padding = new float[3];

        public MaterialBlock(Vec4 diffuseColor, Vec4 specularColor, float specularShininess) {
            this.diffuseColor = diffuseColor;
            this.specularColor = specularColor;
            this.specularShininess = specularShininess;
        }

        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
            diffuseColor.toDbb(bb, offset + 0);
            specularColor.toDbb(bb, offset + Vec4.SIZE);
            bb.putFloat(offset + 2 * Vec4.SIZE, specularShininess);
            return bb;
        }
    }

    public BasicTexture(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl3) {

        initializePrograms(gl3);

        try {
            objectMesh = new Mesh(MESHES_ROOT + OBJECT_MESH_SRC, gl3);
            cube = new Mesh(MESHES_ROOT + CUBE_MESH_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(BasicTexture.class.getName()).log(Level.SEVERE, null, ex);
        }

        float depthZNear = 0.0f, depthZFar = 1.0f;

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(depthZNear, depthZFar);
        gl3.glEnable(GL_DEPTH_CLAMP);

        //Setup our Uniform Buffers
        MaterialBlock mtl = new MaterialBlock(new Vec4(1.0f, 0.673f, 0.043f, 1.0f),
                new Vec4(1.0f, 0.673f, 0.043f, 1.0f), specularShininess);
        ByteBuffer mtlBuffer = GLBuffers.newDirectByteBuffer(MaterialBlock.SIZE);
        mtl.toDbb(mtlBuffer, 0);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL));
        gl3.glBufferData(GL_UNIFORM_BUFFER, MaterialBlock.SIZE, mtlBuffer, GL_STATIC_DRAW);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl3.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT), 0,
                LightBlock.SIZE);
        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0,
                Mat4.SIZE);
        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0,
                MaterialBlock.SIZE);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        createGaussianTextures(gl3);
    }

    private void initializePrograms(GL3 gl3) {

        litShaderProg = new ProgramData(gl3, SHADERS_ROOT, PN_SHADER_SRC, SHADER_GAUSSIAN_SHADER_SRC);

        litTextureProg = new ProgramData(gl3, SHADERS_ROOT, PN_SHADER_SRC, TEXTURE_GAUSSIAN_SHADER_SRC);

        unlit = new UnlitProgData(gl3, SHADERS_ROOT, UNLIT_SHADER_SRC);
    }

    private void createGaussianTextures(GL3 gl3) {
        for (int loop = 0; loop < NUM_GAUSSIAN_TEXTURES; loop++) {
            int cosAngleResolution = calcCosAngleResolution(loop);
            gaussTextures.put(loop, createGaussianTexture(gl3, cosAngleResolution));
        }
        gl3.glGenSamplers(1, gaussSampler);
        gl3.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(gaussSampler.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    }

    private int calcCosAngleResolution(int level) {
        int cosAngleStart = 64;
        return cosAngleStart * ((int) Math.pow(2f, level));
    }

    private int createGaussianTexture(GL3 gl3, int cosAngleResolution) {

        ByteBuffer textureData = buildGaussianData(cosAngleResolution);

        IntBuffer gaussianTexture = GLBuffers.newDirectIntBuffer(1);

        gl3.glGenTextures(1, gaussianTexture);
        gl3.glBindTexture(GL_TEXTURE_1D, gaussianTexture.get(0));
        gl3.glTexImage1D(GL_TEXTURE_1D, 0, GL_R8, cosAngleResolution, 0, GL_RED, GL_UNSIGNED_BYTE, textureData);
        gl3.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAX_LEVEL, 0);
        gl3.glBindTexture(GL_TEXTURE_1D, 0);

        int result = gaussianTexture.get(0);

        BufferUtils.destroyDirectBuffer(gaussianTexture);
        BufferUtils.destroyDirectBuffer(textureData);

        return result;
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
    public void display(GL3 gl3) {

        lightTimer.update();
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());
        Mat4 worldToCamMat = modelMatrix.top();

        Vec3 globalLightDirection = new Vec3(0.707f, 0.707f, 0.0f);

        LightBlock lightData = new LightBlock(
                new Vec4(0.2f, 0.2f, 0.2f, 1.0f),
                lightAttenuation,
                new PerLight[]{
                    new PerLight(
                            worldToCamMat.mul_(new Vec4(globalLightDirection, 0.0f)),
                            new Vec4(0.6f, 0.6f, 0.6f, 1.0f)),
                    new PerLight(
                            worldToCamMat.mul_(calcLightPosition()),
                            new Vec4(0.4f, 0.4f, 0.4f, 1.0f))});

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.toDbb(lightBuffer, 0));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        {
            gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0,
                    MaterialBlock.SIZE);

            modelMatrix.push().applyMatrix(objectPole.calcMatrix()).scale(2.0f);

            Mat3 normMatrix = modelMatrix.top().toMat3_().inverse().transpose();

            ProgramData prog = useTexture ? litTextureProg : litShaderProg;

            gl3.glUseProgram(prog.theProgram);
            gl3.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
            gl3.glUniformMatrix3fv(prog.normalModelToCameraMatrixUnif, 1, false, normMatrix.toDfb(matBuffer));

            gl3.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.GAUSSIAN_TEXTURE);
            gl3.glBindTexture(GL_TEXTURE_1D, gaussTextures.get(currTexture));
            gl3.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, gaussSampler.get(0));

            objectMesh.render(gl3, "lit");

            gl3.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, 0);
            gl3.glBindTexture(GL_TEXTURE_1D, 0);

            gl3.glUseProgram(0);
            gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);

            modelMatrix.pop();
        }

        if (drawLights) {

            modelMatrix.push().translate(calcLightPosition()).scale(0.25f);

            gl3.glUseProgram(unlit.theProgram);
            gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));

            Vec4 lightColor = new Vec4(1.0f);
            gl3.glUniform4fv(unlit.objectColorUnif, 1, lightColor.toDfb(vecBuffer));
            cube.render(gl3, "flat");

            modelMatrix.pop().push().translate(globalLightDirection.mul_(100.0f)).scale(5.0f);

            gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
            cube.render(gl3, "flat");

            gl3.glUseProgram(0);
            modelMatrix.pop();
        }

        if (drawCameraPos) {

            modelMatrix.push().identity().translate(0.0f, 0.0f, -viewPole.getView().radius()).scale(0.25f);

            gl3.glDisable(GL_DEPTH_TEST);
            gl3.glDepthMask(false);
            gl3.glUseProgram(unlit.theProgram);
            gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
            gl3.glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f);
            cube.render(gl3, "flat");
            gl3.glDepthMask(true);
            gl3.glEnable(GL_DEPTH_TEST);
            gl3.glUniform4f(unlit.objectColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            cube.render(gl3, "flat");

            modelMatrix.pop();
        }
    }

    private Vec4 calcLightPosition() {

        float scale = (float) (Math.PI * 2.0f);

        float timeThroughLoop = lightTimer.getAlpha();
        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);

        ret.x = (float) (Math.cos(timeThroughLoop * scale) * lightRadius);
        ret.z = (float) (Math.sin(timeThroughLoop * scale) * lightRadius);

        return ret;
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack perspMatrix = new MatrixStack();

        Mat4 proj = perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar).top();

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, proj.toDfb(matBuffer));
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        viewPole.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        viewPole.mouseMove(e);
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
                animator.remove(glWindow);
                glWindow.destroy();
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

        viewPole.charPress(e);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(litShaderProg.theProgram);
        gl3.glDeleteProgram(litTextureProg.theProgram);
        gl3.glDeleteProgram(unlit.theProgram);

        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
        gl3.glDeleteSamplers(1, gaussSampler);
        gl3.glDeleteTextures(NUM_GAUSSIAN_TEXTURES, gaussTextures);

        objectMesh.dispose(gl3);
        cube.dispose(gl3);

        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(gaussSampler);
        BufferUtils.destroyDirectBuffer(gaussTextures);
        BufferUtils.destroyDirectBuffer(lightBuffer);
    }
}
