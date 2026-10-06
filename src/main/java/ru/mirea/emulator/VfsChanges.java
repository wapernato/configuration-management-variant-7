package ru.mirea.emulator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Изменяет только рабочую копию дерева, созданную для одной команды. */
final class VfsChanges {
    private final VirtualFileSystem vfs;
    private final Map<String, VirtualFileSystem.Entry> entries;

    VfsChanges(VirtualFileSystem vfs, Map<String, VirtualFileSystem.Entry> entries) {
        this.vfs = vfs;
        this.entries = entries;
    }

    void mkdir(String current, String input, boolean parents) {
        VirtualPath.resolve(current, input);
        String path = VirtualPath.expand(input);
        String cursor = path.startsWith("/") ? "/" : current;
        String[] parts = path.split("/", -1);
        boolean created = false;
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()) {
                continue;
            }
            vfs.requireDirectory(cursor);
            if (part.equals("..")) {
                cursor = VirtualPath.parent(cursor);
            } else if (!part.equals(".")) {
                cursor = VirtualPath.join(cursor, part);
                boolean last = Arrays.stream(parts).skip(index + 1).allMatch(String::isEmpty);
                created |= makeDirectory(cursor, parents || last);
            }
        }
        vfs.requireDirectory(cursor);
        if (!parents && !created) {
            throw new IllegalArgumentException("mkdir: " + cursor + ": уже существует");
        }
    }

    private boolean makeDirectory(String path, boolean allowed) {
        if (entries.containsKey(path)) {
            vfs.requireDirectory(path);
            return false;
        }
        if (!allowed) {
            throw new IllegalArgumentException("mkdir: " + path + ": родительский каталог не существует");
        }
        entries.put(path, new VirtualFileSystem.Entry(true, new byte[0]));
        return true;
    }

    void copy(String current, String source, String destination, boolean recursive, boolean noClobber) {
        String from = vfs.resolve(current, source);
        String to = vfs.target(current, destination);
        if (vfs.exists(to) && vfs.entry(to).directory()) {
            to = VirtualPath.join(to, VirtualPath.name(from));
        }
        validateCopy(from, to, recursive);
        if (!vfs.entry(from).directory()) {
            copyNode(from, to, noClobber);
            return;
        }
        List<String> subtree = entries.keySet().stream()
                .filter(path -> path.equals(from) || path.startsWith(from + "/")).toList();
        for (String path : subtree) {
            copyNode(path, to + path.substring(from.length()), noClobber);
        }
    }

    private void validateCopy(String from, String to, boolean recursive) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("cp: источник и назначение совпадают: " + from);
        }
        boolean directory = vfs.entry(from).directory();
        if (directory && !recursive) {
            throw new IllegalArgumentException("cp: для каталога требуется -r: " + from);
        }
        if (directory && (from.equals("/") || to.startsWith(from + "/"))) {
            throw new IllegalArgumentException("cp: нельзя копировать каталог внутрь самого себя");
        }
        vfs.requireDirectory(VirtualPath.parent(to));
    }

    private void copyNode(String from, String to, boolean noClobber) {
        VirtualFileSystem.Entry source = vfs.entry(from);
        if (entries.containsKey(to)) {
            VirtualFileSystem.Entry destination = vfs.entry(to);
            if (source.directory() != destination.directory()) {
                throw new IllegalArgumentException("cp: конфликт файла и каталога: " + to);
            }
            if (source.directory() || noClobber) {
                return;
            }
        }
        vfs.requireDirectory(VirtualPath.parent(to));
        entries.put(to, new VirtualFileSystem.Entry(source.directory(), source.data()));
    }
}
