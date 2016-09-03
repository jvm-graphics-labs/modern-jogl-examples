/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08.quaternionYPR;

import com.jogamp.newt.event.KeyEvent;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.Framework;
import framework.component.Mesh;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glutil.MatrixStack;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author gbarbieri
 */
public class QuaternionYPR extends Framework {

    private final String SHADERS_ROOT = "/tut08/quaternionYPR/shaders", DATA_ROOT = "/tut08/quaternionYPR/data/",
            VERT_SHADER_SRC = "pos-color-local-transform", FRAG_SHADER_SRC = "color-mult-uniform",
            SHIP_SRC = "Ship.xml";

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
    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(20.0f) / 2.0));
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private GimbalAngles angles = new GimbalAngles();
    private Quat orientation = new Quat(1.0f, 0.0f, 0.0f, 0.0f);
    private boolean rightMultiply = true;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        try {
            ship = new Mesh(DATA_ROOT + SHIP_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(QuaternionYPR.class.getName()).log(Level.SEVERE, null, ex);
        }

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(0.0f, 1.0f);
    }

    private void initializeProgram(GL3 gl3) {

        ShaderProgram shaderProgram = new ShaderProgram();

        ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                VERT_SHADER_SRC, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                FRAG_SHADER_SRC, "frag", null, true);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);

        shaderProgram.link(gl3, System.out);

        theProgram = shaderProgram.program();

        vertShaderCode.destroy(gl3);
        fragShaderCode.destroy(gl3);

        modelToCameraMatrixUnif = gl3.glGetUniformLocation(theProgram, "modelToCameraMatrix");
        cameraToClipMatrixUnif = gl3.glGetUniformLocation(theProgram, "cameraToClipMatrix");
        baseColorUnif = gl3.glGetUniformLocation(theProgram, "baseColor");

        float zNear = 1.0f, zFar = 600.0f;

        cameraToClipMatrix.m00 = frustumScale;
        cameraToClipMatrix.m11 = frustumScale;
        cameraToClipMatrix.m22 = (zFar + zNear) / (zNear - zFar);
        cameraToClipMatrix.m23 = -1.0f;
        cameraToClipMatrix.m32 = (2 * zFar * zNear) / (zNear - zFar);

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matBuffer));
        gl3.glUseProgram(0);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack matrixStack = new MatrixStack()
                .translate(new Vec3(0.0f, 0.0f, -200.0f))
                .applyMatrix(Mat4.cast_(orientation));

        gl3.glUseProgram(theProgram);

        matrixStack
                .scale(new Vec3(3.0f, 3.0f, 3.0f))
                .rotateX(-90.0f);

        gl3.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matrixStack.top().toDfb(matBuffer));

        ship.render(gl3, "tint");

        gl3.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        cameraToClipMatrix.m00 = frustumScale * (h / (float) w);
        cameraToClipMatrix.m11 = frustumScale;

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matBuffer));
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);

        ship.dispose(gl3);
    }

    private void offsetOrientation(Vec3 axis, float angDeg) {

        float angRad = (float) Math.toRadians(angDeg);

        axis.normalize();

        axis.mul((float) Math.sin(angRad / 2.0f));
        float scalar = (float) Math.cos(angRad / 2.0f);

        Quat offset = new Quat(scalar, axis);

        if (rightMultiply) {
            orientation = orientation.mul(offset);
        } else {
            orientation = offset.mul(orientation);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        final float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
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
                System.out.println(rightMultiply? "Right-multiply" : "Left-multiply");
                break;
        }
    }
}
