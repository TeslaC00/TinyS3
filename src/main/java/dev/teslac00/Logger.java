package dev.teslac00;

import java.io.PrintStream;

public final class Logger {

    private static final PrintStream STREAM = System.out;

    public static void log(Object... args) {
        StringBuilder builder = new StringBuilder();

        for (Object arg : args)
            builder.append(arg);

        builder.append('\n');
        STREAM.print(builder);
    }

}
