///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut13.basicImpostor;
//
//import com.jogamp.newt.event.KeyEvent;
//import com.jogamp.newt.event.MouseEvent;
//import static com.jogamp.opengl.GL.GL_BACK;
//import static com.jogamp.opengl.GL.GL_CULL_FACE;
//import static com.jogamp.opengl.GL.GL_CW;
//import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
//import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
//import static com.jogamp.opengl.GL.GL_LEQUAL;
//import static com.jogamp.opengl.GL.GL_TRIANGLE_STRIP;
//import static com.jogamp.opengl.GL2ES3.GL_COLOR;
//import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
//import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
//import com.jogamp.opengl.GL3;
//import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
//import com.jogamp.opengl.util.GLBuffers;
//import main.framework.Framework;
//import main.framework.Semantic;
//import main.framework.component.Mesh;
//import glm.mat._3.Mat3;
//import glm.mat._4.Mat4;
//import glm.quat.Quat;
//import glm.vec._3.Vec3;
//import glm.vec._4.Vec4;
//import glutil.BufferUtils;
//import glutil.MatrixStack;
//import glutil.Timer;
//import glutil.UniformBlockArray;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.IntBuffer;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.ParserConfigurationException;
//import one.util.streamex.IntStreamEx;
//import org.xml.sax.SAXException;
//import view.ViewData;
//import view.ViewPole;
//import view.ViewScale;
//
///**
// *
// * @author elect
// */
//public class BasicImpostor extends Framework {
//
//    private final String SHADERS_ROOT = "/tut13/basicImpostor/shaders", MESHES_ROOT = "/tut13/data/",
//            PN_SHADER_SRC = "pn", LIGHTING_SHADER_SRC = "lighting", UNLIT_SHADER_SRC = "unlit",
//            PLANE_MESH_SRC = "LargePlane.xml", SPHERE_MESH_SRC = "UnitSphere.xml", CUBE_MESH_SRC = "UnitCube.xml";
//    private final String[] IMP_SHADERS_SRC = {"basic-impostor", "persp-impostor", "depth-impostor"};
//
//    public static void main(String[] args) {
//        new BasicImpostor("Tutorial 13 - Basic Impostor");
//    }
//
//    private ProgramMeshData litMeshProg;
//    private ProgramImposData[] litImpProgs = new ProgramImposData[Impostors.values().length];
//    private UnlitProgData unlit;
//
//    private ViewData initialViewData = new ViewData(
//            new Vec3(0.0f, 30.0f, 25.0f),
//            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
//            10.0f,
//            0.0f);
//    private ViewScale viewScale = new ViewScale(
//            3.0f, 70.0f,
//            3.5f, 1.5f,
//            5.0f, 1.0f,
//            90.0f / 250.0f);
//    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);
//
//    private Mesh sphere, plane, cube;
//
//    private interface Buffer {
//
//        public static final int PROJECTION = 0;
//        public static final int LIGHT = 1;
//        public static final int MATERIAL = 2;
//        public static final int MAX = 3;
//    }
//    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
//            imposterVAO = GLBuffers.newDirectIntBuffer(1);
//
//    private static final int NUMBER_OF_LIGHTS = 2;
//
//    private class PerLight {
//
//        public static final int SIZE = Vec4.SIZE * 2;
//
//        public Vec4 cameraSpaceLightPos;
//        public Vec4 lightIntensity;
//
//        public PerLight(Vec4 cameraSpaceLightPos, Vec4 lightIntensity) {
//            this.cameraSpaceLightPos = cameraSpaceLightPos;
//            this.lightIntensity = lightIntensity;
//        }
//
//        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
//            cameraSpaceLightPos.toDbb(bb, offset + 0);
//            lightIntensity.toDbb(bb, offset + Vec4.SIZE);
//            return bb;
//        }
//    }
//
//    private class LightBlock {
//
//        public static final int SIZE = Vec4.SIZE * 2 + NUMBER_OF_LIGHTS * PerLight.SIZE;
//
//        public Vec4 ambientIntensity;
//        float lightAttenuation;
//        float[] padding = new float[3];
//        private PerLight[] lights = new PerLight[NUMBER_OF_LIGHTS];
//
//        public LightBlock(Vec4 ambientIntensity, float lightAttenuation, PerLight[] lights) {
//            this.ambientIntensity = ambientIntensity;
//            this.lightAttenuation = lightAttenuation;
//            this.lights = lights;
//        }
//
//        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
//            ambientIntensity.toDbb(bb, offset + 0);
//            bb.putFloat(offset + Vec4.SIZE, lightAttenuation);
//            IntStreamEx.range(lights.length).forEach(i -> lights[i].toDbb(bb, offset + 2 * Vec4.SIZE + i * PerLight.SIZE));
//            return bb;
//        }
//    }
//
//    private ByteBuffer lightBuffer = GLBuffers.newDirectByteBuffer(LightBlock.SIZE);
//
//    private Impostors currImpostor = Impostors.Basic;
//
//    private boolean drawCameraPos = false, drawLights = true;
//
//    private boolean[] drawImposter = {false, false, false, false};
//
//    private float lightHeight = 20.0f, halfLightDistance = 25.0f, lightAttenuation = 1.0f / (halfLightDistance * halfLightDistance);
//
//    private Timer sphereTimer = new Timer(Timer.Type.LOOP, 6.0f);
//
//    private int materialBlockOffset = 0;
//
//    public BasicImpostor(String title) {
//        super(title);
//    }
//
//    @Override
//    public void init(GL3 gl3) {
//
//        initializePrograms(gl3);
//
//        try {
//            sphere = new Mesh(MESHES_ROOT + SPHERE_MESH_SRC, gl3);
//            plane = new Mesh(MESHES_ROOT + PLANE_MESH_SRC, gl3);
//            cube = new Mesh(MESHES_ROOT + CUBE_MESH_SRC, gl3);
//        } catch (ParserConfigurationException | SAXException | IOException ex) {
//            Logger.getLogger(BasicImpostor.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        float depthZNear = 0.0f, depthZFar = 1.0f;
//
//        gl3.glEnable(GL_CULL_FACE);
//        gl3.glCullFace(GL_BACK);
//        gl3.glFrontFace(GL_CW);
//
//        gl3.glEnable(GL_DEPTH_TEST);
//        gl3.glDepthMask(true);
//        gl3.glDepthFunc(GL_LEQUAL);
//        gl3.glDepthRangef(depthZNear, depthZFar);
//        gl3.glEnable(GL_DEPTH_CLAMP);
//
//        gl3.glGenBuffers(Buffer.MAX, bufferName);
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
//        gl3.glBufferData(GL_UNIFORM_BUFFER, LightBlock.SIZE, null, GL_DYNAMIC_DRAW);
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
//        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);
//
//        //Bind the static buffers.
//        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT),
//                0, LightBlock.SIZE);
//        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION),
//                0, Mat4.SIZE);
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//
//        //Empty Vertex Array Object.
//        gl3.glGenVertexArrays(1, imposterVAO);
//
//        bufferName.put(Buffer.MATERIAL, createMaterials(gl3));
//    }
//
//    private void initializePrograms(GL3 gl3) {
//
//        litMeshProg = new ProgramMeshData(gl3, SHADERS_ROOT, PN_SHADER_SRC, LIGHTING_SHADER_SRC);
//
//        IntStreamEx.range(litImpProgs.length).forEach(i -> litImpProgs[i]
//                = new ProgramImposData(gl3, SHADERS_ROOT, IMP_SHADERS_SRC[i]));
//
//        unlit = new UnlitProgData(gl3, SHADERS_ROOT, UNLIT_SHADER_SRC);
//    }
//
//    private class MaterialBlock {
//
//        public static final int SIZE = 3 * Vec4.SIZE;
//
//        public Vec4 diffuseColor;
//        public Vec4 specularColor;
//        public float specularShininess;
//        public float[] padding = new float[3];
//
//        public MaterialBlock(Vec4 diffuseColor, Vec4 specularColor, float specularShininess) {
//            this.diffuseColor = diffuseColor;
//            this.specularColor = specularColor;
//            this.specularShininess = specularShininess;
//        }
//
//        public ByteBuffer toDbb(ByteBuffer bb, int offset) {
//            diffuseColor.toDbb(bb, offset + 0);
//            specularColor.toDbb(bb, offset + Vec4.SIZE);
//            bb.putFloat(offset + 2 * Vec4.SIZE, specularShininess);
//            return bb;
//        }
//    }
//
//    private int createMaterials(GL3 gl3) {
//
//        UniformBlockArray ubArray = new UniformBlockArray(gl3, MaterialBlock.SIZE, MaterialNames.values().length);
//        materialBlockOffset = ubArray.blockOffset;
//
//        new MaterialBlock(new Vec4(0.5f, 0.5f, 0.5f, 1.0f), new Vec4(0.5f, 0.5f, 0.5f, 1.0f), 0.6f)
//                .toDbb(ubArray.storage, MaterialNames.Terrain.ordinal() * ubArray.blockOffset);
//
//        new MaterialBlock(new Vec4(0.1f, 0.1f, 0.8f, 1.0f), new Vec4(0.8f, 0.8f, 0.8f, 1.0f), 0.1f)
//                .toDbb(ubArray.storage, MaterialNames.BlueShiny.ordinal() * ubArray.blockOffset);
//
//        new MaterialBlock(new Vec4(0.803f, 0.709f, 0.15f, 1.0f), new Vec4(0.803f, 0.709f, 0.15f, 1.0f).mul(0.75f), 0.18f)
//                .toDbb(ubArray.storage, MaterialNames.GoldMetal.ordinal() * ubArray.blockOffset);
//
//        new MaterialBlock(new Vec4(0.4f, 0.4f, 0.4f, 1.0f), new Vec4(0.1f, 0.1f, 0.1f, 1.0f), 0.8f)
//                .toDbb(ubArray.storage, MaterialNames.DullGrey.ordinal() * ubArray.blockOffset);
//
//        new MaterialBlock(new Vec4(0.05f, 0.05f, 0.05f, 1.0f), new Vec4(0.95f, 0.95f, 0.95f, 1.0f), 0.3f)
//                .toDbb(ubArray.storage, MaterialNames.BlackShiny.ordinal() * ubArray.blockOffset);
//
//        int result = ubArray.createBufferObject(gl3);
//
//        ubArray.dispose();
//
//        return result;
//    }
//
//    @Override
//    public void display(GL3 gl3) {
//
//        sphereTimer.update();
//
//        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.75f).put(1, 0.75f).put(2, 1.0f).put(3, 1.0f));
//        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));
//
//        MatrixStack modelMatrix = new MatrixStack(viewPole.calcMatrix());
//        Mat4 worldToCamMat = modelMatrix.top();
//
//        LightBlock lightData = new LightBlock(
//                new Vec4(0.2f, 0.2f, 0.2f, 1.0f),
//                lightAttenuation,
//                new PerLight[]{
//                    new PerLight(
//                            worldToCamMat.mul_(new Vec4(0.707f, 0.707f, 0.0f, 0.0f)),
//                            new Vec4(0.6f, 0.6f, 0.6f, 1.0f)),
//                    new PerLight(
//                            worldToCamMat.mul_(calcLightPosition()),
//                            new Vec4(0.4f, 0.4f, 0.4f, 1.0f))});
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
//        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, LightBlock.SIZE, lightData.toDbb(lightBuffer, 0));
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//
//        {
//            gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
//                    MaterialNames.Terrain.ordinal() * materialBlockOffset, MaterialBlock.SIZE);
//
//            Mat3 normMatrix = modelMatrix.top().toMat3_().inverse().transpose();
//
//            gl3.glUseProgram(litMeshProg.theProgram);
//            gl3.glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
//            gl3.glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.toDfb(matBuffer));
//
//            plane.render(gl3);
//
//            gl3.glUseProgram(0);
//            gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
//        }
//
//        drawSphere(gl3, modelMatrix, new Vec3(0.0f, 10.0f, 0.0f), 4.0f, MaterialNames.BlueShiny, drawImposter[0]);
//
//        drawSphereOrbit(gl3, modelMatrix, new Vec3(0.0f, 10.0f, 0.0f), new Vec3(0.6f, 0.8f, 0.0f), 20.0f,
//                sphereTimer.getAlpha(), 2.0f, MaterialNames.DullGrey, drawImposter[1]);
//
//        drawSphereOrbit(gl3, modelMatrix, new Vec3(-10.0f, 1.0f, 0.0f), new Vec3(0.0f, 1.0f, 0.0f), 10.0f,
//                sphereTimer.getAlpha(), 1.0f, MaterialNames.BlackShiny, drawImposter[2]);
//
//        drawSphereOrbit(gl3, modelMatrix, new Vec3(10.0f, 1.0f, 0.0f), new Vec3(0.0f, 1.0f, 0.0f), 10.0f,
//                sphereTimer.getAlpha() * 2.0f, 1.0f, MaterialNames.GoldMetal, drawImposter[3]);
//
//        if (drawLights) {
//
//            modelMatrix.push().translate(calcLightPosition()).scale(0.5f);
//
//            gl3.glUseProgram(unlit.theProgram);
//            gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
//
//            Vec4 lightColor = new Vec4(1.0f);
//            gl3.glUniform4fv(unlit.objectColorUnif, 1, lightColor.toDfb(vecBuffer));
//            cube.render(gl3, "flat");
//
//            modelMatrix.pop();
//        }
//
//        if (drawCameraPos) {
//
//            modelMatrix.push().identity().translate(0.0f, 0.0f, -viewPole.getView().radius());
//
//            gl3.glDisable(GL_DEPTH_TEST);
//            gl3.glDepthMask(false);
//            gl3.glUseProgram(unlit.theProgram);
//            gl3.glUniformMatrix4fv(unlit.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
//            gl3.glUniform4f(unlit.objectColorUnif, 0.25f, 0.25f, 0.25f, 1.0f);
//            cube.render(gl3, "flat");
//            gl3.glDepthMask(true);
//            gl3.glEnable(GL_DEPTH_TEST);
//            gl3.glUniform4f(unlit.objectColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
//            cube.render(gl3, "flat");
//
//            modelMatrix.pop();
//        }
//    }
//
//    private Vec4 calcLightPosition() {
//
//        float scale = (float) (Math.PI * 2.0f);
//
//        float timeThroughLoop = sphereTimer.getAlpha();
//        Vec4 ret = new Vec4(0.0f, lightHeight, 0.0f, 1.0f);
//
//        ret.x = (float) (Math.cos(timeThroughLoop * scale) * 20.0f);
//        ret.z = (float) (Math.sin(timeThroughLoop * scale) * 20.0f);
//
//        return ret;
//    }
//
//    private void drawSphere(GL3 gl3, MatrixStack modelMatrix, Vec3 position, float radius, MaterialNames material,
//            boolean drawImposter) {
//
//        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
//                material.ordinal() * materialBlockOffset, MaterialBlock.SIZE);
//
//        if (drawImposter) {
//
//            Vec4 cameraSpherePos = modelMatrix.top().mul_(new Vec4(position, 1.0f));
//            gl3.glUseProgram(litImpProgs[currImpostor.ordinal()].theProgram);
//            gl3.glUniform3fv(litImpProgs[currImpostor.ordinal()].cameraSpherePosUnif, 1, cameraSpherePos.toDfb(vecBuffer));
//            gl3.glUniform1f(litImpProgs[currImpostor.ordinal()].sphereRadiusUnif, radius);
//
//            gl3.glBindVertexArray(imposterVAO.get(0));
//
//            gl3.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
//
//            gl3.glBindVertexArray(0);
//            gl3.glUseProgram(0);
//
//        } else {
//
//            modelMatrix.push().translate(position).scale(radius * 2.0f); //The unit sphere has a radius 0.5f.
//
//            Mat3 normMatrix = modelMatrix.top().toMat3_().inverse().transpose();
//
//            gl3.glUseProgram(litMeshProg.theProgram);
//            gl3.glUniformMatrix4fv(litMeshProg.modelToCameraMatrixUnif, 1, false, modelMatrix.top().toDfb(matBuffer));
//            gl3.glUniformMatrix3fv(litMeshProg.normalModelToCameraMatrixUnif, 1, false, normMatrix.toDfb(matBuffer));
//
//            sphere.render(gl3, "lit");
//
//            gl3.glUseProgram(0);
//            modelMatrix.pop();
//        }
//        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, 0);
//    }
//
//    private void drawSphereOrbit(GL3 gl3, MatrixStack modelMatrix, Vec3 orbitCenter, Vec3 orbitAxis, float orbitRadius,
//            float orbitAlpha, float sphereRadius, MaterialNames material, boolean drawImposter) {
//
//        modelMatrix.push().translate(orbitCenter).rotate(orbitAxis, 360.0f * orbitAlpha);
//
//        Vec3 offsetDir = orbitAxis.cross_(new Vec3(0.0f, 1.0f, 0.0f));
//        if (offsetDir.length() < 0.001f) {
//            offsetDir = orbitAxis.cross_(new Vec3(1.0f, 0.0f, 0.0f));
//        }
//
//        offsetDir.normalize();
//
//        modelMatrix.translate(offsetDir.mul_(orbitRadius));
//
//        drawSphere(gl3, modelMatrix, new Vec3(0.0f), sphereRadius, material, drawImposter);
//
//        modelMatrix.pop();
//    }
//
//    @Override
//    public void reshape(GL3 gl3, int w, int h) {
//
//        float zNear = 1.0f, zFar = 1_000f;
//        MatrixStack perspMatrix = new MatrixStack();
//
//        Mat4 proj = perspMatrix.perspective(45.0f, (float) w / h, zNear, zFar).top();
//
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
//        gl3.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, proj.toDfb(matBuffer));
//        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
//
//        gl3.glViewport(0, 0, w, h);
//    }
//
//    @Override
//    public void mousePressed(MouseEvent e) {
//        viewPole.mousePressed(e);
//    }
//
//    @Override
//    public void mouseDragged(MouseEvent e) {
//        viewPole.mouseMove(e);
//    }
//
//    @Override
//    public void mouseReleased(MouseEvent e) {
//        viewPole.mouseReleased(e);
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
//            case KeyEvent.VK_P:
//                sphereTimer.togglePause();
//                break;
//            case KeyEvent.VK_MINUS:
//                sphereTimer.rewind(0.5f);
//                break;
//            case KeyEvent.VK_PLUS:
//                sphereTimer.fastForward(0.5f);
//                break;
//            case KeyEvent.VK_T:
//                drawCameraPos = !drawCameraPos;
//                break;
//            case KeyEvent.VK_G:
//                drawLights = !drawLights;
//                break;
//
//            case KeyEvent.VK_1:
//                drawImposter[0] = !drawImposter[0];
//                break;
//            case KeyEvent.VK_2:
//                drawImposter[1] = !drawImposter[1];
//                break;
//            case KeyEvent.VK_3:
//                drawImposter[2] = !drawImposter[2];
//                break;
//            case KeyEvent.VK_4:
//                drawImposter[3] = !drawImposter[3];
//                break;
//
//            case KeyEvent.VK_L:
//                currImpostor = Impostors.Basic;
//                break;
//            case KeyEvent.VK_J:
//                currImpostor = Impostors.Perspective;
//                break;
//            case KeyEvent.VK_H:
//                currImpostor = Impostors.Depth;
//                break;
//        }
//
//        viewPole.charPress(e);
//    }
//
//    @Override
//    public void end(GL3 gl3) {
//
//        IntStreamEx.range(litImpProgs.length).forEach(i -> gl3.glDeleteProgram(litImpProgs[i].theProgram));
//        gl3.glDeleteProgram(litMeshProg.theProgram);
//        gl3.glDeleteProgram(unlit.theProgram);
//
//        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
//        gl3.glDeleteVertexArrays(1, imposterVAO);
//
//        sphere.dispose(gl3);
//        plane.dispose(gl3);
//        cube.dispose(gl3);
//
//        BufferUtils.destroyDirectBuffer(bufferName);
//        BufferUtils.destroyDirectBuffer(imposterVAO);
//        BufferUtils.destroyDirectBuffer(lightBuffer);
//    }
//
//    enum MaterialNames {
//        Terrain, BlueShiny, GoldMetal, DullGrey, BlackShiny;
//    };
//
//    enum Impostors {
//        Basic, Perspective, Depth;
//    }
//}
