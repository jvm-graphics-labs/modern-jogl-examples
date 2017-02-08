/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_LINE_LOOP;
import static com.jogamp.opengl.GL.GL_LINE_STRIP;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_TRIANGLE_FAN;
import static com.jogamp.opengl.GL.GL_TRIANGLE_STRIP;

/**
 *
 * @author elect
 */
public class PrimitiveType {

    private String primitiveName;
    private int glPrimType;

    public PrimitiveType(String primitiveName, int glPrimType) {
        this.primitiveName = primitiveName;
        this.glPrimType = glPrimType;
    }

    public static PrimitiveType get(String type) {
        for (PrimitiveType primitiveType : allPrimitiveTypes) {
            if (type.equals(primitiveType.primitiveName)) {
                return primitiveType;
            }
        }
        throw new Error("Unknown 'cmd' field (" + type + ").");
    }

    private static PrimitiveType[] allPrimitiveTypes = {
        new PrimitiveType("triangles", GL_TRIANGLES),
        new PrimitiveType("tri-strip", GL_TRIANGLE_STRIP),
        new PrimitiveType("tri-fan", GL_TRIANGLE_FAN),
        new PrimitiveType("lines", GL_LINES),
        new PrimitiveType("line-strip", GL_LINE_STRIP),
        new PrimitiveType("line-loop", GL_LINE_LOOP),
        new PrimitiveType("points", GL_POINTS)};
    
    public int glPrimType() {
        return glPrimType;
    }
}
