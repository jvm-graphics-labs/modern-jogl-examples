/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tut08.interpolation;

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
public class Interpolation extends Framework {

    private final String SHADERS_ROOT = "/tut08/interpolation/shaders", DATA_ROOT = "/tut08/interpolation/data/",
            VERT_SHADER_SRC = "pos-color-local-transform", FRAG_SHADER_SRC = "color-mult-uniform",
            SHIP_SRC = "Ship.xml";

    public static void main(String[] args) {
        new Interpolation("Tutorial 08 - Interpolation");
    }

    public Interpolation(String title) {
        super(title);
    }

    private Mesh ship;
    private int theProgram, modelToCameraMatrixUnif, cameraToClipMatrixUnif, baseColorUnif;
    private float frustumScale = (float) (1.0f / Math.tan(Math.toRadians(20.0f) / 2.0));
    private Mat4 cameraToClipMatrix = new Mat4(0.0f);
    private Orientation orient = new Orientation();

    @Override
    public void init(GL3 gl3) {

        initializeProgram(gl3);

        try {
            ship = new Mesh(DATA_ROOT + SHIP_SRC, gl3);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(Interpolation.class.getName()).log(Level.SEVERE, null, ex);
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

        orient.updateTime();

        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        MatrixStack matrixStack = new MatrixStack()
                .translate(new Vec3(0.0f, 0.0f, -200.0f))
                .applyMatrix(Mat4.cast_(orient.getOrient()));

        gl3.glUseProgram(theProgram);
        matrixStack
                .scale(new Vec3(3.0f, 3.0f, 3.0f))
                .rotateX(-90.0f);
        //Set the base color for this object.
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
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                animator.remove(glWindow);
                glWindow.destroy();
                break;

            case KeyEvent.VK_SPACE:
                boolean slerp = orient.toggleSlerp();
                System.out.println(slerp ? "Slerp" : "Lerp");
                break;
        }

        for (int i = 0; i < orientKeys.length; i++) {
            if (e.getKeyCode() == orientKeys[i]) {
                applyOrientation(i);
            }
        }
    }

    private void applyOrientation(int index) {
        if (!orient.isAnimating()) {
            orient.animateToOrient(index);
        }
    }

    private final short[] orientKeys = {
        KeyEvent.VK_Q,
        KeyEvent.VK_W,
        KeyEvent.VK_E,
        KeyEvent.VK_R,
        //        
        KeyEvent.VK_T,
        KeyEvent.VK_Y,
        KeyEvent.VK_U};
    
    @Override
    public void end(GL3 gl3) {

        gl3.glDeleteProgram(theProgram);

        ship.dispose(gl3);

        BufferUtils.destroyDirectBuffer(matBuffer);
    }
}
