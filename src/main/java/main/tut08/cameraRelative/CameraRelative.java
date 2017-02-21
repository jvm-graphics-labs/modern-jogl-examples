///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut08.cameraRelative;
//
//import com.jogamp.newt.event.KeyEvent;
//import static com.jogamp.opengl.GL.GL_BACK;
//import static com.jogamp.opengl.GL.GL_CULL_FACE;
//import static com.jogamp.opengl.GL.GL_CW;
//import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
//import static com.jogamp.opengl.GL.GL_LEQUAL;
//import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
//import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
//import static com.jogamp.opengl.GL2ES3.GL_COLOR;
//import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
//import com.jogamp.opengl.GL3;
//import com.jogamp.opengl.util.glsl.ShaderCode;
//import com.jogamp.opengl.util.glsl.ShaderProgram;
//import glutil.BufferUtils;
//import main.framework.Framework;
//import main.framework.component.Mesh;
//import glm.Glm;
//import glm.mat._4.Mat4;
//import glm.quat.Quat;
//import glm.vec._3.Vec3;
//import glutil.MatrixStack;
//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.ParserConfigurationException;
//import org.xml.sax.SAXException;
//
///**
// *
// * @author gbarbieri
// */
//public class CameraRelative extends Framework {
//
//    private final String SHADERS_ROOT = "/tut08/cameraRelative/shaders", MESHES_ROOT = "/tut08/data/",
//            VERT_SHADER_SRC = "pos-color-local-transform", FRAG_SHADER_SRC = "color-mult-uniform",
//            SHIP_SCR = "Ship.xml", PLANE_SRC = "UnitPlane.xml";
//
//    public static void main(String[] args) {
//        new CameraRelative("Tutorial 08 - Camera Relative");
//    }
//
//    public CameraRelative(String title) {
//        super(title);
//    }
//
//    private interface OffsetRelative {
//
//        public final static int MODEL_RELATIVE = 0;
//        public final static int WORLD_RELATIVE = 1;
//        public final static int CAMERA_RELATIVE = 2;
//        public final static int MAX = 3;
//    }
//
//    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;
//    private Mesh ship, plane;
//    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(20.0f) / 2.0));
//    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
//
//    private Vec3 camTarget = new Vec3(0.0f, 10.0f, 0.0f);
//    private Quat orientation = new Quat(1.0f, 0.0f, 0.0f, 0.0f);
//
//    //In spherical coordinates.
//    private Vec3 sphereCamRelPos = new Vec3(90.0f, 0.0f, 66.0f);
//
//    private int offset = OffsetRelative.MODEL_RELATIVE;
//
//    @Override
//    public void init(GL3 gl3) {
//
//        initializeProgram(gl3);
//        try {
//            ship = new Mesh(MESHES_ROOT + SHIP_SCR, gl3);
//            plane = new Mesh(MESHES_ROOT + PLANE_SRC, gl3);
//        } catch (ParserConfigurationException | SAXException | IOException ex) {
//            Logger.getLogger(CameraRelative.class.getName()).log(Level.SEVERE, null, ex);
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
//    }
//
//    private void initializeProgram(GL3 gl3) {
//
//        ShaderProgram shaderProgram = new ShaderProgram();
//
//        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
//                VERT_SHADER_SRC, "vert", null, true);
//        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
//                FRAG_SHADER_SRC, "frag", null, true);
//
//        shaderProgram.add(vertShaderCode);
//        shaderProgram.add(fragShaderCode);
//
//        shaderProgram.link(gl3, System.out);
//
//        theProgram = shaderProgram.program();
//
//        vertShaderCode.destroy(gl3);
//        fragShaderCode.destroy(gl3);
//
//        modelToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "modelToCameraMatrix");
//        cameraToClipMatrixUnif = gl3.glGetUniformLocation(theProgram, "cameraToClipMatrix");
//        baseColorUnif = gl3.glGetUniformLocation(theProgram, "baseColor");
//
//        float zNear = 1.0f;
//        float zFar = 600.0f;
//
//        cameraToClipMatrix.m00 = frustumScale;
//        cameraToClipMatrix.m11 = frustumScale;
//        cameraToClipMatrix.m22 = (zFar + zNear) / (zNear - zFar);
//        cameraToClipMatrix.m23 = -1.0f;
//        cameraToClipMatrix.m32 = (2 * zFar * zNear) / (zNear - zFar);
//
//        gl3.glUseProgram(theProgram);
//        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matBuffer));
//        gl3.glUseProgram(0);
//    }
//
//    @Override
//    public void display(GL3 gl3) {
//
//        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
//        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));
//
//        MatrixStack currMatrix = new MatrixStack();
//
//        Vec3 camPos = resolveCamPosition();
//
//        currMatrix.setMatrix(calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)));
//
//        gl3.glUseProgram(theProgram);
//
//        {
//            currMatrix
//                    .push()
//                    .scale(new Vec3(100.0f, 1.0f, 100.0f));
//
//            gl3.glUniform4f(baseColorUnif, 0.2f, 0.5f, 0.2f, 1.0f);
//            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().toDfb(matBuffer));
//
//            plane.render(gl3);
//
//            currMatrix.pop();
//        }
//
//        {
//            currMatrix.push()
//                    .translate(camTarget)
//                    .applyMatrix(Mat4.cast_(orientation))
//                    .rotateX(-90.0f);
//
//            gl3.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
//            gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().toDfb(matBuffer));
//
//            ship.render(gl3, "tint");
//
//            currMatrix.pop();
//        }
//        gl3.glUseProgram(theProgram);
//    }
//
//    private Vec3 resolveCamPosition() {
//
//        float phi = (float) Math.toRadians(sphereCamRelPos.x);
//        float theta = (float) Math.toRadians(sphereCamRelPos.y + 90.0f);
//
//        float sinTheta = (float) Math.sin(theta);
//        float cosTheta = (float) Math.cos(theta);
//        float cosPhi = (float) Math.cos(phi);
//        float sinPhi = (float) Math.sin(phi);
//
//        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);
//
//        return dirToCamera.mul(sphereCamRelPos.z).add(camTarget);
//    }
//
//    private Mat4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {
//
//        Vec3 lookDir = lookPt.sub_(cameraPt).normalize();
//        Vec3 upDir = upPt.normalize();
//
//        Vec3 rightDir = lookDir.cross_(upDir).normalize();
//        Vec3 perpUpDir = rightDir.cross_(lookDir);
//
//        Mat4 rotationMat = new Mat4(1.0f);
//        rotationMat.c0(rightDir, 0.0f);
//        rotationMat.c1(perpUpDir, 0.0f);
//        rotationMat.c2(lookDir.negate(), 0.0f);
//
//        rotationMat = rotationMat.transpose();
//
//        Mat4 translationMat = new Mat4(1.0f);
//        translationMat.c3(cameraPt.negate(), 1.0f);
//
//        return rotationMat.mul(translationMat);
//    }
//
//    @Override
//    public void reshape(GL3 gl3, int w, int h) {
//
//        cameraToClipMatrix.m00 = frustumScale * (h / (float) w);
//        cameraToClipMatrix.m11 = frustumScale;
//
//        gl3.glUseProgram(theProgram);
//        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matBuffer));
//        gl3.glUseProgram(0);
//
//        gl3.glViewport(0, 0, w, h);
//    }
//
//    @Override
//    public void end(GL3 gl3) {
//
//        gl3.glDeleteProgram(theProgram);
//
//        plane.dispose(gl3);
//        ship.dispose(gl3);
//    }
//
//    @Override
//    public void keyPressed(KeyEvent e) {
//
//        final float smallAngleIncrement = 9.0f;
//
//        switch (e.getKeyCode()) {
//
//            case KeyEvent.VK_ESCAPE:
//                animator.remove(glWindow);
//                glWindow.destroy();
//                break;
//
//            case KeyEvent.VK_W:
//                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), +smallAngleIncrement);
//                break;
//            case KeyEvent.VK_S:
//                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), -smallAngleIncrement);
//                break;
//
//            case KeyEvent.VK_A:
//                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), +smallAngleIncrement);
//                break;
//            case KeyEvent.VK_D:
//                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), -smallAngleIncrement);
//                break;
//
//            case KeyEvent.VK_Q:
//                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), +smallAngleIncrement);
//                break;
//            case KeyEvent.VK_E:
//                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), -smallAngleIncrement);
//                break;
//
//            case KeyEvent.VK_SPACE:
//
//                offset = (offset + 1) % OffsetRelative.MAX;
//                 {
//                    switch (offset) {
//
//                        case OffsetRelative.MODEL_RELATIVE:
//                            System.out.println("MODEL_RELATIVE");
//                            break;
//
//                        case OffsetRelative.WORLD_RELATIVE:
//                            System.out.println("WORLD_RELATIVE");
//                            break;
//
//                        case OffsetRelative.CAMERA_RELATIVE:
//                            System.out.println("CAMERA_RELATIVE");
//                            break;
//                    }
//                    break;
//                }
//
//            case KeyEvent.VK_I:
//                sphereCamRelPos.y -= e.isShiftDown() ? 1.125f : 11.25f;
//                break;
//
//            case KeyEvent.VK_K:
//                sphereCamRelPos.y += e.isShiftDown() ? 1.125f : 11.25f;
//                break;
//
//            case KeyEvent.VK_J:
//                sphereCamRelPos.x -= e.isShiftDown() ? 1.125f : 11.25f;
//                break;
//
//            case KeyEvent.VK_L:
//                sphereCamRelPos.x += e.isShiftDown() ? 1.125f : 11.25f;
//                break;
//        }
//
//        sphereCamRelPos.y = Glm.clamp(sphereCamRelPos.y, -78.75f, 10.0f);
//    }
//
//    private void offsetOrientation(Vec3 axis, float angDeg) {
//
//        float angRad = (float) Math.toRadians(angDeg);
//
//        axis.normalize();
//
//        axis.mul((float) Math.sin(angRad / 2.0f));
//        float scalar = (float) Math.cos(angRad / 2.0f);
//
//        Quat offsetQuat = new Quat(scalar, axis);
//
//        switch (offset) {
//
//            case OffsetRelative.MODEL_RELATIVE:
//                orientation = orientation.mul(offsetQuat);
//                break;
//
//            case OffsetRelative.WORLD_RELATIVE:
//                orientation = offsetQuat.mul(orientation);
//                break;
//
//            case OffsetRelative.CAMERA_RELATIVE: {
//
//                Vec3 camPos = resolveCamPosition();
//                Mat4 camMat = calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f));
//
//                Quat viewQuat = Quat.cast_(camMat);
//                Quat invViewQuat = viewQuat.conjugate_();
//
//                Quat worldQuat = invViewQuat.mul(offsetQuat).mul(viewQuat);
//                orientation = worldQuat.mul(orientation);
//            }
//            break;
//        }
//
//        orientation.normalize();
//    }
//}
