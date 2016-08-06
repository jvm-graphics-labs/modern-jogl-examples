/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

import com.jogamp.newt.event.KeyEvent;
import glm.glm;
import glm.mat._4.Mat4;
import glm.quat.Quat;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
import glm.vec._2.i.Vec2i;
import glm.vec._3.Vec3;

/**
 * Mouse-based control over the orientation and position of the camera. This
 * view controller is based on a target point, which is centered in the camera,
 * and an orientation around that target point that represents the camera. The
 * Pole allows the user to rotate around this point, move closer to/farther from
 * it, and to move the point itself.
 *
 * This Pole is given a ViewDef that defines a number of size parameters and
 * limitations.
 *
 * This Pole is given an action button, which it will listen for click events
 * from. If the mouse button is clicked and no modifiers are pressed, the the
 * view will rotate around the object in both the view-local X and Y axes. If
 * the CTRL key is held, then it will rotate about the X or Y axes, based on how
 * far the mouse is from the starting point in the X or Y directions. If the ALT
 * key is held, then the camera will spin in the view-local Z direction.
 *
 * Scrolling the mouse wheel up or down moves the camera closer or farther from
 * the object, respectively. The distance is taken from
 * ViewDef::largeRadiusDelta. If the SHIFT key is held while scrolling, then the
 * movement will be the ViewDef::smallRadiusDelta value instead.
 *
 * The target point can be moved, relative to the current view, with the WASD
 * keys. W/S move forward and backwards, while A/D move left and right,
 * respectively. Q and E move down and up, respectively. If the
 * rightKeyboardCtrls parameter of the constructor is set, then it uses the
 * IJKLUO keys instead of WASDQE. The offset applied to the position is
 * ViewDef::largePosOffset; if SHIFT is held, then ViewDef::smallPosOffset is
 * used instead.
 *
 * @author gbarbieri
 */
public class ViewPole extends ViewProvider {

    private ViewData currView;
    private ViewScale viewScale;

    private ViewData initialView;
    private int actionButton;
    private boolean rightKeyboardCtrls;

    //Used when rotating.
    private boolean isDragging;
    private int rotateMode;

    private float degStartDragSpin;
    private Vec2i startDragMouseLoc;
    private Quat startDragOrient;

    public ViewPole(ViewData initialView, ViewScale viewScale, int actionButton) {
        this(initialView, viewScale, actionButton, false);
    }

    /**
     * Creates a view pole with the given initial target position, view
     * definition, and action button.
     *
     * @param initialView initialView The starting state of the view.
     * @param viewScale viewScale The viewport definition to use.
     * @param actionButton actionButton The mouse button to listen for. All
     * other mouse buttons are ignored.
     * @param rightKeyboardCtrls if true, then it uses IJKLUO instead of WASDQE
     * keys.
     */
    public ViewPole(ViewData initialView, ViewScale viewScale, int actionButton, boolean rightKeyboardCtrls) {

        currView = initialView;
        this.viewScale = viewScale;
        this.initialView = initialView;
        this.actionButton = actionButton;
        this.rightKeyboardCtrls = rightKeyboardCtrls;
        isDragging = false;
    }

    /**
     * Generates the world-to-camera matrix for the view.
     *
     * @return
     */
    public Mat4 calcMatrix() {

        Mat4 theMat = new Mat4(1.0f);

        /**
         * Remember: these transforms are in reverse order.
         *
         * In this space, we are facing in the correct direction. Which means
         * that the camera point is directly behind us by the radius number of
         * units.
         */
        theMat.translate(0.0f, 0.0f, -currView.radius);

        //Rotate the world to look in the right direction..
        Quat fullRotation = glm.angleAxis_(currView.degSpinRotation, new Vec3(0.0f, 0.0f, 1.0f));
        fullRotation.mul(currView.orient);

        theMat.mul(Mat4.cast_(fullRotation));

        // Translate the world by the negation of the lookat point, placing the origin at the lookat point.
        theMat.translate(currView.targetPos.negate_());

        return theMat;
    }

    /**
     * Sets the scaling factor for orientation changes.
     *
     * @param rotateScale The scaling factor is the number of degrees to rotate
     * the view per window space pixel. The scale is the same for all mouse
     * movements.
     */
    public void setRotationScale(float rotateScale) {
        viewScale.rotationScale = rotateScale;
    }

    /**
     * @return Gets the current scaling factor for orientation changes.
     */
    public float getRotationScale() {
        return viewScale.rotationScale;
    }

    /**
     * @return Retrieves the current viewing information.
     */
    public ViewData getView() {
        return currView;
    }

    /**
     * Resets the view to the initial view. Will fail if currently dragging.
     */
    public void reset() {
        if (!isDragging) {
            currView = initialView;
        }
    }

    public void processXchange(int xDiff) {

        float degAngleDiff = (xDiff * viewScale.rotationScale);

        //Rotate about the world-space Y axis.
        currView.orient = startDragOrient.mul_(glm.angleAxis_(degAngleDiff, new Vec3(0.0f, 1.0f, 0.0f)));
    }

    public void processYchange(int yDiff) {

        float degAngleDiff = (yDiff * viewScale.rotationScale);

        //Rotate about the world-space X axis.
        currView.orient = glm.angleAxis_(degAngleDiff, new Vec3(0.0f, 1.0f, 0.0f)).mul(startDragOrient);
    }

    private void processXYchange(Vec2i diff) {

        float degXAngleDiff = diff.x * viewScale.rotationScale;
        float degYAngleDiff = diff.y * viewScale.rotationScale;

        // Rotate about the world-space Y axis.
        currView.orient = startDragOrient.mul_(glm.angleAxis_(degXAngleDiff, new Vec3(0.0f, 1.0f, 0.0f)));
        //Rotate about the local-space X axis.
        currView.orient = glm.angleAxis_(degYAngleDiff, new Vec3(1.0f, 0.0f, 0.0f)).mul(currView.orient);
    }

    private void processSpinAxis(Vec2i diff) {

        float degSpinDiff = diff.x * viewScale.rotationScale;
        currView.degSpinRotation = degSpinDiff + degStartDragSpin;
    }

    private void beginDragRotate(Vec2i start, int rotMode) {

        rotateMode = rotMode;

        startDragMouseLoc = start;

        degStartDragSpin = currView.degSpinRotation;

        startDragOrient = currView.orient;

        isDragging = true;
    }

    private void onDragRotate(MouseEvent mouseEvent) {

        Vec2i diff = new Vec2i(mouseEvent.getX() - startDragMouseLoc.x, mouseEvent.getY() - startDragMouseLoc.y);

        switch (rotateMode) {

            case RotateMode.DUAL_AXIS_ROTATE:
                processXYchange(diff);
                break;

            case RotateMode.BIAXIAL_ROTATE:
                if (Math.abs(diff.x) > Math.abs(diff.y)) {
                    processXchange(diff.x);
                } else {
                    processYchange(diff.y);
                }
                break;

            case RotateMode.XZ_AXIS_ROTATE:
                processXchange(diff.x);
                break;

            case RotateMode.Y_AXIS_ROTATE:
                processYchange(diff.y);
                break;

            case RotateMode.SPIN_VIEW_AXIS:
                processSpinAxis(diff);
                break;

            default:
                break;
        }
    }

    private void endDragRotate(MouseEvent mouseEvent) {
        endDragRotate(mouseEvent, true);
    }

    private void endDragRotate(MouseEvent mouseEvent, boolean keepResults) {

        if (keepResults) {
            onDragRotate(mouseEvent);
        } else {
            currView.orient = startDragOrient;
        }
        isDragging = false;
    }

    private void moveCloser(boolean largeStep) {

        currView.radius -= largeStep ? viewScale.largeRadiusDelta : viewScale.smallRadiusDelta;

        if (currView.radius < viewScale.minRadius) {
            currView.radius = viewScale.minRadius;
        }
    }

    private void moveAway(boolean largeStep) {

        currView.radius += largeStep ? viewScale.largeRadiusDelta : viewScale.smallRadiusDelta;

        if (currView.radius > viewScale.maxRadius) {
            currView.radius = viewScale.maxRadius;
        }
    }

    public void mouseMove(MouseEvent mouseEvent) {

        if (isDragging) {
            onDragRotate(mouseEvent);
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {

        //Ignore all other button presses when dragging.
        if (!isDragging) {

            if (mouseEvent.getButton() == actionButton) {

                Vec2i position = new Vec2i(mouseEvent.getX(), mouseEvent.getY());

                if (mouseEvent.isControlDown()) {
                    beginDragRotate(position, RotateMode.BIAXIAL_ROTATE);
                } else if (mouseEvent.isAltDown()) {
                    beginDragRotate(position, RotateMode.SPIN_VIEW_AXIS);
                } else {
                    beginDragRotate(position, RotateMode.DUAL_AXIS_ROTATE);
                }
            }
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {

        //Ignore all other button releases when not dragging
        if (isDragging) {

            if (mouseEvent.getButton() == actionButton) {

                if (rotateMode == RotateMode.DUAL_AXIS_ROTATE
                        || rotateMode == RotateMode.BIAXIAL_ROTATE
                        || rotateMode == RotateMode.SPIN_VIEW_AXIS) {

                    endDragRotate(mouseEvent);
                }
            }
        }
    }

    public void mouseWheel(MouseWheelEvent mouseWheelEvent) {

        if (mouseWheelEvent.getWheelRotation() < 0) {
            moveCloser(!mouseWheelEvent.isShiftDown());
        } else {
            moveAway(!mouseWheelEvent.isShiftDown());
        }
    }

    public void charPress(KeyEvent keyEvent) {

        float offset = keyEvent.isShiftDown() ? viewScale.smallPosOffset : viewScale.largePosOffset;

        if (rightKeyboardCtrls) {

            switch (keyEvent.getKeyCode()) {

                case KeyEvent.VK_I:
                    offsetTargetPos(TargetOffsetDir.FORWARD, offset);
                    break;
                case KeyEvent.VK_K:
                    offsetTargetPos(TargetOffsetDir.BACKWARD, offset);
                    break;
                case KeyEvent.VK_L:
                    offsetTargetPos(TargetOffsetDir.RIGHT, offset);
                    break;
                case KeyEvent.VK_J:
                    offsetTargetPos(TargetOffsetDir.LEFT, offset);
                    break;
                case KeyEvent.VK_O:
                    offsetTargetPos(TargetOffsetDir.UP, offset);
                    break;
                case KeyEvent.VK_U:
                    offsetTargetPos(TargetOffsetDir.DOWN, offset);
                    break;
            }
        } else {

            switch (keyEvent.getKeyCode()) {

                case KeyEvent.VK_W:
                    offsetTargetPos(TargetOffsetDir.FORWARD, offset);
                    break;
                case KeyEvent.VK_S:
                    offsetTargetPos(TargetOffsetDir.BACKWARD, offset);
                    break;
                case KeyEvent.VK_D:
                    offsetTargetPos(TargetOffsetDir.RIGHT, offset);
                    break;
                case KeyEvent.VK_A:
                    offsetTargetPos(TargetOffsetDir.LEFT, offset);
                    break;
                case KeyEvent.VK_E:
                    offsetTargetPos(TargetOffsetDir.UP, offset);
                    break;
                case KeyEvent.VK_Q:
                    offsetTargetPos(TargetOffsetDir.DOWN, offset);
                    break;
            }
        }
    }

    private void offsetTargetPos(int dir, float worldDistance) {

        Vec3 offsetDir = offsets[dir];
        offsetTargetPos(offsetDir.mul_(worldDistance));
    }

    private void offsetTargetPos(Vec3 cameraOffset) {

        Mat4 currMat = calcMatrix();
        Quat orientation = Quat.cast_(currMat);

        Quat invOrient = orientation.conjugate();
        Vec3 worldOffset = invOrient.mul(cameraOffset);

        currView.targetPos.add(worldOffset);
    }

    private interface RotateMode {

        public static final int DUAL_AXIS_ROTATE = 0;
        public static final int BIAXIAL_ROTATE = 1;
        public static final int XZ_AXIS_ROTATE = 2;
        public static final int Y_AXIS_ROTATE = 3;
        public static final int SPIN_VIEW_AXIS = 4;
    }

    private interface TargetOffsetDir {

        public static final int UP = 0;
        public static final int DOWN = 1;
        public static final int FORWARD = 2;
        public static final int BACKWARD = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
    }

    private Vec3[] offsets = {
        new Vec3(+0.0f, +1.0f, +0.0f),
        new Vec3(+0.0f, -1.0f, +0.0f),
        new Vec3(+0.0f, +0.0f, -1.0f),
        new Vec3(+0.0f, +0.0f, +1.0f),
        new Vec3(+1.0f, +0.0f, +0.0f),
        new Vec3(-1.0f, +0.0f, +0.0f)};
}
