/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthPeelingGL3;

import com.jogamp.opengl.util.GLBuffers;
import glutil.ViewData;
import glutil.ViewPole;
import glutil.ViewScale;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec3;

/**
 *
 * @author gbarbieri
 */
public class DepthPeelingGL3 implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {

    private boolean depthPeelingMode = false;
    private int imageWidth = 1024;
    private int imageHeight = 768;
    private GLCanvas gLCanvas;
    private int[] depthTextureId;
    private int[] colorTextureId;
    private int[] fboId;
    private int[] colorBlenderTextureId;
    private int[] colorBlenderFboId;
    private float FOVY = 30.0f;
    private float zNear = 0.0001f;
    private float zFar = 10.0f;
    private int oldX, oldY, newX, newY;
    private boolean rotating = false;
    private boolean panning = false;
    private boolean scaling = false;
//    private float[] rot = new 
//    private String filename = "C:\\Users\\gbarbieri\\Documents\\Models\\Frontlader5.stl";
//    private String filename = "C:\\Users\\gbarbieri\\Documents\\Models\\ATLAS_RADLADER.stl";
    private String filename = "C:\\temp\\model.stl";
//    private float[] min = new float[3];
//    private float[] max = new float[3];
    private int[] drawnBuffers = new int[]{
        GL3.GL_COLOR_ATTACHMENT0,
        GL3.GL_COLOR_ATTACHMENT1,
        GL3.GL_COLOR_ATTACHMENT2,
        GL3.GL_COLOR_ATTACHMENT3,
        GL3.GL_COLOR_ATTACHMENT4,
        GL3.GL_COLOR_ATTACHMENT5,
        GL3.GL_COLOR_ATTACHMENT6};
    private int geoPassesNumber;
    private int passesNumber = 4;
    private Program dpInit;
    private Program dpPeel;
    private Program dpBlend;
    private Program dpFinal;
    private int[] queryId = new int[1];
    private float[] pos = new float[]{0.0f, 0.0f, 2.0f};
    private float[] rot = new float[]{0.0f, 0.0f};
    private float[] transl = new float[]{0.0f, 0.0f, 0.0f};
    private float scale = 1.0f;
    private float opacity = 0.6f;
    private int quadDisplayList;
    private float[] backgroundColor = new float[]{1.0f, 1.0f, 1.0f};
    private FloatBuffer primitiveData;
    private ArrayList<float[]> triangles;
    private int[] quadVBO;
    private int[] quadVAO;
    private int[] modelVBO;
    private int[] modelVAO;
    private ViewPole viewPole;
    private int[] mvpMatrixesUBO;
    private float[] modelVertexAttributes;

    public DepthPeelingGL3() {
        initGL();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DepthPeelingGL3 depthPeeling = new DepthPeelingGL3();

        Frame frame = new Frame("Depth peeling GL3");

        frame.add(depthPeeling.getgLCanvas());

        frame.setSize(depthPeeling.getgLCanvas().getWidth(), depthPeeling.getgLCanvas().getHeight());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private void initGL() {
        GLProfile gLProfile = GLProfile.getDefault();

        GLCapabilities gLCapabilities = new GLCapabilities(gLProfile);

        gLCanvas = new GLCanvas(gLCapabilities);

        gLCanvas.setSize(imageWidth, imageHeight);

        gLCanvas.addGLEventListener(this);
        gLCanvas.addKeyListener(this);
        gLCanvas.addMouseListener(this);
        gLCanvas.addMouseMotionListener(this);
    }

    @Override
    public void init(GLAutoDrawable glad) {
        System.out.println("init");

        gLCanvas.setAutoSwapBufferMode(false);

        GL3 gl3 = glad.getGL().getGL3();
        int projectionBlockBinding = 0;
//        GL4 gl4 = glad.getGL().getGL4();

//        System.err.println("   GL_VERSION_3_0: "+gl2.isExtensionAvailable("GL_VERSION_3_0"));
//        System.err.println("   GL_VERSION_3_1: "+gl2.isExtensionAvailable("GL_VERSION_3_1"));
//        System.err.println("   GL_VERSION_3_2: "+gl2.isExtensionAvailable("GL_VERSION_3_2"));
//        System.err.println("   GL_VERSION_3_3: "+gl2.isExtensionAvailable("GL_VERSION_3_3"));
//        System.err.println("   GL_VERSION_4_0: "+gl2.isExtensionAvailable("GL_VERSION_4_0"));

        ViewData initialViewData = new ViewData(new Vec3(0.0f, 0.5f, 0.0f), new Quat(0.0f, 0.0f, 0.0f, 1.0f), 50.0f, 0.0f);

        ViewScale viewScale = new ViewScale(3.0f, 20.0f, 1.5f, 0.5f, 0.0f, 0.0f, 90.0f / 250.0f);

        viewPole = new ViewPole(initialViewData, viewScale);
        
        initUBO(gl3, projectionBlockBinding);

        initDepthPeelingRenderTargets(gl3);
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);

//        try {
//            loadModel(gl3);
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(DepthPeelingGL3.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            Logger.getLogger(DepthPeelingGL3.class.getName()).log(Level.SEVERE, null, ex);
//        }

        readAsciiStl(gl3);

        buildShaders(gl3, projectionBlockBinding);

        initFullScreenQuad(gl3);
    }

    private void initUBO(GL3 gl3, int projectionBlockIndex) {

        mvpMatrixesUBO = new int[1];
        int size = 16 * 4;

        gl3.glGenBuffers(1, mvpMatrixesUBO, 0);
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, mvpMatrixesUBO[0]);
        {
            gl3.glBufferData(GL3.GL_UNIFORM_BUFFER, size * 2, null, GL3.GL_DYNAMIC_DRAW);

            gl3.glBindBufferRange(GL3.GL_UNIFORM_BUFFER, projectionBlockIndex, mvpMatrixesUBO[0], 0, size * 2);

            //  Modelview Matrix
            Mat4 modelviewMatrix = viewPole.calcMatrix();

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 16 * 4, size, GLBuffers.newDirectFloatBuffer(modelviewMatrix.toFloatArray()));

            //  Porjection Matrix
            Mat4 projectionMatrix = Jglm.perspective(FOVY, imageWidth / imageHeight, zNear, zFar);

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, size, GLBuffers.newDirectFloatBuffer(projectionMatrix.toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);
    }

    private void initFullScreenQuad(GL3 gl3) {

        initQuadVBO(gl3);

        initQuadVAO(gl3);
    }

    private void initQuadVAO(GL3 gl3) {

        quadVAO = new int[1];

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, quadVBO[0]);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(quadVAO));
        gl3.glBindVertexArray(quadVAO[0]);
        {
            gl3.glEnableVertexAttribArray(0);
            {
                gl3.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 0, 0);
            }
        }
        gl3.glBindVertexArray(0);
    }

    private void initQuadVBO(GL3 gl3) {

        float[] vertexAttributes = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f};

        quadVBO = new int[1];

        gl3.glGenBuffers(1, IntBuffer.wrap(quadVBO));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, quadVBO[0]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexAttributes);

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, vertexAttributes.length * 4, buffer, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void buildShaders(GL3 gl3, int projectionBlockIndex) {
        System.out.print("buildShaders... ");

        String shadersFilepath = "/depthPeeling/depthPeelingGL3/shaders/";

        dpInit = new Program(gl3, shadersFilepath, new String[]{"shade_VS.glsl", "dpInit_VS.glsl"},
                new String[]{"shade_FS.glsl", "dpInit_FS.glsl"}, projectionBlockIndex);

//        dpPeel = new Program(gl3, shadersFilepath, new String[]{"shade_VS.glsl", "dpPeel_VS.glsl"},
//                new String[]{"shade_FS.glsl", "dpPeel_FS.glsl"}, projectionBlockIndex);
//
//        dpBlend = new Program(gl3, shadersFilepath, "dpBlend_VS.glsl", "dpBlend_FS.glsl", projectionBlockIndex);
//
//        dpFinal = new Program(gl3, shadersFilepath, "dpFinal_VS.glsl", "dpFinal_FS.glsl", projectionBlockIndex);
        
        System.out.println("ok");
    }

    private void readAsciiStl(GL3 gl3) {
        try {
            FileReader fr;
            int vertexLocal = 0;
            int attributesGlobal = 0;

            fr = new FileReader(new File(this.filename));
            BufferedReader br = new BufferedReader(fr);

            String line = "";
            String values[];
            float[] data = new float[3 * 3 * 2];
            float[] vertex = new float[9];
            float[] normal = new float[3];

            // Count triangles
            int triangles = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("facet")) {
                    triangles++;
                }
            }
            System.out.println("triangles: " + triangles);
            //  3 Vertexes, 3 coordinates, 2 attributes
            modelVertexAttributes = new float[triangles * 3 * 3 * 2];
            br.close();
            fr.close();

            fr = new FileReader(new File(this.filename));
            br = new BufferedReader(fr);
            line = "";
            //            int triangles_read = 0;

            while ((line = br.readLine()) != null) {

                line = line.trim().toLowerCase();

                // Read normals
                if (line.startsWith("facet")) {

                    int normalLocal = 0;
                    values = line.split(" ");

                    for (int i = 2; i < values.length; i++) {

                        if (!values[i].isEmpty()) {

                            normal[normalLocal] = Float.parseFloat(values[i]);
                            normalLocal++;
                        }
                    }
                }

                // Read points
                if (line.startsWith("vertex")) {

                    values = line.split(" ");

                    for (int i = 1; i < values.length; i++) {
                        if (!values[i].isEmpty()) {
                            vertex[vertexLocal] = Float.parseFloat(values[i]);
                            vertexLocal++;
                        }
                    }
                }

                if (vertexLocal == 9) {
                    //  Fill vertex and normals interleaved
                    for (int i = 0; i < 3; i++) {

                        data[i * 6] = vertex[i * 3];
                        data[i * 6 + 1] = vertex[i * 3 + 1];
                        data[i * 6 + 2] = vertex[i * 3 + 2];
                        data[i * 6 + 3] = normal[0];
                        data[i * 6 + 4] = normal[1];
                        data[i * 6 + 5] = normal[2];
                    }

                    //                    System.out.print("data[" + attributesGlobal + "] ");
                    //                    for (int i = 0; i < data.length; i++) {
                    //                        System.out.print(data[i] + " ");
                    //                    }
                    //                    System.out.println("");

                    System.arraycopy(data, 0, modelVertexAttributes, attributesGlobal * 3 * 3 * 2, data.length);

                    vertexLocal = 0;
                    attributesGlobal++;
                }
            }

            br.close();
            fr.close();


            System.out.println("Done, number of triangles: " + modelVertexAttributes.length / 18);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DepthPeelingGL3.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DepthPeelingGL3.class.getName()).log(Level.SEVERE, null, ex);
        }

        initModelVBO(gl3);

        initModelVAO(gl3);
    }

    private void initModelVAO(GL3 gl3) {

        modelVAO = new int[1];

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, modelVBO[0]);

        gl3.glGenVertexArrays(1, IntBuffer.wrap(modelVAO));
        gl3.glBindVertexArray(modelVAO[0]);
        {
            gl3.glEnableVertexAttribArray(0);
            {
                gl3.glVertexAttribPointer(0, 3, GL3.GL_FLOAT, false, 0, 0);
            }
        }
        gl3.glBindVertexArray(0);
    }

    private void initModelVBO(GL3 gl3) {

        modelVBO = new int[1];

        gl3.glGenBuffers(1, IntBuffer.wrap(modelVBO));

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, modelVBO[0]);
        {
            FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(modelVertexAttributes);

            gl3.glBufferData(GL3.GL_ARRAY_BUFFER, modelVertexAttributes.length * 4, buffer, GL3.GL_STATIC_DRAW);
        }
        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
    }

    private void initDepthPeelingRenderTargets(GL3 gl3) {

        depthTextureId = new int[2];
        colorTextureId = new int[2];
        fboId = new int[2];

        gl3.glGenTextures(2, depthTextureId, 0);
        gl3.glGenTextures(2, colorTextureId, 0);
        gl3.glGenFramebuffers(2, fboId, 0);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTextureId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_DEPTH_COMPONENT32F, imageWidth, imageHeight, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);


            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTextureId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA, imageWidth, imageHeight, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);


            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[i]);

            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_TEXTURE_RECTANGLE, depthTextureId[i], 0);
            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_RECTANGLE, colorTextureId[i], 0);
        }

        colorBlenderTextureId = new int[1];

        gl3.glGenTextures(1, colorBlenderTextureId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTextureId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA, imageWidth, imageHeight, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        colorBlenderFboId = new int[1];

        gl3.glGenFramebuffers(1, colorBlenderFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);

        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_RECTANGLE, colorBlenderTextureId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_TEXTURE_RECTANGLE, depthTextureId[0], 0);
    }

    private void deleteDepthPeelingRenderTargets(GL3 gl3) {
        gl3.glDeleteFramebuffers(2, fboId, 0);
        gl3.glDeleteFramebuffers(1, colorBlenderFboId, 0);

        gl3.glDeleteTextures(2, depthTextureId, 0);
        gl3.glDeleteTextures(2, colorTextureId, 0);
        gl3.glDeleteTextures(1, colorBlenderTextureId, 0);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
//        System.out.println("display");

        GL3 gl3 = glad.getGL().getGL3();

        geoPassesNumber = 0;

//        gl3.glMatrixMode(GL2.GL_MODELVIEW);
//        gl3.glLoadIdentity();
//        glu.gluLookAt(pos[0], pos[1], pos[2], pos[0], pos[1], 0.0f, 0.0f, 1.0f, 0.0f);
//        gl3.glRotatef(rot[0], 1.0f, 0.0f, 0.0f);
//        gl3.glRotatef(rot[1], 0.0f, 1.0f, 0.0f);
//        gl3.glTranslated(transl[0], transl[1], transl[2]);
//        gl3.glScalef(scale, scale, scale);

        renderDepthPeeling(gl3);

//        gl2.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
//        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
//        gl2.glColor3f(0.5f, 0.5f, 0.5f);
//        drawModel(gl2);

        glad.swapBuffers();
    }

    private void renderDepthPeeling(GL3 gl3) {
        /**
         * (1) Initialize min depth buffer.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glDrawBuffer(drawnBuffers[0]);

        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl3.glEnable(GL3.GL_DEPTH_TEST);

        dpInit.bind(gl3);
        {
            gl3.glUniform1f(dpInit.getAlphaUnLoc(), opacity);
            
            drawModel(gl3);
        }
        dpInit.unbind(gl3);

//        /**
//         * (2) Depth peeling + blending.
//         */
//        int layersNumber = (passesNumber - 1) * 2;
////        System.out.println("layersNumber: " + layersNumber);
//        for (int layer = 1; layer < layersNumber; layer++) {
//            int currentId = layer % 2;
//            int previousId = 1 - currentId;
//
//            gl3.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[currentId]);
//            gl3.glDrawBuffer(drawnBuffers[0]);
//
//            gl3.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//            gl3.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
//
//            gl3.glDisable(GL2.GL_BLEND);
//
//            gl3.glEnable(GL2.GL_DEPTH_TEST);
//            {
//                dpPeel.bind(gl3);
//                dpPeel.bindTextureRECT(gl3, "DepthTex", depthTextureId[previousId], 0);
//                dpPeel.setUniform(gl3, "Alpha", opacity, 1);
//                {
//                    drawModel(gl3);
//                }
//                dpPeel.unbind(gl3);
//
//                gl3.glBindFramebuffer(GL2.GL_FRAMEBUFFER, colorBlenderFboId[0]);
//                gl3.glDrawBuffer(drawnBuffers[0]);
//            }
//            gl3.glDisable(GL2.GL_DEPTH_TEST);
//
//            gl3.glEnable(GL2.GL_BLEND);
//            {
//                gl3.glBlendEquation(GL2.GL_FUNC_ADD);
//                gl3.glBlendFuncSeparate(GL2.GL_DST_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE_MINUS_SRC_ALPHA);
//
//                dpBlend.bind(gl3);
//                dpBlend.bindTextureRECT(gl3, "TempTex", colorTextureId[currentId], 0);
//                {
//                    gl3.glCallList(quadDisplayList);
//                }
//                dpBlend.unbind(gl3);
//            }
//            gl3.glDisable(GL2.GL_BLEND);
//        }
//
//        /**
//         * (3) Final pass.
//         */
//        gl3.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
//        gl3.glDrawBuffer(GL2.GL_BACK);
//        gl3.glDisable(GL2.GL_DEPTH_TEST);
//
//        dpFinal.bind(gl3);
//        dpFinal.setUniform(gl3, "BackgroundColor", backgroundColor, 3);
//        dpFinal.bindTextureRECT(gl3, "ColorTex", colorBlenderTextureId[0], 0);
//        gl3.glCallList(quadDisplayList);
//        dpFinal.unbind(gl3);
    }

    private void drawModel(GL3 gl3) {

        gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, modelVBO[0]);

        gl3.glBindVertexArray(modelVAO[0]);
        {
            //  Render, passing the vertex number
            gl3.glDrawArrays(GL3.GL_TRIANGLES, 0, modelVertexAttributes.length / 18);
        }
        geoPassesNumber++;
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL3 gl3 = glad.getGL().getGL3();

        if (imageWidth != width || imageHeight != height) {
            imageWidth = width;
            imageHeight = height;

            deleteDepthPeelingRenderTargets(gl3);
            initDepthPeelingRenderTargets(gl3);

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        }

        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, mvpMatrixesUBO[0]);
        {
            //  Projection Matrix
            Mat4 projectionMatrix = Jglm.perspective(FOVY, imageWidth / imageHeight, zNear, zFar);

            gl3.glBufferSubData(GL3.GL_UNIFORM_BUFFER, 0, 16 * 4, GLBuffers.newDirectFloatBuffer(projectionMatrix.toFloatArray()));
        }
        gl3.glBindBuffer(GL3.GL_UNIFORM_BUFFER, 0);

        gl3.glViewport(0, 0, imageWidth, imageHeight);
    }

    public GLCanvas getgLCanvas() {
        return gLCanvas;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        newX = e.getX();
        newY = e.getY();

        scaling = false;
        panning = false;
        rotating = false;

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.isShiftDown()) {
                scaling = true;
            } else if (e.isControlDown()) {
                panning = true;
            } else {
                rotating = true;
            }
        }

        gLCanvas.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        oldX = newX;
        oldY = newY;
        newX = e.getX();
        newY = e.getY();

        float rel_x = (newX - oldX) / (float) imageWidth;
        float rel_y = (newY - oldY) / (float) imageHeight;
        if (rotating) {
            rot[1] += (rel_x * 180);
            rot[0] += (rel_y * 180);
        } else if (panning) {
            pos[0] -= rel_x;
            pos[1] += rel_y;
        } else if (scaling) {
            pos[2] -= rel_y * pos[2];
        }

        gLCanvas.display();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}