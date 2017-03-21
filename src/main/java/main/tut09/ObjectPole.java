package main.tut09;

import com.jogamp.newt.event.MouseEvent;
import glm.mat.Mat4;
import glm.quat.Quat;
import glm.vec._2.Vec2i;
import glm.vec._3.Vec3;

import static glm.GlmKt.glm;

/**
 * Created by elect on 21/03/17.
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
     * @param initialData    The starting position and orientation of the object in
     *                       world space.
     * @param rotateScale    The number of degrees to rotate the object per window
     *                       space pixel
     * @param actionButton   The mouse button to listen for. All other mouse
     *                       buttons are ignored.
     * @param lookAtProvider An object that will compute a view matrix. This
     *                       defines the view space that orientations can be relative to. If it is
     *                       NULL, then orientations will be relative to the world.
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
        translateMat.set(3, po.getPosition(), 1.0f);

        return translateMat.times_(po.getOrientation().toMat4());
    }

    /**
     * Sets the scaling factor for orientation changes.
     *
     * @param rotateScale The scaling factor is the number of degrees to rotate
     *                    the object per window space pixel. The scale is the same for all mouse
     *                    movements.
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
        return glm.angleAxis(glm.toRad(degAngle), axisVectors[axis]);
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
        po.setOrientation(rot.times(fromInitial ? startDragOrient : po.getOrientation()).normalize_());
    }

    public void rotateLocalDegrees(Quat rot) {
        rotateLocalDegrees(rot, false);
    }

    public void rotateLocalDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        po.setOrientation((fromInitial ? startDragOrient : po.getOrientation()).times(rot).normalize_());
    }

    public void rotateViewDegrees(Quat rot) {
        rotateViewDegrees(rot, false);
    }

    public void rotateViewDegrees(Quat rot, boolean fromInitial) {
        if (!isDragging) {
            fromInitial = false;
        }
        if (view != null) {
            Quat viewQuat = view.calcMatrix().toQuat();
            Quat invViewQuat = viewQuat.conjugate();
            po.setOrientation((invViewQuat.times(rot).times(viewQuat).times(fromInitial ? startDragOrient : po.getOrientation())).normalize_());
        } else {
            rotateWorldDegrees(rot, fromInitial);
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {

            Vec2i position = new Vec2i(mouseEvent.getX(), mouseEvent.getY());

            Vec2i diff = position.minus(prevMousePos);

            switch (rotateMode) {

                case RotateMode.DUAL_AXIS:
                    Quat rot = calcRotationQuat(Axis.Y, diff.x * rotateScale);
                    rot = calcRotationQuat(Axis.X, diff.y * rotateScale).times(rot).normalize();
                    rotateViewDegrees(rot);

                    break;

                case RotateMode.BIAXIAL:

                    Vec2i initDiff = position.minus(startDragMousePos);
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

                prevMousePos.put(mouseEvent.getX(), mouseEvent.getY());
                startDragMousePos.put(mouseEvent.getX(), mouseEvent.getY());
                startDragOrient = po.getOrientation();

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
