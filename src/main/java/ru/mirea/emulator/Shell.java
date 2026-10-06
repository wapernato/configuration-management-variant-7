package ru.mirea.emulator;

import java.util.List;

/** Диспетчер команд: общий исполнитель для GUI и стартовых скриптов. */
public final class Shell {
    private final VirtualFileSystem vfs;
    private final BasicCommands commands;
    private final AdditionalCommands additional;

    /** Создать оболочку с пустой виртуальной файловой системой. */
    public Shell() {
        this(VirtualFileSystem.empty());
    }

    /** Использовать загруженное дерево VFS в текущем сеансе. */
    public Shell(VirtualFileSystem vfs) {
        this.vfs = vfs;
        commands = new BasicCommands(vfs);
        additional = new AdditionalCommands(vfs);
    }

    /** Диагностика числа загруженных узлов. */
    public String vfsSummary() {
        return vfs.summary();
    }

    /** Текущий каталог для приглашения терминала. */
    public String currentDirectory() {
        return commands.currentDirectory();
    }

    /** Исполнить одну строку и вернуть ошибку без исключения в GUI. */
    public CommandResult execute(String line) {
        try {
            List<String> words = CommandParser.parse(line);
            if (words.isEmpty()) {
                return CommandResult.success("");
            }
            return dispatch(words.get(0), words.subList(1, words.size()));
        } catch (IllegalArgumentException exception) {
            return CommandResult.failure(exception.getMessage());
        }
    }

    private CommandResult dispatch(String name, List<String> arguments) {
        return switch (name) {
            case "ls" -> commands.ls(arguments);
            case "cd" -> commands.cd(arguments);
            case "pwd" -> commands.pwd(arguments);
            case "wc" -> commands.wc(arguments);
            case "echo" -> commands.echo(arguments);
            case "cp" -> additional.cp(currentDirectory(), arguments);
            case "mkdir" -> additional.mkdir(currentDirectory(), arguments);
            case "exit" -> exit(arguments);
            default -> CommandResult.failure("неизвестная команда: " + name);
        };
    }

    private CommandResult exit(List<String> arguments) {
        if (!arguments.isEmpty()) {
            return CommandResult.failure("exit не принимает аргументы");
        }
        return new CommandResult("Завершение эмулятора.", false, true);
    }
}
