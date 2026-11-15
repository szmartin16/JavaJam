package com.javaJam.windows;

import com.javaJam.controller.MusicController;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class WindowsTaskbarThumbnail {

    private final Window window;
    private final Runnable previousAction;
    private final Runnable playPauseAction;
    private final Runnable nextAction;

    private TaskbarCom taskbarCom;
    private WinDef.HWND hwnd;

    private WinDef.HICON previousIcon;
    private WinDef.HICON playIcon;
    private WinDef.HICON pauseIcon;
    private WinDef.HICON nextIcon;

    private WinUser.WindowProc newWndProc;
    private Pointer oldWndProcPtr;

    private int taskbarCreatedMessage;
    private MusicController musicController;

    public WindowsTaskbarThumbnail(
            Window window,
            Runnable previousAction,
            Runnable playPauseAction,
            Runnable nextAction,
            MusicController musicController) {
        this.window = window;
        this.previousAction = previousAction;
        this.playPauseAction = playPauseAction;
        this.nextAction = nextAction;
        this.musicController = musicController;
    }

    public void initialize() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }

        if (!window.isDisplayable()) {
            System.err.println("[TASKBAR] Window is not displayable yet.");
            return;
        }

        try {
            previousIcon = TaskbarIconFactory.createHIcon(TaskbarIconFactory.loadSvg("icons/prev.svg", 32, 32));
            playIcon = TaskbarIconFactory.createHIcon(TaskbarIconFactory.loadSvg("icons/play.svg", 32, 32));
            pauseIcon = TaskbarIconFactory.createHIcon(TaskbarIconFactory.loadSvg("icons/pause.svg", 32, 32));
            nextIcon = TaskbarIconFactory.createHIcon(TaskbarIconFactory.loadSvg("icons/next.svg", 32, 32));

            hwnd = new WinDef.HWND(Native.getComponentPointer(window));

            Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
            PointerByReference pbr = new PointerByReference();

            WinNT.HRESULT hr = Ole32.INSTANCE.CoCreateInstance(
                    TaskbarCom.CLSID_TASKBAR_LIST,
                    null,
                    0x17,
                    TaskbarCom.IID_TASKBAR_LIST3,
                    pbr
            );

            if (hr == null || hr.intValue() < 0) {
                System.err.println("[TASKBAR] CoCreateInstance failed with HRESULT: " + (hr != null ? hr.intValue() : "null"));
                return;
            }

            taskbarCom = new TaskbarCom(pbr.getValue());
            taskbarCom.hrInit();


            taskbarCreatedMessage = User32.INSTANCE.RegisterWindowMessage("TaskbarButtonCreated");

            newWndProc = new WinUser.WindowProc() {
                @Override
                public WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
                    if (uMsg == taskbarCreatedMessage) {
                        addButtons();
                    } else if (uMsg == 0x0111) { // WM_COMMAND
                        int buttonId = wParam.intValue() & 0xFFFF;
                        handleButtonClick(buttonId);
                    }

                    if (oldWndProcPtr != null) {
                        return User32.INSTANCE.CallWindowProc(oldWndProcPtr, hWnd, uMsg, wParam, lParam);
                    }
                    return User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
                }
            };

            oldWndProcPtr = NativeWindowHelper.setWindowProc(hwnd, newWndProc);

            addButtons();
            System.out.println("[TASKBAR] Thumbnail toolbar successfully initialized.");

        } catch (Exception e) {
            System.err.println("[TASKBAR] Initialization failed: " + e.getMessage());
            System.out.println("[DEBUG] JVM Architecture: " + System.getProperty("os.arch"));
            System.out.println("[DEBUG] Java Data Model: " + System.getProperty("sun.arch.data.model") + "-bit");
            e.printStackTrace();
        }
    }

    private void addButtons() {
        if (taskbarCom == null || hwnd == null) return;

        TaskbarCom.THUMBBUTTON btnPrev = createButton(1001, "Previous", previousIcon);
        TaskbarCom.THUMBBUTTON btnPlayPause = createButton(1002, musicController.getPlaybackStatus() ? "Pause" : "Play",
                musicController.getPlaybackStatus() ? pauseIcon : playIcon);
        TaskbarCom.THUMBBUTTON btnNext = createButton(1003, "Next", nextIcon);

        try {
            taskbarCom.thumbBarAddButtons(hwnd, new TaskbarCom.THUMBBUTTON[]{btnPrev, btnPlayPause, btnNext});
        } catch (Exception e) {
            taskbarCom.thumbBarUpdateButtons(hwnd, new TaskbarCom.THUMBBUTTON[]{btnPrev, btnPlayPause, btnNext});
        }
    }

    public void setPlaying() {
        if (taskbarCom == null || hwnd == null) return;

        boolean isPlaying = musicController.getPlaybackStatus();

        TaskbarCom.THUMBBUTTON playPauseBtn = createButton(
                1002,
                isPlaying ? "Pause" : "Play",
                isPlaying ? pauseIcon : playIcon
        );

        taskbarCom.thumbBarUpdateButtons(hwnd, new TaskbarCom.THUMBBUTTON[]{playPauseBtn});
    }

    public void dispose() {
        if (hwnd != null && oldWndProcPtr != null) {
            NativeWindowHelper.restoreWindowProc(
                    hwnd,
                    oldWndProcPtr
            );
        }

        if (taskbarCom != null) {
            taskbarCom.Release();
            taskbarCom = null;
        }

        if (previousIcon != null) User32.INSTANCE.DestroyIcon(previousIcon);
        if (playIcon != null) User32.INSTANCE.DestroyIcon(playIcon);
        if (pauseIcon != null) User32.INSTANCE.DestroyIcon(pauseIcon);
        if (nextIcon != null) User32.INSTANCE.DestroyIcon(nextIcon);

        hwnd = null;
    }

    private TaskbarCom.THUMBBUTTON createButton(int id, String tooltip, WinDef.HICON icon) {
        TaskbarCom.THUMBBUTTON button = new TaskbarCom.THUMBBUTTON();
        button.dwMask = TaskbarCom.THB_ICON | TaskbarCom.THB_TOOLTIP | TaskbarCom.THB_FLAGS;
        button.iId = id;
        button.hIcon = icon;
        button.setTooltip(tooltip);
        button.dwFlags = TaskbarCom.THBF_ENABLED;
        return button;
    }

    private void handleButtonClick(int buttonId) {
        switch (buttonId) {
            case 1001:
                if (previousAction != null) SwingUtilities.invokeLater(previousAction);
                break;
            case 1002:
                if (playPauseAction != null) SwingUtilities.invokeLater(playPauseAction);
                break;
            case 1003:
                if (nextAction != null) SwingUtilities.invokeLater(nextAction);
                break;
            default:
                break;
        }
    }

}