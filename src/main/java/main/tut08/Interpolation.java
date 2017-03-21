
package main.tut08;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import glm.mat.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import main.framework.Framework;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;
import uno.time.Timer;

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
public class Interpolation extends Framework {

    public static void main(String[] args) {
        new Interpolation("Tutorial 08 - Interpolation");
    }

    public Interpolation(String title) {
        super(title);
    }

    private Mesh ship;

    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;

    private float frustumScale = calcFrustumScale(20);

    private float calcFrustumScale(float fovDeg) {
        float fovRad = glm.toRad(fovDeg);
        return 1.0f / glm.tan(fovRad / 2.0f);
    }

    private Mat4 cameraToClipMatrix = new Mat4(0.0f);

    private Orientation orient = new Orientation();

    @Override
    public void init(GL3 gl) {

        initializeProgram(gl);

        try {
            ship = new Mesh(gl, getClass(), "tut08/Ship.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(Interpolation.class.getName()).log(Level.SEVERE, null, ex);
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

        orient.updateTime();

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack matrixStack = new MatrixStack()
                .translate(0.0f, 0.0f, -200.0f)
                .applyMatrix(orient.getOrient().toMat4());

        gl.glUseProgram(theProgram);
        matrixStack
                .scale(new Vec3(3.0f, 3.0f, 3.0f))
                .rotateX(-90.0f);
        //Set the base color for this object.
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
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_SPACE:
                boolean slerp = orient.toggleSlerp();
                System.out.println(slerp ? "Slerp" : "Lerp");
                break;
        }

        for (int i = 0; i < orientKeys.length; i++)
            if (e.getKeyCode() == orientKeys[i])
                applyOrientation(i);
    }

    private void applyOrientation(int index) {
        if (!orient.isAnimating())
            orient.animateToOrient(index);
    }

    private final short[] orientKeys = {
            KeyEvent.VK_Q,
            KeyEvent.VK_W,
            KeyEvent.VK_E,
            KeyEvent.VK_R,

            KeyEvent.VK_T,
            KeyEvent.VK_Y,
            KeyEvent.VK_U};

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(theProgram);

        ship.dispose(gl);
    }

    public class Orientation {

        private boolean isAnimating = false;
        private int currentOrient = 0;
        private boolean slerp = false;
        private final Animation anim = new Animation();

        public boolean toggleSlerp() {
            slerp = !slerp;
            return slerp;
        }

        public Quat getOrient() {
            if (isAnimating)
                return anim.getOrient(orients[currentOrient], slerp);
            else
                return orients[currentOrient];
        }

        public boolean isAnimating() {
            return isAnimating;
        }

        public void updateTime() {
            if (isAnimating) {
                boolean isFinished = anim.updateTime();
                if (isFinished) {
                    isAnimating = false;
                    currentOrient = anim.getFinalX();
                }
            }
        }

        public void animateToOrient(int destination) {
            if (currentOrient == destination)
                return;

            anim.startAnimation(destination, 1.0f);
            isAnimating = true;
        }

        private class Animation {

            private int finalOrient;
            private Timer currTimer;

            public boolean updateTime() {
                return currTimer.update();
            }

            public Quat getOrient(Quat initial, boolean slerp) {
                if (slerp)
                    return slerp(initial, orients[finalOrient], currTimer.getAlpha());
                else
                    return lerp(initial, orients[finalOrient], currTimer.getAlpha());
            }

            public void startAnimation(int destination, float duration) {
                finalOrient = destination;
                currTimer = new Timer(Timer.Type.Single, duration);
            }

            public int getFinalX() {
                return finalOrient;
            }
        }
    }

    private Quat slerp(Quat v0, Quat v1, float alpha) {

        float dot = glm.dot(v0, v1);
        final float DOT_THRESHOLD = 0.9995f;
        if (dot > DOT_THRESHOLD)
            return lerp(v0, v1, alpha);

        dot = glm.clamp(dot, -1.0f, 1.0f);
        float theta0 = glm.acos(dot);
        float theta = theta0 * alpha;

        Quat v2 = v1.minus(v0.times(dot));
        v2.normalize_();

        return v0.times(glm.cos(theta)).plus(v2.times(glm.sin(theta)));
    }

    // TODO check lerp thinkness
    private Quat lerp(Quat v0, Quat v1, float alpha) {

        Vec4 start = v0.vectorize();
        Vec4 end = v1.vectorize();
        Vec4 interp = glm.mix(start, end, alpha);

        System.out.println("alpha: " + alpha + ", " + interp);

        interp.normalize_();
        return new Quat(interp);
    }

    private final Quat[] orients = {
            new Quat(0.7071f, 0.7071f, 0.0f, 0.0f),
            new Quat(0.5f, 0.5f, -0.5f, 0.5f),
            new Quat(-0.4895f, -0.7892f, -0.3700f, -0.02514f),
            new Quat(0.4895f, 0.7892f, 0.3700f, 0.02514f),

            new Quat(0.3840f, -0.1591f, -0.7991f, -0.4344f),
            new Quat(0.5537f, 0.5208f, 0.6483f, 0.0410f),
            new Quat(0.0f, 0.0f, 1.0f, 0.0f)};
}
