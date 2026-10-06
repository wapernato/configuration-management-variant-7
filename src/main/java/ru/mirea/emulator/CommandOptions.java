package ru.mirea.emulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Короткие объединённые опции и разделитель -- для файловых команд. */
record CommandOptions(Set<Character> flags, List<String> paths) {
    static CommandOptions parse(List<String> arguments, String allowed) {
        Set<Character> flags = new HashSet<>();
        List<String> paths = new ArrayList<>();
        boolean options = true;
        for (String argument : arguments) {
            if (options && argument.equals("--")) {
                options = false;
            } else if (options && argument.startsWith("-") && argument.length() > 1) {
                addFlags(flags, argument, allowed);
            } else {
                if (argument.isEmpty()) {
                    throw new IllegalArgumentException("пустой путь");
                }
                paths.add(argument);
            }
        }
        return new CommandOptions(Set.copyOf(flags), List.copyOf(paths));
    }

    private static void addFlags(Set<Character> flags, String argument, String allowed) {
        for (int index = 1; index < argument.length(); index++) {
            char flag = argument.charAt(index);
            if (allowed.indexOf(flag) < 0) {
                throw new IllegalArgumentException("неподдерживаемая опция: -" + flag);
            }
            flags.add(flag);
        }
    }
}
