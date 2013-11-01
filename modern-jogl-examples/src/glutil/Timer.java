/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package glutil;

import jglm.Jglm;

/**
 *
 * @author gbarbieri
 */
public class Timer {

    private Type type;
    private float secDuration;
    private boolean hasUpdated;
    private boolean isPaused;
    private float absPreviousTime;
    private float secAccumTime;
    private long start;

    public Timer(Type type, float duration) {

        this.type = type;
        this.secDuration = duration;

        hasUpdated = false;
        isPaused = false;
        absPreviousTime = 0.0f;
        secAccumTime = 0.0f;

        start = System.currentTimeMillis();
    }

    public void update() {

        float absCurrenTime = (System.currentTimeMillis() - start) / 1000.0f;
        
        if(!hasUpdated){
            
            absPreviousTime = absCurrenTime;
            
            hasUpdated = true;
        }
        
        if(isPaused){
            
            absPreviousTime = absCurrenTime;
        }
        
        float deltaTime = absCurrenTime - absPreviousTime;
        
        secAccumTime += deltaTime;
        
        absPreviousTime = absCurrenTime;
    }
    
    public float getAlpha(){
        
        switch(type){
            
            case Loop:
                return (secAccumTime % secDuration) / secDuration;
                
            case Single:
                return Jglm.clamp(secAccumTime/secDuration, 0.0f, 1.0f);
        }
        
        return -1.0f;
    }
    
    public void togglePause(){
        
        isPaused = !isPaused;
    }

    public enum Type {

        Loop,
        Single,
        Infinite;
    }
}
