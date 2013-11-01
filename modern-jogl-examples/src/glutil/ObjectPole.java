/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

import glutil.ViewPole.RotatingMode;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Quat;
import jglm.Vec2;
import jglm.Vec3;
import jglm.Vec4;

/**
 *
 * @author gbarbieri
 */
public class ObjectPole {

    private ObjectData position;
    private ObjectData initialPosition;
    private float rotateScale;
    private boolean isDragging;
    private ViewPole.RotatingMode rotatingMode;
    private Vec2 prevMousePos;
    private Vec2 startDragMousePos;
    private Quat startDragOrient;
    private ViewPole viewPole;

    public ObjectPole(ObjectData initialData, float rotateScale, ViewPole viewPole) {

        this.position = initialData;
        this.initialPosition = initialData;
        this.rotateScale = rotateScale;
        this.viewPole = viewPole;

        isDragging = false;
    }

    public Mat4 calcMatrix() {

        Mat4 translateMat = new Mat4(1.0f);
        translateMat.c3 = new Vec4(position.getPosition(), 1.0f);

        return translateMat.mult(position.getOrientation().toMatrix());
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

    public void mouseReleased(MouseEvent mouseEvent) {

        if (isDragging) {

            if (SwingUtilities.isRightMouseButton(mouseEvent)) {

                mouseMove(mouseEvent);
                
                isDragging = false;
            }
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {

            Vec2 positionVec2 = new Vec2(mouseEvent.getX(), mouseEvent.getY());

            Vec2 diff = positionVec2.minus(prevMousePos);

            switch (rotatingMode) {

                case DUAL_AXIS:

                    Quat rotation = Jglm.angleAxis(diff.x * rotateScale, new Vec3(0.0f, 1.0f, 0.0f));

                    rotation = Jglm.angleAxis(diff.y * rotateScale, new Vec3(1.0f, 0.0f, 0.0f)).mult(rotation);

                    rotation.normalize();
                    
                    rotateViewDegrees(rotation);
            }
            
            prevMousePos = positionVec2;
        }
    }

    private void rotateViewDegrees(Quat rotation) {

        Quat viewQuat = viewPole.calcMatrix().toQuaternion();

        viewQuat.print("viewQuat");
        
        Quat invViewQuat = viewQuat.conjugate();

        Quat tmp = invViewQuat.mult(rotation);

        tmp = tmp.mult(viewQuat);

        position.setOrientation(tmp.mult(position.getOrientation()));
    }
}