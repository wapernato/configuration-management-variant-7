package ru.mirea.emulator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Загружает UTF-8 CSV path,type,content; файлы представлены в Base64. */
public final class VfsLoader {
    private VfsLoader() { }

    /** Прочитать CSV один раз; при отсутствии параметра создать пустую VFS. */
    public static VirtualFileSystem load(Path path) {
        if (path == null) {
            return VirtualFileSystem.empty();
        }
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Ошибка загрузки VFS " + path + ": "
                    + exception.getMessage(), exception);
        }
    }

    /** Проверить весь CSV до публикации дерева; порядок записей не важен. */
    public static VirtualFileSystem parse(String text) {
        List<List<String>> rows = CsvReader.parse(text);
        if (rows.isEmpty() || !rows.get(0).equals(List.of("path", "type", "content"))) {
            throw new IllegalArgumentException("ожидается заголовок path,type,content");
        }
        Map<String, VirtualFileSystem.Entry> entries = new TreeMap<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                add(entries, rows.get(index));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("запись " + (index + 1) + ": "
                        + exception.getMessage(), exception);
            }
        }
        validateTree(entries);
        return new VirtualFileSystem(entries);
    }

    private static void add(Map<String, VirtualFileSystem.Entry> entries, List<String> row) {
        if (row.size() != 3) {
            throw new IllegalArgumentException("ожидается ровно 3 поля");
        }
        String path = row.get(0);
        if (!path.startsWith("/") || !VirtualPath.resolve("/", path).equals(path)) {
            throw new IllegalArgumentException("путь должен быть абсолютным и каноническим: " + path);
        }
        if (entries.containsKey(path)) {
            throw new IllegalArgumentException("повтор пути: " + path);
        }
        entries.put(path, decode(row.get(1), row.get(2)));
    }

    private static VirtualFileSystem.Entry decode(String type, String content) {
        if (type.equals("directory")) {
            if (!content.isEmpty()) {
                throw new IllegalArgumentException("каталог не может содержать данные");
            }
            return new VirtualFileSystem.Entry(true, new byte[0]);
        }
        if (!type.equals("file")) {
            throw new IllegalArgumentException("неизвестный тип: " + type);
        }
        return new VirtualFileSystem.Entry(false, Base64.getDecoder().decode(content));
    }

    private static void validateTree(Map<String, VirtualFileSystem.Entry> entries) {
        if (!entries.containsKey("/") || !entries.get("/").directory()) {
            throw new IllegalArgumentException("отсутствует корневой каталог /");
        }
        for (String path : entries.keySet()) {
            VirtualFileSystem.Entry parent = entries.get(VirtualPath.parent(path));
            if (parent == null || !parent.directory()) {
                throw new IllegalArgumentException(path + ": родитель должен быть существующим каталогом");
            }
        }
    }
}
