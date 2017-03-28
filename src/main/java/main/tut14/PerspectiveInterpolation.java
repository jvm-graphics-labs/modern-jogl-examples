
package main.tut14;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLRunnable;
import main.framework.Framework;
import main.framework.component.Mesh;
import org.xml.sax.SAXException;
import uno.glm.MatrixStack;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static uno.glsl.UtilKt.programOf;

/**
 * @author elect
 */
public class PerspectiveInterpolation extends Framework {

    public static void main(String[] args) {
        new PerspectiveInterpolation("Tutorial 14 - Perspective Interpolation");
    }

    private ProgramData smoothInterp, linearInterp;

    private Mesh realHallway, fauxHallway;

    private boolean useFakeHallway = false, useSmoothInterpolation = true;

    public PerspectiveInterpolation(String title) {
        super(title);
    }

    @Override
    public void init(GL3 gl) {

        initializePrograms(gl);

        try {
            realHallway = new Mesh(gl, getClass(), "tut14/RealHallway.xml");
            fauxHallway = new Mesh(gl, getClass(), "tut14/FauxHallway.xml");
        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
            Logger.getLogger(PerspectiveInterpolation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initializePrograms(GL3 gl) {

        smoothInterp = new ProgramData(gl, "smooth-vertex-colors");
        linearInterp = new ProgramData(gl, "no-correct-vertex-colors");

        float zNear = 1.0f, zFar = 1_000f;
        MatrixStack persMatrix = new MatrixStack();
        persMatrix.perspective(60.0f, 1.0f, zNear, zFar);

        gl.glUseProgram(smoothInterp.theProgram);
        gl.glUniformMatrix4fv(smoothInterp.cameraToClipMatrixUnif, 1, false, persMatrix.top().to(matBuffer));
        gl.glUseProgram(linearInterp.theProgram);
        gl.glUniformMatrix4fv(linearInterp.cameraToClipMatrixUnif, 1, false, matBuffer);
        gl.glUseProgram(0);
    }

    @Override
    public void display(GL3 gl) {

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));

        if (useSmoothInterpolation)
            gl.glUseProgram(smoothInterp.theProgram);
        else
            gl.glUseProgram(linearInterp.theProgram);

        if (useFakeHallway)
            fauxHallway.render(gl);
        else
            realHallway.render(gl);

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GL3 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_ESCAPE:
                quit();
                break;

            case KeyEvent.VK_S:
                useFakeHallway = !useFakeHallway;
                System.out.println(useFakeHallway ? "Fake Hallway." : "Real Hallway.");
                break;
            case KeyEvent.VK_P:
                useSmoothInterpolation = !useSmoothInterpolation;
                System.out.println(useSmoothInterpolation ? "Perspective correct interpolation." : "Just linear interpolation");
                break;
            case KeyEvent.VK_SPACE:
                window.invoke(false, new GLRunnable() {
                    @Override
                    public boolean run(GLAutoDrawable drawable) {

                        GL3 gl = drawable.getGL().getGL3();

                        realHallway.dispose(gl);
                        fauxHallway.dispose(gl);

                        try {
                            realHallway = new Mesh(gl, getClass(), "tut14/RealHallway.xml");
                            fauxHallway = new Mesh(gl, getClass(), "tut14/FauxHallway.xml");
                        } catch (ParserConfigurationException | SAXException | IOException | URISyntaxException ex) {
                            Logger.getLogger(PerspectiveInterpolation.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        return false;
                    }
                });
                break;
        }
    }

    @Override
    public void end(GL3 gl) {

        gl.glDeleteProgram(smoothInterp.theProgram);
        gl.glDeleteProgram(linearInterp.theProgram);

        fauxHallway.dispose(gl);
        realHallway.dispose(gl);
    }

    private class ProgramData {

        public int theProgram;

        public int cameraToClipMatrixUnif;

        public ProgramData(GL3 gl, String shader) {

            theProgram = programOf(gl, getClass(), "tut14", shader + ".vert", shader + ".frag");

            cameraToClipMatrixUnif = gl.glGetUniformLocation(theProgram, "cameraToClipMatrix");
        }
    }
}
