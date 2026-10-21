package com.steeplesoft.shellprogress;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Runs the shell script's terminal progress demonstration from Java.
 */
public final class ShellProgress {

    private static final int BAR_WIDTH = 20;
    private static final long PROGRESS_DELAY_MILLIS = 25L;
    private static final long CLEAR_DELAY_MILLIS = 250L;

    private final PrintStream output;
    private final boolean supportsOsc94;
    private final String directoryName;

    /**
     * Creates a progress display with the output and terminal behavior to use.
     *
     * @param output stream receiving terminal escape sequences
     * @param supportsOsc94 whether the terminal supports OSC 9;4 progress
     * @param directoryName title restored when progress is cleared
     */
    ShellProgress(PrintStream output, boolean supportsOsc94, String directoryName) {
        this.output = output;
        this.supportsOsc94 = supportsOsc94;
        this.directoryName = directoryName;
    }

    /**
     * Runs the progress demonstration using the current terminal environment.
     *
     * @param args ignored command-line arguments
     * @throws InterruptedException if the demonstration is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        boolean supportsOsc94 = supportsOsc94(System.getenv());
        String directoryName = Path.of("").toAbsolutePath().getFileName().toString();
        ShellProgress progress = new ShellProgress(System.out, supportsOsc94, directoryName);
        progress.runDemo();
    }

    /**
     * Determines whether the environment identifies a terminal supporting OSC 9;4.
     *
     * @param environment environment variables used for terminal detection
     * @return whether OSC 9;4 should be emitted
     */
    static boolean supportsOsc94(Map<String, String> environment) {
        String terminalProgram = environment.get("TERM_PROGRAM");
        if ("iTerm.app".equals(terminalProgram)
                || "ghostty".equals(terminalProgram)
                || "WezTerm".equals(terminalProgram)
                || (terminalProgram != null && terminalProgram.startsWith("WezTerm"))) {
            return true;
        }

        if (hasValue(environment, "WT_SESSION")
                || hasValue(environment, "KITTY_WINDOW_ID")
                || hasValue(environment, "KONSOLE_VERSION")
                || hasValue(environment, "WEZTERM_EXECUTABLE")
                || hasValue(environment, "GHOSTTY_RESOURCES_DIR")) {
            return true;
        }

        String vteVersion = environment.get("VTE_VERSION");
        if (vteVersion != null) {
            String numericVersion = vteVersion.replaceAll("[^0-9]", "");
            if (!numericVersion.isEmpty()) {
                try {
                    return Long.parseLong(numericVersion) >= 7900;
                } catch (NumberFormatException ignored) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether an environment marker is present and non-empty.
     *
     * @param environment environment variables to inspect
     * @param name marker name
     * @return whether the marker has a value
     */
    private static boolean hasValue(Map<String, String> environment, String name) {
        return !environment.getOrDefault(name, "").isEmpty();
    }

    /**
     * Writes the progress value using OSC 9;4 or the fallback title bar.
     *
     * @param percent progress percentage from 0 through 100
     */
    void setProgress(int percent) {
        if (supportsOsc94) {
            output.printf("\033]9;4;1;%d\007", percent);
            return;
        }

        int filled = BAR_WIDTH * percent / 100;
        int empty = BAR_WIDTH - filled;
        String bar = "█".repeat(filled) + "░".repeat(empty);
        output.printf(Locale.ROOT, "\033]0;[%s] %3d%%\007", bar, percent);
    }

    /**
     * Restores the terminal title after progress finishes.
     */
    void clearProgress() {
        if (supportsOsc94) {
            output.print("\033]9;4;0\007");
        } else {
            output.printf("\033]0;%s\007", directoryName);
        }
    }

    /**
     * Displays progress from zero to one hundred, then clears it.
     *
     * @throws InterruptedException if the demonstration is interrupted
     */
    private void runDemo() throws InterruptedException {
        output.println("Running progress demo (OSC 9;4 native progress if available)…");
        for (int percent = 0; percent <= 100; percent++) {
            setProgress(percent);
            Thread.sleep(PROGRESS_DELAY_MILLIS);
        }
        Thread.sleep(CLEAR_DELAY_MILLIS);
        clearProgress();
        output.println("Done.");
    }
}

