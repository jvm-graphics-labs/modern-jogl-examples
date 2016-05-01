/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import static com.jogamp.opengl.GL.GL_BYTE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_HALF_FLOAT;
import static com.jogamp.opengl.GL.GL_SHORT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_INT;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.ByteBuffer;
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
    private boolean isIntegral;
    private float[] dataArray;
    private ByteBuffer dataArray_;
    private int offset;

    private Attribute() {
    }

    public static Attribute create(Element element) {

        Attribute attribute = new Attribute();

        attribute.index = Integer.parseInt(element.getAttribute("index"));
        if (!((0 <= attribute.index) && (attribute.index < 16))) {
            throw new Error("Attribute index must be between 0 and 16 (" + attribute.index + ").");
        }

        attribute.size = Integer.parseInt(element.getAttribute("size"));
        if (!((1 <= attribute.size) && (attribute.size < 5))) {
            throw new Error("Attribute size must be between 1 and 4 (" + attribute.size + ").");
        }

        AttributeType attributeType = AttributeType.get(element.getAttribute("type"));
        attribute.type = attributeType.nameFromFile;

        attribute.isIntegral = false;
        String integralAttrib = element.getAttribute("integral");
        if (!integralAttrib.isEmpty()) {

            switch (integralAttrib) {
                case "true":
                    attribute.isIntegral = true;
                    break;
                case "false":
                    attribute.isIntegral = false;
                    break;
                default:
                    throw new Error("Incorrect 'integral' value for the 'attribute' (" + integralAttrib + ").");
            }
            if (attributeType.normalized) {
                throw new Error("Attribute cannot be both 'integral' and a normalized 'type'.");
            }
            if (attributeType.glType == GL_FLOAT
                    || attributeType.glType == GL_HALF_FLOAT
                    || attributeType.glType == GL_DOUBLE) {
                throw new Error("Attribute cannot be both 'integral' and a floating-point 'type'.");
            }
        }

//        System.out.println("index: " + index + " type: " + type + " size: " + size);
        String textContent = element.getTextContent();

        StringTokenizer stringTokenizer = new StringTokenizer(textContent);

        int numberOfObjects = 0;
        while (stringTokenizer.hasMoreElements()) {
            numberOfObjects++;
            stringTokenizer.nextElement();
        }
        if (numberOfObjects == 0) {
            throw new Error("The attribute must have an array of values.");
        }
        if (numberOfObjects % attribute.size != 0) {
            throw new Error("The attribute's data must be a multiple of its size in elements.");
        }

        attribute.dataArray_ = GLBuffers.newDirectByteBuffer(numberOfObjects * attributeType.numBytes);
        attribute.dataArray = new float[numberOfObjects];

        stringTokenizer = new StringTokenizer(textContent);

        for (int i = 0; i < numberOfObjects; i++) {
            String s = (String) stringTokenizer.nextElement();
//            System.out.println("s[" + i + "]: " + s);
            switch (attributeType.glType) {
                case GL_FLOAT:
                case GL_HALF_FLOAT:
                    attribute.dataArray_.putFloat(Float.parseFloat(s));
                    attribute.dataArray[i] = Float.parseFloat(s);
                    break;
                case GL_INT:
                case GL_UNSIGNED_INT:
                    attribute.dataArray_.putInt(Integer.parseInt(s));
                    break;
                case GL_SHORT:
                case GL_UNSIGNED_SHORT:
                    attribute.dataArray_.putShort(Short.parseShort(s));
                    break;
                case GL_BYTE:
                case GL_UNSIGNED_BYTE:
                    attribute.dataArray_.put(Byte.parseByte(s));
                    break;
            }
        }
        attribute.dataArray_.position(0);
        return attribute;
    }

    public Attribute(Element element) {

        index = Integer.parseInt(element.getAttribute("index"));
//        if(a)

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

        dataArray = new float[numberOfObjects];

        stringTokenizer = new StringTokenizer(textContent);

        for (int i = 0; i < numberOfObjects; i++) {
            String s = (String) stringTokenizer.nextElement();
//            System.out.println("s[" + i + "]: " + s);
            dataArray[i] = Float.parseFloat(s);
        }
    }

    public float[] getContent() {
        return dataArray;
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

    public ByteBuffer getDataArray_() {
        return dataArray_;
    }
}
