package cpw.mods.fml.relauncher;

import com.google.common.base.Throwables;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class FMLRelaunchLog {
    public static FMLRelaunchLog log = new FMLRelaunchLog();
    static File minecraftHome;
    private static boolean configured;
    private static Thread consoleLogThread;
    private static PrintStream errCache;
    private Logger myLog;
    private static FileHandler fileHandler;
    private static FMLLogFormatter formatter;
    static String logFileNamePattern;

    private static class ConsoleLogWrapper extends Handler {
        private ConsoleLogWrapper() {
        }

        @Override // java.util.logging.Handler
        public void publish(LogRecord record) {
            boolean currInt = Thread.interrupted();
            try {
                ConsoleLogThread.recordQueue.put(record);
            } catch (InterruptedException e) {
                e.printStackTrace(FMLRelaunchLog.errCache);
            }
            if (currInt) {
                Thread.currentThread().interrupt();
            }
        }

        @Override // java.util.logging.Handler
        public void flush() {
        }

        @Override // java.util.logging.Handler
        public void close() throws SecurityException {
        }
    }

    /* loaded from: Forge Config-0.0.1.jar:cpw/mods/fml/relauncher/FMLRelaunchLog$ConsoleLogThread.class */
    private static class ConsoleLogThread implements Runnable {
        static ConsoleHandler wrappedHandler = new ConsoleHandler();
        static LinkedBlockingQueue<LogRecord> recordQueue = new LinkedBlockingQueue<>();

        private ConsoleLogThread() {
        }

        @Override // java.lang.Runnable
        public void run() {
            while (true) {
                try {
                    LogRecord lr = recordQueue.take();
                    wrappedHandler.publish(lr);
                } catch (InterruptedException e) {
                    e.printStackTrace(FMLRelaunchLog.errCache);
                    Thread.interrupted();
                }
            }
        }
    }

    /* loaded from: Forge Config-0.0.1.jar:cpw/mods/fml/relauncher/FMLRelaunchLog$LoggingOutStream.class */
    private static class LoggingOutStream extends ByteArrayOutputStream {
        private Logger log;
        private StringBuilder currentMessage = new StringBuilder();

        public LoggingOutStream(Logger log) {
            this.log = log;
        }

        @Override // java.io.OutputStream, java.io.Flushable
        public void flush() throws IOException {
            synchronized (FMLRelaunchLog.class) {
                super.flush();
                String record = toString();
                super.reset();
                this.currentMessage.append(record.replace(FMLLogFormatter.LINE_SEPARATOR, "\n"));
                int lastIdx = -1;
                int idx = this.currentMessage.indexOf("\n", (-1) + 1);
                while (idx >= 0) {
                    this.log.log(Level.INFO, this.currentMessage.substring(lastIdx + 1, idx));
                    lastIdx = idx;
                    idx = this.currentMessage.indexOf("\n", lastIdx + 1);
                }
                if (lastIdx >= 0) {
                    String rem = this.currentMessage.substring(lastIdx + 1);
                    this.currentMessage.setLength(0);
                    this.currentMessage.append(rem);
                }
            }
        }
    }

    private FMLRelaunchLog() {
    }

    private static void configureLogging() {
        LogManager.getLogManager().reset();
        Logger globalLogger = Logger.getLogger("global");
        globalLogger.setLevel(Level.OFF);
        log.myLog = Logger.getLogger("ForgeModLoader");
        Logger stdOut = Logger.getLogger("STDOUT");
        stdOut.setParent(log.myLog);
        Logger stdErr = Logger.getLogger("STDERR");
        stdErr.setParent(log.myLog);
        log.myLog.setLevel(Level.ALL);
        log.myLog.setUseParentHandlers(false);
        consoleLogThread = new Thread(new ConsoleLogThread());
        consoleLogThread.setDaemon(true);
        consoleLogThread.start();
        formatter = new FMLLogFormatter();
        try {
            File logPath = new File(minecraftHome, logFileNamePattern);
            fileHandler = new FileHandler(logPath.getPath(), 0, 3) { // from class: cpw.mods.fml.relauncher.FMLRelaunchLog.1
                @Override // java.util.logging.FileHandler, java.util.logging.StreamHandler, java.util.logging.Handler
                public synchronized void close() throws SecurityException {
                }
            };
            resetLoggingHandlers();
            errCache = System.err;
            System.setOut(new PrintStream((OutputStream) new LoggingOutStream(stdOut), true));
            System.setErr(new PrintStream((OutputStream) new LoggingOutStream(stdErr), true));
            configured = true;
        } catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }

    private static void resetLoggingHandlers() {
        ConsoleLogThread.wrappedHandler.setLevel(Level.parse(System.getProperty("fml.log.level", "INFO")));
        log.myLog.addHandler(new ConsoleLogWrapper());
        ConsoleLogThread.wrappedHandler.setFormatter(formatter);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(formatter);
        log.myLog.addHandler(fileHandler);
    }

    public static void loadLogConfiguration(File logConfigFile) {
        if (logConfigFile != null && logConfigFile.exists() && logConfigFile.canRead()) {
            try {
                LogManager.getLogManager().readConfiguration(new FileInputStream(logConfigFile));
                resetLoggingHandlers();
            } catch (Exception e) {
                log(Level.SEVERE, e, "Error reading logging configuration file %s", logConfigFile.getName());
            }
        }
    }

    public static void log(String logChannel, Level level, String format, Object... data) {
        if (!configured) {
            configureLogging();
        }
        makeLog(logChannel);
        Logger.getLogger(logChannel).log(level, String.format(format, data));
    }

    public static void log(Level level, String format, Object... data) {
        if (!configured) {
            configureLogging();
        }
        log.myLog.log(level, String.format(format, data));
    }

    public static void log(String logChannel, Level level, Throwable ex, String format, Object... data) {
        makeLog(logChannel);
        Logger.getLogger(logChannel).log(level, String.format(format, data), ex);
    }

    public static void log(Level level, Throwable ex, String format, Object... data) {
        if (!configured) {
            configureLogging();
        }
        log.myLog.log(level, String.format(format, data), ex);
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

    public Logger getLogger() {
        return this.myLog;
    }

    public static void makeLog(String logChannel) {
        Logger l = Logger.getLogger(logChannel);
        l.setParent(log.myLog);
    }
}
