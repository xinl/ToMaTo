package edu.upenn.cis.tomato.data;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import com.ibm.wala.cast.js.translator.RangePosition;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;

import edu.upenn.cis.tomato.data.ViolationSite.ViolationTypes;

// Contains all the information related to a violation site in an externally referenced JavaScript library

public class ViolationSite implements Serializable {

    private static final long serialVersionUID = 5819064120096621229L;
    // Violation Type
    public ViolationTypes type;
    // Used to encode detailed information for different violation types;
    public String description;
    // URL
    private URL url;
    private Position position;

    public static enum ViolationTypes {
        FunctionInvocation;
    }

    public ViolationSite() {
    }

    public ViolationSite(URL url, Position position) {
        this.url = url;
        this.position = position;
    }

    public ViolationTypes getType() {
        return type;
    }

    public void setType(ViolationTypes type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Position getSite() {
        return position;
    }

    public void setSite(Position site) {
        this.position = site;
    }

    public void printViolationSite() {
        System.out.print("[Type] " + this.type);
        System.out.println(" --- [Description] " + this.description);
        System.out.println("[URL] " + this.url);
        System.out.println("[Position] " + this.position.getFirstOffset() + "-" + this.position.getLastOffset());
        System.out.println("");
    }
}
