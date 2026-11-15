package com.javaJam.windows;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

import java.util.Arrays;

public class TaskbarCom extends Unknown {

    public static final Guid.GUID CLSID_TASKBAR_LIST =
            new Guid.GUID("{56FDF344-FD6D-11D0-958A-006097C9A090}");

    public static final Guid.GUID IID_TASKBAR_LIST3 =
            new Guid.GUID(
                    "{EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF}"
            );

    private static final int VTABLE_HR_INIT = 3;
    private static final int VTABLE_THUMB_ADD_BUTTONS = 15;
    private static final int VTABLE_THUMB_UPDATE_BUTTONS = 16;

    public static final int THB_ICON = 0x02;
    public static final int THB_TOOLTIP = 0x04;
    public static final int THB_FLAGS = 0x08;

    public static final int THBF_ENABLED = 0x00;

    public TaskbarCom(Pointer pointer) {
        super(pointer);
    }

    public WinNT.HRESULT hrInit() {
        WinNT.HRESULT result = (WinNT.HRESULT) _invokeNativeObject(
                VTABLE_HR_INIT,
                new Object[]{getPointer()},
                WinNT.HRESULT.class
        );
        checkResult(result, "HrInit");
        return result;
    }

    public WinNT.HRESULT thumbBarAddButtons(WinDef.HWND hwnd, THUMBBUTTON[] buttons) {
        if (buttons == null || buttons.length == 0 || buttons.length > 7) {
            throw new IllegalArgumentException("Windows supports 1 to 7 thumbnail buttons");
        }

        THUMBBUTTON[] nativeButtons = (THUMBBUTTON[]) new THUMBBUTTON().toArray(buttons.length);
        for (int i = 0; i < buttons.length; i++) {
            nativeButtons[i].copyFrom(buttons[i]);
            nativeButtons[i].write();
        }

        WinNT.HRESULT result = (WinNT.HRESULT) _invokeNativeObject(
                VTABLE_THUMB_ADD_BUTTONS,
                new Object[]{getPointer(), hwnd, buttons.length, nativeButtons[0].getPointer()},
                WinNT.HRESULT.class
        );
        checkResult(result, "ThumbBarAddButtons");
        return result;
    }

    public WinNT.HRESULT thumbBarUpdateButtons(WinDef.HWND hwnd, THUMBBUTTON[] buttons) {
        if (buttons == null || buttons.length == 0 || buttons.length > 7) {
            throw new IllegalArgumentException("Invalid thumbnail button count");
        }

        THUMBBUTTON[] nativeButtons = (THUMBBUTTON[]) new THUMBBUTTON().toArray(buttons.length);
        for (int i = 0; i < buttons.length; i++) {
            nativeButtons[i].copyFrom(buttons[i]);
            nativeButtons[i].write();
        }

        WinNT.HRESULT result = (WinNT.HRESULT) _invokeNativeObject(
                VTABLE_THUMB_UPDATE_BUTTONS,
                new Object[]{getPointer(), hwnd, buttons.length, nativeButtons[0].getPointer()},
                WinNT.HRESULT.class
        );
        checkResult(result, "ThumbBarUpdateButtons");
        return result;
    }

    private void checkResult(WinNT.HRESULT result, String operation) {
        if (result == null || result.intValue() < 0) {
            throw new IllegalStateException(
                    operation + " failed. HRESULT: " + (result == null ? "null" : String.format("0x%08X", result.intValue()))
            );
        }
    }

    @Structure.FieldOrder({"dwMask", "iId", "iBitmap", "hIcon", "szTip", "dwFlags"})
    public static class THUMBBUTTON extends Structure {
        public int dwMask;
        public int iId;
        public int iBitmap;
        public WinDef.HICON hIcon;
        public char[] szTip = new char[260];
        public int dwFlags;

        public void setTooltip(String tooltip) {
            Arrays.fill(szTip, '\0');
            if (tooltip == null) return;

            int length = Math.min(tooltip.length(), szTip.length - 1);
            tooltip.getChars(0, length, szTip, 0);
        }

        public void copyFrom(THUMBBUTTON other) {
            this.dwMask = other.dwMask;
            this.iId = other.iId;
            this.iBitmap = other.iBitmap;
            this.hIcon = other.hIcon;
            this.szTip = other.szTip.clone();
            this.dwFlags = other.dwFlags;
        }
    }
}