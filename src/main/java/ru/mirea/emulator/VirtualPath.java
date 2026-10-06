package ru.mirea.emulator;

import java.util.ArrayDeque;
import java.util.Deque;

/** POSIX-пути внутри VFS; не обращается к файловой системе реальной ОС. */
final class VirtualPath {
    private VirtualPath() { }

    static String resolve(String current, String input) {
        if (input.isEmpty() || input.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("неверный или пустой путь");
        }
        String path = input.equals("~") ? "/" : input;
        if (path.startsWith("~/")) {
            path = path.substring(1);
        }
        String absolute = path.startsWith("/") ? path : current + "/" + path;
        Deque<String> parts = new ArrayDeque<>();
        for (String part : absolute.split("/")) {
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
            } else if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        return "/" + String.join("/", parts);
    }

    static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash <= 0 ? "/" : path.substring(0, slash);
    }

    static String name(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
