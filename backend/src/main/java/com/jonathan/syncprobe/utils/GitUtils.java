package com.jonathan.syncprobe.utils;

import java.nio.file.Files;
import java.nio.file.Path;

/** GitUtils: included all non state logic; utility functions
 * */
public class GitUtils {
    public static void cloneRepo(Path targetDir, String repoUrl) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "git", "clone", repoUrl, targetDir.toString()
            );
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0){
                throw new RuntimeException("Git clone failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cloning repository", e);
        }
    }
    public static void pullLatest(Path repoDir){} // pull the latest changes
    public static boolean isGitRepository(Path dir){
        return Files.exists(dir.resolve(".git"));
    }

}
