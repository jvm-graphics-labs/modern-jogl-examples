/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package depthPeeling.depthpeelingGL2;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.GLBuffers;
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
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gbarbieri
 */
public class DepthPeelingGL2 implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {

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
//    private String filename = "C:\\Users\\gbarbieri\\Documents\\Models\\Frontlader5_3.stl";
    private String filename = "/depthPeeling/data/Frontlader5.stl";
//    private String filename = "C:\\Users\\gbarbieri\\Documents\\Models\\ATLAS_RADLADER.stl";
//    private String filename = "C:\\temp\\model.stl";
    private float[] min = new float[3];
    private float[] max = new float[3];
    private int[] drawnBuffers = new int[]{
        GL2.GL_COLOR_ATTACHMENT0,
        GL2.GL_COLOR_ATTACHMENT1,
        GL2.GL_COLOR_ATTACHMENT2,
        GL2.GL_COLOR_ATTACHMENT3,
        GL2.GL_COLOR_ATTACHMENT4,
        GL2.GL_COLOR_ATTACHMENT5,
        GL2.GL_COLOR_ATTACHMENT6};
    private int geoPassesNumber;
    private int passesNumber = 4;
    private GLSLProgramObject shaderInit;
    private GLSLProgramObject shaderPeel;
    private GLSLProgramObject shaderBlend;
    private GLSLProgramObject shaderFinal;
    private int[] queryId = new int[1];
    private float[] pos = new float[]{0.0f, 0.0f, 2.0f};
    private float[] rot = new float[]{0.0f, 0.0f};
    private float[] transl = new float[]{0.0f, 0.0f, 0.0f};
    private float scale = 1.0f;
    private float[] opacity = new float[]{0.1f};
    private int quadDisplayList;
    private float[] backgroundColor = new float[]{1.0f, 1.0f, 1.0f};
    private FloatBuffer primitiveData;
    private int[] primitiveDataBufferID = new int[1];
    private ArrayList<float[]> triangles;

    public DepthPeelingGL2() {
        initGL();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DepthPeelingGL2 depthPeeling = new DepthPeelingGL2();

        Frame frame = new Frame("Depth peeling");

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

        GL2 gl2 = glad.getGL().getGL2();
//        GL4 gl4 = glad.getGL().getGL4();

//        System.err.println("   GL_VERSION_3_0: "+gl2.isExtensionAvailable("GL_VERSION_3_0"));
//        System.err.println("   GL_VERSION_3_1: "+gl2.isExtensionAvailable("GL_VERSION_3_1"));
//        System.err.println("   GL_VERSION_3_2: "+gl2.isExtensionAvailable("GL_VERSION_3_2"));
//        System.err.println("   GL_VERSION_3_3: "+gl2.isExtensionAvailable("GL_VERSION_3_3"));
//        System.err.println("   GL_VERSION_4_0: "+gl2.isExtensionAvailable("GL_VERSION_4_0"));
        
        initDepthPeelingRenderTargets(gl2);
        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

        try {
            loadModel(gl2);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DepthPeelingGL2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(DepthPeelingGL2.class.getName()).log(Level.SEVERE, null, ex);
        }

        buildShaders(gl2);

        makeFullScreenQuad(gl2);
    }

    private void makeFullScreenQuad(GL2 gl2) {
        GLU glu = GLU.createGLU(gl2);

        quadDisplayList = gl2.glGenLists(1);
        gl2.glNewList(quadDisplayList, GL2.GL_COMPILE);

        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glPushMatrix();
        {
            gl2.glLoadIdentity();
            glu.gluOrtho2D(0.0f, 1.0f, 0.0f, 1.0f);

            gl2.glBegin(GL2.GL_QUADS);
            {
                gl2.glVertex2f(0.0f, 0.0f);
                gl2.glVertex2f(1.0f, 0.0f);
                gl2.glVertex2f(1.0f, 1.0f);
                gl2.glVertex2f(0.0f, 1.0f);
            }
            gl2.glEnd();
        }
        gl2.glPopMatrix();

        gl2.glEndList();
    }

    private void buildShaders(GL2 gl2) {
        System.out.println("buildShaders..");

        shaderInit = new GLSLProgramObject();
        shaderInit.attachVertexShader(gl2, "shade_vertex_shader.glsl");
        shaderInit.attachVertexShader(gl2, "depth_peeling_init_vertex_shader.glsl");
        shaderInit.attachFragmentShader(gl2, "shade_fragment_shader.glsl");
        shaderInit.attachFragmentShader(gl2, "depth_peeling_init_fragment_shader.glsl");
        shaderInit.link(gl2);

        shaderPeel = new GLSLProgramObject();
        shaderPeel.attachVertexShader(gl2, "shade_vertex_shader.glsl");
        shaderPeel.attachVertexShader(gl2, "depth_peeling_peel_vertex_shader.glsl");
        shaderPeel.attachFragmentShader(gl2, "shade_fragment_shader.glsl");
        shaderPeel.attachFragmentShader(gl2, "depth_peeling_peel_fragment_shader.glsl");
        shaderPeel.link(gl2);

        shaderBlend = new GLSLProgramObject();
        shaderBlend.attachVertexShader(gl2, "depth_peeling_blend_vertex_shader.glsl");
        shaderBlend.attachFragmentShader(gl2, "depth_peeling_blend_fragment_shader.glsl");
        shaderBlend.link(gl2);

        shaderFinal = new GLSLProgramObject();
        shaderFinal.attachVertexShader(gl2, "depth_peeling_final_vertex_shader.glsl");
        shaderFinal.attachFragmentShader(gl2, "depth_peeling_final_fragment_shader.glsl");
        shaderFinal.link(gl2);
    }

    private void loadModel(GL2 gl2) throws FileNotFoundException, IOException, URISyntaxException {
        System.out.println("loadModel..");
        triangles = new ArrayList<>();

//        FileReader fileReader = new FileReader(new File(filename));
        FileReader fileReader = new FileReader(new File(getClass().getResource(filename).toURI()));
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line = "";
        int pointer;
        String values[];
        float[] data = new float[12];
        pointer = 0;

        for (int i = 0; i < 3; i++) {
            min[i] = Float.MAX_VALUE;
            max[i] = Float.MIN_VALUE;
        }

        while ((line = bufferedReader.readLine()) != null) {

            line = line.trim().toLowerCase();

            // Read normals
            if (line.startsWith("facet")) {
                values = line.split(" ");
                for (int i = 2; i < values.length; i++) {
                    if (!values[i].isEmpty()) {
                        data[pointer + 9] = Float.parseFloat(values[i]);
                        pointer++;
                    }
                }
            }

            // Read points
            if (line.startsWith("vertex")) {
                values = line.split(" ");
                for (int i = 1; i < values.length; i++) {
                    if (!values[i].isEmpty()) {
                        data[pointer - 3] = Float.parseFloat(values[i]);
                        pointer++;
                    }
                }
            }

            if (pointer == 12) {
                for (int i = 0; i < 3; i++) {
                    min[i] = Math.min(data[i], min[i]);
                    min[i] = Math.min(data[i + 3], min[i]);
                    min[i] = Math.min(data[i + 6], min[i]);

                    max[i] = Math.max(data[i], max[i]);
                    max[i] = Math.max(data[i + 3], max[i]);
                    max[i] = Math.max(data[i + 6], max[i]);
                }
                pointer = 0;

                float t[] = new float[12];
                System.arraycopy(data, 0, t, 0, 12);
                triangles.add(t);
            }
        }

        bufferedReader.close();
        fileReader.close();

        createVBO(gl2, triangles);
    }

    public void createVBO(GL2 gl2, ArrayList<float[]> triangles) {

        float factor = 10000;

        /**
         * Interleaved primitive Vertex Buffer Object (vertex - normal)
         */
//        glError = gl2.glGetError();
        /**
         * Create the interleaved VBO Buffer capacity = triangle number * 3
         * vertexes per triangle * 3 coordinates per vertex * 2 interleaved
         * components (vertex and normal)
         */
        primitiveData = GLBuffers.newDirectFloatBuffer(triangles.size() * 3 * 3 * 2);

        //  Layout t[] = [x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz]
        //  Interleave vertex and normal data
        for (float[] t : triangles) {
            primitiveData.put(new float[]{t[0] / factor, t[1] / factor, t[2] / factor,
                t[9], t[10], t[11],
                t[3] / factor, t[4] / factor, t[5] / factor,
                t[9], t[10], t[11],
                t[6] / factor, t[7] / factor, t[8] / factor,
                t[9], t[10], t[11]});
        }

        //  Rewind
        primitiveData.flip();

        //  Generate the buffer  
        gl2.glGenBuffers(1, primitiveDataBufferID, 0);

        //  Bind the buffer
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, primitiveDataBufferID[0]);

        //  Fill the buffer, data amount in Bytes
        gl2.glBufferData(GL2.GL_ARRAY_BUFFER, triangles.size() * 3 * 3 * 2 * 4,
                primitiveData, GL2.GL_STATIC_DRAW);
        //  Clear the data
        primitiveData.clear();

        //  Bind back the default frame buffer
        gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private void initDepthPeelingRenderTargets(GL2 gl2) {

        depthTextureId = new int[2];
        colorTextureId = new int[2];
        fboId = new int[2];

        gl2.glGenTextures(2, depthTextureId, 0);
        gl2.glGenTextures(2, colorTextureId, 0);
        gl2.glGenFramebuffers(2, fboId, 0);

        for (int i = 0; i < 2; i++) {

            gl2.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, depthTextureId[i]);

            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);

            gl2.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_DEPTH_COMPONENT32F, imageWidth, imageHeight, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, null);


            gl2.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, colorTextureId[i]);

            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);

            gl2.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_RGBA, imageWidth, imageHeight, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);


            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[i]);

            gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_TEXTURE_RECTANGLE_ARB, depthTextureId[i], 0);
            gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_RECTANGLE_ARB, colorTextureId[i], 0);
        }

        colorBlenderTextureId = new int[1];

        gl2.glGenTextures(1, colorBlenderTextureId, 0);

        gl2.glBindTexture(GL2.GL_TEXTURE_RECTANGLE_ARB, colorBlenderTextureId[0]);

        gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl2.glTexParameteri(GL2.GL_TEXTURE_RECTANGLE_ARB, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);

        gl2.glTexImage2D(GL2.GL_TEXTURE_RECTANGLE_ARB, 0, GL2.GL_RGBA, imageWidth, imageHeight, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);

        colorBlenderFboId = new int[1];

        gl2.glGenFramebuffers(1, colorBlenderFboId, 0);

        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, colorBlenderFboId[0]);

        gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_RECTANGLE_ARB, colorBlenderTextureId[0], 0);
        gl2.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_TEXTURE_RECTANGLE_ARB, depthTextureId[0], 0);
    }

    private void deleteDepthPeelingRenderTargets(GL2 gl2) {
        gl2.glDeleteFramebuffers(2, fboId, 0);
        gl2.glDeleteFramebuffers(1, colorBlenderFboId, 0);

        gl2.glDeleteTextures(2, depthTextureId, 0);
        gl2.glDeleteTextures(2, colorTextureId, 0);
        gl2.glDeleteTextures(1, colorBlenderTextureId, 0);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        System.out.println("dispose");
    }

    @Override
    public void display(GLAutoDrawable glad) {
        System.out.println("display, passesNumber: "+passesNumber);

        GL2 gl2 = glad.getGL().getGL2();
        GLU glu = GLU.createGLU(gl2);

        geoPassesNumber = 0;

        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glLoadIdentity();
        glu.gluLookAt(pos[0], pos[1], pos[2], pos[0], pos[1], 0.0f, 0.0f, 1.0f, 0.0f);
        gl2.glRotatef(rot[0], 1.0f, 0.0f, 0.0f);
        gl2.glRotatef(rot[1], 0.0f, 1.0f, 0.0f);
        gl2.glTranslated(transl[0], transl[1], transl[2]);
        gl2.glScalef(scale, scale, scale);

        renderDepthPeeling(gl2);

//        gl2.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
//        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
//        gl2.glColor3f(0.5f, 0.5f, 0.5f);
//        drawModel(gl2);

        glad.swapBuffers();
    }

    private void renderDepthPeeling(GL2 gl2) {
        /**
         * (1) Initialize min depth buffer.
         */
        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, colorBlenderFboId[0]);
//        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl2.glDrawBuffer(drawnBuffers[0]);

        gl2.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl2.glEnable(GL2.GL_DEPTH_TEST);

        shaderInit.bind(gl2);
        shaderInit.setUniform(gl2, "Alpha", opacity, 1);
        {
            drawModel(gl2);
        }
        shaderInit.unbind(gl2);

        /**
         * (2) Depth peeling + blending.
         */
        int layersNumber = (passesNumber - 1) * 2;
//        System.out.println("layersNumber: " + layersNumber);
        for (int layer = 1; layer < layersNumber; layer++) {
            
            int currentId = layer % 2;
            int previousId = 1 - currentId;

            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId[currentId]);
//            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
            gl2.glDrawBuffer(drawnBuffers[0]);

            gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            gl2.glDisable(GL2.GL_BLEND);

            gl2.glEnable(GL2.GL_DEPTH_TEST);
            {
                shaderPeel.bind(gl2);
                shaderPeel.bindTextureRECT(gl2, "DepthTex", depthTextureId[previousId], 0);
                shaderPeel.setUniform(gl2, "Alpha", opacity, 1);
                {
                    drawModel(gl2);
                }
                shaderPeel.unbind(gl2);

                gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, colorBlenderFboId[0]);
                gl2.glDrawBuffer(drawnBuffers[0]);
            }
            gl2.glDisable(GL2.GL_DEPTH_TEST);

            gl2.glEnable(GL2.GL_BLEND);
            {
                gl2.glBlendEquation(GL2.GL_FUNC_ADD);
                gl2.glBlendFuncSeparate(GL2.GL_DST_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE_MINUS_SRC_ALPHA);

                shaderBlend.bind(gl2);
                shaderBlend.bindTextureRECT(gl2, "TempTex", colorTextureId[currentId], 0);
                {
                    gl2.glCallList(quadDisplayList);
                }
                shaderBlend.unbind(gl2);
            }
            gl2.glDisable(GL2.GL_BLEND);
        }

        /**
         * (3) Final pass.
         */
        gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        gl2.glDrawBuffer(GL2.GL_BACK);
        gl2.glDisable(GL2.GL_DEPTH_TEST);

        shaderFinal.bind(gl2);
        shaderFinal.setUniform(gl2, "BackgroundColor", backgroundColor, 3);
        shaderFinal.bindTextureRECT(gl2, "ColorTex", colorBlenderTextureId[0], 0);
        gl2.glCallList(quadDisplayList);
        shaderFinal.unbind(gl2);
    }

    private void drawModel(GL2 gl2) {
//        GLUT glut = new GLUT();
//
//        glut.glutSolidTeapot(0.5f);

        int vertexStride = 3 * 2 * 4;
        int vertexPointer = 0;
        int normalPointer = 3 * 4;
        
        //  Enable lighting
//        gl2.glEnable(GL2.GL_LIGHTING);
//        gl2.glEnable(GL2.GL_LIGHT0);

            //  Enable client-side vertex capability
            gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);

            //  Enable client-side normal capability
            gl2.glEnableClientState(GL2.GL_NORMAL_ARRAY);

            //  Select the primitive buffer
            gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, primitiveDataBufferID[0]);

            //  Specify the vertex layout format
            gl2.glVertexPointer(3, GL2.GL_FLOAT, vertexStride, vertexPointer);

            //  Specify the normal layout format
            gl2.glNormalPointer(GL2.GL_FLOAT, vertexStride, normalPointer);

            //  Render, passing the vertex number
            gl2.glDrawArrays(GL2.GL_TRIANGLES, 0, triangles.size() * 3);

            //  Disable client-side vertex capability
            gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

            //  Disable client-side normal capability
            gl2.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        
        //  Disable lighting
//        gl2.glDisable(GL2.GL_LIGHT0);
//        gl2.glDisable(GL2.GL_LIGHTING);

        geoPassesNumber++;
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        System.out.println("reshape");

        GL2 gl2 = glad.getGL().getGL2();

        if (imageWidth != width || imageHeight != height) {
            imageWidth = width;
            imageHeight = height;

            deleteDepthPeelingRenderTargets(gl2);
            initDepthPeelingRenderTargets(gl2);

            gl2.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        }

        gl2.glMatrixMode(GL2.GL_PROJECTION);

        gl2.glLoadIdentity();

        GLU glu = GLU.createGLU(gl2);

        glu.gluPerspective(FOVY, (float) imageWidth / (float) imageHeight, zNear, zFar);

        gl2.glMatrixMode(GL2.GL_MODELVIEW);

        gl2.glViewport(0, 0, imageWidth, imageHeight);
    }

    public GLCanvas getgLCanvas() {
        return gLCanvas;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_Y:
                passesNumber--;
                break;

            case KeyEvent.VK_X:
                passesNumber++;
                break;
        }
        
        gLCanvas.display();
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