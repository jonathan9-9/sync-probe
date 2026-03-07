package com.jonathan.syncprobe.service;

import com.jonathan.syncprobe.model.CodeChunk;
import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.DocChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits document text into smaller chunks with unique IDs.
 */
@Service
public class ChunkingService {

    private final int CHUNK_SIZE = 500; // characters per chunk

    /**
     * Splits a single document into multiple DocChunks.
     *
     * @param filePath file path of the document
     * @param text     full text of the document
     * @return List of DocChunk objects
     */
    public List<DocChunk> chunkDocument(String filePath, String text) {
        List<DocChunk> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int chunkIndex = 0;
        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, text.length());
            String chunkText = text.substring(i, end);

            String chunkId = filePath + "-chunk-" + chunkIndex;

            chunks.add(new DocChunk(filePath, chunkId, chunkText));

            chunkIndex++;
        }

        return chunks;
    }

    /**
     * Splits a source file into multiple CodeChunk blocks.
     */
    public List<CodeChunk> chunkCode(String filePath, String source, String language) {
        List<CodeChunk> chunks = new ArrayList<>();
        if (source == null || source.isEmpty()) return chunks;

        int chunkIndex = 0;
        int startLine = 1;
        for (int i = 0; i < source.length(); i += CHUNK_SIZE) {
            int end = Math.min(i + CHUNK_SIZE, source.length());
            String chunkText = source.substring(i, end);
            String chunkId = filePath + "-code-chunk-" + chunkIndex;
            int endLine = startLine + countLines(chunkText) - 1;

            chunks.add(new CodeChunk(
                    filePath,
                    chunkId,
                    chunkText,
                    null,
                    language,
                    startLine,
                    Math.max(startLine, endLine)
            ));

            startLine = Math.max(startLine, endLine) + 1;
            chunkIndex++;
        }

        return chunks;
    }

    /**
     * Splits document objects into standard Chunk objects.
     */
    public List<Chunk> chunkDocuments(List<DocChunk> docs) {
        List<Chunk> chunks = new ArrayList<>();
        if (docs == null || docs.isEmpty()) {
            return chunks;
        }

        for (DocChunk doc : docs) {
            if (doc == null || doc.getContent() == null || doc.getContent().isBlank()) {
                continue;
            }
            String path = doc.getPath() != null ? doc.getPath() : doc.getFilePath();
            chunks.addAll(chunkDocument(path, doc.getContent()));
        }
        return chunks;
    }

    /**
     * Splits multiple documents into chunks.
     *
     * @param filePaths list of file paths
     * @param texts     list of corresponding document texts
     * @return all DocChunks from all documents
     */
    public List<DocChunk> chunkDocuments(List<String> filePaths, List<String> texts) {
        List<DocChunk> allChunks = new ArrayList<>();
        for (int i = 0; i < filePaths.size(); i++) {
            allChunks.addAll(chunkDocument(filePaths.get(i), texts.get(i)));
        }
        return allChunks;
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
