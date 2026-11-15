
package com.javaJam.windows;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public final class NativeWindowHelper {

    private static final NativeLibrary USER32 =
            NativeLibrary.getInstance("user32");

    private static final boolean IS_64_BIT =
            Native.POINTER_SIZE == 8;

    private NativeWindowHelper() {}

    /**
     * Installs a new window procedure and returns
     * the original window procedure pointer.
     */
    public static Pointer setWindowProc(
            WinDef.HWND hwnd,
            WinUser.WindowProc callback) {

        String functionName = IS_64_BIT
                ? "SetWindowLongPtrW"
                : "SetWindowLongW";

        Function function = USER32.getFunction(
                functionName,
                Function.ALT_CONVENTION
        );

        return function.invokePointer(
                new Object[]{
                        hwnd,
                        WinUser.GWL_WNDPROC,
                        callback
                }
        );
    }

    /**
     * Restores the original window procedure.
     */
    public static Pointer restoreWindowProc(
            WinDef.HWND hwnd,
            Pointer originalProc) {

        String functionName = IS_64_BIT
                ? "SetWindowLongPtrW"
                : "SetWindowLongW";

        Function function = USER32.getFunction(
                functionName,
                Function.ALT_CONVENTION
        );

        return function.invokePointer(
                new Object[]{
                        hwnd,
                        WinUser.GWL_WNDPROC,
                        originalProc
                }
        );
    }
}
