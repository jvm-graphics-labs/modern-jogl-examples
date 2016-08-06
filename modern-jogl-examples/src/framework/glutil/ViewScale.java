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
    float minRadius;
    // The farthest the radius to the target point can get.
    float maxRadius;
    // The radius change to use when the SHIFT key isn't held while mouse wheel scrolling.
    float largeRadiusDelta;
    // The radius change to use when the SHIFT key \em is held while mouse wheel scrolling.
    float smallRadiusDelta;
    // The position offset to use when the SHIFT key isn't held while pressing a movement key.
    float largePosOffset;
    // The position offset to use when the SHIFT key \em is held while pressing a movement key.
    float smallPosOffset;
    // The number of degrees to rotate the view per window space pixel the mouse moves when dragging.
    float rotationScale;

    public ViewScale(float minRadius, float maxRadius, float largeRadiusDelta, float smallRadiusDelta, float largePosOffset, float smallPosOffset, float rotationScale) {

        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.largeRadiusDelta = largeRadiusDelta;
        this.smallRadiusDelta = smallRadiusDelta;
        this.largePosOffset = largePosOffset;
        this.smallPosOffset = smallPosOffset;
        this.rotationScale = rotationScale;
    }
}
