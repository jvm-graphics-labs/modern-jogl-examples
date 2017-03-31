/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import uno.caps.Caps;
import uno.debug.GlDebugOutput;
import glm.vec._2.Vec2i;

import java.nio.FloatBuffer;

import static uno.buffer.UtilKt.destroyBuffers;

/**
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

    public Caps caps;

    public Framework(String title) {
        setup(title);
    }

    public Framework() {
    }

    public void setup(String title) {

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
        window.setSize(windowSize.x, windowSize.y);

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

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                new Thread(new Runnable() {
                    public void run() {

                        //stop the animator thread when user close the window
                        animator.stop();
                        // This is actually redundant since the JVM will terminate when all threads are closed.
                        // It's useful just in case you create a thread and you forget to stop it.
                        System.exit(1);
                    }
                }).start();
            }
        });
    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        caps = new Caps(gl, Caps.Profile.CORE);

        init(gl);
    }

    protected void init(GL3 gl) {
    }

    @Override
    public final void display(GLAutoDrawable drawable) {
        display(drawable.getGL().getGL3());
    }

    protected void display(GL3 gl) {
    }

    @Override
    public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        reshape(drawable.getGL().getGL3(), width, height);
    }

    protected void reshape(GL3 gl, int width, int height) {
    }

    @Override
    public final void dispose(GLAutoDrawable drawable) {
        end(drawable.getGL().getGL3());
        destroyBuffers(clearColor, clearDepth, matBuffer, vecBuffer);
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

    /**
     * Note: calling System.exit() synchronously inside the draw, reshape or init callbacks can lead to deadlocks on
     * certain platforms (in particular, X11) because the JAWT's locking routines cause a global AWT lock to be grabbed.
     * Instead run the exit routine in another thread.
     */
    protected void quit() {
        new Thread(new Runnable() {
            public void run() {
                window.destroy();
            }
        }).start();
    }
}
