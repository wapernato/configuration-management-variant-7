package ru.mirea.emulator;

import java.util.ArrayList;
import java.util.List;

/** Основные команды этапа 4; пути и содержимое относятся только к VFS. */
final class BasicCommands {
    private final VirtualFileSystem vfs;
    private String current = "/";
    private String previous;

    BasicCommands(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    String currentDirectory() {
        return current;
    }

    CommandResult cd(List<String> arguments) {
        if (arguments.size() > 1) {
            throw new IllegalArgumentException("cd: ожидается не более одного пути");
        }
        String input = arguments.isEmpty() ? "/" : arguments.get(0);
        boolean back = input.equals("-");
        if (back && previous == null) {
            throw new IllegalArgumentException("cd: предыдущий каталог не задан");
        }
        String target = vfs.resolve(current, back ? previous : input);
        vfs.requireDirectory(target);
        previous = current;
        current = target;
        return CommandResult.success(back ? current : "");
    }

    CommandResult pwd(List<String> arguments) {
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException("pwd не принимает аргументы");
        }
        return CommandResult.success(current);
    }

    CommandResult ls(List<String> arguments) {
        CommandOptions options = CommandOptions.parse(arguments, "al");
        List<String> paths = options.paths().isEmpty() ? List.of(".") : options.paths();
        List<String> blocks = new ArrayList<>();
        for (String input : paths) {
            String path = vfs.resolve(current, input);
            String output = list(path, options);
            blocks.add(paths.size() > 1 ? input + ":\n" + output : output);
        }
        return CommandResult.success(String.join("\n\n", blocks));
    }

    private String list(String path, CommandOptions options) {
        if (!vfs.entry(path).directory()) {
            return describe(path, VirtualPath.name(path), options);
        }
        List<String> output = new ArrayList<>();
        if (options.flags().contains('a')) {
            output.add(describe(path, ".", options));
            output.add(describe(VirtualPath.parent(path), "..", options));
        }
        for (String child : vfs.children(path)) {
            String name = VirtualPath.name(child);
            if (!name.startsWith(".") || options.flags().contains('a')) {
                output.add(describe(child, name, options));
            }
        }
        return String.join("\n", output);
    }

    private String describe(String path, String name, CommandOptions options) {
        VirtualFileSystem.Entry entry = vfs.entry(path);
        String display = name + (entry.directory() ? "/" : "");
        return options.flags().contains('l')
                ? (entry.directory() ? "d 0 " : "f " + entry.data().length + " ") + display : display;
    }

    CommandResult wc(List<String> arguments) {
        CommandOptions options = CommandOptions.parse(arguments, "lwcm");
        if (options.paths().isEmpty()) {
            throw new IllegalArgumentException("wc: требуется хотя бы один файл; stdin не поддерживается");
        }
        boolean text = options.flags().isEmpty() || options.flags().contains('w') || options.flags().contains('m');
        WordCounts total = new WordCounts(0, 0, 0, 0);
        List<String> output = new ArrayList<>();
        for (String input : options.paths()) {
            WordCounts count = WordCounts.count(vfs.read(vfs.resolve(current, input)), text);
            total = total.plus(count);
            output.add(count.format(options.flags(), input));
        }
        if (options.paths().size() > 1) {
            output.add(total.format(options.flags(), "total"));
        }
        return CommandResult.success(String.join("\n", output));
    }

    CommandResult echo(List<String> arguments) {
        boolean newline = arguments.isEmpty() || !arguments.get(0).equals("-n");
        List<String> words = newline ? arguments : arguments.subList(1, arguments.size());
        return CommandResult.success(String.join(" ", words), newline);
    }
}
