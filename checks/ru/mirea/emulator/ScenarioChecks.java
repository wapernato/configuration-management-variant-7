package ru.mirea.emulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Каждый поставляемый стартовый сценарий: exit, остановка на ошибке, сохранность CSV. */
public final class ScenarioChecks {
    private ScenarioChecks() { }

    /** Исполнить все сценарии в отдельных сеансах VFS. */
    public static void main(String[] arguments) throws Exception {
        Path csv = Path.of("examples/stage3/deep.csv");
        byte[] before = Files.readAllBytes(csv);
        for (String directory : List.of("stage3", "stage4")) {
            try (var files = Files.list(Path.of("examples", directory))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
                    scenario(file, VfsLoader.load(csv));
                }
            }
        }
        Check.truth(Arrays.equals(before, Files.readAllBytes(csv)));
        Check.report("ScenarioChecks");
    }

    private static void scenario(Path file, VirtualFileSystem vfs) {
        Shell shell = new Shell(vfs);
        List<String> executed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        StartupRunner.Outcome result = new StartupRunner().run(file, line -> {
            executed.add(line);
            return shell.execute(line);
        }, errors::add);
        boolean success = file.getFileName().toString().equals("startup-success.txt");
        Check.equal(success ? StartupRunner.Status.EXIT : StartupRunner.Status.FAILED, result.status());
        Check.equal(success ? 0 : 1, errors.size());
        Check.truth(executed.stream().noneMatch(line -> line.contains("NEVER_EXECUTED")));
        if (!success) {
            Check.equal(2, result.line());
        }
    }
}
