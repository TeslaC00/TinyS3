package dev.teslac00;

import java.io.PrintStream;

public final class Logger {

    public enum LEVEL {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE
    }

    public static LEVEL level = LEVEL.INFO;
    private static final PrintStream STREAM = System.out;

    private static void log(String message, Object... args) {
        StringBuilder builder = new StringBuilder(message);

        for (Object arg : args)
            builder.append(arg);

        builder.append('\n');
        STREAM.print(builder);
    }

    public static void error(String message, Object... args) {
        STREAM.print("[ERROR]: ");
        log(message, args);
    }

    public static void debug(String message, Object... args) {
        if (level.ordinal() < LEVEL.DEBUG.ordinal()) return;
        STREAM.print("[DEBUG]: ");
        log(message, args);
    }

    public static void info(String message, Object... args) {
        if (level.ordinal() < LEVEL.INFO.ordinal()) return;
        STREAM.print("[INFO] : ");
        log(message, args);
    }

    public static void warn(String message, Object... args) {
        if (level.ordinal() < LEVEL.WARN.ordinal()) return;
        STREAM.print("[WARN] : ");
        log(message, args);
    }

    public static void trace(String message, Object... args) {
        if (level.ordinal() < LEVEL.TRACE.ordinal()) return;
        STREAM.print("[TRACE]: ");
        log(message, args);
    }

}
