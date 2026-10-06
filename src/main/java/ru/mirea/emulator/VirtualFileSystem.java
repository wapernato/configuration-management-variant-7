package ru.mirea.emulator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Дерево VFS хранится только в памяти; содержимое файлов не выходит в ОС. */
public final class VirtualFileSystem {
    /** Узел дерева с защитным копированием двоичных данных. */
    public record Entry(boolean directory, byte[] data) {
        /** Создать независимый от исходного массива узел. */
        public Entry {
            data = data.clone();
        }

        /** Получить копию содержимого, не раскрывая внутренний массив. */
        @Override
        public byte[] data() {
            return data.clone();
        }
    }

    private final Map<String, Entry> entries;

    VirtualFileSystem(Map<String, Entry> entries) {
        this.entries = new TreeMap<>(entries);
    }

    /** Пустая VFS с единственным корневым каталогом. */
    public static VirtualFileSystem empty() {
        return new VirtualFileSystem(Map.of("/", new Entry(true, new byte[0])));
    }

    /** Разрешить путь с проверкой промежуточных каталогов и завершающего /. */
    public String resolve(String current, String input) {
        VirtualPath.resolve(current, input);
        String path = input.equals("~") ? "/" : input;
        if (path.startsWith("~/")) {
            path = path.substring(1);
        }
        String cursor = path.startsWith("/") ? "/" : current;
        for (String part : path.split("/", -1)) {
            if (part.isEmpty()) {
                continue;
            }
            requireDirectory(cursor);
            if (part.equals("..")) {
                cursor = VirtualPath.parent(cursor);
            } else if (!part.equals(".")) {
                cursor = cursor.equals("/") ? "/" + part : cursor + "/" + part;
                entry(cursor);
            }
        }
        if (path.endsWith("/")) {
            requireDirectory(cursor);
        }
        return cursor;
    }

    /** Узел по абсолютному нормализованному пути. */
    public Entry entry(String path) {
        Entry result = entries.get(path);
        if (result == null) {
            throw new IllegalArgumentException(path + ": файл или каталог не найден");
        }
        return result;
    }

    /** Прочитать файл; обращение к каталогу является ошибкой. */
    public byte[] read(String path) {
        Entry node = entry(path);
        if (node.directory()) {
            throw new IllegalArgumentException(path + ": это каталог");
        }
        return node.data();
    }

    /** Непосредственные потомки каталога, отсортированные по имени. */
    public List<String> children(String path) {
        requireDirectory(path);
        return entries.keySet().stream().filter(candidate -> !candidate.equals("/")
                && VirtualPath.parent(candidate).equals(path)).toList();
    }

    /** Проверить существование каталога. */
    public void requireDirectory(String path) {
        if (!entry(path).directory()) {
            throw new IllegalArgumentException(path + ": не каталог");
        }
    }

    /** Число каталогов и файлов для диагностики загрузки. */
    public String summary() {
        long directories = entries.values().stream().filter(Entry::directory).count();
        return "[vfs] каталогов: " + directories + ", файлов: " + (entries.size() - directories);
    }
}
