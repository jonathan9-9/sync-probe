package com.jonathan.syncprobe.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@Service("localProvider")
public class LocalFileProvider implements FileLoader {
    /**
     * Reads the content of a file from a given path
     * @param path the path to the file
     * @return returns the content of the file
     * */
    @Override
    public String loadContent(String path) {
        try {
            return Files.readString(Paths.get(path));
        }  catch (IOException e) {
            return "Error: Could not read file at " + path + ". " + e.getMessage();
        }
    }


}
