/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mesh;

import java.util.StringTokenizer;
import org.w3c.dom.Element;

/**
 *
 * @author gbarbieri
 */
public class Attribute {

    private int index;
    private String type;
    private int size;
    private float[] content;
    private int offset;

    public Attribute(Element element) {

        index = Integer.parseInt(element.getAttribute("index"));

        type = element.getAttribute("type");

        size = Integer.parseInt(element.getAttribute("size"));

//        System.out.println("index: " + index + " type: " + type + " size: " + size);

        String textContent = element.getTextContent();

        StringTokenizer stringTokenizer = new StringTokenizer(textContent);

        int numberOfObjects = 0;
        while (stringTokenizer.hasMoreElements()) {
            numberOfObjects++;
            stringTokenizer.nextElement();
        }

        content = new float[numberOfObjects];

        stringTokenizer = new StringTokenizer(textContent);

        for (int i = 0; i < numberOfObjects; i++) {
            String s = (String) stringTokenizer.nextElement();
//            System.out.println("s[" + i + "]: " + s);
            content[i] = Float.parseFloat(s);
        }
    }

    public float[] getContent() {
        return content;
    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}