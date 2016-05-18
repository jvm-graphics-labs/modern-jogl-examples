/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import com.jogamp.newt.event.MouseEvent;
import glm.glm;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import glm.vec._4.Vec4;

/**
 *
 * Mouse-based control over the orientation and position of an object.
 *
 * This Pole speaks of three spaces: local, world, and view. Local refers to the
 * coordinate system of vertices given to the matrix that this Pole generates.
 * World represents the \em output coordinate system. So vertices enter in local
 * and are transformed to world. Note that this does not have to actually be the
 * real world-space. It could be the space of some parent hierarchy, but the
 * ObjectPole is not really designed for that.
 *
 * View represents the space provided by the ViewProvider given to the
 * constructor. The assumption that this Pole makes when using the view space
 * matrix is that the matrix the ObjectPole generates will be multiplied by the
 * view matrix given by the ViewProvider. So it is assumed that there is no
 * intermediate space.
 *
 * This Pole is given an action button, which it will listen for click events
 * from. When the action button is held down and the mouse moved, the object's
 * orientation will change, relative to the orientation of the view. But only if
 * a ViewProvider (such as ViewPole) was provided; otherwise, the rotation will
 * be relative to the world.
 *
 * If no modifier keys (shift, ctrl, alt) were held when the click was given,
 * then the object will be oriented in both the view-space X and Y axes. If the
 * CTRL key is held when the click was given, then the object will only rotate
 * around either the X or Y axis. The selection is based on whether the X or the
 * Y mouse coordinate is farthest from the initial position when dragging
 * started. If the ALT key is held, then the object will rotate about the Z
 * axis, and only the X position of the mouse affects the object.
 *
 * @author gbarbieri
 */
public class ObjectPole {

    ViewProvider view;
    ObjectData po;
    ObjectData initialPo;

    float rotateScale;
    int actionButton;

    //Used when rotating.
    int rotateMode;
    boolean isDragging;

    private Vec2 prevMousePos;
    private Vec2 startDragMousePos;
    private Quat startDragOrient;

    public ObjectPole(ObjectData initialData, float rotateScale, int actionButton, ViewProvider lookAtProvider) {

        view = lookAtProvider;
        po = initialData;
        initialPo = initialData;
        this.rotateScale = rotateScale;
        this.actionButton = actionButton;
        isDragging = false;
    }

    public Mat4 calcMatrix() {

        Mat4 translateMat = new Mat4(1.0f);
        translateMat.c3(new Vec4(po.position, 1.0f));

        return translateMat.mul(Mat4.cast_(po.orientation));
    }

    public void setRotationScale(float rotateScale) {
        this.rotateScale = rotateScale;
    }

    public void reset() {
        if (!isDragging) {
            po = initialPo;
        }
    }

    private Quat calcRotationQuat(int axis, float degAngle) {
        return glm.angleAxis_(degAngle, axisVectors[axis]);
    }

    private Vec3[] axisVectors = {
        new Vec3(1.0, 0.0, 0.0),
        new Vec3(0.0, 1.0, 0.0),
        new Vec3(0.0, 0.0, 1.0)};

    public void rotateWorldDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        po.orientation = rot.mul_(fromInitial ? startDragOrient : po.orientation).normalize();
    }

    public void rotateLocalDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        po.orientation = (fromInitial ? startDragOrient : po.orientation).mul_(rot).normalize();
    }

    public void rotateViewDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        if (view != null) {
            Quat viewQuat = Quat.cast_(view.calcMatrix());
            Quat invViewQuat = viewQuat.conjugate_();
            po.orientation = (invViewQuat.mul(rot).mul(viewQuat).mul(fromInitial ? startDragOrient : po.orientation))
                    .normalize();
        } else {
            rotateWorldDegrees(rot, fromInitial);
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {

        if (!isDragging) {

            if (SwingUtilities.isRightMouseButton(mouseEvent)) {

                if (mouseEvent.isAltDown()) {

                    rotateMode = RotatingMode.SPIN;

                } else if (mouseEvent.isControlDown()) {

                    rotateMode = RotatingMode.BIAXIAL;

                } else {

                    rotateMode = RotatingMode.DUAL_AXIS;
                }

                prevMousePos = new Vec2(mouseEvent.getX(), mouseEvent.getY());

                startDragMousePos = prevMousePos;

                startDragOrient = po.getOrientation();

                isDragging = true;
            }
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {

        if (isDragging) {

            if (mouseEvent.getButton() == MouseEvent.BUTTON3) {

                mouseMove(mouseEvent);

                isDragging = false;
            }
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {

            Vec2 positionVec2 = new Vec2(mouseEvent.getX(), mouseEvent.getY());

            Vec2 diff = positionVec2.minus(prevMousePos);

            switch (rotateMode) {

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

//        viewQuat.print("viewQuat");
        Quat invViewQuat = viewQuat.conjugate();

        Quat tmp = invViewQuat.mult(rotation);

        tmp = tmp.mult(viewQuat);

        po.setOrientation(tmp.mult(po.getOrientation()));
    }
}
