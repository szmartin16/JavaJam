package com.javaJam.view;

import com.javaJam.controller.SettingsController;
import com.javaJam.service.Globals;

import javax.swing.*;
import java.awt.*;

import static com.javaJam.service.Globals.*;
import static com.javaJam.service.Theme.*;

public class SettingsView extends JPanel {

    private SettingsController settingsController;
    private JButton backButton;
    private JTextField cutoffFrontField;
    private JTextField cutoffBackField;
    private JComboBox<String> videoBox;


    public SettingsView() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Header Panel with Back Button and Title
        JPanel headerWrapper = new JPanel();
        headerWrapper.setLayout(new BoxLayout(headerWrapper, BoxLayout.Y_AXIS));
        headerWrapper.setOpaque(false);

        backButton = new JButton("Back");
        styleButton(backButton);
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);


        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerWrapper.add(backButton);
        headerWrapper.add(Box.createVerticalStrut(15));
        headerWrapper.add(titleLabel);

        add(headerWrapper, BorderLayout.NORTH);

        //Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_DARK);

        contentPanel.add(createSectionHeader("Playback & Video"));
        contentPanel.add(Box.createVerticalStrut(10));


        videoBox = new JComboBox<>(new String[]{"Force Maximum", "1080p", "720p", "Off"});
        videoBox.setSelectedItem(Globals.getQuality());
        styleComboBox(videoBox);
        contentPanel.add(createSettingRow("Video Quality", videoBox));
        contentPanel.add(Box.createVerticalStrut(15));


        cutoffFrontField = new JTextField(String.valueOf(Globals.getCutoffFront()));
        styleTextField(cutoffFrontField);
        contentPanel.add(createSettingRow("Video Cutoff Front (sec)", cutoffFrontField));
        contentPanel.add(Box.createVerticalStrut(15));


        cutoffBackField = new JTextField(String.valueOf(Globals.getCutoffBack()));
        styleTextField(cutoffBackField);
        contentPanel.add(createSettingRow("Video Cutoff Back (sec)", cutoffBackField));
        contentPanel.add(Box.createVerticalStrut(30));

        //About
        contentPanel.add(createSectionHeader("About JavaJam"));
        contentPanel.add(Box.createVerticalStrut(10));


        JLabel versionLabel = new JLabel("Current Version: " + VERSION_NUMBER);
        versionLabel.setForeground(TEXT_LIGHT);
        contentPanel.add(versionLabel);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JLabel createSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    private JPanel createSettingRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(20, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 45));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_LIGHT);
        label.setPreferredSize(new Dimension(300, 30));

        row.add(label, BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private void styleButton(JButton button) {
        button.setBackground(BUTTON_DARK);
        button.setForeground(TEXT_LIGHT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setBackground(BUTTON_DARK);
        box.setForeground(TEXT_LIGHT);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(BUTTON_DARK);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public void setSettingsController(SettingsController settingsController) {
        this.settingsController = settingsController;
    }

    public JButton getBackButton()
    {
        return backButton;
    }
    public JTextField getCutoffFrontField() {return cutoffFrontField;}
    public JTextField getCutoffBackField() {return cutoffBackField;}
    public JComboBox<String> getVideoBox() {return videoBox;}

}