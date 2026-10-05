package ru.mirea.emulator;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Интеграционные проверки настоящего окна и действий Swing. */
public final class GuiTests {
    private final Checks checks = new Checks();

    /** Проверить окно в графическом сеансе и завершить его после теста. */
    public static void main(String[] arguments) throws Exception {
        GuiTests suite = new GuiTests();
        SwingUtilities.invokeAndWait(suite::run);
        suite.checks.report("GuiTests");
    }

    private void run() {
        TerminalWindow window = new TerminalWindow();
        try {
            window.open();
            checks.equal(HostIdentity.current().title(), window.getTitle());
            dialog(window.terminal());
            history(window.terminal());
            protection(window.terminal());
            preview(window);
            window.terminal().replaceInput("exit");
            action(window.terminal(), "submit");
            checks.equal(false, window.isDisplayable());
        } finally {
            window.dispose();
        }
    }

    private void dialog(TerminalPane terminal) {
        for (String command : new String[]{"ls", "cd \"мои документы\"", "pwd", "ls"}) {
            terminal.replaceInput(command);
            action(terminal, "submit");
        }
        checks.truth(terminal.getText().contains("cd: аргументы = [\"мои документы\"]"));
        checks.truth(terminal.getText().contains("Ошибка: неизвестная команда: pwd"));
        checks.equal("", terminal.input());
    }

    private void history(TerminalPane terminal) {
        terminal.replaceInput("draft");
        action(terminal, "history-up");
        checks.equal("ls", terminal.input());
        action(terminal, "history-up");
        checks.equal("pwd", terminal.input());
        action(terminal, "history-down");
        action(terminal, "history-down");
        checks.equal("draft", terminal.input());
        action(terminal, "clear");
        checks.equal("draft", terminal.input());
        checks.equal(false, terminal.getText().contains("неизвестная команда"));
        terminal.select(terminal.getDocument().getLength(),
                terminal.getDocument().getLength());
        action(terminal, "cancel");
        checks.equal("", terminal.input());
        checks.truth(terminal.getText().contains("draft^C"));
    }

    private void protection(TerminalPane terminal) {
        String text = terminal.getText();
        terminal.select(0, text.length());
        terminal.replaceSelection("");
        checks.equal(text, terminal.getText());
        terminal.replaceInput("ls\ncd\rtest");
        checks.equal("ls cd test", terminal.input());
        terminal.replaceInput("");
    }

    private void action(TerminalPane terminal, String name) {
        terminal.getActionMap().get(name).actionPerformed(
                new ActionEvent(terminal, ActionEvent.ACTION_PERFORMED, name));
    }

    private void preview(TerminalWindow window) {
        String destination = System.getenv("PREVIEW_PATH");
        if (destination == null) {
            return;
        }
        TerminalPane terminal = window.terminal();
        for (String command : new String[]{"ls", "cd \"мои документы\"", "ls a b"}) {
            terminal.replaceInput(command);
            terminal.submit();
        }
        BufferedImage image = new BufferedImage(window.getContentPane().getWidth(),
                window.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
        window.getContentPane().paint(image.getGraphics());
        try {
            ImageIO.write(image, "png", Path.of(destination).toFile());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
