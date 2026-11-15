package com.javaJam.view;

import javax.swing.*;
import java.awt.*;

import static com.javaJam.service.Globals.DEFAULT_USER;
import static com.javaJam.service.Theme.*;

public class ProfileView extends JPanel {


    private JButton backButton;

    public ProfileView() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel headerWrapper = new JPanel();
        headerWrapper.setLayout(new BoxLayout(headerWrapper, BoxLayout.Y_AXIS));
        headerWrapper.setOpaque(false);

        backButton = new JButton("Back");
        styleButton(backButton);
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);


        JLabel titleLabel = new JLabel("User Profile");
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 24f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerWrapper.add(backButton);
        headerWrapper.add(Box.createVerticalStrut(15));
        headerWrapper.add(titleLabel);

        add(headerWrapper, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_DARK);

        //Account Info
        contentPanel.add(createSectionHeader("Account Details"));
        contentPanel.add(Box.createVerticalStrut(10));

        JTextField usernameField = new JTextField(DEFAULT_USER);
        styleTextField(usernameField);
        contentPanel.add(createProfileRow("Username", usernameField));
        contentPanel.add(Box.createVerticalStrut(15));

        JTextField emailField = new JTextField(String.format("%s@javajam.com", DEFAULT_USER));
        styleTextField(emailField);
        contentPanel.add(createProfileRow("Email", emailField));
        contentPanel.add(Box.createVerticalStrut(30));

        //Statistics
        contentPanel.add(createSectionHeader("Listening Stats"));
        contentPanel.add(Box.createVerticalStrut(12));

        JLabel tracksLabel = new JLabel("Total Tracks Played: 142");
        tracksLabel.setForeground(TEXT_LIGHT);
        contentPanel.add(tracksLabel);
        contentPanel.add(Box.createVerticalStrut(8));

        JLabel genreLabel = new JLabel("Favorite Genre: Lo-Fi / Synthwave");
        genreLabel.setForeground(TEXT_LIGHT);
        contentPanel.add(genreLabel);

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

    private JPanel createProfileRow(String labelText, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(20, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 45));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT_LIGHT);
        label.setPreferredSize(new Dimension(120, 30));

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

    private void styleTextField(JTextField field) {
        field.setBackground(BUTTON_DARK);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    public JButton getBackButton()
    {
        return backButton;
    }
}