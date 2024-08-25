package com.example.ddu_e_connect.model;

public class PdfModel {
    private String name;
    private String url;

    public PdfModel() { }

    public PdfModel(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
