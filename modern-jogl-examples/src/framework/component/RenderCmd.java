/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import static com.jogamp.opengl.GL.GL_BYTE;
import static com.jogamp.opengl.GL.GL_SHORT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_INT;
import com.jogamp.opengl.GL3;
import org.w3c.dom.Element;

/**
 *
 * @author elect
 */
public class RenderCmd {

    private boolean isIndexedCmd;
    private int primType;
    private int start;
    private int elemCount;
    private int indexDataType;  //Only if isIndexedCmd is true.
    private int primRestart;    //Only if isIndexedCmd is true.

    private RenderCmd() {
    }

    public static RenderCmd process(Element element, boolean indices) {

        RenderCmd cmd = new RenderCmd();

        String cmdName = element.getAttribute("cmd");
        PrimitiveType primitiveType = PrimitiveType.get(cmdName);

        cmd.primType = primitiveType.getGlPrimType();

        if (indices) {

            cmd.isIndexedCmd = true;
            String primRestartString = element.getAttribute("prim-restart");
            cmd.primRestart = primRestartString.isEmpty() ? -1 : Integer.parseInt(primRestartString);
            String indexDataTypeString = element.getAttribute("type");
            switch (indexDataTypeString) {
                case "byte":
                    cmd.indexDataType = GL_BYTE;
                    break;
                case "ubyte":
                    cmd.indexDataType = GL_UNSIGNED_BYTE;
                    break;
                case "short":
                    cmd.indexDataType = GL_SHORT;
                    break;
                case "ushort":
                    cmd.indexDataType = GL_UNSIGNED_SHORT;
                    break;
                case "int":
                    cmd.indexDataType = GL_INT;
                    break;
                case "uint":
                    cmd.indexDataType = GL_UNSIGNED_INT;
                    break;
                default:
                    throw new Error("index data type unknown");
            }

        } else {

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
        return cmd;
    }

    public void render(GL3 gl3) {
        if (isIndexedCmd) {
            gl3.glDrawElements(primType, elemCount, indexDataType, start);
        } else {
            gl3.glDrawArrays(primType, start, elemCount);
        }
    }

}
