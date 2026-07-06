package com.github.hsindumas.stagger.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hsindumas.stagger.common.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author HsinDumas
 */
public class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    public void shouldWriteAndReadContentWithAutoParentDirectoryCreation() throws Exception {
        Path target = tempDir.resolve("nested/path/demo.txt");
        boolean written = FileUtil.nioWriteFile("hello-stagger", target.toString());

        assertTrue(written);
        assertEquals("hello-stagger", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    public void shouldOverwriteUsingWriteFileNotAppend() throws Exception {
        Path target = tempDir.resolve("nested/overwrite.txt");
        assertTrue(FileUtil.writeFileNotAppend("first", target.toString()));
        assertTrue(FileUtil.writeFileNotAppend("second", target.toString()));

        assertEquals("second", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    public void shouldCopyInputStreamToFileWithParentDirectoryCreation() throws Exception {
        Path target = tempDir.resolve("copy/target.bin");
        byte[] expected = "copy-me".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream source = new ByteArrayInputStream(expected);

        boolean copied = FileUtil.copyInputStreamToFile(source, target.toFile(), StandardCopyOption.REPLACE_EXISTING);

        assertTrue(copied);
        assertArrayEquals(expected, Files.readAllBytes(target));
    }

    @Test
    public void shouldReturnFalseForInvalidPath() {
        assertFalse(FileUtil.nioWriteFile("data", ""));
        assertFalse(FileUtil.writeFileNotAppend("data", null));
    }
}
