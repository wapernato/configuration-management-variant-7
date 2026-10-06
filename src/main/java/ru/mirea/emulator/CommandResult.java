package ru.mirea.emulator;

/** Результат команды, включая ошибку и запрос завершения приложения. */
public record CommandResult(String output, boolean error, boolean exit, boolean newline) {
    /** Обычный результат: непустой вывод завершается переводом строки. */
    public CommandResult(String output, boolean error, boolean exit) {
        this(output, error, exit, !output.isEmpty());
    }
    /** Создать успешный результат. */
    public static CommandResult success(String output) {
        return new CommandResult(output, false, false);
    }

    /** Управлять завершающим переводом строки, например для echo -n. */
    public static CommandResult success(String output, boolean newline) {
        return new CommandResult(output, false, false, newline);
    }

    /** Создать сообщение об ошибке. */
    public static CommandResult failure(String message) {
        return new CommandResult("Ошибка: " + message, true, false);
    }
}
