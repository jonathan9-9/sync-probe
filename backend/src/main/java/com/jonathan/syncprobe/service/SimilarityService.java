package com.jonathan.syncprobe.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// computes cosine similarity and scoring
@Service
public class SimilarityService {
    private final FileLoader fileLoader;

    public SimilarityService(@Qualifier("localProvider") FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    public void analyze(String codePath, String docPath) {
        String code = fileLoader.loadContent(codePath);
        String doc = fileLoader.loadContent(docPath);
        // AI logic here including cosine similarity...
    }

}
