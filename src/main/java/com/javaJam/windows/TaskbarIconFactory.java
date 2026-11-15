package com.javaJam.windows;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class TaskbarIconFactory {

    private TaskbarIconFactory() {}

    public static final int CBM_INIT = 0x04;

    public interface ExtendedGDI32 extends GDI32 {
        ExtendedGDI32 INSTANCE = Native.load("gdi32", ExtendedGDI32.class);

        WinDef.HBITMAP CreateBitmap(
                int nWidth,
                int nHeight,
                int nPlanes,
                int nBitCount,
                com.sun.jna.Pointer lpBits
        );
    }

    public interface ExtendedUser32 extends User32 {
        ExtendedUser32 INSTANCE = Native.load("user32", ExtendedUser32.class);
        WinDef.HICON CreateIconIndirect(ICONINFO iconinfo);
    }

    @Structure.FieldOrder({"fIcon", "xHotspot", "yHotspot", "hbmMask", "hbmColor"})
    public static class ICONINFO extends Structure {
        public boolean fIcon;
        public int xHotspot;
        public int yHotspot;
        public WinDef.HBITMAP hbmMask;
        public WinDef.HBITMAP hbmColor;
    }

    public static BufferedImage loadSvg(String path, int width, int height) {
        try {
            String normalizedPath = (path != null && path.startsWith("/")) ? path.substring(1) : path;

            URL resourceUrl = TaskbarIconFactory.class.getClassLoader().getResource(normalizedPath);
            if (resourceUrl == null) {
                System.err.println("[ICON_FACTORY] ERROR: Could not find SVG resource on classpath: " + normalizedPath);
            }

            FlatSVGIcon svgIcon = new FlatSVGIcon(normalizedPath, width, height);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            svgIcon.paintIcon(null, g2d, 0, 0);
            g2d.dispose();
            return image;
        } catch (Exception e) {
            System.err.println("[ICON_FACTORY] Failed to load SVG (" + path + "): " + e.getMessage());
            BufferedImage fallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = fallback.createGraphics();
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();
            return fallback;
        }
    }

    public static WinDef.HICON createHIcon(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int bytesPerPixel = 4;
        int imageSize = width * height * bytesPerPixel;

        Memory pixels = new Memory(imageSize);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int offset = ((height - 1 - y) * width + x) * bytesPerPixel;

                pixels.setByte(offset, (byte) (argb & 0xFF));
                pixels.setByte(offset + 1, (byte) ((argb >> 8) & 0xFF));
                pixels.setByte(offset + 2, (byte) ((argb >> 16) & 0xFF));
                pixels.setByte(offset + 3, (byte) ((argb >> 24) & 0xFF));
            }
        }

        WinGDI.BITMAPINFO info = new WinGDI.BITMAPINFO();
        info.bmiHeader.biSize = info.bmiHeader.size();
        info.bmiHeader.biWidth = width;
        info.bmiHeader.biHeight = -height;
        info.bmiHeader.biPlanes = (short) 1;
        info.bmiHeader.biBitCount = (short) 32;
        info.bmiHeader.biCompression = WinGDI.BI_RGB;

        WinDef.HDC screenDC = User32.INSTANCE.GetDC(null);
        WinDef.HBITMAP colorBitmap = GDI32.INSTANCE.CreateDIBitmap(
                screenDC, info.bmiHeader, CBM_INIT, pixels, info, WinGDI.DIB_RGB_COLORS
        );

        WinDef.HBITMAP maskBitmap = ExtendedGDI32.INSTANCE.CreateBitmap(
                width, height, 1, 1, (com.sun.jna.Pointer) null
        );

        User32.INSTANCE.ReleaseDC(null, screenDC);

        if (colorBitmap == null || maskBitmap == null) {
            throw new IllegalStateException("Could not create icon bitmaps");
        }

        ICONINFO iconInfo = new ICONINFO();
        iconInfo.fIcon = true;
        iconInfo.xHotspot = 0;
        iconInfo.yHotspot = 0;
        iconInfo.hbmColor = colorBitmap;
        iconInfo.hbmMask = maskBitmap;
        iconInfo.write();

        WinDef.HICON icon = ExtendedUser32.INSTANCE.CreateIconIndirect(iconInfo);

        GDI32.INSTANCE.DeleteObject(colorBitmap);
        GDI32.INSTANCE.DeleteObject(maskBitmap);

        if (icon == null) {
            throw new IllegalStateException("CreateIconIndirect failed");
        }

        return icon;
    }
}