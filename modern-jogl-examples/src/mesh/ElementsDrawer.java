/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mesh;

import com.jogamp.opengl.GL3;
import java.util.StringTokenizer;
import org.w3c.dom.Element;

/**
 *
 * @author gbarbieri
 */
public class ElementsDrawer {

    private int cmd;
    private int type;
//    private short[] indices;
    private int[] indices;
    private int offset;

    public ElementsDrawer(Element element) {
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

        String typeS = element.getAttribute("type");

        if (typeS.equals("ushort") || typeS.equals("uint")) {
            type = GL3.GL_UNSIGNED_INT;
        } else {
            type = -1;
        }

//        System.out.println("cmd: " + cmdS + " type: " + typeS);

        String content = element.getTextContent();

        StringTokenizer stringTokenizer = new StringTokenizer(content);

        int numberOfObjects = 0;
        while (stringTokenizer.hasMoreElements()) {
            numberOfObjects++;
            stringTokenizer.nextElement();
        }

        indices = new int[numberOfObjects];
        stringTokenizer = new StringTokenizer(content);

        for (int i = 0; i < numberOfObjects; i++) {
            indices[i] = Integer.parseInt((String) stringTokenizer.nextElement());
        }
    }

//    public short[] getIndices() {
    public int[] getIndices() {
        return indices;
    }

    public int getCmd() {
        return cmd;
    }

    public int getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}