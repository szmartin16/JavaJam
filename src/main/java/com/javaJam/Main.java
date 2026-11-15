package com.javaJam;
import com.javaJam.controller.MainController;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainController controller = new MainController();
            controller.start();
        });
    }
}