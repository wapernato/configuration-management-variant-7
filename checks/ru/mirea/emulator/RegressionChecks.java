package ru.mirea.emulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Регрессия парсера, конфигурации и исполнения стартового скрипта. */
public final class RegressionChecks {
    private RegressionChecks() { }

    /** Проверить прежнюю функциональность независимо от GUI. */
    public static void main(String[] arguments) throws Exception {
        parser();
        configuration();
        script();
        cli();
        Check.report("RegressionChecks");
    }

    private static void parser() {
        Check.equal(List.of("cd", "мои документы"), CommandParser.parse("cd \"мои документы\""));
        Check.equal(List.of("ls", "folder name"), CommandParser.parse("ls 'folder name'"));
        Check.equal(List.of("cd", "my documents"), CommandParser.parse("cd my\\ documents"));
        Check.equal(List.of("x", "abc def"), CommandParser.parse("x ab\"c d\"ef"));
        Check.equal(List.of("x", ""), CommandParser.parse("x ''"));
        Check.equal(List.of("x", "$HOME", "#name"), CommandParser.parse("x '$HOME' #name"));
        Check.rejects(() -> CommandParser.parse("ls \""));
        Check.rejects(() -> CommandParser.parse("cd \\"));
        Check.truth(new Shell().execute("unknown").error());
        Check.truth(new Shell().execute("exit 1").error());
        Check.truth(new Shell().execute("exit").exit());
        Check.equal("", new Shell().execute("   ").output());
    }

    private static void configuration() {
        AppConfig config = AppConfig.parse(new String[]{"--vfs", "a b.csv", "--startup-script", "x.txt"});
        Check.equal(Path.of("a b.csv").toAbsolutePath(), config.vfs());
        Check.equal(Path.of("x.txt").toAbsolutePath(), config.startupScript());
        Check.truth(AppConfig.parse(new String[]{"--help"}).help());
        for (String[] values : List.of(new String[]{"--bad"}, new String[]{"--vfs"},
                new String[]{"--vfs", ""}, new String[]{"--vfs", "a", "--vfs", "b"})) {
            Check.rejects(() -> AppConfig.parse(values));
        }
        Check.equal(System.getProperty("user.name"), HostIdentity.current().user());
        Check.truth(!HostIdentity.current().host().isBlank());
    }

    private static void script() throws Exception {
        Path file = Files.createTempFile("startup-check", ".txt");
        List<String> executed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Shell shell = new Shell();
        try {
            Files.writeString(file, "\nls\nunknown\nls NEVER_EXECUTED\n");
            StartupRunner.Outcome result = new StartupRunner().run(file, line -> {
                executed.add(line);
                return shell.execute(line);
            }, errors::add);
            Check.equal(StartupRunner.Status.FAILED, result.status());
            Check.equal(3, result.line());
            Check.equal(List.of("ls", "unknown"), executed);
            Check.equal(1, errors.size());
            Files.writeString(file, "ls\nexit\nunknown\n");
            Check.equal(StartupRunner.Status.EXIT,
                    new StartupRunner().run(file, shell::execute, errors::add).status());
        } finally {
            Files.delete(file);
        }
        Check.equal(StartupRunner.Status.FAILED,
                new StartupRunner().run(file, shell::execute, errors::add).status());
    }

    private static void cli() throws Exception {
        Check.equal(0, process("--help"));
        Check.equal(2, process("--unknown"));
        Check.equal(3, process("--vfs", "examples/stage3/missing.csv"));
        Check.equal(3, process("--vfs", "examples/stage3/invalid.csv"));
    }

    private static int process(String... options) throws Exception {
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"),
                "ru.mirea.emulator.Main"));
        command.addAll(List.of(options));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        return process.waitFor();
    }
}
