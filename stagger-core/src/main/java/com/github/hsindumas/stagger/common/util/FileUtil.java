package com.github.hsindumas.stagger.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * File helpers.
 */
public final class FileUtil {

    private FileUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean mkdir(String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }
        return new File(path).mkdir();
    }

    public static boolean mkdirs(String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }
        return new File(path).mkdirs();
    }

    public static boolean writeFileNotAppend(String content, String path) {
        return nioWriteFile(content, path);
    }

    public static String getFileContent(String path) {
        if (StringUtil.isEmpty(path)) {
            return StringUtil.EMPTY;
        }
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return StringUtil.EMPTY;
        }
    }

    public static String getFileContent(InputStream inputStream) {
        if (inputStream == null) {
            return StringUtil.EMPTY;
        }
        try (InputStream in = inputStream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return StringUtil.EMPTY;
        }
    }

    public static File[] getResourceFolderFiles(String path) {
        if (StringUtil.isEmpty(path)) {
            return new File[0];
        }
        String normalizedPath = normalizeResourcePath(path);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(normalizedPath);
        if (resource == null) {
            return new File[0];
        }

        String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
            return readFileProtocol(resource);
        }
        if ("jar".equals(protocol)) {
            return readJarProtocol(resource, normalizedPath);
        }
        return new File[0];
    }

    public static boolean nioWriteFile(String content, String path) {
        if (StringUtil.isEmpty(path)) {
            return false;
        }
        try {
            Path target = Path.of(path);
            createParentDirectories(target);
            Files.writeString(
                    target,
                    content == null ? StringUtil.EMPTY : content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean copyInputStreamToFile(InputStream inputStream, File target, StandardCopyOption copyOption) {
        if (inputStream == null || target == null) {
            return false;
        }
        try (InputStream in = inputStream) {
            Path targetPath = target.toPath();
            createParentDirectories(targetPath);
            if (copyOption == null) {
                Files.copy(in, targetPath);
            } else {
                Files.copy(in, targetPath, copyOption);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static void createParentDirectories(Path targetPath) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String normalizeResourcePath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    private static File[] readFileProtocol(URL resource) {
        try {
            File folder = new File(resource.toURI());
            File[] files = folder.listFiles();
            return files == null ? new File[0] : files;
        } catch (Exception ex) {
            return new File[0];
        }
    }

    private static File[] readJarProtocol(URL resource, String normalizedPath) {
        List<File> extracted = new ArrayList<>();
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            String entryPrefix = connection.getEntryName();
            if (StringUtil.isEmpty(entryPrefix)) {
                entryPrefix = normalizedPath;
            }
            Path tempDir = Files.createTempDirectory("stagger-resource-");
            tempDir.toFile().deleteOnExit();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    if (jarEntry.isDirectory()) {
                        continue;
                    }
                    String entryName = jarEntry.getName();
                    if (!entryName.startsWith(entryPrefix + "/")) {
                        continue;
                    }
                    String relative = entryName.substring(entryPrefix.length() + 1);
                    if (relative.contains("/")) {
                        continue;
                    }
                    Path targetFile = tempDir.resolve(relative);
                    try (InputStream in = jarFile.getInputStream(jarEntry)) {
                        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        targetFile.toFile().deleteOnExit();
                        extracted.add(targetFile.toFile());
                    }
                }
            }
        } catch (Exception ex) {
            return new File[0];
        }
        return extracted.toArray(new File[0]);
    }
}
