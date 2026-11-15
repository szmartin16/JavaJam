package com.javaJam.controller;

import com.javaJam.view.MusicView;
import com.javaJam.view.SettingsView;
import com.javaJam.service.Globals;

public class SettingsController {

    private final SettingsView settingsView;
    private final MusicView musicView;

    public SettingsController(MusicView musicView, SettingsView settingsView) {
        this.musicView = musicView;
        this.settingsView = settingsView;
        loadCurrentSettings();
        startUp();
    }

    private void loadCurrentSettings() {
        settingsView.getCutoffFrontField().setText(String.valueOf(Globals.getCutoffFront()));
        settingsView.getCutoffBackField().setText(String.valueOf(Globals.getCutoffBack()));
        settingsView.getVideoBox().setSelectedItem(Globals.getQuality());
    }

    private void startUp() {
        settingsView.getBackButton().addActionListener(e -> onBack());
        settingsView.getCutoffFrontField().addActionListener(e -> saveFrontCutoff());
        settingsView.getCutoffBackField().addActionListener(e -> saveBackCutoff());
        settingsView.getVideoBox().addActionListener(e -> saveQuality());
    }

    private void saveFrontCutoff() {
        try {
            String text = settingsView.getCutoffFrontField().getText().trim();
            if (!text.isEmpty()) {
                int val = Math.max(0, Integer.parseInt(text)); // Prevent negative cutoffs
                Globals.PREFS.putInt("CUTOFF_FRONT", val);
            }
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] Invalid number format in Cutoff Front field.");
        }
    }

    private void saveBackCutoff() {
        try {
            String text = settingsView.getCutoffBackField().getText().trim();
            if (!text.isEmpty()) {
                int val = Math.max(0, Integer.parseInt(text));
                Globals.PREFS.putInt("CUTOFF_BACK", val);
            }
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] Invalid number format in Cutoff Back field.");
        }
    }

    private void saveQuality() {
        Object selected = settingsView.getVideoBox().getSelectedItem();
        if (selected != null) {
            Globals.PREFS.put("QUALITY", selected.toString());
        }
    }

    private void onBack() {
        saveFrontCutoff();
        saveBackCutoff();
        saveQuality();
        musicView.showMainView();
    }
}