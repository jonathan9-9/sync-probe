package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeChunk extends Chunk {
    private String language;
    private int startLine;
    private int endLine;

    public CodeChunk() {}

    public CodeChunk(String filePath,
                     String chunkId,
                     String content,
                     String symbol,
                     String language,
                     int startLine,
                     int endLine) {
        super(chunkId, content, "code", symbol, filePath);
        this.language = language;
        this.startLine = startLine;
        this.endLine = endLine;
    }
}
