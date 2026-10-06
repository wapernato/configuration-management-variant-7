package ru.mirea.emulator;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Настройки второго этапа: пути к VFS и стартовому скрипту. */
public record AppConfig(Path vfs, Path startupScript, boolean help) {
    private static final Set<String> OPTIONS = Set.of("--vfs", "--startup-script");

    /** Настройки без заданных путей. */
    public static AppConfig defaults() {
        return new AppConfig(null, null, false);
    }

    /** Разобрать параметры, отклоняя неизвестные и повторяющиеся опции. */
    public static AppConfig parse(String[] arguments) {
        Map<String, String> values = new HashMap<>();
        boolean help = false;
        for (int index = 0; index < arguments.length; index++) {
            String option = arguments[index];
            if (option.equals("--help") || option.equals("-h")) {
                help = true;
            } else {
                validateOption(option, values);
                index++;
                values.put(option, readValue(arguments, index, option));
            }
        }
        return new AppConfig(path(values.get("--vfs")),
                path(values.get("--startup-script")), help);
    }

    private static void validateOption(String option, Map<String, String> values) {
        if (!OPTIONS.contains(option)) {
            throw new IllegalArgumentException("неизвестный параметр: " + option);
        }
        if (values.containsKey(option)) {
            throw new IllegalArgumentException("повтор параметра: " + option);
        }
    }

    private static String readValue(String[] arguments, int index, String option) {
        if (index >= arguments.length || arguments[index].isBlank()
                || arguments[index].startsWith("--")) {
            throw new IllegalArgumentException("для " + option + " требуется путь");
        }
        return arguments[index];
    }

    private static Path path(String value) {
        return value == null ? null : Path.of(value).toAbsolutePath().normalize();
    }

    /** Все параметры для отладочного вывода при каждом запуске. */
    public List<String> debugLines() {
        return List.of("[config] vfs = " + display(vfs),
                "[config] startup-script = " + display(startupScript));
    }

    private static String display(Path path) {
        return path == null ? "<не задан>" : path.toString();
    }

    /** Справка по запуску приложения. */
    public static String usage() {
        return "Использование: ./run.sh [--vfs ПУТЬ] [--startup-script ПУТЬ]\n"
                + "  --vfs             UTF-8 CSV VFS; файлы в Base64 (без параметра пустая VFS)\n"
                + "  --startup-script  UTF-8 скрипт; остановка при первой ошибке\n"
                + "  -h, --help        Показать справку без запуска GUI";
    }
}
