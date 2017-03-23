/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import org.w3c.dom.Element;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import static com.jogamp.opengl.GL.*;

/**
 *
 * @author elect
 */
public class RenderCmd {

    private boolean isIndexedCmd;
    private int primType;
    private int start;
    private int elemCount;
    //Only if isIndexedCmd is true.
    private int indexDataType;
    private int primRestart;
    private AttributeType attribType;
    private ByteBuffer dataArray;

    private RenderCmd() {
    }

    public static RenderCmd process(Element element, boolean indices) {

        RenderCmd cmd = new RenderCmd();

        String cmdName = element.getAttribute("cmd");
        PrimitiveType primitiveType = PrimitiveType.get(cmdName);

        cmd.primType = primitiveType.glPrimType();

        if (indices) {
            processIndices(element, cmd);
        } else {
            processArrays(element, cmd);
        }
        return cmd;
    }

    private static void processIndices(Element element, RenderCmd cmd) {

        cmd.isIndexedCmd = true;
        String primRestartString = element.getAttribute("prim-restart");
        cmd.primRestart = primRestartString.isEmpty() ? -1 : Integer.parseInt(primRestartString);

        String stringType = element.getAttribute("type");
        if (!stringType.equals("uint") && !stringType.equals("ushort") && !stringType.equals("ubyte")) {
            throw new Error("Improper 'type' attribute value on 'index' element (" + stringType + ").");
        }

        cmd.attribType = AttributeType.get(stringType);
//        System.out.println("index, attribType: " + cmd.attribType.nameFromFile);
        //Read the text
        String textContent = element.getTextContent();

        StringTokenizer stringTokenizer = new StringTokenizer(textContent);

        int numberOfObjects = 0;
        while (stringTokenizer.hasMoreElements()) {
            numberOfObjects++;
            stringTokenizer.nextElement();
        }
        if (numberOfObjects == 0) {
            throw new Error("The index element must have an array of values.");
        }

        cmd.dataArray = GLBuffers.newDirectByteBuffer(numberOfObjects * cmd.attribType.numBytes());

        stringTokenizer = new StringTokenizer(textContent);

        for (int i = 0; i < numberOfObjects; i++) {
            String s = (String) stringTokenizer.nextElement();
//            System.out.println("s[" + i + "]: " + s);
            switch (cmd.attribType.glType()) {
                case GL_UNSIGNED_INT:
                    cmd.dataArray.putInt(Integer.parseInt(s));
                    break;
                case GL_UNSIGNED_SHORT:
                    cmd.dataArray.putShort(Short.parseShort(s));
                    break;
                case GL_UNSIGNED_BYTE:
                    cmd.dataArray.put(Byte.parseByte(s));
                    break;
            }
        }
        cmd.dataArray.position(0);
    }

    private static void processArrays(Element element, RenderCmd cmd) {

        cmd.isIndexedCmd = false;
        cmd.start = Integer.parseInt(element.getAttribute("start"));
        if (cmd.start < 0) {
            throw new Error("`array` 'start' index must be between 0 or greater.");
        }
        cmd.elemCount = Integer.parseInt(element.getAttribute("count"));
        if (cmd.elemCount < 0) {
            throw new Error("`array` 'count' must be between 0 or greater.");
        }
    }

    public void fillBoundBufferObject(GL3 gl, int offset) {
        attribType.writeToBuffer(gl, GL_ELEMENT_ARRAY_BUFFER, dataArray, offset);
    }

    public int calcByteSize() {
        return dataArray.capacity();
    }

    public void render(GL3 gl) {
        if (isIndexedCmd) {
//            System.out.println("glDrawElements(" + (primType == GL_TRIANGLE_FAN ? "GL_TRIANGLE_FAN"
//                    : primType == GL_TRIANGLE_STRIP ? "GL_TRIANGLE_STRIP" : primType) + ", " + elemCount + ", "
//                    + (indexDataType == GL_UNSIGNED_SHORT ? "GL_UNSIGNED_SHORT" : indexDataType) + ", "
//                    + (start * attribType.numBytes) + ")");
            gl.glDrawElements(primType, elemCount, indexDataType, start * attribType.numBytes());
        } else {
            gl.glDrawArrays(primType, start, elemCount);
        }
    }

    public boolean isIndexedCmd() {
        return isIndexedCmd;
    }

    public void start(int start) {
        this.start = start;
    }

    public void elemCount(int elemCount) {
        this.elemCount = elemCount;
    }
    
    public ByteBuffer dataArray() {
        return dataArray;
    }
    
    public AttributeType attribType() {
        return attribType;
    }
    
    public void indexDataType(int indexDataType) {
        this.indexDataType = indexDataType;
    }
}
