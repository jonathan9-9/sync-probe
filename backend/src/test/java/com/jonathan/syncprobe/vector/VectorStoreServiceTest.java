package com.jonathan.syncprobe.vector;

import com.jonathan.syncprobe.model.Chunk;
import com.jonathan.syncprobe.model.EmbeddingChunk;
import com.jonathan.syncprobe.persistence.entity.ChunkEmbeddingRecord;
import com.jonathan.syncprobe.persistence.repository.ChunkEmbeddingRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorStoreServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    @InjectMocks
    private VectorStoreService vectorStoreService;

    @Test
    void storePersistsAllChunks() {
        Chunk docChunk = new Chunk("doc-1", "doc text", "doc", null, "/docs/a.md");
        Chunk codeChunk = new Chunk("code-1", "code text", "code", "MyClass", "/src/MyClass.java");

        EmbeddingChunk docEmbedding = new EmbeddingChunk(docChunk, new double[]{0.1, 0.2});
        EmbeddingChunk codeEmbedding = new EmbeddingChunk(codeChunk, new double[]{0.3, 0.4});

        when(embeddingStore.add(any(Embedding.class), any(TextSegment.class)))
                .thenReturn("emb-doc", "emb-code");

        vectorStoreService.store("scan-1", List.of(docEmbedding, codeEmbedding));

        ArgumentCaptor<ChunkEmbeddingRecord> captor = ArgumentCaptor.forClass(ChunkEmbeddingRecord.class);
        verify(chunkEmbeddingRepository, times(2)).save(captor.capture());

        List<ChunkEmbeddingRecord> saved = captor.getAllValues();
        assertThat(saved)
                .extracting(ChunkEmbeddingRecord::getScanId)
                .containsExactly("scan-1", "scan-1");
        assertThat(saved)
                .extracting(ChunkEmbeddingRecord::getChunkType)
                .containsExactlyInAnyOrder("doc", "code");
        assertThat(saved)
                .extracting(ChunkEmbeddingRecord::getEmbeddingId)
                .containsExactlyInAnyOrder("emb-doc", "emb-code");
    }

    @Test
    void reloadFromDatabaseSupportsSimilaritySearch() {
        ChunkEmbeddingRecord record = new ChunkEmbeddingRecord();
        record.setScanId("scan-2");
        record.setChunkId("code-1");
        record.setChunkType("code");
        record.setPath("/src/MyClass.java");
        record.setContent("code text");
        record.setEmbeddingId("emb-code");
        record.setVector("0.3,0.4");

        when(chunkEmbeddingRepository.findAll()).thenReturn(List.of(record));

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "emb-code",
                Embedding.from(new float[]{0.3f, 0.4f}),
                TextSegment.from("code text", new Metadata()));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<Chunk> results = vectorStoreService.similaritySearch(new double[]{0.3, 0.4});

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPath()).isEqualTo("/src/MyClass.java");
    }

    @Test
    void findLowSimilarityLoadsFromDbWhenCacheEmpty() {
        ChunkEmbeddingRecord doc = new ChunkEmbeddingRecord();
        doc.setScanId("scan-3");
        doc.setChunkId("doc-1");
        doc.setChunkType("doc");
        doc.setContent("doc text");
        doc.setEmbeddingId("emb-doc");
        doc.setVector("0.1,0.2");

        ChunkEmbeddingRecord code = new ChunkEmbeddingRecord();
        code.setScanId("scan-3");
        code.setChunkId("code-1");
        code.setChunkType("code");
        code.setContent("code text");
        code.setEmbeddingId("emb-code");
        code.setVector("0.2,0.3");

        when(chunkEmbeddingRepository.findByScanId("scan-3")).thenReturn(List.of(doc, code));

        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "emb-code",
                Embedding.from(new float[]{0.2f, 0.3f}),
                TextSegment.from("code text", new Metadata()));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<Chunk> stale = vectorStoreService.findLowSimilarityChunks("scan-3");

        assertThat(stale).isEmpty();
    }
}

