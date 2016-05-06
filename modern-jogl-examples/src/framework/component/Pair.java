/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import java.util.ArrayList;

/**
 *
 * @author GBarbieri
 */
public class Pair {

    private String name = "";
    private ArrayList<Integer> attributes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Integer> getAttributes() {
        return attributes;
    }

    public void setAttributes(ArrayList<Integer> attributes) {
        this.attributes = attributes;
    }

    
}
