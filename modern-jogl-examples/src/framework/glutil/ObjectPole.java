/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import com.jogamp.newt.event.MouseEvent;
import glm.glm;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import glm.vec._2.i.Vec2i;
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

    private ViewProvider view;
    private ObjectData po;
    private ObjectData initialPo;

    private float rotateScale;
    private int actionButton;

    //Used when rotating.
    private int rotateMode;
    private boolean isDragging;

    private Vec2i prevMousePos;
    private Vec2i startDragMousePos;
    private Quat startDragOrient;

    /**
     * Creates an object pole with a given initial position and orientation.
     *
     * @param initialData The starting position and orientation of the object in
     * world space.
     * @param rotateScale The number of degrees to rotate the object per window
     * space pixel
     * @param actionButton The mouse button to listen for. All other mouse
     * buttons are ignored.
     * @param lookAtProvider An object that will compute a view matrix. This
     * defines the view space that orientations can be relative to. If it is
     * NULL, then orientations will be relative to the world.
     */
    public ObjectPole(ObjectData initialData, float rotateScale, int actionButton, ViewProvider lookAtProvider) {

        view = lookAtProvider;
        po = initialData;
        initialPo = initialData;
        this.rotateScale = rotateScale;
        this.actionButton = actionButton;
        isDragging = false;
        prevMousePos = new Vec2i();
        startDragMousePos = new Vec2i();
        startDragOrient = new Quat();
    }

    /**
     * Generates the local-to-world matrix for this object.
     *
     * @return
     */
    public Mat4 calcMatrix() {

        Mat4 translateMat = new Mat4(1.0f);
        translateMat.c3(new Vec4(po.position(), 1.0f));

        return translateMat.mul(Mat4.cast_(po.orientation()));
    }

    /**
     * Sets the scaling factor for orientation changes.
     *
     * @param rotateScale The scaling factor is the number of degrees to rotate
     * the object per window space pixel. The scale is the same for all mouse
     * movements.
     */
    public void setRotationScale(float rotateScale) {
        this.rotateScale = rotateScale;
    }

    /**
     * @return The current scaling factor for orientation changes.
     */
    public float getRotationScale() {
        return rotateScale;
    }

    /**
     * @return The current position and orientation of the object.
     */
    public ObjectData getPosOrient() {
        return po;
    }

    /**
     * Resets the object to the initial position/orientation. Will fail if
     * currently dragging.
     */
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

    public void rotateWorldDegrees(Quat rot) {
        rotateViewDegrees(rot, false);
    }

    public void rotateWorldDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        po.orientation(rot.mul_(fromInitial ? startDragOrient : po.orientation()).normalize());
    }

    public void rotateLocalDegrees(Quat rot) {
        rotateLocalDegrees(rot, false);
    }

    public void rotateLocalDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        po.orientation((fromInitial ? startDragOrient : po.orientation()).mul_(rot).normalize());
    }

    public void rotateViewDegrees(Quat rot) {
        rotateViewDegrees(rot, false);
    }

    public void rotateViewDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        if (view != null) {
            Quat viewQuat = Quat.cast_(view.calcMatrix());
            Quat invViewQuat = viewQuat.conjugate_();
            po.orientation((invViewQuat.mul(rot).mul(viewQuat).mul(fromInitial ? startDragOrient : po.orientation()))
                    .normalize());
        } else {
            rotateWorldDegrees(rot, fromInitial);
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {

            Vec2i position = new Vec2i(mouseEvent.getX(), mouseEvent.getY());

            Vec2i diff = position.sub_(prevMousePos);

            switch (rotateMode) {

                case RotateMode.DUAL_AXIS:
                    System.out.println("rotateScale " + rotateScale);
                    Quat rot = calcRotationQuat(Axis.Y, diff.x * rotateScale);
                    rot = calcRotationQuat(Axis.X, diff.y * rotateScale).mul(rot).normalize();
                    rotateViewDegrees(rot);

                    break;

                case RotateMode.BIAXIAL:

                    Vec2i initDiff = position.sub_(startDragMousePos);
                    int axis;
                    float degAngle;
                    if (Math.abs(initDiff.x) > Math.abs(initDiff.y)) {
                        axis = Axis.Y;
                        degAngle = initDiff.x * rotateScale;
                    } else {
                        axis = Axis.X;
                        degAngle = initDiff.y * rotateScale;
                    }

                    rot = calcRotationQuat(axis, degAngle);
                    rotateViewDegrees(rot, true);

                    break;

                case RotateMode.SPIN:

                    rotateViewDegrees(calcRotationQuat(Axis.Z, -diff.x * rotateScale));

                    break;
            }

            prevMousePos = position;
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {

        //Ignore button presses when dragging.
        if (!isDragging) {

            if (mouseEvent.getButton() == actionButton) {

                if (mouseEvent.isAltDown()) {

                    rotateMode = RotateMode.SPIN;

                } else if (mouseEvent.isControlDown()) {

                    rotateMode = RotateMode.BIAXIAL;

                } else {

                    rotateMode = RotateMode.DUAL_AXIS;
                }

                prevMousePos.set(mouseEvent.getX(), mouseEvent.getY());
                startDragMousePos.set(mouseEvent.getX(), mouseEvent.getY());
                startDragOrient = po.orientation();

                isDragging = true;
            }
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {

        //Ignore up buttons if not dragging.
        if (isDragging) {

            if (mouseEvent.getButton() == actionButton) {

                mouseMove(mouseEvent);

                isDragging = false;
            }
        }
    }

    private interface RotateMode {

        public static final int DUAL_AXIS = 0;
        public static final int BIAXIAL = 1;
        public static final int SPIN = 2;
    }

    private interface Axis {

        public static final int X = 0;
        public static final int Y = 1;
        public static final int Z = 2;
        public static final int MAX = 3;
    }
}
