/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08.gimbalLock;

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
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.Framework;
import framework.component.Mesh;
import framework.glutil.MatrixStack_;
import glm.mat._4.Mat4;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import one.util.streamex.IntStreamEx;
import org.xml.sax.SAXException;

/**
 *
 * @author gbarbieri
 */
public class GimbalLock extends Framework {

    private final String SHADERS_ROOT = "/tut08/gimbalLock/shaders", DATA_ROOT = "/tut08/gimbalLock/data/",
            VERT_SHADER_SRC = "pos-color-local-transform", FRAG_SHADER_SRC = "color-mult-uniform",
            SHIP_SRC = "Ship.xml";
    private final String[] GIMBALS_SCR = {"LargeGimbal.xml", "MediumGimbal.xml", "SmallGimbal.xml"};

    public static void main(String[] args) {
        new GimbalLock("Tutorial 08 - Gimbal Lock");
    }

    public GimbalLock(String title) {
        super(title);
    }

    private interface Gimbal {

        public static final int LARGE = 0;
        public static final int MEDIUM = 1;
        public static final int SMALL = 2;
        public static final int MAX = 3;
    }

    private enum GimbalAxis {
        X, Y, Z
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

    private Mesh[] gimbals = new Mesh[Gimbal.MAX];
    private Mesh object;
    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;
    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(20.0f) / 2.0));
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private FloatBuffer matrixBuffer = GLBuffers.newDirectFloatBuffer(16),
            vectorBuffer = GLBuffers.newDirectFloatBuffer(4);
    private GimbalAngles angles = new GimbalAngles();
    private boolean drawGimbals = true;

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        try {
            for (int loop = 0; loop < Gimbal.MAX; loop++) {
                gimbals[loop] = new Mesh(DATA_ROOT + GIMBALS_SCR[loop], gl3);
            }
            object = new Mesh(DATA_ROOT + SHIP_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(GimbalLock.class.getName()).log(Level.SEVERE, null, ex);
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
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matrixBuffer));
        gl3.glUseProgram(0);
    }

    @Override
    public void display(GL3 gl3) {

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack_ currMatrix = new MatrixStack_()
                .translate(new Vec3(0.0f, 0.0f, -200.0f))
                .rotateX(angles.angleX);
        drawGimbal(gl3, currMatrix, GimbalAxis.X, new Vec4(0.4f, 0.4f, 1.0f, 1.0f));
        currMatrix.rotateY(angles.angleY);
        drawGimbal(gl3, currMatrix, GimbalAxis.Y, new Vec4(0.0f, 1.0f, 0.0f, 1.0f));
        currMatrix.rotateY(angles.angleZ);
        drawGimbal(gl3, currMatrix, GimbalAxis.Z, new Vec4(1.0f, 0.3f, 0.3f, 1.0f));

        gl3.glUseProgram(theProgram);
        currMatrix
                .scale(new Vec3(3.0f))
                .rotateX(-90);
        //Set the base color for this object.
        gl3.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().toDfb(matrixBuffer));

        object.render(gl3, "tint");

        gl3.glUseProgram(0);
    }

    private void drawGimbal(GL3 gl3, MatrixStack_ matrixStack, GimbalAxis axis, Vec4 baseColor) {

        if (!drawGimbals) {
            return;
        }

        matrixStack.push();

        switch (axis) {

            case X:
                break;

            case Y:
                matrixStack
                        .rotateZ(90.0f)
                        .rotateX(90.0f);
                break;

            case Z:
                matrixStack
                        .rotateY(90.0f)
                        .rotateX(90.0f);
                break;
        }

        gl3.glUseProgram(theProgram);
        //Set the base color for this object.
        gl3.glUniform4fv(baseColorUnif, 1, baseColor.toDfb(vectorBuffer));
        gl3.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matrixStack.top().toDfb(matrixBuffer));

        gimbals[axis.ordinal()].render(gl3);

        gl3.glUseProgram(0);
        matrixStack.pop();
    }

    @Override
    public void reshape(GL3 gl3, int w, int h) {

        cameraToClipMatrix.m00 = frustumScale * (h / (float) w);
        cameraToClipMatrix.m11 = frustumScale;

        gl3.glUseProgram(theProgram);
        gl3.glUniformMatrix4fv(cameraToClipMatrixUnif, 1, false, cameraToClipMatrix.toDfb(matrixBuffer));
        gl3.glUseProgram(0);

        gl3.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);

        object.dispose(gl3);
        IntStreamEx.range(Gimbal.MAX).forEach(i -> gimbals[i].dispose(gl3));

        BufferUtils.destroyDirectBuffer(matrixBuffer);
    }

    @Override
    public void keyboard(KeyEvent e) {

        final float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;

            case KeyEvent.VK_W:
                angles.angleX += smallAngleIncrement;
                break;
            case KeyEvent.VK_S:
                angles.angleX -= smallAngleIncrement;
                break;

            case KeyEvent.VK_A:
                angles.angleY += smallAngleIncrement;
                break;
            case KeyEvent.VK_D:
                angles.angleY -= smallAngleIncrement;
                break;

            case KeyEvent.VK_Q:
                angles.angleZ += smallAngleIncrement;
                break;
            case KeyEvent.VK_E:
                angles.angleZ -= smallAngleIncrement;
                break;

            case KeyEvent.VK_SPACE:
                drawGimbals = !drawGimbals;
                break;
        }
    }
}
