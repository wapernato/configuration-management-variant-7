package ru.mirea.emulator;

import java.util.Objects;

/** Проверки без сторонних библиотек и флага -ea. */
final class Checks {
    private int count;

    /** Сравнить ожидаемое и фактическое значения. */
    void equal(Object expected, Object actual) {
        count++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Ожидалось: " + expected + "; получено: " + actual);
        }
    }

    /** Проверить истинность условия. */
    void truth(boolean condition) {
        equal(true, condition);
    }

    /** Напечатать число успешно пройденных проверок. */
    void report(String suite) {
        System.out.println(suite + ": " + count + " проверок пройдено");
    }
}
