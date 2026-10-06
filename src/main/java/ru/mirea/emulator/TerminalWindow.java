package ru.mirea.emulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Графический терминал Swing с оформлением тёмной оболочки. */
public final class TerminalWindow extends JFrame {
    private final TerminalPane terminal;
    private final AppConfig config;

    /** Открыть окно с реальными данными ОС в заголовке. */
    public TerminalWindow() {
        this(AppConfig.defaults());
    }

    /** Создать окно с параметрами второго этапа. */
    public TerminalWindow(AppConfig config) {
        this(config, new Shell(VfsLoader.load(config.vfs())));
    }

    /** Создать окно с уже проверенной VFS. */
    public TerminalWindow(AppConfig config, Shell shell) {
        this.config = config;
        HostIdentity identity = HostIdentity.current();
        setTitle(identity.title());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(650, 400));
        setSize(1040, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(chrome("●  shell", "JAVA  /  STAGES 01 - 04"), BorderLayout.NORTH);
        terminal = new TerminalPane(identity, this::dispose, shell);
        config.debugLines().forEach(line -> terminal.printMessage(line, TerminalTheme.MUTED));
        terminal.printMessage(shell.vfsSummary(), TerminalTheme.MUTED);
        add(scrollPane(), BorderLayout.CENTER);
        add(chrome("↑ / ↓  история    Ctrl+L  очистка    Ctrl+C  отмена",
                "UTF-8  ·  GUI REPL"), BorderLayout.SOUTH);
    }

    private JScrollPane scrollPane() {
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
        return scroll;
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
        if (config.startupScript() != null) {
            SwingUtilities.invokeLater(() -> new StartupRunner().run(
                    config.startupScript(), terminal::submitLine,
                    message -> terminal.printMessage(message, TerminalTheme.RED)));
        }
    }

    /** Доступ к области терминала для интеграционных проверок. */
    public TerminalPane terminal() {
        return terminal;
    }
}
