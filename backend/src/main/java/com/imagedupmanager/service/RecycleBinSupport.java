package com.imagedupmanager.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects whether a volume has an operating system Recycle Bin (Windows). Used by
 * {@link FileTrashDelegator} to choose between the Windows Recycle Bin and the internal
 * trash, avoiding a silent permanent delete on volumes without a Recycle Bin (e.g.
 * removable USB drives).
 *
 * <p>Fail-closed: non-Windows platforms, unknown drives, network/CD drives and fixed
 * volumes where {@code SHQueryRecycleBin} does not answer {@code S_OK} report that no
 * Recycle Bin is available.
 */
@Component
public class RecycleBinSupport {

    private static final int S_OK = 0;
    /** Only fixed disks keep a Windows Recycle Bin; removable/network/CD drives do not. */
    private static final int DRIVE_FIXED = 3;

    private final Map<String, Boolean> recycleBinCache = new ConcurrentHashMap<>();

    /**
     * @return {@code true} when the volume containing {@code file} is expected to have a
     *     Windows Recycle Bin
     */
    public boolean supportsRecycleBin(Path file) {
        if (!Platform.isWindows()) {
            return false;
        }
        Path absolute = file.toAbsolutePath();
        Path root = absolute.getRoot();
        if (root == null) {
            return false;
        }
        String rootPath = root.toString();
        return recycleBinCache.computeIfAbsent(rootPath, this::probeVolume);
    }

    private boolean probeVolume(String rootPath) {
        int driveType = Kernel32Holder.INSTANCE.GetDriveTypeW(new WString(rootPath));
        if (driveType != DRIVE_FIXED) {
            return false;
        }
        SHQUERYRBINFO info = new SHQUERYRBINFO();
        info.cbSize = info.size();
        int result = Shell32Holder.INSTANCE.SHQueryRecycleBin(new WString(rootPath), info);
        return result == S_OK;
    }

    private interface Kernel32 extends StdCallLibrary {
        int GetDriveTypeW(WString lpRootPathName);
    }

    private static final class Kernel32Holder {
        private static final Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        private Kernel32Holder() {
        }
    }

    private interface Shell32 extends StdCallLibrary {
        int SHQueryRecycleBin(WString pszRootPath, SHQUERYRBINFO pSHQueryRBInfo);
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

    /** Windows {@code SHQUERYRBINFO}; {@code cbSize} must be set before calling the API. */
    @Structure.FieldOrder({"cbSize", "i64Size", "i64NumItems"})
    public static class SHQUERYRBINFO extends Structure {
        public int cbSize;
        public long i64Size;
        public long i64NumItems;
    }
}
