package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocChunk extends Chunk {
    private String filePath;
    private String chunkId;

    public DocChunk() {}

    public DocChunk(String filePath, String chunkId, String content){
        super(chunkId, content, "doc", null, filePath);
        this.filePath = filePath;
        this.chunkId = chunkId;
    }
}
