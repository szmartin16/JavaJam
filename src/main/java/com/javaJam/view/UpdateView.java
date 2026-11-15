package com.javaJam.view;

import com.javaJam.controller.MainController;
import com.javaJam.controller.MusicController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

import static com.javaJam.service.Theme.*;

public class UpdateView extends JFrame {

    private final String latestVersion;
    private MainController controller;

    public UpdateView(String latestVersion) {
        this.latestVersion = latestVersion;
        initComponents();
    }

    private void initComponents() {
        setSize(1200, 800);
        setLocationRelativeTo(getOwner());
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("A New Version is Available!");
        titleLabel.setForeground(TEXT_LIGHT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel(
                "<html><div style='text-align: center;'>Version <b>" + latestVersion +
                        "</b> has been released on GitHub.<br>Please update your application!</div></html>"
        );
        descLabel.setForeground(TEXT_LIGHT);
        descLabel.setHorizontalAlignment(JLabel.CENTER);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension buttonSize = new Dimension(160, 42);
        JButton openButton = new JButton("Open Releases");
        openButton.setPreferredSize(buttonSize);
        openButton.setBackground(ACCENT_COLOR);
        openButton.setForeground(Color.WHITE);
        openButton.setFocusPainted(false);
        openButton.setFont(openButton.getFont().deriveFont(Font.BOLD, 13f));
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/szmartin16/JavaJam/releases/latest"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            onExit();
        });

        JButton exitButton = new JButton("Exit");
        exitButton.setPreferredSize(buttonSize);
        exitButton.setBackground(BUTTON_DARK);
        exitButton.setForeground(TEXT_LIGHT);
        exitButton.setFocusPainted(false);
        exitButton.setFont(exitButton.getFont().deriveFont(Font.BOLD, 13f));
        exitButton.addActionListener(e -> onExit());

        buttonPanel.add(openButton);
        buttonPanel.add(exitButton);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(descLabel);
        mainPanel.add(Box.createVerticalStrut(24));
        mainPanel.add(buttonPanel);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(mainPanel);

        setContentPane(wrapper);
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    private void onExit() {
        if (controller != null) {
            controller.shutdown();
        }
        dispose();
        System.exit(0);
    }
}