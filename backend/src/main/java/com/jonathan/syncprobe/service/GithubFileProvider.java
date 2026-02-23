package com.jonathan.syncprobe.service;

import org.springframework.stereotype.Service;

@Service("githubProvider")
public class GithubFileProvider implements FileLoader {
    @Override
    public String loadContent(String path) {
        // add RestTemplate/WebClient logic
        return "Github logic not implemented yet. Requesting: " + path;
    }
}
