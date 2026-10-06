package ru.mirea.emulator;

import java.util.Objects;

/** Независимые проверки без JUnit и без необходимости включать -ea. */
final class Check {
    private static int count;

    private Check() { }

    static void equal(Object expected, Object actual) {
        count++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Ожидалось " + expected + ", получено " + actual);
        }
    }

    static void truth(boolean value) {
        equal(true, value);
    }

    static void rejects(Runnable action) {
        count++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Ожидалась ошибка");
    }

    static void report(String suite) {
        System.out.println(suite + ": " + count + " проверок пройдено");
    }
}
