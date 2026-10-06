package ru.mirea.emulator;

import java.util.List;

/** cp и mkdir выполняются атомарно в отдельной рабочей копии VFS. */
final class AdditionalCommands {
    private final VirtualFileSystem vfs;

    AdditionalCommands(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    CommandResult mkdir(String current, List<String> arguments) {
        CommandOptions options = CommandOptions.parse(arguments, "p");
        if (options.paths().isEmpty()) {
            throw new IllegalArgumentException("mkdir: требуется хотя бы один путь");
        }
        VirtualFileSystem working = vfs.fork();
        for (String path : options.paths()) {
            working.mkdir(current, path, options.flags().contains('p'));
        }
        vfs.replaceWith(working);
        return CommandResult.success("");
    }

    CommandResult cp(String current, List<String> arguments) {
        CommandOptions options = CommandOptions.parse(arguments, "rRn");
        if (options.paths().size() < 2) {
            throw new IllegalArgumentException("cp: требуются источник и назначение");
        }
        int last = options.paths().size() - 1;
        String destination = options.paths().get(last);
        VirtualFileSystem working = vfs.fork();
        if (last > 1) {
            working.requireDirectory(working.resolve(current, destination));
        }
        boolean recursive = options.flags().contains('r') || options.flags().contains('R');
        for (String source : options.paths().subList(0, last)) {
            working.copy(current, source, destination, recursive, options.flags().contains('n'));
        }
        vfs.replaceWith(working);
        return CommandResult.success("");
    }
}
