package com.example.ddu_e_connect.domain.model;

public class ClubsModel {

    private String clubName;
    private String clubDescription;
    private boolean isExpanded;
    private int clubLogoResId;
    private String linkedInUrl;   // Added LinkedIn URL
    private String instagramUrl;   // Added Instagram URL
    private String websiteUrl;     // Added Website URL

    public ClubsModel(String clubName, String clubDescription, boolean isExpanded, int clubLogoResId, String linkedInUrl, String instagramUrl, String websiteUrl) {
        this.clubName = clubName;
        this.clubDescription = clubDescription;
        this.isExpanded = isExpanded;
        this.clubLogoResId = clubLogoResId;
        this.linkedInUrl = linkedInUrl;
        this.instagramUrl = instagramUrl; // Initialize Instagram URL
        this.websiteUrl = websiteUrl; // Initialize Website URL
    }

    public String getClubName() {
        return clubName;
    }

    public String getClubDescription() {
        return clubDescription;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public int getClubLogo() {
        return clubLogoResId;
    }

    public String getLinkedInUrl() {
        return linkedInUrl; // Getter for LinkedIn URL
    }

    public String getInstagramUrl() { // Getter for Instagram URL
        return instagramUrl;
    }

    public String getWebsiteUrl() { // Getter for Website URL
        return websiteUrl;
    }
}
