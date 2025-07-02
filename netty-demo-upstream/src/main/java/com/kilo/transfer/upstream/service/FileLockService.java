package com.kilo.transfer.upstream.service;

import com.google.gson.Gson;
import com.kilo.transfer.upstream.model.LockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileLockService {

    private static final Logger logger = LoggerFactory.getLogger(FileLockService.class);
    private static final String LOCK_FILE_SUFFIX = ".loc";
    private static final long ZOMBIE_LOCK_TIMEOUT = 600 * 1000; // 10 minutes in milliseconds
    private static final long LOCK_UPDATE_INTERVAL = 60; // 60 seconds

    private final String ownerId;
    private final Path scanDirectory;
    private final Gson gson;
    private final ScheduledExecutorService lockUpdateScheduler;

    public FileLockService(String scanDirectory) {
        this.ownerId = UUID.randomUUID().toString();
        this.scanDirectory = Paths.get(scanDirectory);
        this.gson = new Gson();
        this.lockUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        // Periodically scan for files to process
    }

    public void stop() {
        lockUpdateScheduler.shutdown();
    }

    private Path getLockFilePath(Path originalFilePath) {
        return originalFilePath.getParent().resolve(originalFilePath.getFileName().toString() + LOCK_FILE_SUFFIX);
    }

    public boolean acquireLock(Path filePath) {
        Path lockFilePath = getLockFilePath(filePath);
        File lockFile = lockFilePath.toFile();

        try {
            if (lockFile.exists()) {
                LockInfo lockInfo = readLockFile(lockFilePath);
                if (lockInfo != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lockInfo.getTimestamp() > ZOMBIE_LOCK_TIMEOUT) {
                        logger.warn("Found a zombie lock for file: {}. Forcing lock acquisition.", filePath);
                        createOrUpdateLockFile(lockFilePath, 0);
                        return true;
                    } else {
                        logger.info("File {} is already locked by another process (owner: {}).", filePath, lockInfo.getOwner());
                        return false;
                    }
                }
            }

            // Atomically create the lock file
            if (lockFile.createNewFile()) {
                createOrUpdateLockFile(lockFilePath, 0);
                return true;
            }
        } catch (IOException e) {
            logger.error("Error acquiring lock for file: {}", filePath, e);
        }
        return false;
    }

    private void createOrUpdateLockFile(Path lockFilePath, double progress) {
        long timestamp = System.currentTimeMillis();
        LockInfo lockInfo = new LockInfo(progress, timestamp, this.ownerId);
        try (Writer writer = Files.newBufferedWriter(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(lockInfo, writer);
        } catch (IOException e) {
            logger.error("Failed to create or update lock file: {}", lockFilePath, e);
        }
    }

    public LockInfo readLockFile(Path lockFilePath) {
        if (!Files.exists(lockFilePath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(lockFilePath)) {
            return gson.fromJson(reader, LockInfo.class);
        } catch (IOException e) {
            logger.error("Failed to read lock file: {}", lockFilePath, e);
            return null;
        }
    }
    
    public void updateLock(Path filePath, double progress) {
        Path lockFilePath = getLockFilePath(filePath);
        if (isOwner(lockFilePath)) {
            createOrUpdateLockFile(lockFilePath, progress);
        }
    }

    public void releaseLock(Path filePath, boolean deleteOriginal) {
        Path lockFilePath = getLockFilePath(filePath);
        if (isOwner(lockFilePath)) {
            try {
                Files.deleteIfExists(lockFilePath);
                if (deleteOriginal) {
                    Files.deleteIfExists(filePath);
                    logger.info("Released lock and deleted original file for: {}", filePath);
                } else {
                    logger.info("Released lock for file: {}", filePath);
                }
            } catch (IOException e) {
                logger.error("Failed to release lock for file: {}", filePath, e);
            }
        }
    }

    private boolean isOwner(Path lockFilePath) {
        LockInfo lockInfo = readLockFile(lockFilePath);
        return lockInfo != null && this.ownerId.equals(lockInfo.getOwner());
    }

    public void scheduleLockUpdate(Path filePath, Runnable progressTracker) {
        lockUpdateScheduler.scheduleAtFixedRate(() -> {
            if (isOwner(getLockFilePath(filePath))) {
                progressTracker.run();
            }
        }, LOCK_UPDATE_INTERVAL, LOCK_UPDATE_INTERVAL, TimeUnit.SECONDS);
    }
}