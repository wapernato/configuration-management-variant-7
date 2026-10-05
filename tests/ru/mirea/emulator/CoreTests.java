package ru.mirea.emulator;

import java.util.List;

/** Проверки кавычек, заглушек, ошибок и данных ОС без графического сеанса. */
public final class CoreTests {
    private final Checks checks = new Checks();
    private final Shell shell = new Shell();

    /** Запустить проверки; AssertionError возвращает ненулевой код процесса. */
    public static void main(String[] arguments) {
        CoreTests suite = new CoreTests();
        suite.parser();
        suite.stubs();
        suite.errors();
        suite.identity();
        suite.checks.report("CoreTests");
    }

    private void parser() {
        checks.equal(List.of("cd", "мои документы"),
                CommandParser.parse("cd \"мои документы\""));
        checks.equal(List.of("ls", "folder name"),
                CommandParser.parse("ls 'folder name'"));
        checks.equal(List.of("cd", "my documents"),
                CommandParser.parse("cd my\\ documents"));
        checks.equal(List.of("cd", "a\"b"),
                CommandParser.parse("cd \"a\\\"b\""));
        checks.equal(List.of("cd", "abc def"),
                CommandParser.parse("cd ab\"c d\"ef"));
        checks.equal(List.of("cd", ""), CommandParser.parse("cd \"\""));
        checks.equal(List.of("ls", "$HOME"), CommandParser.parse("ls '$HOME'"));
        checks.equal(List.of("ls", "#file"), CommandParser.parse("ls #file"));
        checks.equal(List.of("ls", "a\\b"), CommandParser.parse("ls 'a\\b'"));
        checks.equal(List.of("ls", "a\\q"), CommandParser.parse("ls \"a\\q\""));
        checks.equal(List.of(), CommandParser.parse(" \t "));
    }

    private void stubs() {
        checks.equal("ls: аргументы = []", shell.execute("ls").output());
        checks.equal("cd: аргументы = []", shell.execute("cd").output());
        CommandResult path = shell.execute("cd \"мои документы\"");
        checks.equal("cd: аргументы = [\"мои документы\"]", path.output());
        checks.equal(false, path.error());
        checks.equal(false, path.exit());
        checks.equal("", shell.execute("  ").output());
        checks.truth(shell.execute("exit").exit());
        checks.equal(false, shell.execute("exit").error());
    }

    private void errors() {
        for (String command : List.of("pwd", "ls a b", "cd a b", "cd -x",
                "ls -l", "ls \"\"", "cd ''", "ls \"", "cd \\", "exit 1")) {
            CommandResult result = shell.execute(command);
            checks.truth(result.error());
            checks.truth(result.output().startsWith("Ошибка:"));
            checks.equal(false, result.exit());
        }
        checks.equal(false, shell.execute("ls").error());
    }

    private void identity() {
        checks.equal("Эмулятор - [student@host]",
                new HostIdentity("student", "host").title());
        HostIdentity current = HostIdentity.current();
        checks.equal(System.getProperty("user.name"), current.user());
        checks.truth(!current.host().isBlank());
    }
}
