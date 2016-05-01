/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author gbarbieri
 */
public class VAO {

    private String name;
    private ArrayList<Integer> attribList;

    public static VAO process(Element element) {

        VAO vao = new VAO();
        
        vao.attribList = new ArrayList<>();

        vao.name = element.getAttribute("name");

        NodeList children = element.getChildNodes();

        if (children != null && children.getLength() > 0) {

            for (int i = 0; i < children.getLength(); i++) {

                Node childNode = children.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element childElement = (Element) childNode;

//                    System.out.println("attrib: " + childElement.getAttribute("attrib"));

                    int attrib = Integer.parseInt(childElement.getAttribute("attrib"));

//                    System.out.println("found new attrib: " + attrib);

                    vao.attribList.add(attrib);
                }
            }
        }
        return vao;
    }
    
    private VAO() {        
    }

    public VAO(ArrayList<Integer> attribList) {

        name = "";
        
        this.attribList = attribList;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Integer> getAttribList() {
        return attribList;
    }
}