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
        SwingUtilities.invokeLater(() -> new TerminalWindow(config).open());
    }
}
