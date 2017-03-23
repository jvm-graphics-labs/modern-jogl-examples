/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import one.util.streamex.IntStreamEx;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jogamp.opengl.GL.*;
import static uno.buffer.UtilKt.destroyBuffer;

/**
 *
 * @author gbarbieri
 */
public class Mesh {

    private List<Attribute> attribs = new ArrayList<>();
    private List<RenderCmd> primatives = new ArrayList<>();
    private List<RenderCmd> indexData = new ArrayList<>();
    private List<Pair> namedVaoList = new ArrayList<>();

    private IntBuffer vao = GLBuffers.newDirectIntBuffer(1),
            attribArraysBuffer = GLBuffers.newDirectIntBuffer(1),
            indexBuffer = GLBuffers.newDirectIntBuffer(1);

    private HashMap<String, Integer> namedVAOs = new HashMap<>();

    public Mesh(GL3 gl, Class context, String xml) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {

        String xml_ = xml;
        if(!xml.startsWith("/"))
            xml_ = "/" + xml;
        URI uri = context.getResource(xml_).toURI();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(uri.toString());
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
                namedVaoList.add(new Pair());
                VAO.process(element, namedVaoList.get(namedVaoList.size() - 1));
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
        for (int loop = 0; loop < attribs.size(); loop++) {

//            attribBufferSize = (attribBufferSize % 16 != 0)
//                    ? (attribBufferSize + (16 - attribBufferSize % 16)) : attribBufferSize;
            attribStartLocs[loop] = attribBufferSize;
            Attribute attrib = attribs.get(loop);

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
        gl.glGenVertexArrays(1, vao);
        gl.glBindVertexArray(vao.get(0));

        //Create the buffer object.
        gl.glGenBuffers(1, attribArraysBuffer);
        gl.glBindBuffer(GL_ARRAY_BUFFER, attribArraysBuffer.get(0));
        gl.glBufferData(GL_ARRAY_BUFFER, attribBufferSize, null, GL_STATIC_DRAW);

        //Fill in our data and set up the attribute arrays.
        for (int loop = 0; loop < attribs.size(); loop++) {
            Attribute attrib = attribs.get(loop);
            attrib.fillBoundBufferObject(gl, attribStartLocs[loop]);
            attrib.setupAttributeArray(gl, attribStartLocs[loop]);
        }

        //Fill the named VAOs.
        int vaoBackup = vao.get(0);
        for (Pair namedVao : namedVaoList) {

            vao.put(0, -1);
            gl.glGenVertexArrays(1, vao);
            gl.glBindVertexArray(vao.get(0));

            for (Integer attribIx : namedVao.attributes()) {

                int attribOffset = -1;

                for (int count = 0; count < attribs.size(); count++) {

                    if (attribs.get(count).index() == attribIx) {

                        attribOffset = count;
                        break;
                    }
                }
                attribs.get(attribOffset).setupAttributeArray(gl, attribStartLocs[attribOffset]);
            }
            namedVAOs.put(namedVao.name(), vao.get(0));
        }
        vao.put(0, vaoBackup);

        gl.glBindVertexArray(0);

        //Get the size of our index buffer data.
        int indexBufferSize = 0;
        int[] indexStartLocs = new int[indexData.size()];
        for (int loop = 0; loop < indexData.size(); loop++) {

            indexBufferSize = (indexBufferSize % 16 != 0)
                    ? (indexBufferSize + (16 - indexBufferSize % 16)) : indexBufferSize;

            indexStartLocs[loop] = indexBufferSize;

            indexBufferSize += indexData.get(loop).calcByteSize();
        }

        //Create the index buffer object.
        if (indexBufferSize != 0) {

            gl.glBindVertexArray(vao.get(0));

            gl.glGenBuffers(1, indexBuffer);
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(0));
            gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, null, GL_STATIC_DRAW);

            //Fill with data.
            IntStreamEx.range(indexData.size()).forEach(i
                    -> indexData.get(i).fillBoundBufferObject(gl, indexStartLocs[i]));

            //Fill in indexed rendering commands.
            for (int loop = 0; loop < indexData.size(); loop++) {
                RenderCmd prim = primatives.get(loop);
                if (prim.isIndexedCmd()) {
                    prim.start(indexStartLocs[loop] / indexData.get(loop).attribType().numBytes());
                    prim.elemCount(indexData.get(loop).dataArray().capacity()
                            / indexData.get(loop).attribType().numBytes());
                    prim.indexDataType(indexData.get(loop).attribType().glType());
                }
            }

            vaoBackup = vao.get(0);
            namedVAOs.entrySet().forEach(entry -> {
                vao.put(0, entry.getValue());
                gl.glBindVertexArray(vao.get(0));
                gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(0));
            });
            vao.put(0, vaoBackup);

            gl.glBindVertexArray(0);
        }
    }

    public void render(GL3 gl) {

        if (vao.get(0) == 0) {
            return;
        }

        gl.glBindVertexArray(vao.get(0));
        primatives.forEach(renderCmd -> renderCmd.render(gl));
        gl.glBindVertexArray(0);
    }

    public void render(GL3 gl, String meshName) {

        if (!namedVAOs.containsKey(meshName)) {
            return;
        }

        gl.glBindVertexArray(namedVAOs.get(meshName));
        primatives.forEach(renderCmd -> renderCmd.render(gl));
        gl.glBindVertexArray(0);
    }

    public void dispose(GL3 gl) {

        attribs.forEach(attrib -> destroyBuffer(attrib.dataArray()));
        indexData.forEach(cmd -> destroyBuffer(cmd.dataArray()));
        
        gl.glDeleteBuffers(1, attribArraysBuffer);
        if (!indexData.isEmpty()) {
            gl.glDeleteBuffers(1, indexBuffer);
        }

        gl.glDeleteVertexArrays(1, vao);
        namedVAOs.forEach((s, i) -> {
            vao.put(0, i);
            gl.glDeleteVertexArrays(1, vao);
        });
    }

    public List<Attribute> attribs() {
        return attribs;
    }

    public List<RenderCmd> primatives() {
        return primatives;
    }

    public List<RenderCmd> indexData() {
        return indexData;
    }
}
