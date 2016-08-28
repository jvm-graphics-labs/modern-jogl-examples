/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.glutil;

/**
 * Utility object describing the scale of the ViewPole.
 *
 * @author gbarbieri
 */
public class ViewScale {

    // The closest the radius to the target point can get.
    private float minRadius;
    // The farthest the radius to the target point can get.
    private float maxRadius;
    // The radius change to use when the SHIFT key isn't held while mouse wheel scrolling.
    private float largeRadiusDelta;
    // The radius change to use when the SHIFT key \em is held while mouse wheel scrolling.
    private float smallRadiusDelta;
    // The position offset to use when the SHIFT key isn't held while pressing a movement key.
    private float largePosOffset;
    // The position offset to use when the SHIFT key \em is held while pressing a movement key.
    private float smallPosOffset;
    // The number of degrees to rotate the view per window space pixel the mouse moves when dragging.
    private float rotationScale;

    public ViewScale(float minRadius, float maxRadius, float largeRadiusDelta, float smallRadiusDelta, float largePosOffset, float smallPosOffset, float rotationScale) {

        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.largeRadiusDelta = largeRadiusDelta;
        this.smallRadiusDelta = smallRadiusDelta;
        this.largePosOffset = largePosOffset;
        this.smallPosOffset = smallPosOffset;
        this.rotationScale = rotationScale;
    }

    public float rotationScale() {
        return rotationScale;
    }

    public void rotationScale(float rotationScale) {
        this.rotationScale = rotationScale;
    }

    public float minRadius() {
        return minRadius;
    }

    public void minRadius(float minRadius) {
        this.minRadius = minRadius;
    }

    public float maxRadius() {
        return maxRadius;
    }

    public void maxRadius(float maxRadius) {
        this.maxRadius = maxRadius;
    }

    public float largeRadiusDelta() {
        return largeRadiusDelta;
    }

    public void largeRadiusDelta(float largeRadiusDelta) {
        this.largeRadiusDelta = largeRadiusDelta;
    }

    public float smallRadiusDelta() {
        return smallRadiusDelta;
    }

    public void smallRadiusDelta(float smallRadiusDelta) {
        this.smallRadiusDelta = smallRadiusDelta;
    }

    public float largePosOffset() {
        return largePosOffset;
    }

    public void largePosOffset(float largePosOffset) {
        this.largePosOffset = largePosOffset;
    }

    public float smallPosOffset() {
        return smallPosOffset;
    }

    public void smallPosOffset(float smallPosOffset) {
        this.smallPosOffset = smallPosOffset;
    }
}
