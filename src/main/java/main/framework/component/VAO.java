/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main.framework.component;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author gbarbieri
 */
public class VAO {

    public static void process(Element element, Pair pair) {

        pair.name(element.getAttribute("name"));

        NodeList children = element.getChildNodes();

        if (children != null && children.getLength() > 0) {

            for (int i = 0; i < children.getLength(); i++) {

                Node childNode = children.item(i);

                if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element childElement = (Element) childNode;

                    int attrib = Integer.parseInt(childElement.getAttribute("attrib"));

                    pair.attributes().add(attrib);
                }
            }
        }
    }
}
