package ru.mirea.emulator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Проверить реальные коды выхода CLI без запуска GUI. */
public final class CliTests {
    private final Checks checks = new Checks();

    /** Запустить отдельные JVM для справки и ошибок параметров. */
    public static void main(String[] arguments) throws Exception {
        CliTests suite = new CliTests();
        suite.checkProcess(0, "Использование:", "--help", "--vfs", "my vfs.csv",
                "--startup-script", "my script.txt");
        suite.checkProcess(2, "требуется путь", "--vfs");
        suite.checkProcess(2, "требуется путь", "--startup-script");
        suite.checkProcess(2, "неизвестный параметр", "--unknown");
        suite.checkProcess(2, "повтор параметра", "--vfs", "a", "--vfs", "b");
        suite.checks.report("CliTests");
    }

    private void checkProcess(int code, String message, String... options) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = new ArrayList<>(List.of(java, "-Djava.awt.headless=true",
                "-cp", System.getProperty("java.class.path"), "ru.mirea.emulator.Main"));
        command.addAll(List.of(options));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        checks.equal(code, process.waitFor());
        checks.truth(output.contains(message));
        checks.equal(false, output.contains("HeadlessException"));
    }
}
