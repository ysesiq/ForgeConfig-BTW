package cpw.mods.fml.common;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FMLLog {
    private static FMLRelaunchLog coreLog = FMLRelaunchLog.log;

    public static void log(String logChannel, Level level, String format, Object... data) {
        FMLRelaunchLog fMLRelaunchLog = coreLog;
        FMLRelaunchLog.log(logChannel, level, format, data);
    }

    public static void log(Level level, String format, Object... data) {
        FMLRelaunchLog fMLRelaunchLog = coreLog;
        FMLRelaunchLog.log(level, format, data);
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data) {
        FMLRelaunchLog fMLRelaunchLog = coreLog;
        FMLRelaunchLog.log(logChannel, level, ex, format, data);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        FMLRelaunchLog fMLRelaunchLog = coreLog;
        FMLRelaunchLog.log(level, ex, format, data);
    }

    public static void severe(String format, Object... data) {
        log(Level.SEVERE, format, data);
    }

    public static void warning(String format, Object... data) {
        log(Level.WARNING, format, data);
    }

    public static void info(String format, Object... data) {
        log(Level.INFO, format, data);
    }

    public static void fine(String format, Object... data) {
        log(Level.FINE, format, data);
    }

    public static void finer(String format, Object... data) {
        log(Level.FINER, format, data);
    }

    public static void finest(String format, Object... data) {
        log(Level.FINEST, format, data);
    }

    public static Logger getLogger() {
        return coreLog.getLogger();
    }

    public static void makeLog(String logChannel) {
        FMLRelaunchLog fMLRelaunchLog = coreLog;
        FMLRelaunchLog.makeLog(logChannel);
    }
}
