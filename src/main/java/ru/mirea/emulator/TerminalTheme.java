package ru.mirea.emulator;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/** Цвета и шрифт терминала с безопасным моноширинным запасным вариантом. */
public final class TerminalTheme {
    public static final Color BACKGROUND = new Color(0x10151C);
    public static final Color CHROME = new Color(0x181F29);
    public static final Color FOREGROUND = new Color(0xDBE5F0);
    public static final Color MUTED = new Color(0x8796AA);
    public static final Color GREEN = new Color(0x8BD5A0);
    public static final Color BLUE = new Color(0x8CBDF5);
    public static final Color RED = new Color(0xFF8993);
    public static final int FONT_SIZE = 15;

    private TerminalTheme() { }

    /** Выбрать установленный моноширинный шрифт. */
    public static Font font() {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String name : new String[]{"JetBrains Mono", "Menlo", "Consolas"}) {
            if (Arrays.asList(installed).contains(name)) {
                return new Font(name, Font.PLAIN, FONT_SIZE);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE);
    }

    /** Создать атрибуты цвета текста. */
    public static SimpleAttributeSet style(Color color) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        return attributes;
    }
}
