package ru.mirea.emulator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;

/** Единая область терминала: защищённая история и ввод после приглашения. */
public final class TerminalPane extends JTextPane {
    private static final int HISTORY_LIMIT = 500;
    private final Shell shell = new Shell();
    private final List<String> history = new ArrayList<>();
    private final HostIdentity identity;
    private final Runnable onExit;
    private int inputStart;
    private int historyPosition;
    private String draft = "";
    private boolean writing;

    /** Создать терминал с историей и клавиатурными действиями. */
    public TerminalPane(HostIdentity identity, Runnable onExit) {
        this.identity = identity;
        this.onExit = onExit;
        setFont(TerminalTheme.font());
        setBackground(TerminalTheme.BACKGROUND);
        setForeground(TerminalTheme.FOREGROUND);
        setCaretColor(TerminalTheme.GREEN);
        setSelectionColor(new Color(0x2D4663));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        ((AbstractDocument) getDocument()).setDocumentFilter(new InputFilter());
        bind("ENTER", "submit", this::submit);
        bind("UP", "history-up", () -> navigateHistory(-1));
        bind("DOWN", "history-down", () -> navigateHistory(1));
        bind("ctrl L", "clear", this::clearScreen);
        bind("ctrl C", "cancel", this::cancelInput);
        bind("HOME", "input-home", () -> setCaretPosition(inputStart));
        append("SHELL EMULATOR  /  VARIANT 07\n", TerminalTheme.BLUE);
        append("ls [путь]  ·  cd [путь]  ·  exit\n\n", TerminalTheme.MUTED);
        prompt();
    }

    private void bind(String key, String name, Runnable action) {
        getInputMap().put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    /** Сглаживать моноширинный текст при отображении и сохранении превью. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D smooth = (Graphics2D) graphics.create();
        smooth.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paintComponent(smooth);
        smooth.dispose();
    }

    /** Исполнить текущую строку, записать результат и показать приглашение. */
    public void submit() {
        String line = input();
        append("\n", TerminalTheme.FOREGROUND);
        remember(line);
        CommandResult result = shell.execute(line);
        if (!result.output().isEmpty()) {
            append(result.output() + "\n", result.error()
                    ? TerminalTheme.RED : TerminalTheme.FOREGROUND);
        }
        if (result.exit()) {
            onExit.run();
        } else {
            prompt();
        }
    }

    /** Получить только редактируемую часть текущей строки. */
    public String input() {
        try {
            return getDocument().getText(inputStart,
                    getDocument().getLength() - inputStart);
        } catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** Заменить текущий ввод, сохранив предыдущий диалог. */
    public void replaceInput(String text) {
        select(inputStart, getDocument().getLength());
        replaceSelection(text);
        setCaretPosition(getDocument().getLength());
    }

    private void remember(String line) {
        if (!line.isBlank()) {
            history.add(line);
            if (history.size() > HISTORY_LIMIT) {
                history.remove(0);
            }
        }
        historyPosition = history.size();
        draft = "";
    }

    private void navigateHistory(int direction) {
        if (historyPosition == history.size()) {
            draft = input();
        }
        historyPosition = Math.max(0,
                Math.min(history.size(), historyPosition + direction));
        replaceInput(historyPosition == history.size()
                ? draft : history.get(historyPosition));
    }

    private void clearScreen() {
        String current = input();
        writing = true;
        setText("");
        writing = false;
        prompt();
        replaceInput(current);
    }

    private void cancelInput() {
        if (getSelectedText() != null) {
            copy();
            return;
        }
        append("^C\n", TerminalTheme.MUTED);
        historyPosition = history.size();
        draft = "";
        prompt();
    }

    private void prompt() {
        append(identity.user() + "@" + identity.host(), TerminalTheme.GREEN);
        append(":", TerminalTheme.MUTED);
        append("~", TerminalTheme.BLUE);
        append("$ ", TerminalTheme.FOREGROUND);
        inputStart = getDocument().getLength();
        setCharacterAttributes(TerminalTheme.style(TerminalTheme.FOREGROUND), true);
        setCaretPosition(inputStart);
    }

    private void append(String text, Color color) {
        writing = true;
        try {
            getStyledDocument().insertString(getDocument().getLength(), text,
                    TerminalTheme.style(color));
        } catch (BadLocationException exception) {
            throw new IllegalStateException(exception);
        } finally {
            writing = false;
        }
        setCaretPosition(getDocument().getLength());
    }

    private final class InputFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass bypass, int offset,
                String text, AttributeSet attributes) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass bypass, int offset, int length,
                String text, AttributeSet attributes) throws BadLocationException {
            if (writing) {
                bypass.replace(offset, length, text, attributes);
            } else if (offset >= inputStart) {
                String singleLine = text == null ? ""
                        : text.replace('\n', ' ').replace('\r', ' ');
                bypass.replace(offset, length, singleLine,
                        TerminalTheme.style(TerminalTheme.FOREGROUND));
            }
        }

        @Override
        public void remove(FilterBypass bypass, int offset, int length)
                throws BadLocationException {
            if (writing || offset >= inputStart) {
                bypass.remove(offset, length);
            }
        }
    }
}
