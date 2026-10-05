package ru.mirea.emulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Исполнение настоящих UTF-8 файлов до первой ошибки или exit. */
final class StartupTests {
    private final Checks checks;
    private final List<String> executed = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private StartupTests(Checks checks) {
        this.checks = checks;
    }

    /** Запустить файловые проверки и удалить только собственные временные файлы. */
    static void run(Checks checks) {
        try {
            new StartupTests(checks).scenarios();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private void scenarios() throws IOException {
        checkSuccess();
        checkFailure("\nls\npwd\ncd skipped\n", 3);
        checkFailure("ls\ncd \"unfinished\nls skipped\n", 2);
        checkFailure("ls\ncd a b\nls skipped\n", 2);
        checkExit();
        checkReadFailure();
    }

    private void checkSuccess() throws IOException {
        StartupRunner.Outcome outcome = runFile("\nls\ncd \"мои документы\"\n\n");
        checks.equal(StartupRunner.Status.COMPLETED, outcome.status());
        checks.equal(4, outcome.line());
        checks.equal(List.of("ls", "cd \"мои документы\""), executed);
        checks.truth(errors.isEmpty());
    }

    private void checkFailure(String script, int line) throws IOException {
        StartupRunner.Outcome outcome = runFile(script);
        checks.equal(StartupRunner.Status.FAILED, outcome.status());
        checks.equal(line, outcome.line());
        checks.equal(2, executed.size());
        checks.equal(1, errors.size());
        checks.truth(errors.get(0).contains("строка " + line));
        checks.truth(executed.stream().noneMatch(command -> command.contains("skipped")));
    }

    private void checkExit() throws IOException {
        StartupRunner.Outcome outcome = runFile("ls\nexit\ncd skipped\n");
        checks.equal(StartupRunner.Status.EXIT, outcome.status());
        checks.equal(List.of("ls", "exit"), executed);
        checks.truth(errors.isEmpty());
    }

    private StartupRunner.Outcome runFile(String content) throws IOException {
        Path file = Files.createTempFile("variant7-script ", ".txt");
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return execute(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private StartupRunner.Outcome execute(Path file) {
        executed.clear();
        errors.clear();
        Shell shell = new Shell();
        return new StartupRunner().run(file, line -> {
            executed.add(line);
            return shell.execute(line);
        }, errors::add);
    }

    private void checkReadFailure() throws IOException {
        Path file = Files.createTempFile("variant7-invalid", ".txt");
        try {
            Files.write(file, new byte[]{(byte) 0xC3, (byte) 0x28});
            checks.equal(StartupRunner.Status.FAILED, execute(file).status());
            checks.equal(1, errors.size());
            checks.truth(executed.isEmpty());
        } finally {
            Files.deleteIfExists(file);
        }
        checks.equal(StartupRunner.Status.FAILED, execute(file).status());
        checks.equal(1, errors.size());
        checks.truth(executed.isEmpty());
        checks.truth(errors.get(0).contains(file.toString()));
    }
}
