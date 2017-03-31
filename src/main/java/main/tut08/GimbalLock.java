
package main.tut08;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import glm.mat.Mat4;
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
public class GimbalLock extends Framework {

    public static void main(String[] args) {
        new GimbalLock().setup("Tutorial 08 - Gimbal Lock");
    }

    private final String[] GIMBALS_SCR = {"LargeGimbal.xml", "MediumGimbal.xml", "SmallGimbal.xml"};

    private interface Gimbal {

        int LARGE = 0;
        int MEDIUM = 1;
        int SMALL = 2;
        int MAX = 3;
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

    private float frustumScale = calcFrustumScale(20);

    private float calcFrustumScale(float fovDeg) {
        float fovRad = glm.toRad(fovDeg);
        return 1.0f / glm.tan(fovRad / 2.0f);
    }

    private Mat4 cameraToClipMatrix = new Mat4(0.0f);

    private GimbalAngles angles = new GimbalAngles();

    private boolean drawGimbals = true;

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            for (int loop = 0; loop < Gimbal.MAX; loop++) {
                gimbals[loop] = new Mesh(gl, getClass(), "tut08/" + GIMBALS_SCR[loop]);
            }
            object = new Mesh(gl, getClass(), "tut08/Ship.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(GimbalLock.class.getName()).log(Level.SEVERE, null, ex);
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

        MatrixStack currMatrix = new MatrixStack()
                .translate(new Vec3(0.0f, 0.0f, -200.0f))
                .rotateX(angles.angleX);
        drawGimbal(gl, currMatrix, GimbalAxis.X, new Vec4(0.4f, 0.4f, 1.0f, 1.0f));

        currMatrix.rotateY(angles.angleY);
        drawGimbal(gl, currMatrix, GimbalAxis.Y, new Vec4(0.0f, 1.0f, 0.0f, 1.0f));

        currMatrix.rotateY(angles.angleZ);
        drawGimbal(gl, currMatrix, GimbalAxis.Z, new Vec4(1.0f, 0.3f, 0.3f, 1.0f));

        gl.glUseProgram(theProgram);
        currMatrix
                .scale(new Vec3(3.0f))
                .rotateX(-90);
        //Set the base color for this object.
        gl.glUniform4f(baseColorUnif, 1.0f, 1.0f, 1.0f, 1.0f);
        gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, currMatrix.top().to(matBuffer));

        object.render(gl, "tint");

        gl.glUseProgram(0);
    }

    private void drawGimbal(GL3 gl, MatrixStack matrixStack, GimbalAxis axis, Vec4 baseColor) {

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

        gl.glUseProgram(theProgram);
        //Set the base color for this object.
        gl.glUniform4fv(baseColorUnif, 1, baseColor.to(vecBuffer));
        gl.glUniformMatrix4fv(modelToCameraMatrixUnif, 1, false, matrixStack.top().to(matBuffer));

        gimbals[axis.ordinal()].render(gl);

        gl.glUseProgram(0);
        matrixStack.pop();
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

        object.dispose(gl);
        for (int i = 0; i < Gimbal.MAX; i++)
            gimbals[i].dispose(gl);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        final float smallAngleIncrement = 9.0f;

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
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
