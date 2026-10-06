package ru.mirea.emulator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Unit-тесты этапа 5: режимы, конфликты, атомарность и изоляция от ОС. */
class Stage5Test {
    private VirtualFileSystem vfs;
    private Shell shell;
    @TempDir
    Path temp;

    @BeforeEach
    void prepare() {
        vfs = VfsLoader.parse(fixture());
        shell = new Shell(vfs);
    }

    private String fixture() {
        return "path,type,content\n/,directory,\n/a,directory,\n/a/b,directory,\n"
                + "/a/b/deep.txt,file," + encode("deep\n") + "\n"
                + "/hello.txt,file," + encode("hello world\n") + "\n"
                + "/other.txt,file," + encode("other\n") + "\n"
                + "/empty.txt,file,\n/binary.bin,file,AP+ACg==\n/-source.txt,file,eA==\n";
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private void ok(String command) {
        CommandResult result = shell.execute(command);
        assertFalse(result.error(), command + ": " + result.output());
    }

    @Test
    void mkdirCreatesDirectoryAndSupportsCd() {
        ok("mkdir new");
        assertTrue(vfs.entry("/new").directory());
        ok("cd new");
        assertEquals("/new", shell.currentDirectory());
        assertEquals("", shell.execute("ls").output());
    }

    @Test
    void mkdirCreatesSeveralQuotedPaths() {
        ok("mkdir 'мои файлы' second");
        assertTrue(vfs.entry("/мои файлы").directory());
        assertTrue(vfs.entry("/second").directory());
    }

    @Test
    void parentsCreatesDeepTreeAndAllowsExistingDirectory() {
        ok("mkdir -p new/one/two");
        ok("mkdir -p new/one/two");
        ok("mkdir -p /");
        assertTrue(vfs.entry("/new/one/two").directory());
    }

    @Test
    void mkdirRelativePathsAndParentNavigation() {
        ok("cd /a/b");
        ok("mkdir ../sibling");
        ok("mkdir -p ./x/../y");
        assertTrue(vfs.entry("/a/sibling").directory());
        assertTrue(vfs.entry("/a/b/x").directory());
        assertTrue(vfs.entry("/a/b/y").directory());
    }

    @Test
    void mkdirOptionSeparatorSupportsDashName() {
        ok("mkdir -- -new");
        assertTrue(vfs.entry("/-new").directory());
    }

    @Test
    void mkdirRollsBackAllOperandsOnFailure() {
        assertTrue(shell.execute("mkdir first /missing/child").error());
        assertFalse(vfs.exists("/first"));
        assertFalse(vfs.exists("/missing"));
    }

    @Test
    void mkdirParentsRollsBackOnFileConflict() {
        assertTrue(shell.execute("mkdir -p new/child hello.txt/child").error());
        assertFalse(vfs.exists("/new"));
        assertArrayEquals("hello world\n".getBytes(StandardCharsets.UTF_8), vfs.read("/hello.txt"));
    }

    @Test
    void copyFileToNewNameAndExistingDirectory() {
        ok("cp hello.txt copy.txt");
        ok("cp hello.txt /a");
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/copy.txt"));
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/a/hello.txt"));
    }

    @Test
    void copyOverwritesFileByDefault() {
        ok("cp hello.txt other.txt");
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/other.txt"));
    }

    @Test
    void noClobberKeepsOldData() {
        byte[] before = vfs.read("/other.txt");
        ok("cp -n hello.txt other.txt");
        assertArrayEquals(before, vfs.read("/other.txt"));
        ok("cp -n hello.txt new.txt");
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/new.txt"));
    }

    @Test
    void copiesBinaryAndEmptyFilesExactly() {
        ok("cp binary.bin binary-copy.bin");
        ok("cp empty.txt empty-copy.txt");
        assertArrayEquals(new byte[]{0, (byte) 255, (byte) 128, 10}, vfs.read("/binary-copy.bin"));
        assertEquals(0, vfs.read("/empty-copy.txt").length);
    }

    @Test
    void multipleSourcesRequireDirectoryAndCopyAll() {
        ok("cp hello.txt other.txt /a");
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/a/hello.txt"));
        assertArrayEquals(vfs.read("/other.txt"), vfs.read("/a/other.txt"));
    }

    @Test
    void recursiveCopyCreatesIndependentDeepTree() {
        ok("cp -r /a /tree");
        assertArrayEquals(vfs.read("/a/b/deep.txt"), vfs.read("/tree/b/deep.txt"));
        ok("mkdir /tree/new");
        assertFalse(vfs.exists("/a/new"));
        byte[] copy = vfs.read("/tree/b/deep.txt");
        copy[0] = 0;
        assertEquals((byte) 'd', vfs.read("/a/b/deep.txt")[0]);
        assertEquals((byte) 'd', vfs.read("/tree/b/deep.txt")[0]);
    }

    @Test
    void recursiveUppercaseAndExistingDestinationDirectory() {
        ok("mkdir holder");
        ok("cp -R /a /holder");
        assertTrue(vfs.entry("/holder/a/b").directory());
        assertArrayEquals(vfs.read("/a/b/deep.txt"), vfs.read("/holder/a/b/deep.txt"));
    }

    @Test
    void recursiveMergeAndNoClobber() {
        ok("cp -r a tree");
        ok("cp other.txt tree/b/deep.txt");
        ok("mkdir tree/retained");
        ok("mkdir holder");
        ok("cp -r tree holder");
        ok("cp -rn a holder/tree");
        assertTrue(vfs.entry("/holder/tree/retained").directory());
        assertTrue(vfs.entry("/holder/tree/a/b").directory());
    }

    @Test
    void directoryMergePreservesNoClobberFiles() {
        ok("mkdir holder");
        ok("cp -r a holder");
        ok("cp other.txt holder/a/b/deep.txt");
        ok("cp -rn a holder");
        assertArrayEquals(vfs.read("/other.txt"), vfs.read("/holder/a/b/deep.txt"));
        ok("cp -r a holder");
        assertArrayEquals(vfs.read("/a/b/deep.txt"), vfs.read("/holder/a/b/deep.txt"));
    }

    @Test
    void copySeparatorAndQuotedRelativeDestination() {
        ok("mkdir 'с пробелами'");
        ok("cd 'с пробелами'");
        ok("cp -- /-source.txt './имя файла'");
        assertArrayEquals(new byte[]{'x'}, vfs.read("/с пробелами/имя файла"));
    }

    @Test
    void copyRollsBackEarlierSourcesOnFailure() {
        assertTrue(shell.execute("cp hello.txt missing /a").error());
        assertFalse(vfs.exists("/a/hello.txt"));
    }

    @Test
    void recursiveConflictRollsBackWholeTree() {
        ok("mkdir holder holder/a");
        ok("cp hello.txt holder/a/b");
        List<String> before = vfs.children("/holder/a");
        assertTrue(shell.execute("cp -r a holder").error());
        assertEquals(before, vfs.children("/holder/a"));
        assertArrayEquals(vfs.read("/hello.txt"), vfs.read("/holder/a/b"));
    }

    @Test
    void sourceCsvAndHostDirectoryRemainUnchanged() throws Exception {
        Path csv = temp.resolve("source.csv");
        Files.writeString(csv, fixture());
        byte[] before = Files.readAllBytes(csv);
        Shell loaded = new Shell(VfsLoader.load(csv));
        assertFalse(loaded.execute("mkdir only-in-memory").error());
        assertFalse(loaded.execute("cp hello.txt copy-in-memory").error());
        assertArrayEquals(before, Files.readAllBytes(csv));
        try (var files = Files.list(temp)) {
            assertEquals(List.of(csv), files.toList());
        }
        VirtualFileSystem reloaded = VfsLoader.load(csv);
        assertFalse(reloaded.exists("/only-in-memory"));
        assertFalse(reloaded.exists("/copy-in-memory"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mkdir", "mkdir ''", "mkdir -x new", "mkdir /", "mkdir a",
            "mkdir hello.txt", "mkdir missing/child", "mkdir hello.txt/..",
            "cp", "cp hello.txt", "cp -x hello.txt copy", "cp '' copy", "cp missing copy",
            "cp hello.txt hello.txt", "cp a tree", "cp -r a a/b", "cp -r / tree",
            "cp -r a hello.txt", "cp hello.txt other.txt new", "cp hello.txt missing/copy",
            "cp hello.txt new/", "cp hello.txt hello.txt/../copy", "cp hello.txt /a/b"})
    void invalidCommandsReturnErrorWithoutChangingRoot(String command) {
        // Last case conflicts with a directory named b inside the destination a.
        if (command.equals("cp hello.txt /a/b")) {
            ok("mkdir /a/b/hello.txt");
        }
        List<String> before = vfs.children("/");
        assertTrue(shell.execute(command).error(), command);
        assertEquals(before, vfs.children("/"));
    }
}
