///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package main.tut14.perspectiveInterpolation;
//
//import com.jogamp.newt.event.KeyEvent;
//import static com.jogamp.opengl.GL2ES3.GL_COLOR;
//import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
//import com.jogamp.opengl.GL3;
//import main.framework.Framework;
//import main.framework.component.Mesh;
//import glutil.MatrixStack;
//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.parsers.ParserConfigurationException;
//import org.xml.sax.SAXException;
//
///**
// *
// * @author elect
// */
//public class PerspectiveInterpolation extends Framework {
//
//    private final String SHADERS_ROOT = "/tut14/perspectiveInterpolation/shaders", MESHES_ROOT = "/tut14/data/",
//            SMOOTH_VERTEX_COLORS_SHADER_SRC = "smooth-vertex-colors",
//            NO_CORRECT_VERTEX_COLORS_SHADER_SRC = "no-correct-vertex-colors",
//            REAL_HALLWAY_MESH_SRC = "RealHallway.xml", FAUX_HALLWAY_MESH_SRC = "FauxHallway.xml";
//
//    public static void main(String[] args) {
//        new PerspectiveInterpolation("Tutorial 14 - Perspective Interpolation");
//    }
//
//    private ProgramData smoothInterp, linearInterp;
//
//    private Mesh realHallway, fauxHallway;
//
//    private boolean useFakeHallway = false, useSmoothInterpolation = true, reload = false;
//
//    public PerspectiveInterpolation(String title) {
//        super(title);
//    }
//
//    @Override
//    public void init(GL3 gl3) {
//
//        initializePrograms(gl3);
//
//        try {
//            realHallway = new Mesh(MESHES_ROOT + REAL_HALLWAY_MESH_SRC, gl3);
//            fauxHallway = new Mesh(MESHES_ROOT + FAUX_HALLWAY_MESH_SRC, gl3);
//        } catch (ParserConfigurationException | SAXException | IOException ex) {
//            Logger.getLogger(PerspectiveInterpolation.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    private void initializePrograms(GL3 gl3) {
//
//        smoothInterp = new ProgramData(gl3, SHADERS_ROOT, SMOOTH_VERTEX_COLORS_SHADER_SRC);
//        linearInterp = new ProgramData(gl3, SHADERS_ROOT, NO_CORRECT_VERTEX_COLORS_SHADER_SRC);
//
//        float zNear = 1.0f, zFar = 1_000f;
//        MatrixStack persMatrix = new MatrixStack();
//        persMatrix.perspective(60.0f, 1.0f, zNear, zFar);
//
//        gl3.glUseProgram(smoothInterp.theProgram);
//        gl3.glUniformMatrix4fv(smoothInterp.cameraToClipMatrixUnif, 1, false, persMatrix.top().toDfb(matBuffer));
//        gl3.glUseProgram(linearInterp.theProgram);
//        gl3.glUniformMatrix4fv(linearInterp.cameraToClipMatrixUnif, 1, false, persMatrix.top().toDfb(matBuffer));
//        gl3.glUseProgram(0);
//    }
//
//    @Override
//    public void display(GL3 gl3) {
//
//        if (reload) {
//
//            reload = false;
//
//            realHallway.dispose(gl3);
//            fauxHallway.dispose(gl3);
//
//            try {
//                realHallway = new Mesh(MESHES_ROOT + REAL_HALLWAY_MESH_SRC, gl3);
//                fauxHallway = new Mesh(MESHES_ROOT + FAUX_HALLWAY_MESH_SRC, gl3);
//            } catch (ParserConfigurationException | SAXException | IOException ex) {
//                Logger.getLogger(PerspectiveInterpolation.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0.0f).put(1, 0.0f).put(2, 0.0f).put(3, 0.0f));
//        gl3.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1.0f));
//
//        if (useSmoothInterpolation) {
//            gl3.glUseProgram(smoothInterp.theProgram);
//        } else {
//            gl3.glUseProgram(linearInterp.theProgram);
//        }
//
//        if (useFakeHallway) {
//            fauxHallway.render(gl3);
//        } else {
//            realHallway.render(gl3);
//        }
//
//        gl3.glUseProgram(0);
//    }
//
//    @Override
//    public void reshape(GL3 gl3, int w, int h) {
//        gl3.glViewport(0, 0, w, h);
//    }
//
//    @Override
//    public void keyPressed(KeyEvent e) {
//
//        switch (e.getKeyCode()) {
//
//            case KeyEvent.VK_ESCAPE:
//                animator.remove(glWindow);
//                glWindow.destroy();
//                break;
//
//            case KeyEvent.VK_S:
//                useFakeHallway = !useFakeHallway;
//                System.out.println(useFakeHallway ? "Fake Hallway." : "Real Hallway.");
//                break;
//            case KeyEvent.VK_P:
//                useSmoothInterpolation = !useSmoothInterpolation;
//                System.out.println(
//                        useSmoothInterpolation ? "Perspective correct interpolation." : "Just linear interpolation");
//                break;
//            case KeyEvent.VK_SPACE:
//                reload = true;
//                break;
//        }
//    }
//
//    @Override
//    public void end(GL3 gl3) {
//
//        gl3.glDeleteProgram(smoothInterp.theProgram);
//        gl3.glDeleteProgram(linearInterp.theProgram);
//
//        fauxHallway.dispose(gl3);
//        realHallway.dispose(gl3);
//    }
//}
