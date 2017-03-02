/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework;

import buffer.BufferUtils;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import debug.GlDebugOutput;
import vec._2.Vec2i;

import java.nio.FloatBuffer;

/**
 *
 * @author elect
 */
public class Framework implements GLEventListener, KeyListener, MouseListener {

    private final boolean DEBUG = false;
    protected GLWindow window;
    protected Animator animator;
    protected Vec2i windowSize = new Vec2i(500);
    protected FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4),
            clearDepth = GLBuffers.newDirectFloatBuffer(1);
    public static FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16),
            vecBuffer = GLBuffers.newDirectFloatBuffer(4);

    public Framework(String title) {
        initGL(title);
    }

    private void initGL(String title) {

//        Display display = NewtFactory.createDisplay(null);
//        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

//        window = GLWindow.create(screen, glCapabilities);
        window = GLWindow.create(glCapabilities);

        if (DEBUG) {
            window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        }

        window.setUndecorated(false);
        window.setAlwaysOnTop(false);
        window.setFullscreen(false);
        window.setPointerVisible(true);
        window.confinePointer(false);
        window.setTitle(title);
        window.setSize(windowSize.x(), windowSize.y());

        window.setVisible(true);

        if (DEBUG) {
            window.getContext().addGLDebugListener(new GlDebugOutput());
        }

        window.addGLEventListener(this);
        window.addKeyListener(this);
        window.addMouseListener(this);

        animator = new Animator();
        animator.add(window);
        animator.start();
    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        init(gl3);

    }

    protected void init(GL3 gl) {

    }

    @Override
    public final void display(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        display(gl3);
    }

    protected void display(GL3 gl) {

    }

    @Override
    public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3 gl3 = drawable.getGL().getGL3();

        reshape(gl3, width, height);
    }

    protected void reshape(GL3 gl, int width, int height) {

    }

    @Override
    public final void dispose(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        end(gl3);

        BufferUtils.destroyDirectBuffer(clearColor);
        BufferUtils.destroyDirectBuffer(clearDepth);
        BufferUtils.destroyDirectBuffer(matBuffer);
        BufferUtils.destroyDirectBuffer(vecBuffer);

        System.exit(1);
    }

    protected void end(GL3 gl) {

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
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
    }

}
