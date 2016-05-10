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
import java.util.HashMap;
import java.util.Map;
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
    private ArrayList<Pair> namedVaoList = new ArrayList<>();

    private IntBuffer vao = GLBuffers.newDirectIntBuffer(1), attribArraysBuffer = GLBuffers.newDirectIntBuffer(1),
            indexBuffer = GLBuffers.newDirectIntBuffer(1);

    private HashMap<String, Integer> namedVAOs = new HashMap<>();

    public Mesh(String xml, GL3 gl3) throws ParserConfigurationException, SAXException, IOException {

        InputStream inputStream = getClass().getResourceAsStream(xml);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

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
        gl3.glGenVertexArrays(1, vao);
        gl3.glBindVertexArray(vao.get(0));

        //Create the buffer object.
        gl3.glGenBuffers(1, attribArraysBuffer);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, attribArraysBuffer.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, attribBufferSize, null, GL_STATIC_DRAW);

        //Fill in our data and set up the attribute arrays.
        for (int loop = 0; loop < attribs.size(); loop++) {
            Attribute attrib = attribs.get(loop);
            attrib.fillBoundBufferObject(gl3, attribStartLocs[loop]);
            attrib.setupAttributeArray(gl3, attribStartLocs[loop]);
        }

        //Fill the named VAOs.
        int vaoBackup = vao.get(0);
        for (Pair namedVao : namedVaoList) {

            vao.put(0, -1);
            gl3.glGenVertexArrays(1, vao);
            gl3.glBindVertexArray(vao.get(0));

            for (Integer attribIx : namedVao.getAttributes()) {

                int attribOffset = -1;

                for (int count = 0; count < attribs.size(); count++) {

                    if (attribs.get(count).index == attribIx) {

                        attribOffset = count;
                        break;
                    }
                }
                attribs.get(attribOffset).setupAttributeArray(gl3, attribStartLocs[attribOffset]);
            }
            namedVAOs.put(namedVao.getName(), vao.get(0));
        }
        vao.put(0, vaoBackup);

        gl3.glBindVertexArray(0);

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

            gl3.glBindVertexArray(vao.get(0));

            gl3.glGenBuffers(1, indexBuffer);
            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(0));
            gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, null, GL_STATIC_DRAW);

            //Fill with data.
            for (int loop = 0; loop < indexData.size(); loop++) {
                indexData.get(loop).fillBoundBufferObject(gl3, indexStartLocs[loop]);
            }

            //Fill in indexed rendering commands.
            for (int loop = 0; loop < indexData.size(); loop++) {
                RenderCmd prim = primatives.get(loop);
                if (prim.isIndexedCmd) {
                    prim.start = indexStartLocs[loop] / indexData.get(loop).attribType.numBytes;
                    prim.elemCount = indexData.get(loop).dataArray.capacity() / indexData.get(loop).attribType.numBytes;
                    prim.indexDataType = indexData.get(loop).attribType.glType;
                }
            }

            vaoBackup = vao.get(0);
            for (Map.Entry<String, Integer> entry : namedVAOs.entrySet()) {
                vao.put(0, entry.getValue());
                gl3.glBindVertexArray(vao.get(0));
                gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.get(0));
            }
            vao.put(0, vaoBackup);

            gl3.glBindVertexArray(0);
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

    public void render(GL3 gl3, String meshName) {

        if (!namedVAOs.containsKey(meshName)) {
            return;
        }

        gl3.glBindVertexArray(namedVAOs.get(meshName));
        for (RenderCmd renderCmd : primatives) {
            renderCmd.render(gl3);
        }
        gl3.glBindVertexArray(0);
    }

    public void dispose(GL3 gl3) {
        for (Attribute attrib : attribs) {
            BufferUtils.destroyDirectBuffer(attrib.getDataArray());
        }
        for (RenderCmd cmd : indexData) {
            BufferUtils.destroyDirectBuffer(cmd.dataArray);
        }
        gl3.glDeleteBuffers(1, attribArraysBuffer);
        if (!indexData.isEmpty()) {
            gl3.glDeleteBuffers(1, indexBuffer);
        }
        gl3.glDeleteVertexArrays(1, vao);
    }

    public ArrayList<Attribute> getAttribs() {
        return attribs;
    }

    public ArrayList<RenderCmd> getPrimatives() {
        return primatives;
    }

    public ArrayList<RenderCmd> getIndexData() {
        return indexData;
    }
}
