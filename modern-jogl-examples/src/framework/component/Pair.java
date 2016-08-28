/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package framework.component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GBarbieri
 */
public class Pair {

    private String name = "";
    private List<Integer> attributes = new ArrayList<>();

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public List<Integer> attributes() {
        return attributes;
    }

    public void attributes(ArrayList<Integer> attributes) {
        this.attributes = attributes;
    }

}
