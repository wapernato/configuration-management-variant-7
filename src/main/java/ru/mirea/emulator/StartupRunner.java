package ru.mirea.emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

/** Исполняет стартовый скрипт до первой ошибки или команды exit. */
public final class StartupRunner {
    /** Причина окончания исполнения файла. */
    public enum Status { COMPLETED, FAILED, EXIT }

    /** Статус и номер последней прочитанной физической строки. */
    public record Outcome(Status status, int line) { }

    /** Читать UTF-8 файл и показывать ввод/вывод через общий исполнитель GUI. */
    public Outcome run(Path path, Function<String, CommandResult> execute,
            Consumer<String> errorOutput) {
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                CommandResult result = executeAndEcho(line, execute);
                if (result.error()) {
                    report(path, lineNumber, result.output(), errorOutput);
                    return new Outcome(Status.FAILED, lineNumber);
                }
                if (result.exit()) {
                    return new Outcome(Status.EXIT, lineNumber);
                }
            }
            return new Outcome(Status.COMPLETED, lineNumber);
        } catch (IOException exception) {
            report(path, lineNumber, "невозможно прочитать UTF-8 файл: "
                    + exception.getMessage(), errorOutput);
            return new Outcome(Status.FAILED, lineNumber);
        }
    }

    private CommandResult executeAndEcho(String line,
            Function<String, CommandResult> execute) {
        System.out.println("$ " + line);
        CommandResult result = execute.apply(line);
        if (!result.output().isEmpty() || result.newline()) {
            System.out.print(result.output() + (result.newline() ? "\n" : ""));
        }
        return result;
    }

    private void report(Path path, int line, String message,
            Consumer<String> errorOutput) {
        String location = line == 0 ? "" : ", строка " + line;
        String error = "Ошибка стартового скрипта " + path + location + ": " + message;
        System.err.println(error);
        errorOutput.accept(error);
    }
}
