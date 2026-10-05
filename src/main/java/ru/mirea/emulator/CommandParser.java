package ru.mirea.emulator;

import java.util.ArrayList;
import java.util.List;

/** Разбирает слова, кавычки и экранирование без вызова оболочки ОС. */
public final class CommandParser {
    private CommandParser() { }

    /** Разобрать строку в аргументы; незакрытые кавычки вызывают ошибку. */
    public static List<String> parse(String line) {
        State state = new State();
        for (int index = 0; index < line.length(); index++) {
            accept(state, line.charAt(index));
        }
        if (state.escaped || state.quote != '\0') {
            throw new IllegalArgumentException(
                    "незавершённое экранирование или незакрытая кавычка");
        }
        finishWord(state);
        return List.copyOf(state.words);
    }

    private static void accept(State state, char character) {
        if (state.escaped) {
            appendEscaped(state, character);
        } else if (character == '\\' && state.quote != '\'') {
            state.escaped = true;
            state.started = true;
        } else if (state.quote != '\0') {
            acceptQuoted(state, character);
        } else {
            acceptUnquoted(state, character);
        }
    }

    private static void appendEscaped(State state, char character) {
        if (state.quote == '"' && character != '"'
                && character != '\\' && character != '$'
                && character != '`') {
            state.word.append('\\');
        }
        state.word.append(character);
        state.escaped = false;
    }

    private static void acceptQuoted(State state, char character) {
        if (character == state.quote) {
            state.quote = '\0';
        } else {
            state.word.append(character);
        }
    }

    private static void acceptUnquoted(State state, char character) {
        if (character == '\'' || character == '"') {
            state.quote = character;
            state.started = true;
        } else if (Character.isWhitespace(character)) {
            finishWord(state);
        } else {
            state.word.append(character);
            state.started = true;
        }
    }

    private static void finishWord(State state) {
        if (state.started) {
            state.words.add(state.word.toString());
            state.word.setLength(0);
            state.started = false;
        }
    }

    private static final class State {
        private final List<String> words = new ArrayList<>();
        private final StringBuilder word = new StringBuilder();
        private char quote;
        private boolean escaped;
        private boolean started;
    }
}
