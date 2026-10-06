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
        String path = expand(input);
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

    static String expand(String input) {
        if (input.equals("~")) {
            return "/";
        }
        return input.startsWith("~/") ? input.substring(1) : input;
    }

    static String join(String parent, String name) {
        return parent.equals("/") ? "/" + name : parent + "/" + name;
    }

    static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash <= 0 ? "/" : path.substring(0, slash);
    }

    static String name(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
