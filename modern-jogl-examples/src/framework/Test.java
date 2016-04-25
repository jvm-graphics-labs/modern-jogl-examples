/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import glm.vec._2.i.Vec2i;

/**
 *
 * @author elect
 */
public class Test implements GLEventListener, KeyListener {

    private final boolean DEBUG = true;
    private GLWindow glWindow;
    private Animator animator;
    private Vec2i windowSize = new Vec2i(500);

    public Test(String title) {
        initGL(title);
    }

    private void initGL(String title) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        glWindow = GLWindow.create(screen, glCapabilities);

        if (DEBUG) {
//            glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
            System.out.println("" + glWindow.getContextCreationFlags());
        }

        assert glWindow != null;

        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle(title);
        glWindow.setSize(windowSize.x, windowSize.y);

        glWindow.setVisible(true);

        if (DEBUG) {
//            glWindow.getContext().addGLDebugListener(new GlDebugOutput());
//            glWindow.getContext().makeCurrent();
//            glWindow.getContext().enableGLDebugMessage(false);
        }

        glWindow.addGLEventListener(this);
        glWindow.addKeyListener(this);

        animator = new Animator();
        animator.add(glWindow);
        animator.setRunAsFastAsPossible(false);
        animator.setExclusiveContext(true);
        animator.start();
    }

    @Override
    public final void init(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        init(gl3);

    }

    protected void init(GL3 gl3) {

    }

    @Override
    public final void display(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        display(gl3);
    }

    protected void display(GL3 gl3) {

    }

    @Override
    public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3 gl3 = drawable.getGL().getGL3();

        reshape(gl3, width, height);
    }

    protected void reshape(GL3 gl3, int width, int height) {

    }

    @Override
    public final void dispose(GLAutoDrawable drawable) {

        GL3 gl3 = drawable.getGL().getGL3();

        end(gl3);

        animator.stop();
        System.exit(0);
    }

    protected void end(GL3 gl3) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                animator.stop();
                glWindow.destroy();
                break;
        }

        keyboard(e);
    }

    protected void keyboard(KeyEvent keyEvent) {

    }

}
