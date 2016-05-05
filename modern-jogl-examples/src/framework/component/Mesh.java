/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import framework.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author gbarbieri
 */
public class Mesh {

    private ArrayList<Attribute> attribs = new ArrayList<>();
    private ArrayList<RenderCmd> primatives = new ArrayList<>();
    private ArrayList<RenderCmd> indexData = new ArrayList<>();
    private ArrayList<VAO> namedVaoList = new ArrayList<>();

    private IntBuffer vao = GLBuffers.newDirectIntBuffer(1), attribArraysBuffer = GLBuffers.newDirectIntBuffer(1),
            indexBuffer = GLBuffers.newDirectIntBuffer(1);

    public Mesh(String xml, GL3 gl3) {

        InputStream inputStream = getClass().getResourceAsStream(xml);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);
            Element rootElement = document.getDocumentElement();

            NodeList nodeList = rootElement.getElementsByTagName("attribute");
            if (nodeList != null && nodeList.getLength() > 0) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    attribs.add(new Attribute((Element) nodeList.item(i)));
                }
            } else {
                throw new Error("`mesh` node must have at least one `attribute` child. File: " + xml);
            }

            nodeList = rootElement.getElementsByTagName("vao");
            if (nodeList != null && nodeList.getLength() > 0) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
//                    VAO vao = VAO.process(element);
//                    namedVaoList.add(vao);
                }
            }

            nodeList = rootElement.getElementsByTagName("indices");
            if (nodeList != null && nodeList.getLength() > 0) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    RenderCmd cmd = RenderCmd.process((Element) nodeList.item(i), true);
                    primatives.add(cmd);
                    indexData.add(cmd);
                }
            }

            nodeList = rootElement.getElementsByTagName("arrays");
            if (nodeList != null && nodeList.getLength() > 0) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    primatives.add(RenderCmd.process((Element) nodeList.item(i), false));
                }
            }

            //Figure out how big of a buffer object for the attribute data we need.
            int attribBufferSize = 0, numElements = 0;
            int[] attribStartLocs = new int[attribs.size()];
            for (int iLoop = 0; iLoop < attribs.size(); iLoop++) {

                attribBufferSize = (attribBufferSize % 16 != 0)
                        ? (attribBufferSize + (16 - attribBufferSize % 16)) : attribBufferSize;

                attribStartLocs[iLoop] = attribBufferSize;
                Attribute attrib = attribs.get(iLoop);

                attribBufferSize += attrib.calcByteSize();

                if (numElements != 0) {
                    if (numElements != attrib.numElements()) {
                        throw new Error("Some of the attribute arrays have different element counts.");
                    }
                } else {
                    numElements = attrib.numElements();
                }
            }

            //Create the "Everything" VAO.
            gl3.glGenVertexArrays(1, vao);
            gl3.glBindVertexArray(vao.get(0));

            //Create the buffer object.
            gl3.glGenBuffers(1, attribArraysBuffer);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, attribArraysBuffer.get(0));
            gl3.glBufferData(GL_ARRAY_BUFFER, attribBufferSize, null, GL_STATIC_DRAW);

            //Fill in our data and set up the attribute arrays.
            for (int iLoop = 0; iLoop < attribs.size(); iLoop++) {
                Attribute attrib = attribs.get(iLoop);
                attrib.fillBoundBufferObject(gl3, attribStartLocs[iLoop]);
                attrib.setupAttributeArray(gl3, attribStartLocs[iLoop]);
            }

            //Fill the named VAOs.
            gl3.glBindVertexArray(0);

            //Get the size of our index buffer data.
            int indexBufferSize = 0;
            int[] indexStartLocs = new int[indexData.size()];
            for (int iLoop = 0; iLoop < indexData.size(); iLoop++) {

                indexBufferSize = (indexBufferSize % 16 != 0)
                        ? (indexBufferSize + (16 - indexBufferSize % 16)) : indexBufferSize;

                indexStartLocs[iLoop] = indexBufferSize;

                indexBufferSize += indexData.get(iLoop).calcByteSize();
            }

            //Create the index buffer object.
            if (indexBufferSize != 0) {

                gl3.glBindVertexArray(vao.get(0));

                gl3.glGenBuffers(1, indexBuffer);
                gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(0));
                gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, null, GL_STATIC_DRAW);

                //Fill with data.
                for (int iLoop = 0; iLoop < indexData.size(); iLoop++) {
                    indexData.get(iLoop).fillBoundBufferObject(gl3, indexStartLocs[iLoop]);
                }

                //Fill in indexed rendering commands.
                for (int iLoop = 0; iLoop < indexData.size(); iLoop++) {
                    RenderCmd prim = primatives.get(iLoop);
                    if (prim.isIndexedCmd) {
                        prim.start = indexStartLocs[iLoop] / indexData.get(iLoop).attribType.numBytes;
                        prim.elemCount = indexData.get(iLoop).dataArray.capacity() / indexData.get(iLoop).attribType.numBytes;
                        prim.indexDataType = indexData.get(iLoop).attribType.glType;
                    }
                }

                gl3.glBindVertexArray(0);
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(Mesh.class.getName()).log(Level.SEVERE, "Could not find the mesh file: " + xml, ex);
        }
    }

    public void render(GL3 gl3) {

        if (vao.get(0) == 0) {
            return;
        }

        gl3.glBindVertexArray(vao.get(0));
        for (RenderCmd renderCmd : primatives) {
            renderCmd.render(gl3);
        }
        gl3.glBindVertexArray(0);
    }

    public void render(GL3 gl3, String vaoName) {

    }

    public void dispose(GL3 gl3) {
        for (Attribute attrib : attribs) {
            BufferUtils.destroyDirectBuffer(attrib.getDataArray_());
        }
        for (RenderCmd cmd : indexData) {
            BufferUtils.destroyDirectBuffer(cmd.dataArray);
        }
        gl3.glDeleteBuffers(1, attribArraysBuffer);
        if (indexData.size() != 0) {
            gl3.glDeleteBuffers(1, indexBuffer);
        }
        gl3.glDeleteVertexArrays(1, vao);
    }
}
