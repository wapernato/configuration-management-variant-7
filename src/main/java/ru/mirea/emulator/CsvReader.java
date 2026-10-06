package ru.mirea.emulator;

import java.util.ArrayList;
import java.util.List;

/** Читает CSV с запятыми, CRLF, кавычками и переводами строк внутри полей. */
final class CsvReader {
    private final List<List<String>> rows = new ArrayList<>();
    private final List<String> row = new ArrayList<>();
    private final StringBuilder field = new StringBuilder();
    private boolean quoted;
    private boolean closed;

    private CsvReader() { }

    static List<List<String>> parse(String text) {
        CsvReader reader = new CsvReader();
        String source = text.startsWith("\uFEFF") ? text.substring(1) : text;
        for (int index = 0; index < source.length(); index++) {
            char value = source.charAt(index);
            if (value == '"' && reader.quoted && index + 1 < source.length()
                    && source.charAt(index + 1) == '"') {
                reader.field.append('"');
                index++;
            } else {
                reader.accept(value);
                if (value == '\r' && !reader.quoted && index + 1 < source.length()
                        && source.charAt(index + 1) == '\n') {
                    index++;
                }
            }
        }
        return reader.finish();
    }

    private void accept(char value) {
        if (quoted) {
            if (value == '"') {
                quoted = false;
                closed = true;
            } else {
                field.append(value);
            }
        } else if (value == ',') {
            endField();
        } else if (value == '\n' || value == '\r') {
            endRow();
        } else {
            acceptText(value);
        }
    }

    private void acceptText(char value) {
        if (closed || (value == '"' && field.length() != 0)) {
            throw new IllegalArgumentException("CSV: неверные кавычки в записи " + (rows.size() + 1));
        }
        if (value == '"') {
            quoted = true;
        } else {
            field.append(value);
        }
    }

    private void endField() {
        row.add(field.toString());
        field.setLength(0);
        closed = false;
    }

    private void endRow() {
        endField();
        rows.add(List.copyOf(row));
        row.clear();
    }

    private List<List<String>> finish() {
        if (quoted) {
            throw new IllegalArgumentException("CSV: незакрытые кавычки");
        }
        if (closed || field.length() != 0 || !row.isEmpty()) {
            endRow();
        }
        return List.copyOf(rows);
    }
}
