package com.example.wine.model;

public class Incident {
    private String id;
    private String title;
    private String summary;
    private String status;

    public Incident(String id, String title, String summary, String status) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.status = status;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
}
