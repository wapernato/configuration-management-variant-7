package ru.mirea.emulator;

import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Проверка реального окна, интерактивного ввода и стартового сценария. */
public final class GuiChecks {
    private GuiChecks() { }

    /** Все действия с компонентами выполняются в потоке событий Swing. */
    public static void main(String[] arguments) throws Exception {
        SwingUtilities.invokeAndWait(GuiChecks::interactive);
        SwingUtilities.invokeAndWait(GuiChecks::startup);
        Check.report("GuiChecks");
    }

    private static TerminalWindow window() {
        AppConfig config = new AppConfig(Path.of("examples/stage3/deep.csv"), null, false);
        TerminalWindow window = new TerminalWindow(config);
        window.open();
        return window;
    }

    private static void interactive() {
        TerminalWindow window = window();
        try {
            Check.equal(HostIdentity.current().title(), window.getTitle());
            Check.truth(window.isShowing());
            TerminalPane pane = window.terminal();
            Check.truth(!pane.submitLine("ls -al").error());
            Check.truth(pane.getText().contains("hello.txt"));
            pane.submitLine("cd /a/b/c");
            Check.truth(pane.getText().endsWith("/a/b/c$ "));
            pane.replaceInput("draft");
            action(pane, "history-up");
            Check.equal("cd /a/b/c", pane.input());
            action(pane, "history-down");
            Check.equal("draft", pane.input());
            action(pane, "clear");
            Check.equal("draft", pane.input());
            Check.truth(!pane.getText().contains("hello.txt"));
            action(pane, "cancel");
            Check.equal("", pane.input());
            protectedOutput(pane);
            pane.submitLine("ls");
            Check.truth(pane.submitLine("cd /missing").error());
            Check.truth(!pane.submitLine("echo 'Продолжаем после ошибки'").error());
            preview(window);
            pane.submitLine("exit");
            Check.equal(false, window.isDisplayable());
        } finally {
            window.dispose();
        }
    }

    private static void protectedOutput(TerminalPane pane) {
        String before = pane.getText();
        pane.select(0, 3);
        pane.replaceSelection("");
        Check.equal(before, pane.getText());
        pane.setCaretPosition(0);
        pane.replaceSelection("ls");
        Check.truth(pane.getText().startsWith(before));
        Check.equal("ls", pane.input());
    }

    private static void startup() {
        TerminalWindow window = window();
        try {
            var errors = new ArrayList<String>();
            StartupRunner.Outcome result = new StartupRunner().run(
                    Path.of("examples/stage4/startup-success.txt"), window.terminal()::submitLine, errors::add);
            Check.equal(StartupRunner.Status.EXIT, result.status());
            Check.truth(errors.isEmpty());
            Check.equal(false, window.isDisplayable());
        } finally {
            window.dispose();
        }
    }

    private static void action(TerminalPane pane, String name) {
        pane.getActionMap().get(name).actionPerformed(new ActionEvent(pane, 0, name));
    }

    private static void preview(TerminalWindow window) {
        String path = System.getenv("PREVIEW_PATH");
        if (path == null) {
            return;
        }
        BufferedImage image = new BufferedImage(window.getWidth(), window.getHeight(), BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        window.printAll(graphics);
        graphics.dispose();
        try {
            ImageIO.write(image, "png", Path.of(path).toFile());
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
