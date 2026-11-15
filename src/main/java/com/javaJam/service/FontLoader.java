package com.javaJam.service;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public class FontLoader {

    public static Font loadResourceFont(String resourcePath, float size) {
        try (InputStream is = FontLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[FONT] Could not find font resource: " + resourcePath);
                return new Font("SansSerif", Font.PLAIN, (int) size);
            }

            Font customFont = Font.createFont(Font.TRUETYPE_FONT, is);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);

            return customFont.deriveFont(size);

        } catch (Exception e) {
            System.err.println("[FONT ERROR] Failed to load font: " + e.getMessage());
            e.printStackTrace();
            return new Font("SansSerif", Font.PLAIN, (int) size);
        }
    }
}