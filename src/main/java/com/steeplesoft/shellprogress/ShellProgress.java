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

    private final PrintStream output;
    private final Map<String, String> environment;
    private final String directoryName;

    /**
     * Creates a progress display with the output and terminal behavior to use.
     *
     * @param output stream receiving terminal escape sequences
     * @param environment terminal environment variables used for detection
     * @param directoryName title restored when progress is cleared
     */
    ShellProgress(PrintStream output, Map<String, String> environment, String directoryName) {
        this.output = output;
        this.environment = Map.copyOf(environment);
        this.directoryName = directoryName;
    }

    /**
     * Creates a progress display with a fixed iTerm2 detection result for callers that only
     * need to choose between native progress and the title fallback.
     *
     * @param output stream receiving terminal escape sequences
     * @param iterm whether the output is an iTerm2 terminal
     * @param directoryName title restored when progress is cleared
     */
    ShellProgress(PrintStream output, boolean iterm, String directoryName) {
        this(output, iterm ? Map.of("TERM_PROGRAM", "iTerm.app") : Map.of(), directoryName);
    }

    /**
     * Runs the progress demonstration using the current terminal environment.
     *
     * @param args ignored command-line arguments
     * @throws InterruptedException if the demonstration is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        String directoryName = Path.of("").toAbsolutePath().getFileName().toString();
        ShellProgress progress = new ShellProgress(System.out, System.getenv(), directoryName);
        progress.runDemo();
    }

    /**
     * Checks the terminal markers used by the shell script to identify OSC 9;4 support.
     *
     * @return whether the terminal supports OSC 9;4 progress sequences
     */
    private boolean supportsOsc94() {
        String terminalProgram = environment.getOrDefault("TERM_PROGRAM", "");
        if (terminalProgram.equals("iTerm.app") || terminalProgram.equals("ghostty")
                || terminalProgram.startsWith("WezTerm")) {
            return true;
        }
        if (hasEnvironmentValue("WT_SESSION", "KITTY_WINDOW_ID", "KONSOLE_VERSION",
                "WEZTERM_EXECUTABLE", "GHOSTTY_RESOURCES_DIR")) {
            return true;
        }

        String vteVersion = environment.getOrDefault("VTE_VERSION", "").replaceAll("[^0-9]", "");
        try {
            return !vteVersion.isEmpty() && Integer.parseInt(vteVersion) >= 7900;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * Checks whether any named environment variable is present and nonempty.
     *
     * @param names environment variable names to check
     * @return whether at least one variable has a nonempty value
     */
    private boolean hasEnvironmentValue(String... names) {
        for (String name : names) {
            if (!environment.getOrDefault(name, "").isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether iTerm2-specific tab color sequences should be emitted.
     *
     * @return whether the configured terminal is iTerm2
     */
    private boolean isIterm() {
        return "iTerm.app".equals(environment.get("TERM_PROGRAM"));
    }

    /**
     * Emits an OSC sequence, using tmux passthrough when running inside tmux.
     *
     * @param sequence OSC sequence to emit
     */
    private void emitOsc(String sequence) {
        if (hasEnvironmentValue("TMUX")) {
            output.print("\033Ptmux;");
            output.print(sequence.replace("\033", "\033\033"));
            output.print("\033\\");
        } else {
            output.print(sequence);
        }
    }

    /**
     * Writes the progress value using OSC 9;4 where supported or the fallback title bar.
     *
     * @param percent progress percentage from 0 through 100
     */
    void setProgress(int percent) {
        percent = Math.max(0, Math.min(100, percent));
        if (supportsOsc94()) {
            emitOsc(String.format(Locale.ROOT, "\033]9;4;1;%d\007", percent));
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
        if (supportsOsc94()) {
            emitOsc("\033]9;4;0\007");
        } else {
            output.printf("\033]0;%s\007", directoryName);
        }
    }

    /**
     * Shows indeterminate progress when the terminal supports OSC 9;4.
     */
    void setIndeterminate() {
        if (supportsOsc94()) {
            emitOsc("\033]9;4;3\007");
        }
    }

    /**
     * Marks progress as failed in terminals supporting OSC 9;4.
     *
     * @param percent optional progress percentage, or {@code null} for no percentage
     */
    void setError(Integer percent) {
        if (supportsOsc94()) {
            String sequence = percent == null ? "\033]9;4;2\007"
                    : String.format(Locale.ROOT, "\033]9;4;2;%d\007", percent);
            emitOsc(sequence);
        }
    }

    /**
     * Sets the iTerm2 tab color when that terminal is in use.
     *
     * @param red red brightness from 0 through 255
     * @param green green brightness from 0 through 255
     * @param blue blue brightness from 0 through 255
     */
    void setTabColor(int red, int green, int blue) {
        if (isIterm()) {
            output.printf(Locale.ROOT, "\033]6;1;bg;red;brightness;%d\007", red);
            output.printf(Locale.ROOT, "\033]6;1;bg;green;brightness;%d\007", green);
            output.printf(Locale.ROOT, "\033]6;1;bg;blue;brightness;%d\007", blue);
        }
    }

    /**
     * Resets the iTerm2 tab color to its default.
     */
    void resetTabColor() {
        if (isIterm()) {
            output.print("\033]6;1;bg;*;default\007");
        }
    }

    /**
     * Displays progress from zero to one hundred, then clears it.
     *
     * @throws InterruptedException if the demonstration is interrupted
     */
    private void runDemo() throws InterruptedException {
        output.printf("Testing OSC 9;4 progress (supports_osc94 = %s)%n", supportsOsc94() ? "yes" : "no");
        output.printf("Inside tmux? %s%n", hasEnvironmentValue("TMUX") ? "yes" : "");
        for (int percent = 0; percent <= 100; percent += 5) {
            setProgress(percent);
            Thread.sleep(40L);
        }
        clearProgress();
        output.println("Done.");
    }
}
