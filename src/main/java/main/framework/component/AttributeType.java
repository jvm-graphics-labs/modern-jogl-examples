/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import com.jogamp.opengl.GL3;

import java.nio.ByteBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_INT;

/**
 *
 * @author elect
 */
public class AttributeType {

    private final String nameFromFile;
    private final boolean normalized;
    private final int glType;
    private final int numBytes;

    private AttributeType(String nameFromFile, boolean normalized, int glType, int numBytes) {
        this.nameFromFile = nameFromFile;
        this.normalized = normalized;
        this.glType = glType;
        this.numBytes = numBytes;
    }

    public void writeToBuffer(GL3 gl, int buffer, ByteBuffer theData, int offset) {
//        System.out.println("glBufferData(" + (buffer == GL_ARRAY_BUFFER ? "GL_ARRAY_BUFFER"
//                : buffer == GL_ELEMENT_ARRAY_BUFFER ? "GL_ELEMENT_ARRAY_BUFFER" : buffer)
//                + ", " + offset + ", " + theData.capacity() + ")");
        gl.glBufferSubData(buffer, offset, theData.capacity(), theData);
    }

    public static AttributeType get(String type) {
        for (AttributeType attributeType : allAttributeTypes) {
            if (type.equals(attributeType.nameFromFile)) {
                return attributeType;
            }
        }
        throw new Error("Unknown 'type' field (" + type + ").");
    }

    private static final AttributeType[] allAttributeTypes = {
        new AttributeType("float", false, GL_FLOAT, Float.BYTES),
        new AttributeType("half", false, GL_HALF_FLOAT, Float.BYTES / 2),
        new AttributeType("int", false, GL_INT, Integer.BYTES),
        new AttributeType("uint", false, GL_UNSIGNED_INT, Integer.BYTES),
        new AttributeType("norm-int", true, GL_INT, Integer.BYTES),
        new AttributeType("norm-uint", true, GL_UNSIGNED_INT, Integer.BYTES),
        new AttributeType("short", false, GL_SHORT, Short.BYTES),
        new AttributeType("ushort", false, GL_UNSIGNED_SHORT, Short.BYTES),
        new AttributeType("norm-short", true, GL_SHORT, Short.BYTES),
        new AttributeType("norm-ushort", true, GL_UNSIGNED_SHORT, Short.BYTES),
        new AttributeType("byte", false, GL_BYTE, Byte.BYTES),
        new AttributeType("ubyte", false, GL_UNSIGNED_BYTE, Byte.BYTES),
        new AttributeType("norm-byte", true, GL_BYTE, Byte.BYTES),
        new AttributeType("norm-ubyte", true, GL_UNSIGNED_BYTE, Byte.BYTES)};
    
    public boolean normalized() {
        return normalized;
    }
    
    public int glType() {
        return glType;
    }
    
    public int numBytes() {
        return numBytes;
    }
}
