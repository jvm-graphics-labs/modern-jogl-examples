/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.tut12.sceneLighting;

import glutil.Timer;
import glutil.interpolator.LightVectorData;
import glutil.interpolator.TimedLinearInterpolator;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author elect
 */
class LightManager {

    private final int NUMBER_OF_LIGHTS = 4;
    private final int NUMBER_OF_POINT_LIGHTS = NUMBER_OF_LIGHTS - 1;
    
    private Timer sunTimer = new Timer(Timer.Type.LOOP, 30.0f);
    private TimedLinearInterpolator<LightVectorData> ambientInterpolator = new TimedLinearInterpolator<>();
    private TimedLinearInterpolator<LightVectorData> backgroundInterpolator = new TimedLinearInterpolator<>();
    private TimedLinearInterpolator<LightVectorData> sunlightInterpolator = new TimedLinearInterpolator<>();
    private TimedLinearInterpolator<LightVectorData> maxIntensityInterpolator = new TimedLinearInterpolator<>();
            
    private List<Timer> lightTimers = new ArrayList<>();

    public LightManager() {
        
        
    }
    
    
    
    public void setSunlightValues(List<Light.SunlightValue> values) {

        List<LightVectorData> ambient = new ArrayList<>();
        List<LightVectorData> light = new ArrayList<>();
        List<LightVectorData> background = new ArrayList<>();
        
        values.forEach(value -> {
            ambient.add(new LightVectorData(value.ambient, value.normTime));
            light.add(new LightVectorData(value.sunlightIntensity, value.normTime));
            background.add(new LightVectorData(value.backgroundColor, value.normTime));
        });

        ambientInterpolator.SetValues(ambient);
        sunlightInterpolator.SetValues(light);
        backgroundInterpolator.SetValues(background);
        
    }
}
