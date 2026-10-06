package ru.mirea.emulator;

import javax.swing.SwingUtilities;

/** Точка входа в GUI-эмулятор оболочки. */
public final class Main {
    private Main() { }

    /** Запустить интерфейс в потоке событий Swing. */
    public static void main(String[] arguments) {
        AppConfig config;
        try {
            config = AppConfig.parse(arguments);
        } catch (IllegalArgumentException exception) {
            System.err.println("Ошибка конфигурации: " + exception.getMessage());
            System.err.println(AppConfig.usage());
            System.exit(2);
            return;
        }
        if (config.help()) {
            System.out.println(AppConfig.usage());
            return;
        }
        config.debugLines().forEach(System.out::println);
        launch(config);
    }

    private static void launch(AppConfig config) {
        try {
            Shell shell = new Shell(VfsLoader.load(config.vfs()));
            System.out.println(shell.vfsSummary());
            SwingUtilities.invokeLater(() -> new TerminalWindow(config, shell).open());
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(3);
        }
    }
}
