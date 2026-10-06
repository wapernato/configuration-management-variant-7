package ru.mirea.emulator;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.swing.SwingUtilities;

/** Реальная семантика ls/cd/wc/echo, Unicode, пути и приглашение GUI. */
public final class BasicChecks {
    private final Shell shell = new Shell(VfsLoader.load(Path.of("examples/stage3/deep.csv")));

    private BasicChecks() { }

    /** Проверить команды в памяти и компонент терминала в потоке Swing. */
    public static void main(String[] arguments) throws Exception {
        BasicChecks suite = new BasicChecks();
        suite.list();
        suite.navigation();
        suite.counts();
        suite.echo();
        suite.errors();
        suite.unicode();
        SwingUtilities.invokeAndWait(suite::terminal);
        Check.report("BasicChecks");
    }

    private void list() {
        Check.truth(ok("ls").contains("hello.txt"));
        Check.truth(!ok("ls").contains(".hidden"));
        Check.truth(ok("ls -a").startsWith("./\n../\n.hidden"));
        Check.truth(ok("ls -la").contains("f 24 hello.txt"));
        Check.equal("hello.txt", ok("ls /hello.txt"));
        Check.equal("deep.txt", ok("ls /a/b/c"));
        Check.equal("a,b.txt", ok("ls 'папка с пробелами'"));
        Check.truth(ok("ls /hello.txt /empty.txt").contains("/empty.txt:\nempty.txt"));
        Check.equal(ok("ls -a -l"), ok("ls -al"));
    }

    private void navigation() {
        Check.truth(shell.execute("cd -").error());
        Check.equal("", ok("cd /a/b/c"));
        Check.equal("/a/b/c", ok("pwd"));
        Check.equal("deep.txt", ok("ls ."));
        Check.equal("", ok("cd .."));
        Check.equal("/a/b", shell.currentDirectory());
        Check.equal("/a/b/c", ok("cd -"));
        ok("cd ../../..");
        Check.equal("/", ok("pwd"));
        ok("cd 'мои документы'");
        Check.equal("текст.txt", ok("ls"));
        ok("cd ~");
        ok("cd");
        Check.equal("/", shell.currentDirectory());
        Check.truth(shell.execute("cd hello.txt").error());
        Check.equal("/", shell.currentDirectory());
        Check.truth(shell.execute("ls /hello.txt/..").error());
        Check.truth(shell.execute("ls /hello.txt/").error());
    }

    private void counts() {
        Check.equal("2 4 24 hello.txt", ok("wc hello.txt"));
        Check.equal("2 hello.txt", ok("wc -l hello.txt"));
        Check.equal("4 hello.txt", ok("wc -w hello.txt"));
        Check.equal("24 hello.txt", ok("wc -c hello.txt"));
        Check.equal("24 hello.txt", ok("wc -m hello.txt"));
        Check.equal("2 4 24 hello.txt", ok("wc -lwc hello.txt"));
        Check.equal("0 0 0 empty.txt", ok("wc empty.txt"));
        Check.equal("4 binary.bin", ok("wc -c binary.bin"));
        Check.equal("1 binary.bin", ok("wc -l binary.bin"));
        Check.truth(shell.execute("wc binary.bin").error());
        Check.equal("2 hello.txt\n0 empty.txt\n2 total", ok("wc -l hello.txt empty.txt"));
    }

    private void echo() {
        Check.equal("a b c d", ok("echo 'a b' c d"));
        Check.equal("$HOME #x", ok("echo '$HOME' #x"));
        Check.equal("", ok("echo"));
        Check.truth(shell.execute("echo").newline());
        Check.equal("hello", ok("echo -n hello"));
        Check.equal(false, shell.execute("echo -n hello").newline());
        Check.equal("-x", ok("echo -x"));
    }

    private void errors() {
        for (String command : List.of("ls -x", "ls ''", "ls /missing", "cd a b", "cd ''",
                "wc", "wc /", "wc -x hello.txt", "pwd extra", "echo \"", "exit 1")) {
            Check.truth(shell.execute(command).error());
        }
        Check.truth(!shell.execute("ls").error());
        Shell dash = new Shell(VfsLoader.parse("path,type,content\n/,directory,\n/-name,file,eA==\n"));
        Check.equal("-name", dash.execute("ls -- -name").output());
        Check.equal("1 -name", dash.execute("wc -c -- -name").output());
    }

    private void unicode() {
        String text = "Привет мир\n🙂\u00a0друг";
        String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        Shell unicode = new Shell(VfsLoader.parse("path,type,content\n/,directory,\n/u,file," + encoded));
        Check.equal("1 4 17 34 u", unicode.execute("wc -lwmc u").output());
        Check.equal("1 u", unicode.execute("wc -l u").output());
    }

    private void terminal() {
        TerminalPane pane = new TerminalPane(new HostIdentity("student", "host"), () -> { }, shell);
        Check.truth(!pane.submitLine("cd /a/b/c").error());
        Check.truth(pane.getText().endsWith("student@host:/a/b/c$ "));
        pane.submitLine("echo -n TOKEN");
        Check.truth(pane.getText().endsWith("TOKENstudent@host:/a/b/c$ "));
        pane.submitLine("echo");
        Check.truth(pane.getText().contains("$ echo\n\nstudent@host"));
        Check.truth(pane.submitLine("cd /missing").error());
        Check.truth(!pane.submitLine("ls").error());
    }

    private String ok(String line) {
        CommandResult result = shell.execute(line);
        if (result.error()) {
            throw new AssertionError(line + ": " + result.output());
        }
        return result.output();
    }
}
