package com.steeplesoft.shellprogress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ShellProgressTest {

    @Test
    void writesNativeProgressForITerm() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ShellProgress progress = new ShellProgress(new PrintStream(output, true, StandardCharsets.UTF_8), true, "project");

        progress.setProgress(42);

        assertEquals("\033]9;4;1;42\007", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void writesTitleProgressOutsideITerm() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ShellProgress progress = new ShellProgress(new PrintStream(output, true, StandardCharsets.UTF_8), false, "project");

        progress.setProgress(25);

        assertEquals("\033]0;[█████░░░░░░░░░░░░░░░]  25%\007", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void clearsProgressUsingTheCurrentDirectoryName() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ShellProgress progress = new ShellProgress(new PrintStream(output, true, StandardCharsets.UTF_8), false, "project");

        progress.clearProgress();

        assertEquals("\033]0;project\007", output.toString(StandardCharsets.UTF_8));
    }
}
