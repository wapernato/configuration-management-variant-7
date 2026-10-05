package ru.mirea.emulator;

import javax.swing.SwingUtilities;

/** Точка входа в GUI-эмулятор оболочки. */
public final class Main {
    private Main() { }

    /** Запустить интерфейс в потоке событий Swing. */
    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(() -> new TerminalWindow().open());
    }
}
