package ru.mirea.emulator;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/** Загрузка CSV, двоичные данные, вложенность и неизменность файла-источника. */
public final class VfsChecks {
    private static final String ROOT = "path,type,content\n/,directory,\n";

    private VfsChecks() { }

    /** Проверить валидные деревья и отклонение повреждённых файлов. */
    public static void main(String[] arguments) throws Exception {
        valid();
        invalid();
        paths();
        sourceUnchanged();
        Check.report("VfsChecks");
    }

    private static void valid() {
        Check.equal(List.of(), VfsLoader.parse(ROOT).children("/"));
        VirtualFileSystem vfs = VfsLoader.load(Path.of("examples/stage3/deep.csv"));
        Check.equal(List.of("/a/b/c/deep.txt"), vfs.children("/a/b/c"));
        Check.truth(Arrays.equals(new byte[]{0, (byte) 255, (byte) 128, 10}, vfs.read("/binary.bin")));
        Check.equal(0, vfs.read("/empty.txt").length);
        Check.equal("comma name\n", new String(vfs.read("/папка с пробелами/a,b.txt")));
        byte[] copy = vfs.read("/binary.bin");
        copy[0] = 22;
        Check.equal((byte) 0, vfs.read("/binary.bin")[0]);
        Check.truth(VfsLoader.parse("\uFEFF" + ROOT.replace("\n", "\r\n")).entry("/").directory());
        Check.truth(VfsLoader.parse("path,type,content\n/x,file,eA==\n/,directory,\n")
                .entry("/").directory());
        Check.equal(List.of(List.of("a,b", "c\"d", "e\nf")),
                CsvReader.parse("\"a,b\",\"c\"\"d\",\"e\nf\"\n"));
        Check.truth(VfsLoader.load(null).entry("/").directory());
    }

    private static void invalid() {
        for (String text : List.of("", "wrong,header\n", "path,type,content\n/x,file,eA==\n",
                ROOT + "/,directory,\n", ROOT + "/x,unknown,\n", ROOT + "/x,file,@@\n",
                ROOT + "/x,directory,eA==\n", ROOT + "/a/x,file,eA==\n",
                ROOT + "x,file,eA==\n", ROOT + "/a/../x,file,eA==\n",
                ROOT + "/x,file,eA==,extra\n", ROOT + "/x,file,eA==\n/x/y,file,eA==\n",
                ROOT + "/x\"bad,file,\n", ROOT + "\"unterminated\n",
                ROOT + "\"/x\"bad,file,eA==\n", "path,type,content\n/,file,\n")) {
            Check.rejects(() -> VfsLoader.parse(text));
        }
        Check.rejects(() -> VfsLoader.load(Path.of("examples/stage3/missing.csv")));
        Check.rejects(() -> VfsLoader.load(Path.of("examples/stage3/invalid.csv")));
        Check.rejects(() -> VirtualFileSystem.empty().read("/"));
        Check.rejects(() -> VirtualFileSystem.empty().entry("/missing"));
    }

    private static void paths() {
        Check.equal("/", VirtualPath.resolve("/", "../../.."));
        Check.equal("/a/c", VirtualPath.resolve("/a/b", "../c/./"));
        Check.equal("/a", VirtualPath.resolve("/b", "/a//"));
        Check.equal("/", VirtualPath.resolve("/a", "~"));
        Check.equal("/a", VirtualPath.resolve("/b", "~/a"));
        Check.rejects(() -> VirtualPath.resolve("/", ""));
        Check.rejects(() -> VirtualPath.resolve("/", "bad\nname"));
    }

    private static void sourceUnchanged() throws Exception {
        Path path = Path.of("examples/stage3/files.csv");
        byte[] before = Files.readAllBytes(path);
        VirtualFileSystem vfs = VfsLoader.load(path);
        vfs.read("/hello.txt");
        vfs.children("/");
        Check.truth(Arrays.equals(before, Files.readAllBytes(path)));
        Path invalid = Files.createTempFile("vfs-invalid-utf8", ".csv");
        try {
            Files.write(invalid, new byte[]{(byte) 0xC3, (byte) 0x28});
            Check.rejects(() -> VfsLoader.load(invalid));
        } finally {
            Files.delete(invalid);
        }
    }
}
