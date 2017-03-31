
package main.tut14;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.spi.DDSImage;
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
import uno.mousePole.*;
import uno.time.Timer;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
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
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL2ES2.GL_RED;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import static glm.GlmKt.glm;
import static uno.buffer.UtilKt.destroyBuffer;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class MaterialTexture extends Framework {

    public static void main(String[] args) {
        new MaterialTexture().setup("Tutorial 14 - Material Texture");
    }

    private ProgramData[] programs = new ProgramData[ShaderMode.MAX];
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

    private static final int NUMBER_OF_LIGHTS = 2, NUM_MATERIALS = 2, NUM_GAUSSIAN_TEXTURES = 4;

    private ShaderPair[] shaderPairs = {
            new ShaderPair("pn.vert", "fixed-shininess.frag"),
            new ShaderPair("pnt.vert", "texture-shininess.frag"),
            new ShaderPair("pnt.vert", "texture-compute.frag")};

    private Mesh objectMesh, cube, plane;

    private interface Buffer {

        int PROJECTION = 0;
        int LIGHT = 1;
        int MATERIAL = 2;
        int MAX = 3;
    }

    private interface Texture {

        int SHINE = NUM_GAUSSIAN_TEXTURES;
        int MAX = SHINE + 1;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX), samplerName = GLBuffers.newDirectIntBuffer(1);

    private int materialOffset, currMaterial = 0, currTexture = NUM_GAUSSIAN_TEXTURES - 1;

    private Timer lightTimer = new Timer(Timer.Type.Loop, 6.0f);

    private float halfLightDistance = 25.0f, lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance),
            lightHeight = 1.0f, lightRadius = 3.0f;

    private int mode = ShaderMode.FIXED;

    boolean drawLights = true;
    boolean drawCameraPos = false;
    boolean useInfinity = true;

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            objectMesh = new Mesh(gl, getClass(), "tut14/Infinity.xml");
            cube = new Mesh(gl, getClass(), "tut14/UnitCube.xml");
            plane = new Mesh(gl, getClass(), "tut14/UnitPlane.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(MaterialTexture.class.getName()).log(Level.SEVERE, null, ex);
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

        //Setup our Uniform Buffers
        setupMaterials(gl);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT), 0, LightBlock.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);
        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0, MaterialBlock.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glGenTextures(Texture.MAX, textureName);
        createGaussianTextures(gl);
        createShininessTexture(gl);
    }

    private void initializePrograms(GL3 gl) {

        for (int i = 0; i < ShaderMode.MAX; i++)
            programs[i] = new ProgramData(gl, shaderPairs[i]);

        unlit = new UnlitProgData(gl, "unlit");
    }

    private void setupMaterials(GL3 gl) {

        UniformBlockArray mtls = new UniformBlockArray(gl, MaterialBlock.SIZE, NUM_MATERIALS);

        MaterialBlock mtl = new MaterialBlock();
        mtl.diffuseColor = new Vec4(1.0f, 0.673f, 0.043f, 1.0f);
        mtl.specularColor = new Vec4(1.0f, 0.673f, 0.043f, 1.0f).times(0.4f);
        mtl.specularShininess = 0.125f;
        mtls.set(0, mtl.toBuffer());

        mtl.diffuseColor = new Vec4(0.01f, 0.01f, 0.01f, 1.0f);
        mtl.specularColor = new Vec4(0.99f, 0.99f, 0.99f, 1.0f);
        mtl.specularShininess = 0.125f;
        mtls.set(1, mtl.toBuffer());

        mtls.uploadBufferObject(gl, bufferName.get(Buffer.MATERIAL));
        materialOffset = mtls.getArrayOffset();

        mtls.dispose();
    }

    private void createGaussianTextures(GL3 gl) {
        for (int loop = 0; loop < NUM_GAUSSIAN_TEXTURES; loop++) {
            int cosAngleResolution = calcCosAngleResolution(loop);
            createGaussianTexture(gl, loop, cosAngleResolution, 128);
        }
        gl.glGenSamplers(1, samplerName);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private int calcCosAngleResolution(int level) {
        int cosAngleStart = 64;
        return cosAngleStart * ((int) glm.pow(2f, level));
    }

    private void createGaussianTexture(GL3 gl, int index, int cosAngleResolution, int shininessResolution) {

        ByteBuffer textureData = buildGaussianData(cosAngleResolution, shininessResolution);

        gl.glBindTexture(GL_TEXTURE_1D, textureName.get(index));
        gl.glTexImage1D(GL_TEXTURE_1D, 0, GL_R8, cosAngleResolution, 0, GL_RED, GL_UNSIGNED_BYTE, textureData);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAX_LEVEL, 0);
        gl.glBindTexture(GL_TEXTURE_1D, 0);

        destroyBuffer(textureData);
    }

    private ByteBuffer buildGaussianData(int cosAngleResolution, int shininessResolution) {

        ByteBuffer textureData = GLBuffers.newDirectByteBuffer(cosAngleResolution * shininessResolution);

        for (int iShin = 1; iShin < shininessResolution; iShin++) {

            float shininess = iShin / (float) shininessResolution;

            for (int iCosAng = 0; iCosAng < cosAngleResolution; iCosAng++) {

                float cosAng = iCosAng / (float) (cosAngleResolution - 1);
                float angle = glm.acos(cosAng);
                float exponent = angle / shininess;
                exponent = -(exponent * exponent);
                float gaussianTerm = glm.exp(exponent);

                textureData.put(iCosAng, (byte) (gaussianTerm * 255f));
            }
        }
        return textureData;
    }

    private void createShininessTexture(GL3 gl) {

        try {
            File file = new File(getClass().getResource("/tut14/main.dds").toURI());

            DDSImage image = DDSImage.read(file);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.SHINE));

            gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, image.getWidth(), image.getHeight(), 0, GL_RED,
                    GL_UNSIGNED_BYTE, image.getMipMap(0).getData());

            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

            gl.glBindTexture(GL_TEXTURE_2D, 0);

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(MaterialTexture.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void display(GL3 gl) {

        lightTimer.update();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());
        Mat4 worldToCamMat = modelMatrix.top();

        LightBlock lightData = new LightBlock();

        lightData.ambientIntensity = new Vec4(0.2f, 0.2f, 0.2f, 1.0f);
        lightData.lightAttenuation = lightAttenuation;

        Vec3 globalLightDirection = new Vec3(0.707f, 0.707f, 0.0f);

        lightData.lights[0].cameraSpaceLightPos = worldToCamMat.times(new Vec4(globalLightDirection, 0.0f));
        lightData.lights[0].lightIntensity = new Vec4(0.6f, 0.6f, 0.6f, 1.0f);

        lightData.lights[1].cameraSpaceLightPos = worldToCamMat.times(calcLightPosition());
        lightData.lights[1].lightIntensity = new Vec4(0.4f, 0.4f, 0.4f, 1.0f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.toBuffer());
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        {
            Mesh mesh = useInfinity ? objectMesh : plane;

            gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
                    currMaterial * materialOffset, MaterialBlock.SIZE);

            modelMatrix
                    .push()
                    .applyMatrix(objectPole.calcMatrix())
                    .scale(useInfinity ? 2.0f : 4.0f);

            Mat3 normMatrix = modelMatrix.top().toMat3();
            normMatrix.inverse_().transpose_();

            ProgramData prog = programs[mode];

            gl.glUseProgram(prog.theProgram);
            gl.glUniformMatrix4fv(prog.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));
            gl.glUniformMatrix3fv(prog.normalModelToCameraMatrixUnif, 1, false, normMatrix.to(matBuffer));

            gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.GAUSSIAN_TEXTURE);
            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(currTexture));
            gl.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, samplerName.get(0));

            gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.SHININESS_TEXTURE);
            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.SHINE));
            gl.glBindSampler(Semantic.Sampler.SHININESS_TEXTURE, samplerName.get(0));

            if (mode != ShaderMode.FIXED)
                mesh.render(gl, "lit-tex");
            else
                mesh.render(gl, "lit");

            gl.glBindSampler(Semantic.Sampler.GAUSSIAN_TEXTURE, 0);
            gl.glBindSampler(Semantic.Sampler.SHININESS_TEXTURE, 0);
            gl.glBindTexture(GL_TEXTURE_2D, 0);

            gl.glUseProgram(0);
            gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);

            modelMatrix.pop();
        }
        if (drawLights) {

            modelMatrix
                    .push()
                    .translate(calcLightPosition()).
                    scale(0.25f);

            gl.glUseProgram(unlit.theProgram);
            gl.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().to(matBuffer));

            Vec4 lightColor = new Vec4(1.0f);
            gl.glUniform4fv(unlit.objectColorUnif, 1, lightColor.to(vecBuffer));
            cube.render(gl, "flat");

            modelMatrix
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
                    .translate(new Vec3(0.0f, 0.0f, -viewPole.getView().radius))
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
            case KeyEvent.VK_Y:
                useInfinity = !useInfinity;
                break;

            case KeyEvent.VK_SPACE:
                mode = (mode + 1) % ShaderMode.MAX;
                System.out.println(shaderModeNames[mode]);
                break;
        }

        if (KeyEvent.VK_1 <= e.getKeyCode() && KeyEvent.VK_9 >= e.getKeyCode()) {
            int number = e.getKeyCode() - KeyEvent.VK_1;
            if (number < NUM_GAUSSIAN_TEXTURES) {
                System.out.println("Angle Resolution: " + calcCosAngleResolution(number));
                currTexture = number;
            }
            if (number >= 9 - NUM_MATERIALS) {
                number = number - (9 - NUM_MATERIALS);
                System.out.println("Material Number: " + number);
                currMaterial = number;
            }
        }

        viewPole.keyPressed(e);
    }

    private String[] shaderModeNames = {"Fixed Shininess with Gaussian Texture", "Texture Shininess with Gaussian Texture",
            "Texture Shininess with computed Gaussian"};

    @Override
    public void end(GL3 gl) {

        for (int i = 0; i < ShaderMode.MAX; i++)
            gl.glDeleteProgram(programs[i].theProgram);
        gl.glDeleteProgram(unlit.theProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteSamplers(1, samplerName);
        gl.glDeleteTextures(Texture.MAX, textureName);

        objectMesh.dispose(gl);
        cube.dispose(gl);

        destroyBuffers(bufferName, samplerName, textureName, LightBlock.buffer);
    }

    interface ShaderMode {

        int FIXED = 0;
        int TEXTURED = 1;
        int TEXTURED_COMPUTE = 2;
        int MAX = 3;
    }

    private static class PerLight {

        public static final int SIZE = Vec4.SIZE * 2;

        public Vec4 cameraSpaceLightPos;
        public Vec4 lightIntensity;

        public ByteBuffer to(ByteBuffer buffer, int offset) {
            cameraSpaceLightPos.to(buffer, offset);
            lightIntensity.to(buffer, offset + Vec4.SIZE);
            return buffer;
        }
    }

    private static class LightBlock {

        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;
        public static final ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public Vec4 ambientIntensity;
        public float lightAttenuation;
        float[] padding = new float[3];
        public PerLight[] lights = {new PerLight(), new PerLight()};

        public ByteBuffer toBuffer() {
            ambientIntensity.to(buffer);
            buffer.putFloat(Vec4.SIZE, lightAttenuation);
            for (int i = 0; i < NUMBER_OF_LIGHTS; i++)
                lights[i].to(buffer, 2 * Vec4.SIZE + i * PerLight.SIZE);
            return buffer;
        }
    }

    private static class MaterialBlock {

        public static final int SIZE = 3 * Vec4.SIZE;
        public static final ByteBuffer buffer = GLBuffers.newDirectByteBuffer(SIZE);

        public Vec4 diffuseColor;
        public Vec4 specularColor;
        public float specularShininess;
        public float[] padding = new float[3];

        public ByteBuffer toBuffer() {
            diffuseColor.to(buffer);
            specularColor.to(buffer, Vec4.SIZE);
            return buffer.putFloat(2 * Vec4.SIZE, specularShininess);
        }
    }

    private class ProgramData {

        public int theProgram;

        public int modelToCameraMatrixUnif;
        public int normalModelToCameraMatrixUnif;

        public ProgramData(GL3 gl, ShaderPair shaderPair) {

            theProgram = programOf(gl, getClass(), "tut14", shaderPair.vertex, shaderPair.fragment);

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
            gl.glUniform1i(
                    gl.glGetUniformLocation(theProgram, "shininessTexture"),
                    Semantic.Sampler.SHININESS_TEXTURE);
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

    private class ShaderPair {

        public String vertex, fragment;

        public ShaderPair(String vertex, String fragment) {
            this.vertex = vertex;
            this.fragment = fragment;
        }
    }
}