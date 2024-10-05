package com.example.ddu_e_connect.model;

public class FolderModel {
    private String name;
    private boolean isPdf; // Flag to determine if it's a PDF

    public FolderModel(String name, boolean isPdf) {
        this.name = name;
        this.isPdf = isPdf;
    }

    public String getName() {
        return name;
    }

    public boolean isPdf() {
        return isPdf; // Getter for isPdf
    }
}
