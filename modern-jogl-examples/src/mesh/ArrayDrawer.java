/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mesh;

import javax.media.opengl.GL3;
import org.w3c.dom.Element;

/**
 *
 * @author gbarbieri
 */
public class ArrayDrawer {
    
    private int cmd;
    private int start;
    private int count;
    
    public ArrayDrawer(Element element)    {
        
        String cmdS = element.getAttribute("cmd");
        
        switch (cmdS) {
            case "triangles":
                cmd = GL3.GL_TRIANGLES;
                break;
            case "tri-fan":
                cmd = GL3.GL_TRIANGLE_FAN;
                break;
            case "tri-strip":
                cmd = GL3.GL_TRIANGLE_STRIP;
                break;
            default:
                cmd = -1;
                break;
        }
        
        String startS = element.getAttribute("start");
        
        start = Integer.parseInt(startS);
        
        String countS = element.getAttribute("count");
        
        count = Integer.parseInt(countS);
    }

    public int getCmd() {
        return cmd;
    }

    public int getStart() {
        return start;
    }

    public int getCount() {
        return count;
    }
}
