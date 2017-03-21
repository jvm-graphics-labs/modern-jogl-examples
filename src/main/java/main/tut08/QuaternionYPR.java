
package main.tut08;

import com.jogamp.newt.event.KeyEvent;

import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static glm.GlmKt.glm;
import static uno.glsl.UtilKt.programOf;

import com.jogamp.opengl.GL3;
import main.framework.Framework;
import main.framework.component.Mesh;
import glm.mat.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import uno.glm.MatrixStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @author gbarbieri
 */
public class QuaternionYPR extends Framework {

    public static void main(String[] args) {
        new QuaternionYPR("Tutorial 08 - Quaternion YPR");
    }

    public QuaternionYPR(String title) {
        super(title);
    }

    private class GimbalAngles {

        public float angleX;
        public float angleY;
        public float angleZ;

        public GimbalAngles() {
            angleX = 0.0f;
            angleY = 0.0f;
            angleZ = 0.0f;
        }
    }

    private Mesh ship;

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;

    private float frustumScale = calcFrustumScale(20);

    private float calcFrustumScale(float fovDeg) {
        float fovRad = glm.toRad(fovDeg);
        return 1.0f / glm.tan(fovRad / 2.0f);
    }

    private Mat4 cameraToClipMatrix = new Mat4(0.0f);

    private GimbalAngles angles = new GimbalAngles();

    private Quat orientation = new Quat(1.0f, 0.0f, 0.0f, 0.0f);

    private boolean rightMultiply = true;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            ship = new Mesh(gl, getClass(), "tut08/Ship.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(QuaternionYPR.class.getName()).log(Level.SEVERE, null, ex);
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

        float zNear = 1.0f, zFar = 600.0f;

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

        MatrixStack matrixStack = new MatrixStack()
                .translate(0.0f, 0.0f, -200.0f)
                .applyMatrix(orientation.toMat4());

        gl.glUseProgram(theProgram);

        matrixStack
                .scale(new Vec3(3.0f, 3.0f, 3.0f))
                .rotateX(-90.0f);

        gl.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
        gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matrixStack.top().to(matBuffer));

        ship.render(gl, "tint");

        gl.glUseProgram(0);
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

        ship.dispose(gl);
    }

    private void offsetOrientation(Vec3 axis, float angDeg) {

        float angRad = glm.toRad(angDeg);

        axis.normalize_();

        axis.times_(glm.sin(angRad / 2.0f));
        float scalar = glm.cos(angRad / 2.0f);

        Quat offset = new Quat(scalar, axis);

        if (rightMultiply)
            orientation.times_(offset);
        else
            orientation = offset.times(orientation);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        final float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_W:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), smallAngleIncrement);
                break;
            case KeyEvent.VK_S:
                offsetOrientation(new Vec3(1.0f, 0.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_A:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), smallAngleIncrement);
                break;
            case KeyEvent.VK_D:
                offsetOrientation(new Vec3(0.0f, 0.0f, 1.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_Q:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), smallAngleIncrement);
                break;
            case KeyEvent.VK_E:
                offsetOrientation(new Vec3(0.0f, 1.0f, 0.0f), -smallAngleIncrement);
                break;

            case KeyEvent.VK_SPACE:
                rightMultiply = !rightMultiply;
                System.out.println(rightMultiply ? "Right-multiply" : "Left-multiply");
                break;
        }
    }
}
