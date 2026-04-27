package hk.edu.hku.cs7506.smartcompanion.data.model;

import java.io.Serializable;

public class RouteStep implements Serializable {
    private final String title;
    private final String body;

    public RouteStep(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}

