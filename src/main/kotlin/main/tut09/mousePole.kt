package main.tut09

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.MouseEvent
import glm.glm
import glm.mat.Mat4
import glm.quat.Quat
import glm.vec._2.Vec2i
import glm.vec._3.Vec3

/**
 * Created by elect on 20/03/17.
 */

abstract class ViewProvider {

    /** Computes the camera matrix. */
    abstract fun calcMatrix(res: Mat4): Mat4

    abstract fun calcMatrix(): Mat4
}

/** Utility object containing the ObjectPole's position and orientation information.    */
class ObjectData(

        /** The world-space position of the object. */
        var position: Vec3,
        /** The world-space orientation of the object.  */
        var orientation: Quat)

private val axisVectors = arrayOf(
        Vec3(1, 0, 0),
        Vec3(0, 1, 0),
        Vec3(0, 0, 1))

enum class Axis {X, Y, Z, MAX }
enum class RotateMode {DUAL_AXIS, BIAXIAL, SPIN }

fun calcRotationQuat(axis: Axis, radAngle: Float, res: Quat) = glm.angleAxis(radAngle, axisVectors[axis.ordinal], Quat())

/**
 *  Mouse-based control over the orientation and position of an object.
 *
 *  This Pole deals with three spaces: local, world, and view. Local refers to the coordinate system of vertices given
 *  to the matrix that this Pole generates. World represents the \em output coordinate system. So vertices enter in
 *  local and are transformed to world. Note that this does not have to actually be the real world-space. It could be
 *  the space of the parent node in some object hierarchy, though there is a caveat below.
 *
 *  View represents the space that vertices are transformed into by the ViewProvider's matrix.
 *  The ViewProvider is given to this class's constructor. The assumption that this Pole makes when using the view space
 *  matrix is that the matrix the ObjectPole generates will be right-multiplied by the view matrix given by the
 *  ViewProvider. So it is assumed that there is no intermediate space between world and view.
 *
 *  By defining these three spaces, it is possible to dictate orientation relative to these spaces.
 *  The ViewProvider exists to allow transformations relative to the current camera.
 *
 *  This Pole is given an action button, which it will listen for click events from. When the action button is held down
 *  and the mouse moved, the object's orientation will change. The orientation will be relative to the view's
 *  orientation if a ViewProvider was provided. If not, it will be relative to the world.
 *
 *  If no modifier keys (shift, ctrl, alt) were held when the click was given, then the object will be oriented in both
 *  the X and Y axes of the transformation space. If the CTRL key is held when the click was given, then the object will
 *  only rotate around either the X or Y axis.
 *  The selection is based on whether the X or the Y mouse coordinate is farthest from the initial position when
 *  dragging started.
 *  If the ALT key is held, then the object will rotate about the Z axis, and only the X position of the mouse affects
 *  the object.
 */
class ObjectPole : ViewProvider {

    private var view: ViewProvider?
    private var po: ObjectData
    private var initialPo: ObjectData

    /** The scaling factor is the number of degrees to rotate the object per window space pixel.
     *  The scale is the same for all mouse movements.     */
    private var rotateScale: Float
    private var actionButton: Short

    // Used when rotating.
    private var rotateMode = RotateMode.DUAL_AXIS
    private var isDragging = false

    private var prevMousePos = Vec2i()
    private var startDragMousePos = Vec2i()
    private var startDragOrient = Quat()

    /**
     * Creates an object pole with a given initial position and orientation.
     *
     * @param initialData The starting position and orientation of the object in world space.
     * @param rotateScale The number of degrees to rotate the object per window space pixel
     * @param actionButton The mouse button to listen for. All other mouse buttons are ignored.
     * @param pLookatProvider An object that will compute a view matrix. This defines the view space that orientations
     * can be relative to. If it is NULL, then orientations will be relative to the world.
     */
    constructor(initialData: ObjectData, rotateScale: Float, actionButton: Short, lookatProvider: ViewProvider) {

        view = lookatProvider
        po = initialData
        initialPo = initialData
        this.rotateScale = rotateScale
        this.actionButton = actionButton
    }

    /** Generates the local-to-world matrix for this object.    */
    override fun calcMatrix(res: Mat4): Mat4 {

        res put 1f
        res.set(3, po.position, 1f)

        return res times_ (po.orientation to mat4_A)
    }

    override fun calcMatrix() = calcMatrix(mat4_B)

    /** Retrieves the current position and orientation of the object.   */
    fun getPosOrient() = po

    /** Resets the object to the initial position/orientation. Will fail if currently dragging. */
    fun reset() {
        if (!isDragging) {
            po.position put initialPo.position
            po.orientation put initialPo.orientation
        }
    }


    /* Input Providers
     *
     * These functions provide input, since Poles cannot get input for themselves.     */

    /**
     * Notifies the pole of a mouse button being pressed or released.
     *
     * @param event The mouse event */
    fun mousePressed(event: MouseEvent) {

        // Ignore button presses when dragging.
        if (!isDragging)

            if (event.button == actionButton) {

                rotateMode =
                        if (event.isAltDown)
                            RotateMode.SPIN
                        else if (event.isControlDown)
                            RotateMode.BIAXIAL
                        else
                            RotateMode.DUAL_AXIS

                prevMousePos.put(event.x, event.y)
                startDragMousePos.put(event.x, event.y)
                startDragOrient put po.orientation

                isDragging = true
            }
    }

    fun mouseReleased(event: MouseEvent) {

        // Ignore up buttons if not dragging.
        if (isDragging)

            if (event.button == actionButton) {

                mouseDragged(event)

                isDragging = false
            }
    }

    /** Notifies the pole that the mouse has moved to the given absolute position.  */
    fun mouseDragged(event: MouseEvent) {

        if (isDragging) {

            val position = vec2i_A.put(event.x, event.y)
            val diff = position.minus(prevMousePos, vec2i_B)

            when (rotateMode) {

                RotateMode.DUAL_AXIS -> {

                    val rot = calcRotationQuat(Axis.Y, glm.toRad(diff.x * rotateScale), quat_A)
                    calcRotationQuat(Axis.X, glm.toRad(diff.y * rotateScale), quat_B).times(rot, rot).normalize_()
                    rotateView(rot)
                }

                RotateMode.BIAXIAL -> {

                    val initDiff = position.minus(startDragMousePos, vec2i_C)

                    var axis = Axis.X
                    var degAngle = initDiff.y * rotateScale

                    if (glm.abs(initDiff.x) > glm.abs(initDiff.y)) {
                        axis = Axis.Y
                        degAngle = initDiff.x * rotateScale
                    }
                    val rot = calcRotationQuat(axis, glm.toRad(degAngle), quat_A)
                    rotateView(rot, true)
                }

                RotateMode.SPIN -> rotateView(calcRotationQuat(Axis.Z, glm.toRad(-diff.x * rotateScale), quat_A))
            }
            prevMousePos put position
        }
    }

    fun rotateWorld(rot: Quat, fromInitial: Boolean = false) {

        val fromInitial_ = if (isDragging) fromInitial else false

        val orient = if (fromInitial_) startDragOrient else po.orientation
        po.orientation = rot.times(orient, quat_C).normalize_()
    }

    fun rotateLocal(rot: Quat, fromInitial: Boolean = false) {

        val fromInitial_ = if (isDragging) fromInitial else false

        val orient = if (fromInitial_) startDragOrient else po.orientation
        po.orientation = orient.times(rot, quat_D).normalize_()
    }

    fun rotateView(rot: Quat, fromInitial: Boolean = false) {

        val fromInitial_ = if (isDragging) fromInitial else false

        if (view != null) {

            val viewQuat = view!!.calcMatrix().to(quat_E)
            val invViewQuat = viewQuat.conjugate(quat_F)
            val orient = if (fromInitial_) startDragOrient else po.orientation

            po.orientation = (invViewQuat.times(rot, quat_G) times_ viewQuat times_ orient).normalize_()

        } else
            rotateWorld(rot, fromInitial_)
    }
}


/** Utility object containing the ViewPole's view information.     */
class ViewData(
        /** The starting target position position.  */
        var targetPos: Vec3,
        /** The initial orientation aroudn the target position. */
        var orient: Quat,
        /** The initial radius of the camera from the target point. */
        var radius: Float,
        /** The initial spin rotation of the "up" axis, relative to \a orient   */
        var degSpinRotation: Float)

/** Utility object describing the scale of the ViewPole.    */
class ViewScale(
        /** The closest the radius to the target point can get. */
        var minRadius: Float,
        /** The farthest the radius to the target point can get.    */
        var maxRadius: Float,
        /** The radius change to use when the SHIFT key isn't held while mouse wheel scrolling. */
        var largeRadiusDelta: Float,
        /** The radius change to use when the SHIFT key \em is held while mouse wheel scrolling.    */
        var smallRadiusDelta: Float,
        /** The position offset to use when the SHIFT key isn't held while pressing a movement key. */
        var largePosOffset: Float,
        /** The position offset to use when the SHIFT key \em is held while pressing a movement key.    */
        var smallPosOffset: Float,
        /** The number of degrees to rotate the view per window space pixel the mouse moves when dragging.  */
        var rotationScale: Float)

/**
 * Mouse-based control over the orientation and position of the camera.
 *
 * This view controller is based on a target point, which is centered in the camera, and an orientation around that
 * target point that represents the camera. The Pole allows the user to rotate around this point, move closer to/farther
 * from it, and to move the point itself.
 *
 * This Pole is given a ViewData object that contains the initial viewing orientation, as well as a ViewScale that
 * represents how fast the various movements change the view, as well as its limitations.
 *
 * This Pole is given an action button, which it will listen for click events from. If the mouse button is clicked and
 * no modifiers are pressed, the the view will rotate around the object in both the view-local X and Y axes. If the CTRL
 * key is held, then it will rotate about the X or Y axes, based on how far the mouse is from the starting point in the
 * X or Y directions. If the ALT key is held, then the camera will spin in the view-local Z direction.
 *
 * Scrolling the mouse wheel up or down moves the camera closer or farther from the object, respectively.
 * The distance is taken from ViewScale::largeRadiusDelta. If the SHIFT key is held while scrolling, then the movement
 * will be the ViewScale::smallRadiusDelta value instead.
 *
 * The target point can be moved, relative to the current view, with the WASD keys. W/S move forward and backwards,
 * while A/D move left and right, respectively. Q and E move down and up, respectively. If the rightKeyboardCtrl
 * parameter of the constructor is set, then it uses the IJKLUO keys instead of WASDQE. The offset applied to the
 * position is ViewScale::largePosOffset; if SHIFT is held, then ViewScale::smallPosOffset is used instead.
 */
class ViewPole : ViewProvider {

    private var currView: ViewData
    private var viewScale: ViewScale

    private var initialView: ViewData
    private val actionButton: Short

    //Used when rotating.
    private var isDragging = true
    private var rotateMode = RotateMode.DUAL_AXIS_ROTATE

    private var degStartDragSpin = 0f
    private var startDragMouseLoc = Vec2i()
    private val startDragOrient = Quat()

    var rotationScale
        /** Gets the current scaling factor for orientation changes.    */
        get() = viewScale.rotationScale
        /** Sets the scaling factor for orientation changes.
         *
         * The scaling factor is the number of degrees to rotate the view per window space pixel.
         * The scale is the same for all mouse movements.     */
        set(value) {
            viewScale.rotationScale = value
        }

    /**
     * Creates a view pole with the given initial target position, view definition, and action button.
     *
     * @param initialView The starting state of the view.
     * @param viewScale The viewport definition to use.
     * @param actionButton The mouse button to listen for. All other mouse buttons are ignored.
     * \param bRightKeyboardCtrls If true, then it uses IJKLUO instead of WASDQE keys.
     */
    constructor(initialView: ViewData, viewScale: ViewScale, actionButton: Short = MouseEvent.BUTTON1) {
        currView = initialView
        this.viewScale = viewScale
        this.initialView = initialView
        this.actionButton = actionButton
    }

    /** Generates the world-to-camera matrix for the view.     */
    override fun calcMatrix(res: Mat4): Mat4 {

        res put 1f

        // Remember: these transforms are in reverse order.

        /* In this space, we are facing in the correct direction. Which means that the camera point is directly behind
         * us by the radius number of units.    */
        res.translate_(0.0f, 0.0f, -currView.radius)

        //Rotate the world to look in the right direction..
        val fullRotation = glm.angleAxis(currView.degSpinRotation, 0.0f, 0.0f, 1.0f, quat_H) times_ currView.orient
        res times_ (fullRotation to mat4_C)

        // Translate the world by the negation of the lookat point, placing the origin at the lookat point.
        res translate_ -currView.targetPos

        return res
    }

    override fun calcMatrix() = calcMatrix(mat4_D)

    /** Resets the view to the initial view. Will fail if currently dragging.   */
    fun reset() {
        if (!isDragging)
            currView = initialView
    }

    fun processXChange(xDiff: Int, clearY: Boolean = false) {

        val radAngleDiff = glm.toRad(xDiff * viewScale.rotationScale)

        // Rotate about the world-space Y axis.
        glm.angleAxis(radAngleDiff, 0f, 1f, 0f, quat_I)
        startDragOrient.times(quat_I, currView.orient)
    }

    fun processYChange(yDiff: Int, clearXZ: Boolean = false) {

        val radAngleDiff = glm.toRad(yDiff * viewScale.rotationScale)

        // Rotate about the local-space X axis.
        glm.angleAxis(radAngleDiff, 1f, 0f, 0f, quat_J)
        quat_J.times(startDragOrient, currView.orient)
    }

    fun processXYChange(xDiff: Int, yDiff: Int) {

        val radXAngleDiff = glm.toRad(xDiff * viewScale.rotationScale)
        val radYAngleDiff = glm.toRad(yDiff * viewScale.rotationScale)

        // Rotate about the world-space Y axis.
        glm.angleAxis(radXAngleDiff, 0.0f, 1.0f, 0.0f, quat_K)
        startDragOrient.times(quat_K, currView.orient)
        //Rotate about the local-space X axis.
        glm.angleAxis(radYAngleDiff, 1.0f, 0.0f, 0.0f, quat_L)
        quat_L.times(currView.orient, currView.orient)
    }

    fun processSpinAxis(xDiff: Int, yDiff: Int) {

        val degSpinDiff = xDiff * viewScale.rotationScale
        currView.degSpinRotation = degSpinDiff + degStartDragSpin
    }

    fun beginDragRotate(start: Vec2i, rotMode: RotateMode) {

        rotateMode = rotMode

        startDragMouseLoc put start

        degStartDragSpin = currView.degSpinRotation

        startDragOrient put currView.orient

        isDragging = true
    }

    fun onDragRotate(curr: Vec2i) {

        val diff = curr.minus(startDragMouseLoc, vec2i_D)

        when (rotateMode) {

            RotateMode.DUAL_AXIS_ROTATE -> processXYChange(diff.x, diff.y)

            RotateMode.BIAXIAL_ROTATE ->
                if (glm.abs(diff.x) > glm.abs(diff.y))
                    processXChange(diff.x, true)
                else
                    processYChange(diff.y, true)

            RotateMode.XZ_AXIS_ROTATE -> processXChange(diff.x)

            RotateMode.Y_AXIS_ROTATE -> processYChange(diff.y)

            RotateMode.SPIN_VIEW_AXIS -> processSpinAxis(diff.x, diff.y)
        }
    }

    fun endDragRotate(end: Vec2i, keepResult: Boolean = true) {

        if (keepResult)
            onDragRotate(end)
        else
            currView.orient put startDragOrient

        isDragging = false
    }

    fun moveCloser(largeStep: Boolean = true) {

        currView.radius -= if (largeStep) viewScale.largeRadiusDelta else viewScale.smallRadiusDelta

        if (currView.radius < viewScale.minRadius)
            currView.radius = viewScale.minRadius
    }

    fun moveAway(largeStep: Boolean = true) {

        currView.radius += if (largeStep) viewScale.largeRadiusDelta else viewScale.smallRadiusDelta

        if (currView.radius > viewScale.maxRadius)
            currView.radius = viewScale.maxRadius
    }

    fun mouseDragged(event: MouseEvent) {
        if (isDragging)
            onDragRotate(vec2i_E.put(event.x, event.y))
    }

    /**
     * Input Providers
     *
     * These functions provide input, since Poles cannot get input for themselves. See \ref module_glutil_poles
     * "the Pole manual" for details.   */

    fun mousePressed(event: MouseEvent) {

        val position = vec2i_F.put(event.x, event.y)
        // Ignore all other button presses when dragging.
        if (isDragging)

            if (event.button == actionButton) {

                if (event.isControlDown)
                    beginDragRotate(position, RotateMode.BIAXIAL_ROTATE)
                else if (event.isAltDown)
                    beginDragRotate(position, RotateMode.SPIN_VIEW_AXIS)
                else
                    beginDragRotate(position, RotateMode.DUAL_AXIS_ROTATE)
            }
    }

    fun mouseReleased(event: MouseEvent) {

        // Ignore all other button releases when not dragging
        if (isDragging)

            if (event.button == actionButton)

                if (rotateMode == RotateMode.DUAL_AXIS_ROTATE || rotateMode == RotateMode.SPIN_VIEW_AXIS || rotateMode == RotateMode.BIAXIAL_ROTATE)
                    endDragRotate(vec2i_G.put(event.x, event.y))
    }

    fun mouseWheel(event: MouseEvent) =
            if (event.rotation[1] > 0)
                moveCloser(event.isShiftDown)
            else
                moveAway(event.isShiftDown)

    fun buttonPressed(event: KeyEvent) {

        val distance = if (event.isShiftDown) viewScale.largePosOffset else viewScale.smallPosOffset

        when (event.keyCode) {

            KeyEvent.VK_W -> offsetTargetPos(TargetOffsetDir.FORWARD, distance)
            KeyEvent.VK_K -> offsetTargetPos(TargetOffsetDir.BACKWARD, distance)
            KeyEvent.VK_L -> offsetTargetPos(TargetOffsetDir.RIGHT, distance)
            KeyEvent.VK_J -> offsetTargetPos(TargetOffsetDir.LEFT, distance)
            KeyEvent.VK_O -> offsetTargetPos(TargetOffsetDir.UP, distance)
            KeyEvent.VK_U -> offsetTargetPos(TargetOffsetDir.DOWN, distance)
        }
    }

    fun offsetTargetPos(dir: TargetOffsetDir, worldDistance: Float) {

        val offsetDir = offsets[dir.ordinal]
        offsetTargetPos(offsetDir.times(worldDistance, vec3_A))
    }

    fun offsetTargetPos(cameraoffset: Vec3) {

        val currMat = calcMatrix(mat4_E)
        val orientation = currMat.to(quat_M)

        val invOrient = orientation.conjugate(quat_N)
        val worldOffset = invOrient.times(cameraoffset, vec3_B)

        currView.targetPos plus_ worldOffset
    }

    val offsets = arrayOf(
            Vec3(+0.0f, +1.0f, +0.0f),
            Vec3(+0.0f, -1.0f, +0.0f),
            Vec3(+0.0f, +0.0f, -1.0f),
            Vec3(+0.0f, +0.0f, +1.0f),
            Vec3(+1.0f, +0.0f, +0.0f),
            Vec3(-1.0f, +0.0f, +0.0f))

    enum class TargetOffsetDir {
        UP,
        DOWN,
        FORWARD,
        BACKWARD,
        RIGHT,
        LEFT
    }

    enum class RotateMode {
        DUAL_AXIS_ROTATE,
        BIAXIAL_ROTATE,
        XZ_AXIS_ROTATE,
        Y_AXIS_ROTATE,
        SPIN_VIEW_AXIS,
    }
}

private val mat4_A = Mat4()
private val mat4_B = Mat4()
private val mat4_C = Mat4()
private val mat4_D = Mat4()
private val mat4_E = Mat4()
private val vec2i_A = Vec2i()
private val vec2i_B = Vec2i()
private val vec2i_C = Vec2i()
private val vec2i_D = Vec2i()
private val vec2i_E = Vec2i()
private val vec2i_F = Vec2i()
private val vec2i_G = Vec2i()
private val vec3_A = Vec3()
private val vec3_B = Vec3()
private val quat_A = Quat()
private val quat_B = Quat()
private val quat_C = Quat()
private val quat_D = Quat()
private val quat_E = Quat()
private val quat_F = Quat()
private val quat_G = Quat()
private val quat_H = Quat()
private val quat_I = Quat()
private val quat_J = Quat()
private val quat_K = Quat()
private val quat_L = Quat()
private val quat_M = Quat()
private val quat_N = Quat()