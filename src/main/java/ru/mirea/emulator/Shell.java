package ru.mirea.emulator;

import java.util.List;
import java.util.stream.Collectors;

/** Логика этапа 1: ls и cd являются заглушками, exit завершает диалог. */
public final class Shell {
    private static final int MAX_PATH_ARGUMENTS = 1;
    private final VirtualFileSystem vfs;

    /** Создать оболочку с пустой виртуальной файловой системой. */
    public Shell() {
        this(VirtualFileSystem.empty());
    }

    /** Использовать загруженное дерево VFS в текущем сеансе. */
    public Shell(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    /** Диагностика числа загруженных узлов. */
    public String vfsSummary() {
        return vfs.summary();
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
            case "ls", "cd" -> stub(name, arguments);
            case "exit" -> exit(arguments);
            default -> CommandResult.failure("неизвестная команда: " + name);
        };
    }

    private CommandResult stub(String name, List<String> arguments) {
        if (arguments.size() > MAX_PATH_ARGUMENTS) {
            return CommandResult.failure(name + ": ожидается не более одного пути");
        }
        if (!arguments.isEmpty() && arguments.get(0).isEmpty()) {
            return CommandResult.failure(name + ": пустой путь");
        }
        if (!arguments.isEmpty() && arguments.get(0).startsWith("-")) {
            return CommandResult.failure(name + ": опции пока не поддерживаются");
        }
        String rendered = arguments.stream().map(Shell::quote)
                .collect(Collectors.joining(", ", "[", "]"));
        return CommandResult.success(name + ": аргументы = " + rendered);
    }

    private CommandResult exit(List<String> arguments) {
        if (!arguments.isEmpty()) {
            return CommandResult.failure("exit не принимает аргументы");
        }
        return new CommandResult("Завершение эмулятора.", false, true);
    }

    private static String quote(String argument) {
        return "\"" + argument.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\t", "\\t") + "\"";
    }
}
