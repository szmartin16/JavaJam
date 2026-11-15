package com.javaJam.controller;

import com.formdev.flatlaf.FlatDarkLaf;
import com.javaJam.service.FontLoader;
import com.javaJam.service.UpdateChecker;
import com.javaJam.view.MusicView;
import com.javaJam.view.ProfileView;
import com.javaJam.view.SettingsView;
import com.javaJam.view.UpdateView;

import javax.swing.*;
import java.awt.*;


public class MainController {


    private MusicView musicView;
    private SettingsView settingsView;
    private ProfileView profileView;
    private MusicController musicController;
    private SettingsController settingsController;
    private ProfileController profileController;


    public MainController() {

        Font customAppFont = FontLoader.loadResourceFont("/fonts/DynaPuff.ttf", 13f);

        UIManager.put("defaultFont", customAppFont);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);

        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        musicView = new MusicView();
        settingsView = new SettingsView();
        profileView = new ProfileView();
        musicController = new MusicController(musicView,settingsView,profileView);
        settingsController = new SettingsController(musicView, settingsView);
        profileController = new ProfileController(musicView,profileView);
    }


    public void start() {
        UpdateChecker.checkForUpdates(
                () -> {
                    musicView.setVisible(true);
                },
                (latestVersion) -> {
                    UpdateView updateView = new UpdateView(latestVersion);
                    updateView.setVisible(true);
                    updateView.setController(this);
                }
        );
    }

    public void shutdown() {
        musicController.shutdown();
    }
}