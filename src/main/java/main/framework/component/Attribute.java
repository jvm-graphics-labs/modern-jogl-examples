/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BYTE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_HALF_FLOAT;
import static com.jogamp.opengl.GL.GL_SHORT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_INT;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import org.w3c.dom.Element;

/**
 *
 * @author gbarbieri
 */
public class Attribute {

    private int index = Integer.MAX_VALUE;
    private AttributeType attribType = null;
    private int size = -1;
    private boolean isIntegral = false;
    private ByteBuffer dataArray;

    public Attribute(Element element) {

        index = Integer.parseInt(element.getAttribute("index"));
        if (!((0 <= index) && (index < 16))) {
            throw new Error("Attribute index must be between 0 and 16 (" + index + ").");
        }

        size = Integer.parseInt(element.getAttribute("size"));
        if (!((1 <= size) && (size < 5))) {
            throw new Error("Attribute size must be between 1 and 4 (" + size + ").");
        }

        attribType = AttributeType.get(element.getAttribute("type"));

        isIntegral = false;
        String integralAttrib = element.getAttribute("integral");
        if (!integralAttrib.isEmpty()) {

            switch (integralAttrib) {
                case "true":
                    isIntegral = true;
                    break;
                case "false":
                    isIntegral = false;
                    break;
                default:
                    throw new Error("Incorrect 'integral' value for the 'attribute' (" + integralAttrib + ").");
            }
            if (attribType.normalized()) {
                throw new Error("Attribute cannot be both 'integral' and a normalized 'type'.");
            }
            if (attribType.glType() == GL_FLOAT
                    || attribType.glType() == GL_HALF_FLOAT
                    || attribType.glType() == GL_DOUBLE) {
                throw new Error("Attribute cannot be both 'integral' and a floating-point 'type'.");
            }
        }

//        System.out.println("index: " + index + " type: " + attribType.nameFromFile + " size: " + size);
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
        if (numberOfObjects % size != 0) {
            throw new Error("The attribute's data must be a multiple of its size in elements.");
        }

        dataArray = GLBuffers.newDirectByteBuffer(numberOfObjects * attribType.numBytes());

        stringTokenizer = new StringTokenizer(textContent);

        for (int i = 0; i < numberOfObjects; i++) {
            String s = (String) stringTokenizer.nextElement();
//            System.out.println("s[" + i + "]: " + s);
            switch (attribType.glType()) {
                case GL_FLOAT:
                case GL_HALF_FLOAT:
                    dataArray.putFloat(Float.parseFloat(s));
                    break;
                case GL_INT:
                case GL_UNSIGNED_INT:
                    dataArray.putInt(Integer.parseInt(s));
                    break;
                case GL_SHORT:
                case GL_UNSIGNED_SHORT:
                    dataArray.putShort(Short.parseShort(s));
                    break;
                case GL_BYTE:
                case GL_UNSIGNED_BYTE:
                    dataArray.put(Byte.parseByte(s));
                    break;
            }
        }
        dataArray.position(0);
    }

    public void fillBoundBufferObject(GL3 gl3, int offset) {
        attribType.writeToBuffer(gl3, GL_ARRAY_BUFFER, dataArray, offset);
    }

    public void setupAttributeArray(GL3 gl3, int offset) {
//        System.out.println("glEnableVertexAttribArray(" + index + ")");
        gl3.glEnableVertexAttribArray(index);
        if (isIntegral) {
            gl3.glVertexAttribIPointer(index, size, attribType.glType(), size * attribType.numBytes(), offset);
        } else {
//            System.out.println("glVertexAttribPointer(" + index + ", " + size + ", " + (attribType.glType == GL_FLOAT
//                    ? "GL_FLOAT" : attribType.glType) + ", " + attribType.normalized + ", "
//                    + (size * attribType.numBytes) + ", " + offset + ")");
            gl3.glVertexAttribPointer(index, size, attribType.glType(), attribType.normalized(),
                    size * attribType.numBytes(), offset);
        }
    }

    public int calcByteSize() {
        return dataArray.capacity();
    }

    public int numElements() {
        return dataArray.capacity() / size / attribType.numBytes();
    }

    public int index() {
        return index;
    }

    public ByteBuffer dataArray() {
        return dataArray;
    }
}
