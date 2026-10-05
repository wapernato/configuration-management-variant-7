package ru.mirea.emulator;

import java.nio.file.Path;

/** Проверки параметров, относительных путей, пробелов и ошибок конфигурации. */
final class ConfigTests {
    private ConfigTests() { }

    /** Проверить все поддерживаемые параметры и их неверные сочетания. */
    static void run(Checks checks) {
        checks.equal(AppConfig.defaults(), AppConfig.parse(new String[]{}));
        AppConfig config = AppConfig.parse(new String[]{"--vfs", "my vfs.csv",
                "--startup-script", "my script.txt"});
        checks.equal(Path.of("my vfs.csv").toAbsolutePath(), config.vfs());
        checks.equal(Path.of("my script.txt").toAbsolutePath(), config.startupScript());
        checks.truth(config.debugLines().get(0).contains("my vfs.csv"));
        checks.truth(config.debugLines().get(1).contains("my script.txt"));
        checks.truth(AppConfig.parse(new String[]{"-h"}).help());
        checks.truth(AppConfig.parse(new String[]{"--help"}).help());
        checks.equal(null, AppConfig.parse(new String[]{"--vfs", "vfs.csv"}).startupScript());
        checks.equal(null, AppConfig.parse(new String[]{"--startup-script", "s.txt"}).vfs());
        invalid(checks);
    }

    private static void invalid(Checks checks) {
        String[][] cases = {{"--wrong"}, {"--vfs"}, {"--startup-script"},
            {"--vfs", ""}, {"--vfs", "--startup-script", "s.txt"},
            {"--vfs", "one", "--vfs", "two"},
            {"--startup-script", "one", "--startup-script", "two"},
            {"--vfs", "a\0b"}};
        for (String[] arguments : cases) {
            boolean rejected = false;
            try {
                AppConfig.parse(arguments);
            } catch (IllegalArgumentException exception) {
                rejected = true;
                checks.truth(!exception.getMessage().isBlank());
            }
            checks.truth(rejected);
        }
    }
}
