package ru.mirea.emulator;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Счётчики wc: LF-строки, слова UTF-8, символы Unicode и байты. */
record WordCounts(long lines, long words, long characters, long bytes) {
    static WordCounts count(byte[] data, boolean textNeeded) {
        long lines = 0;
        for (byte value : data) {
            if (value == '\n') {
                lines++;
            }
        }
        String text = textNeeded ? decode(data) : "";
        return new WordCounts(lines, words(text), text.codePointCount(0, text.length()), data.length);
    }

    private static String decode(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("wc: для -w/-m требуется UTF-8; для бинарных данных -c/-l");
        }
    }

    private static long words(String text) {
        long count = 0;
        boolean inside = false;
        for (int point : text.codePoints().toArray()) {
            boolean separator = Character.isWhitespace(point) || Character.isSpaceChar(point);
            if (!separator && !inside) {
                count++;
            }
            inside = !separator;
        }
        return count;
    }

    WordCounts plus(WordCounts other) {
        return new WordCounts(lines + other.lines, words + other.words,
                characters + other.characters, bytes + other.bytes);
    }

    String format(Set<Character> flags, String name) {
        List<String> values = new ArrayList<>();
        if (flags.isEmpty() || flags.contains('l')) {
            values.add(Long.toString(lines));
        }
        if (flags.isEmpty() || flags.contains('w')) {
            values.add(Long.toString(words));
        }
        if (flags.contains('m')) {
            values.add(Long.toString(characters));
        }
        if (flags.isEmpty() || flags.contains('c')) {
            values.add(Long.toString(bytes));
        }
        return String.join(" ", values) + " " + name;
    }
}
