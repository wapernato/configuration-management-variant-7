package ru.mirea.emulator;

/** Результат команды, включая ошибку и запрос завершения приложения. */
public record CommandResult(String output, boolean error, boolean exit) {
    /** Создать успешный результат. */
    public static CommandResult success(String output) {
        return new CommandResult(output, false, false);
    }

    /** Создать сообщение об ошибке. */
    public static CommandResult failure(String message) {
        return new CommandResult("Ошибка: " + message, true, false);
    }
}
