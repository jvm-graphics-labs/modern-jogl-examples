/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut12.sceneLighting;

import com.jogamp.newt.event.MouseEvent;
import static com.jogamp.opengl.GL.GL_BACK;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_CW;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import com.jogamp.opengl.GL3;
import static com.jogamp.opengl.GL3.GL_DEPTH_CLAMP;
import com.jogamp.opengl.util.GLBuffers;
import main.framework.Framework;
import main.framework.Semantic;
import main.framework.component.Mesh;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import view.ObjectData;
import view.ObjectPole;
import view.ViewData;
import view.ViewPole;
import view.ViewScale;

/**
 *
 * @author elect
 */
public class SceneLighting extends Framework {

    static final String SHADERS_ROOT = "/tut12/sceneLighting/shaders",
            POS_TRANSFORM_SHADER_SRC = "pos-transform", UNIFORM_COLOR_SHADER_SRC = "uniform-color";

    public static void main(String[] args) {
        new SceneLighting("Tutorial 12 - Gaussian Specular Lighting");
    }

    private UnlitProgData unlit;

    private ViewData initialViewData = new ViewData(
            new Vec3(-59.5f, 44.0f, 95.0f),
            new Quat(0.92387953f, 0.3826834f, 0.0f, 0.0f),
            50.0f,
            0.0f);
    private ViewScale viewScale = new ViewScale(
            3.0f, 80.0f,
            4.0f, 1.0f,
            5.0f, 1.0f, //No camera movement.
            90.0f / 250.0f);

    private ViewPole viewPole = new ViewPole(initialViewData, viewScale, MouseEvent.BUTTON1);

    private Vec4 skyDaylightColor = new Vec4(0.65f, 0.65f, 1.0f, 1.0f);

    private Scene scene;

    private interface Buffer {

        public static final int LIGHT = 0;
        public static final int PROJECTION = 1;
        public static final int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);

    private boolean drawCameraPos = false, drawLights = true;

    public SceneLighting(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl3) {

        unlit = new UnlitProgData(gl3, SHADERS_ROOT, POS_TRANSFORM_SHADER_SRC, UNIFORM_COLOR_SHADER_SRC);

        scene = new Scene(gl3);

        float depthZNear = 0.0f, depthZFar = 1.0f;

        gl3.glEnable(GL_CULL_FACE);
        gl3.glCullFace(GL_BACK);
        gl3.glFrontFace(GL_CW);

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthMask(true);
        gl3.glDepthFunc(GL_LEQUAL);
        gl3.glDepthRangef(depthZNear, depthZFar);
        gl3.glEnable(GL_DEPTH_CLAMP);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.LIGHT));
//        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW); // TODO

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl3.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        //Bind the static buffers.
//        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.LIGHT, bufferName.get(Buffer.LIGHT),
//                0, Mat4.SIZE); // TODO size
        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION),
                0, Mat4.SIZE);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void setupDaytimeLighting() {

    }
}
