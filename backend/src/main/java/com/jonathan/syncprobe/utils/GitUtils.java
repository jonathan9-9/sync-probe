package com.jonathan.syncprobe.utils;

import java.nio.file.Files;
import java.nio.file.Path;

/** GitUtils: included all non state logic; utility functions
 * */
public class GitUtils {
    public static void cloneRepo(Path targetDir, String repoUrl) {

    }
    public static void pullLatest(Path repoDir){} // pull the latest changes
    public static boolean isGitRepository(Path dir){
        return Files.exists(dir.resolve(".git"));
    }

}
