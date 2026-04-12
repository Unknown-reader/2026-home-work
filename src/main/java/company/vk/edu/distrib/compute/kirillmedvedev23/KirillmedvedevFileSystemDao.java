package company.vk.edu.distrib.compute.kirillmedvedev23;

import company.vk.edu.distrib.compute.Dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.NoSuchElementException;

public class KirillmedvedevFileSystemDao implements Dao<byte[]> {
    private final Path storageDir;

    public KirillmedvedevFileSystemDao(Path storageDir) throws IOException {
        this.storageDir = storageDir;
        Files.createDirectories(storageDir);
    }

    @Override
    public byte[] get(String key) throws NoSuchElementException, IllegalArgumentException, IOException {
        requireValidKey(key);
        Path filePath = storageDir.resolve(sanitizeFileName(key));
        try {
            return Files.readAllBytes(filePath);
        } catch (NoSuchFileException e) {
            throw (NoSuchElementException) new NoSuchElementException("Key not found: " + key).initCause(e);
        }
    }

    @Override
    public void upsert(String key, byte[] value) throws IllegalArgumentException, IOException {
        requireValidKey(key);
        Path filePath = storageDir.resolve(sanitizeFileName(key));
        Files.write(filePath, value, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void delete(String key) throws IllegalArgumentException, IOException {
        requireValidKey(key);
        Path filePath = storageDir.resolve(sanitizeFileName(key));
        Files.deleteIfExists(filePath);
    }

    @Override
    public void close() {
        // nothing to close
    }

    private void requireValidKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key is null or empty");
        }
    }

    private static String sanitizeFileName(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }
}
