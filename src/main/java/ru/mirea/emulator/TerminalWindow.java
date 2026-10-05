package ru.mirea.emulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Графический терминал Swing с оформлением тёмной оболочки. */
public final class TerminalWindow extends JFrame {
    private final TerminalPane terminal;

    /** Открыть окно с реальными данными ОС в заголовке. */
    public TerminalWindow() {
        HostIdentity identity = HostIdentity.current();
        setTitle(identity.title());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(650, 400));
        setSize(1040, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(chrome("●  shell", "JAVA  /  STAGE 01"), BorderLayout.NORTH);
        terminal = new TerminalPane(identity, this::dispose);
        JScrollPane scroll = new JScrollPane(terminal);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = TerminalTheme.MUTED;
                trackColor = TerminalTheme.BACKGROUND;
            }
        });
        add(scroll, BorderLayout.CENTER);
        add(chrome("↑ / ↓  история    Ctrl+L  очистка    Ctrl+C  отмена",
                "UTF-8  ·  GUI REPL"), BorderLayout.SOUTH);
    }

    private JPanel chrome(String left, String right) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(TerminalTheme.CHROME);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        JLabel primary = new JLabel(left);
        primary.setForeground(TerminalTheme.MUTED);
        JLabel secondary = new JLabel(right, SwingConstants.RIGHT);
        secondary.setForeground(TerminalTheme.MUTED);
        panel.add(primary, BorderLayout.WEST);
        panel.add(secondary, BorderLayout.EAST);
        return panel;
    }

    /** Показать окно и передать фокус строке ввода. */
    public void open() {
        setVisible(true);
        terminal.requestFocusInWindow();
    }

    /** Доступ к области терминала для интеграционных проверок. */
    public TerminalPane terminal() {
        return terminal;
    }
}
