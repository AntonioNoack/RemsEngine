package org.apache.logging.log4j;

public class MarkerManager {
    static Marker marker = new Marker();
    public static Marker getMarker(String name){
        return marker;
    }
}
