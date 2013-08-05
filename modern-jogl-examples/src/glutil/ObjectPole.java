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
import jglm.Vec4;
import tut09.BasicLighting.RotatingMode;

/**
 *
 * @author gbarbieri
 */
public class ObjectPole {

    private ObjectData position;
    private ObjectData initialPosition;
    private float rotateScale;
    private boolean isDragging;
    private RotatingMode rotatingMode;
    private Vec2 prevMousePos;
    private Vec2 startDragMousePos;
    private Quat startDragOrient;

    public ObjectPole(ObjectData initialData, float rotateScale) {

        this.position = initialData;
        this.initialPosition = initialData;
        this.rotateScale = rotateScale;

        isDragging = false;
    }

    public Mat4 calcMatrix() {

        Mat4 translateMat = new Mat4(1.0f);
        translateMat.c3 = new Vec4(position.getPosition(), 1.0f);

        return translateMat.times(position.getOrientation().toMatrix());
    }

    public void mousePressed(MouseEvent mouseEvent) {

        if (!isDragging) {

            if (SwingUtilities.isRightMouseButton(mouseEvent)) {

                if (mouseEvent.isAltDown()) {

                    rotatingMode = RotatingMode.SPIN;

                } else if (mouseEvent.isControlDown()) {

                    rotatingMode = RotatingMode.BIAXIAL;

                } else {

                    rotatingMode = RotatingMode.DUAL_AXIS;
                }

                prevMousePos = new Vec2(mouseEvent.getX(), mouseEvent.getY());

                startDragMousePos = prevMousePos;

                startDragOrient = position.getOrientation();

                isDragging = true;
            }
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {

            Vec2 diff = new Vec2(mouseEvent.getX(), mouseEvent.getY());

            diff = diff.minus(startDragMousePos);

            switch (rotatingMode) {

                case DUAL_AXIS:

                    Quat rotation = Jglm.angleAxis(new Vec3(0.0f, 1.0f, 0.0f), diff.x * rotateScale);

                    rotation = Jglm.angleAxis(new Vec3(1.0f, 0.0f, 0.0f), diff.y * rotateScale).mult(rotation);
                    
                    rotation.normalize();
            }
        }
    }
    
//    private rotateViewDegrees(Quat rotation)
}