
package main.tut08;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import glm.mat.Mat4x4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import main.framework.Framework;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static glm.GlmKt.glm;
import static uno.glsl.UtilKt.programOf;

/**
 * @author gbarbieri
 */
public class CameraRelative extends Framework {

    public static void main(String[] args) {
        new CameraRelative("Tutorial 08 - Camera Relative");
    }

    public CameraRelative(String title) {
        super(title);
    }

    private interface OffsetRelative {

        int MODEL = 0;
        int WORLD = 1;
        int CAMERA = 2;
        int MAX = 3;
    }

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;

    private Mesh ship, plane;

    private float frustumScale = calcFrustumScale(20.0f);

    private float calcFrustumScale(float fovDeg) {
        float fovRad = glm.toRad(fovDeg);
        return 1.0f / glm.tan(fovRad / 2.0f);
    }

    private Mat4x4 cameraToClipMatrix = new Mat4x4(0.0f);

    private Vec3 camTarget = new Vec3(0.0f, 10.0f, 0.0f);
    private Quat orientation = new Quat(1.0f, 0.0f, 0.0f, 0.0f);

    //In spherical coordinates.
    private Vec3 sphereCamRelPos = new Vec3(90.0f, 0.0f, 66.0f);

    private int offset = OffsetRelative.MODEL;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            ship = new Mesh(gl, getClass(), "tut08/Ship.xml");
            plane = new Mesh(gl, getClass(), "tut08/UnitPlane.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(CameraRelative.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl.glEnable(GL_CULL_FACE);
        gl.glCullFace(GL_BACK);
        gl.glFrontFace(GL_CW);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDepthRangef(0.0f, 1.0f);
    }

    private void initializeProgram(GL3 gl) {

        theProgram = programOf(gl, getClass(), "tut08", "pos-color-local-transform.vert", "color-mult-uniform.frag");

        modelToCameraMatrixUnif = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix");
        baseColorUnif = gl.glGetUniformLocation(theProgram, "baseColor");

        float zNear = 1.0f;
        float zFar = 600.0f;

        cameraToClipMatrix.v00(frustumScale);
        cameraToClipMatrix.v11(frustumScale);
        cameraToClipMatrix.v22((zFar + zNear) / (zNear - zFar));
        cameraToClipMatrix.v23(-1.0f);
        cameraToClipMatrix.v32((2 * zFar * zNear) / (zNear - zFar));

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.to(matBuffer));
        gl.glUseProgram(0);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack currMatrix = new MatrixStack();

        Vec3 camPos = resolveCamPosition();

        currMatrix.setMatrix(calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f)));

        gl.glUseProgram(theProgram);

        {
            currMatrix
                    .push()
                    .scale(100.0f, 1.0f, 100.0f)
                    .top().to(matBuffer);

            gl.glUniform4f(baseColorUnif, 0.2f, 0.5f, 0.2f, 1.0f);
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matBuffer);

            plane.render(gl);

            currMatrix.pop();
        }

        {
            currMatrix.push()
                    .translate(camTarget)
                    .applyMatrix(orientation.toMat4())
                    .rotateX(-90.0f);

            gl.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
            gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().to(matBuffer));

            ship.render(gl, "tint");

            currMatrix.pop();
        }
        gl.glUseProgram(theProgram);
    }

    private Vec3 resolveCamPosition() {

        float phi = glm.toRad(sphereCamRelPos.x);
        float theta = glm.toRad(sphereCamRelPos.y + 90.0f);

        float sinTheta = glm.sin(theta);
        float cosTheta = glm.cos(theta);
        float cosPhi = glm.cos(phi);
        float sinPhi = glm.sin(phi);

        Vec3 dirToCamera = new Vec3(sinTheta * cosPhi, cosTheta, sinTheta * sinPhi);

        return dirToCamera.times(sphereCamRelPos.z).plus(camTarget);
    }

    private Mat4x4 calcLookAtMatrix(Vec3 cameraPt, Vec3 lookPt, Vec3 upPt) {

        Vec3 lookDir = lookPt.minus(cameraPt).normalize();
        Vec3 upDir = upPt.normalize();

        Vec3 rightDir = lookDir.cross(upDir).normalize();
        Vec3 perpUpDir = rightDir.cross(lookDir);

        Mat4x4 rotationMat = new Mat4x4(1.0f);
        rotationMat.set(0, new Vec4(rightDir, 0.0f));
        rotationMat.set(1, new Vec4(perpUpDir, 0.0f));
        rotationMat.set(2, new Vec4(lookDir.negate(), 0.0f));

        rotationMat.transpose_();

        Mat4x4 translMat = new Mat4x4(1.0f);
        translMat.set(3, new Vec4(cameraPt.negate(), 1.0f));

        return rotationMat.times(translMat);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        cameraToClipMatrix.v00(frustumScale * (h / (float) w));
        cameraToClipMatrix.v11(frustumScale);

        gl.glUseProgram(theProgram);
        gl.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.to(matBuffer));
        gl.glUseProgram(0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);

        plane.dispose(gl);
        ship.dispose(gl);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        final float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(window);
                window.destroy();
                break;

            case KeyEvent.VK_W:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), +smallAngleIncrement);
                break;
            case KeyEvent.VK_S:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_A:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), +smallAngleIncrement);
                break;
            case KeyEvent.VK_D:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_Q:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), +smallAngleIncrement);
                break;
            case KeyEvent.VK_E:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_SPACE:

                offset = (offset + 1) % OffsetRelative.MAX;
            {
                switch (offset) {

                    case OffsetRelative.MODEL:
                        System.out.println("MODEL_RELATIVE");
                        break;

                    case OffsetRelative.WORLD:
                        System.out.println("WORLD_RELATIVE");
                        break;

                    case OffsetRelative.CAMERA:
                        System.out.println("CAMERA_RELATIVE");
                        break;
                }
                break;
            }

            case KeyEvent.VK_I:
                sphereCamRelPos.y -= e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_K:
                sphereCamRelPos.y += e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_J:
                sphereCamRelPos.x -= e.isShiftDown() ? 1.125f : 11.25f;
                break;

            case KeyEvent.VK_L:
                sphereCamRelPos.x += e.isShiftDown() ? 1.125f : 11.25f;
                break;
        }

        sphereCamRelPos.y = glm.clamp(sphereCamRelPos.y, -78.75f, 10.0f);
    }

    private void offsetOrientation(Vec3 axis, float angDeg) {

        float angRad = glm.toRad(angDeg);

        axis.normalize();

        axis.times_(glm.sin(angRad / 2.0f));
        float scalar = glm.cos(angRad / 2.0f);

        Quat offsetQuat = new Quat(scalar, axis);

        switch (offset) {

            case OffsetRelative.MODEL:
                orientation.times_(offsetQuat);
                break;

            case OffsetRelative.WORLD:
                orientation = offsetQuat.times(orientation);
                break;

            case OffsetRelative.CAMERA: {

                Vec3 camPos = resolveCamPosition();
                Mat4x4 camMat = calcLookAtMatrix(camPos, camTarget, new Vec3(0.0f, 1.0f, 0.0f));

                Quat viewQuat = camMat.toQuat();
                Quat invViewQuat = viewQuat.conjugate();

                Quat worldQuat = invViewQuat.times(offsetQuat).times(viewQuat);
                orientation = worldQuat.times(orientation);
            }
            break;
        }

        orientation.normalize_();
    }
}