package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.DocChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Perform code and doc chunking */
@Service
public class ChunkingService {
    public List<Chunk> chunkAll(List<DocChunk> docs) {
        List<Chunk> chunks = new ArrayList<>();
        if (docs == null) {
            return chunks;
        }

        for (DocChunk ignored : docs) {
            chunks.add(new Chunk());
        }
        return chunks;
    }
}
