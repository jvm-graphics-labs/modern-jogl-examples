/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

/**
 *
 * @author gbarbieri
 */
public class ViewScale {

    private float minRadius;
    private float maxRadius;
    private float largeRadiusDelta;
    private float smallRadiusDelta;
    private float largePosOffset;
    private float smallPosOffset;
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

    public float getRotationScale() {
        return rotationScale;
    }

    public float getLargeRadiusDelta() {
        return largeRadiusDelta;
    }

    public float getSmallRadiusDelta() {
        return smallRadiusDelta;
    }

    public float getMinRadius() {
        return minRadius;
    }

    public float getMaxRadius() {
        return maxRadius;
    }
}