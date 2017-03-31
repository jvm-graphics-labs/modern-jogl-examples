
package main.tut16;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import glm.mat.Mat4;
import glm.vec._2.Vec2;
import glm.vec._2.Vec2s;
import main.framework.Framework;
import main.framework.Semantic;
import uno.glm.MatrixStack;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.glsl.UtilKt.programOf;


/**
 * @author gbarbieri
 */
public class GammaRamp extends Framework {

    public static void main(String[] args) {
        new GammaRamp().setup("Tutorial 14 - Material Texture");
    }


    private int noGammaProgram, gammaProgram;

    private interface Buffer {
        int VERTEX = 0;
        int PROJECTION = 1;
        int MAX = 2;
    }

    private interface Texture {
        int NO_GAMMA = 0;
        int GAMMA = 1;
        int MAX = 2;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
    private IntBuffer textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private IntBuffer samplerName = GLBuffers.newDirectIntBuffer(1);

    private boolean[] useGammaCorrect = new boolean[]{false, false};

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        gl.glGenBuffers(Buffer.MAX, bufferName);
        initializeVertexData(gl);

        loadTextures(gl);

        //Setup our Uniform Buffers
        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE, null, GL_DYNAMIC_DRAW);

        gl.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.PROJECTION, bufferName.get(Buffer.PROJECTION), 0, Mat4.SIZE);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    private void initializePrograms(GL3 gl) {

        noGammaProgram = programOf(gl, getClass(), "tut16", "screen-coords.vert", "texture-no-gamma.frag");
        gammaProgram = programOf(gl, getClass(), "tut16", "screen-coords.vert", "texture-gamma.frag");

        int projectionBlock = gl.glGetUniformBlockIndex(noGammaProgram, "Projection");
        gl.glUniformBlockBinding(noGammaProgram, projectionBlock, Semantic.Uniform.PROJECTION);

        int colorTextureUnif = gl.glGetUniformLocation(noGammaProgram, "colorTexture");
        gl.glUseProgram(noGammaProgram);
        gl.glUniform1i(colorTextureUnif, Semantic.Sampler.DIFFUSE);
        gl.glUseProgram(0);

        projectionBlock = gl.glGetUniformBlockIndex(gammaProgram, "Projection");
        gl.glUniformBlockBinding(gammaProgram, projectionBlock, Semantic.Uniform.PROJECTION);

        colorTextureUnif = gl.glGetUniformLocation(gammaProgram, "colorTexture");
        gl.glUseProgram(gammaProgram);
        gl.glUniform1i(colorTextureUnif, Semantic.Sampler.DIFFUSE);
        gl.glUseProgram(0);
    }

    private void initializeVertexData(GL3 gl) {

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexData.capacity() * Short.BYTES, vertexData, GL_STATIC_DRAW);

        gl.glGenVertexArrays(1, vao);

        gl.glBindVertexArray(vao.get(0));
        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, Vec2s.length, GL_UNSIGNED_SHORT, false, Vec2s.SIZE * 2, 0);
        gl.glEnableVertexAttribArray(Semantic.Attr.TEX_COORD);
        gl.glVertexAttribPointer(Semantic.Attr.TEX_COORD, Vec2s.length, GL_UNSIGNED_SHORT, true, Vec2s.SIZE * 2, Vec2s.SIZE);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private ShortBuffer vertexData = GLBuffers.newDirectShortBuffer(new short[]{
            90, 80, 0, 0,
            90, 16, 0, (short) 65535,
            410, 80, (short) 65535, 0,
            410, 16, (short) 65535, (short) 65535,
            90, 176, 0, 0,
            90, 112, 0, (short) 65535,
            410, 176, (short) 65535, 0,
            410, 112, (short) 65535, (short) 65535});

    private void loadTextures(GL3 gl) {

        try {
            gl.glGenTextures(Texture.MAX, textureName);

            File file = new File(getClass().getResource("/tut16/gamma_ramp.png").toURI());

            TextureData textureData = TextureIO.newTextureData(gl.getGLProfile(), file, false, TextureIO.PNG);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.NO_GAMMA));
            gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, textureData.getWidth(), textureData.getHeight(),
                    0, textureData.getPixelFormat(), textureData.getPixelType(), textureData.getBuffer());
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

            gl.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.GAMMA));
            gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8, textureData.getWidth(), textureData.getHeight(),
                    0, textureData.getPixelFormat(), textureData.getPixelType(), textureData.getBuffer());
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);

            gl.glBindTexture(GL_TEXTURE_2D, 0);

            gl.glGenSamplers(1, samplerName);
            gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(GammaRamp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.5f).put(2, 0.3f).put(3, 1.0f));

        gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(useGammaCorrect[0] ? Texture.GAMMA : Texture.NO_GAMMA));
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, samplerName.get(0));

        gl.glBindVertexArray(vao.get(0));

        gl.glUseProgram(noGammaProgram);
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        gl.glBindTexture(GL_TEXTURE_2D, textureName.get(useGammaCorrect[1] ? Texture.GAMMA : Texture.NO_GAMMA));

        gl.glUseProgram(gammaProgram);
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);

        gl.glBindVertexArray(0);
        gl.glUseProgram(0);

        gl.glActiveTexture(GL_TEXTURE0 + Semantic.Sampler.DIFFUSE);
        gl.glBindTexture(GL_TEXTURE_2D, 0);
        gl.glBindSampler(Semantic.Sampler.DIFFUSE, 0);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {

        MatrixStack persMatrix = new MatrixStack();
        persMatrix
                .translate(-1f, 1f, 0f)
                .scale(2f / w, -2f / h, 1f);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.PROJECTION));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, persMatrix.top().to(matBuffer));
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(noGammaProgram);
        gl.glDeleteProgram(gammaProgram);

        gl.glDeleteBuffers(Buffer.MAX, bufferName);
        gl.glDeleteVertexArrays(1, vao);
        gl.glDeleteTextures(Texture.MAX, textureName);
        gl.glDeleteSamplers(1, samplerName);

        destroyBuffers(bufferName, vao, textureName, samplerName, vertexData);
    }

    @Override
    public void keyPressed(KeyEvent ke) {

        switch (ke.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_1:
                useGammaCorrect[0] = !useGammaCorrect[0];
                if (useGammaCorrect[0])
                    System.out.println("Top:\tsRGB texture.");
                else
                    System.out.println("Top:\tlinear texture.");
                break;

            case KeyEvent.VK_2:
                useGammaCorrect[1] = !useGammaCorrect[1];
                if (useGammaCorrect[1])
                    System.out.println("Bottom:\tsRGB texture.");
                else
                    System.out.println("Bottom:\tlinear texture.");
                break;
        }
    }

    private class ProgramData {

        public int theProgram;
        public int modelToCameraMatrixUL;

        public ProgramData(GL3 gl, String vertex, String fragment) {

            theProgram = programOf(gl, getClass(), "tut16", vertex, fragment);

            modelToCameraMatrixUL = gl.glGetUniformLocation(theProgram, "modelToCameraMatrix");

            gl.glUniformBlockBinding(
                    theProgram,
                    gl.glGetUniformBlockIndex(theProgram, "Projection"),
                    Semantic.Uniform.PROJECTION);

            gl.glUseProgram(theProgram);
            gl.glUniform1i(
                    gl.glGetUniformLocation(theProgram, "colorTexture"),
                    Semantic.Sampler.DIFFUSE);
            gl.glUseProgram(0);
        }
    }
}