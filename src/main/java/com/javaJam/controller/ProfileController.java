package com.javaJam.controller;

import com.javaJam.view.MusicView;
import com.javaJam.view.ProfileView;
import com.javaJam.view.SettingsView;

public class ProfileController {

    private ProfileView profileView;
    private MusicView musicView;


    public ProfileController(MusicView musicView, ProfileView profileView) {
        this.musicView = musicView;
        this.profileView = profileView;
        startUp();
    }

    private void startUp() {
        if (profileView.getBackButton() != null) {
            System.out.println("Back button successfully bound!");
            profileView.getBackButton().addActionListener(e -> onBack());
        } else {
            System.err.println("ERROR: Back button is NULL!");
        }
    }


    private void onBack()
    {
        System.out.println("back pressed");
        musicView.showMainView();
    }

}
