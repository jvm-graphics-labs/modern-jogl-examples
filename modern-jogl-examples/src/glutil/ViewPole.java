/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec2;
import jglm.Vec3;
import tut09.BasicLighting.RotatingMode;

/**
 *
 * @author gbarbieri
 */
public class ViewPole {

    private ViewData currView;
    private ViewData initialView;
    private ViewScale viewScale;
    private boolean isDragging;
    private RotatingMode rotatingMode;
    private Vec2 startDragMouseLoc;
    private float degStartDragSpin;
    private Quat startDragOrient;

    public ViewPole(ViewData viewData, ViewScale viewScale) {

        this.currView = viewData;
        this.initialView = viewData;
        this.viewScale = viewScale;

        isDragging = false;
    }

    public Mat4 calcMatrix() {

        Mat4 mat = new Mat4(1.0f);

        mat = Jglm.translate(mat, new Vec3(0.0f, 0.0f, -currView.getRadius()));

        Quat fullRotation = Jglm.angleAxis(new Vec3(0.0f, 0.0f, 1.0f), currView.getDegSpinRotation());
        fullRotation = fullRotation.mult(currView.getOrient());

        mat = mat.times(fullRotation.toMatrix());

        mat = Jglm.translate(mat, currView.getTargetPos().negated());

        return mat;
    }

    public void mousePressed(MouseEvent mouseEvent) {
        
        if (!isDragging) {
            
            if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                
                Vec2 position = new Vec2(mouseEvent.getX(), mouseEvent.getY());

                if (mouseEvent.isControlDown()) {

                    beginDragRotate(position, RotatingMode.BIAXIAL);

                } else if (mouseEvent.isAltDown()) {

                    beginDragRotate(position, RotatingMode.SPIN);

                } else {
                    
                    beginDragRotate(position, RotatingMode.DUAL_AXIS);
                }
            }
        }
    }

    private void beginDragRotate(Vec2 position, RotatingMode rotatingMode) {

        this.rotatingMode = rotatingMode;

        startDragMouseLoc = position;

        degStartDragSpin = currView.getDegSpinRotation();

        startDragOrient = currView.getOrient();

        isDragging = true;
    }

    public void mouseMove(MouseEvent mouseEvent) {
        
        if (isDragging) {
            
            onDragRotate(mouseEvent);
        }
    }

    private void onDragRotate(MouseEvent mouseEvent) {

        Vec2 current = new Vec2(mouseEvent.getX(), mouseEvent.getY());
        
        current = current.minus(startDragMouseLoc);

        switch (rotatingMode) {

            case DUAL_AXIS:
                processXYchange(current);
                break;
        }
    }

    private void processXYchange(Vec2 diff) {
        
        diff = diff.times(viewScale.getRotationScale());

        Quat yWorldSpace = Jglm.angleAxis(new Vec3(0.0f, 1.0f, 0.0f), diff.x);

        currView.setOrient(startDragOrient.mult(yWorldSpace));
        
        Quat xLocalSpace = Jglm.angleAxis(new Vec3(1.0f, 0.0f, 0.0f), diff.y);

        currView.setOrient(xLocalSpace.mult(currView.getOrient()));
    }

    public void mouseRelease(MouseEvent mouseEvent) {

        if (isDragging) {

            if (SwingUtilities.isLeftMouseButton(mouseEvent)) {

                if (rotatingMode == RotatingMode.DUAL_AXIS || rotatingMode == RotatingMode.BIAXIAL || rotatingMode == RotatingMode.SPIN) {

                    endDragRotate(mouseEvent);
                }
            }
        }
    }

    private void endDragRotate(MouseEvent mouseEvent) {
        
        onDragRotate(mouseEvent);
        
        isDragging = false;
    }
}