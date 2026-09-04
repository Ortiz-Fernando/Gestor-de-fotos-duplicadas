package com.imagedupmanager.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends files to the Windows Recycle Bin through Shell32 {@code SHFileOperation} with
 * {@code FOF_ALLOWUNDO} (AGENTS.md #37). Never performs a permanent delete.
 *
 * <p>This implementation must only be used when {@link RecycleBinSupport} confirms that
 * the target volume has a Windows Recycle Bin; otherwise {@link FileTrashDelegator}
 * falls back to the internal trash ({@link InternalFileTrash}). Returns {@code null}
 * because the file is not stored in an application-managed location: the operating
 * system Recycle Bin manages it.
 */
public class WindowsFileTrash implements FileTrash {

    private static final int FO_DELETE = 3;
    private static final short FOF_SILENT = 0x0004;
    private static final short FOF_NOCONFIRMATION = 0x0010;
    private static final short FOF_ALLOWUNDO = 0x0040;

    @Override
    public Path sendToTrash(Path file) {
        if (!Platform.isWindows()) {
            throw new OperationException(
                    "El envío a la Papelera de Windows solo está disponible en Windows.");
        }
        String fullPath = file.toAbsolutePath().toString();
        // SHFileOperation requires a double-null terminated list for pFrom.
        SHFILEOPSTRUCT operation = new SHFILEOPSTRUCT();
        operation.wFunc = FO_DELETE;
        operation.pFrom = new WString(fullPath + "\0");
        operation.fFlags = (short) (FOF_ALLOWUNDO | FOF_NOCONFIRMATION | FOF_SILENT);

        int result = Shell32Holder.INSTANCE.SHFileOperation(operation);
        if (result != 0) {
            throw new OperationException(
                    "No se ha podido enviar el archivo a la Papelera de Windows (código "
                            + result + ").");
        }
        return null;
    }

    private interface Shell32 extends StdCallLibrary {
        int SHFileOperation(SHFILEOPSTRUCT operation);
    }

    private static final class Shell32Holder {
        private static final Shell32 INSTANCE;

        static {
            Map<String, Object> options = new HashMap<>();
            options.put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
            options.put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
            INSTANCE = Native.load("shell32", Shell32.class, options);
        }

        private Shell32Holder() {
        }
    }

    @Structure.FieldOrder({"hwnd", "wFunc", "pFrom", "pTo", "fFlags",
            "fAnyOperationsAborted", "hNameMappings", "lpszProgressTitle"})
    public static class SHFILEOPSTRUCT extends Structure {
        // HWND is an 8-byte pointer on Win64. A 4-byte NativeLong misaligned the whole
        // struct and Windows rejected the call with ERROR_INVALID_PARAMETER (87).
        public Pointer hwnd;
        public int wFunc;
        public WString pFrom;
        public WString pTo;
        public short fFlags;
        public int fAnyOperationsAborted;
        public Pointer hNameMappings;
        public WString lpszProgressTitle;
    }
}
