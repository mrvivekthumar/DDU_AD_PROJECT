package com.example.ddu_e_connect;

public class Model {

    private String clubName;
    private String clubDescription;
    private boolean isExpanded;
    private int clubLogoResId;

    public Model(String clubName, String clubDescription, boolean isExpanded, int clubLogoResId) {
        this.clubName = clubName;
        this.clubDescription = clubDescription;
        this.isExpanded = isExpanded;
        this.clubLogoResId = clubLogoResId;
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
}

