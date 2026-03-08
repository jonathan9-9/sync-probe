package com.jonathan.syncprobe.service;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.jonathan.syncprobe.model.CodeChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
public class CodeIngestionService {

    private final ChunkingService chunkingService;

    public CodeIngestionService(ChunkingService chunkingService) {
        this.chunkingService = chunkingService;
    }

    public List<CodeChunk> parseCode(Path repoPath) {
        List<CodeChunk> chunks = new ArrayList<>();
        if (repoPath == null || !Files.exists(repoPath)) {
            return chunks;
        }

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isJavaFile)
                    .forEach(file -> chunks.addAll(parseJavaFile(file)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse code files from repository: " + repoPath, e);
        }
        return chunks;
    }

    private List<CodeChunk> parseJavaFile(Path file) {
        List<CodeChunk> chunks = new ArrayList<>();
        String source;
        try {
            source = Files.readString(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Java file: " + file, e);
        }

        if (source.isBlank()) {
            return chunks;
        }

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(source);
        } catch (ParseProblemException ex) {
            return chunkingService.chunkCode(file.toString(), source, "java");
        }

        AtomicInteger index = new AtomicInteger(0);
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            extractChunksFromType(file, type, index, chunks);
        }
        for (EnumDeclaration enumDeclaration : cu.findAll(EnumDeclaration.class)) {
            int start = enumDeclaration.getRange().map(r -> r.begin.line).orElse(1);
            int end = enumDeclaration.getRange().map(r -> r.end.line).orElse(start);
            chunks.add(new CodeChunk(
                    file.toString(),
                    file + "-enum-" + index.getAndIncrement(),
                    enumDeclaration.toString(),
                    enumDeclaration.getNameAsString(),
                    "java",
                    start,
                    end
            ));
        }

        if (chunks.isEmpty()) {
            chunks.addAll(chunkingService.chunkCode(file.toString(), source, "java"));
        }
        return chunks;
    }

    private void extractChunksFromType(Path file,
                                       ClassOrInterfaceDeclaration type,
                                       AtomicInteger index,
                                       List<CodeChunk> chunks) {
        String typeName = type.getNameAsString();
        int typeStart = type.getRange().map(r -> r.begin.line).orElse(1);
        int typeEnd = type.getRange().map(r -> r.end.line).orElse(typeStart);
        chunks.add(new CodeChunk(
                file.toString(),
                file + "-type-" + index.getAndIncrement(),
                type.toString(),
                typeName,
                "java",
                typeStart,
                typeEnd
        ));

        for (BodyDeclaration<?> member : type.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                int start = method.getRange().map(r -> r.begin.line).orElse(typeStart);
                int end = method.getRange().map(r -> r.end.line).orElse(start);
                chunks.add(new CodeChunk(
                        file.toString(),
                        file + "-method-" + index.getAndIncrement(),
                        method.toString(),
                        typeName + "#" + method.getNameAsString(),
                        "java",
                        start,
                        end
                ));
            } else if (member instanceof ConstructorDeclaration ctor) {
                int start = ctor.getRange().map(r -> r.begin.line).orElse(typeStart);
                int end = ctor.getRange().map(r -> r.end.line).orElse(start);
                chunks.add(new CodeChunk(
                        file.toString(),
                        file + "-ctor-" + index.getAndIncrement(),
                        ctor.toString(),
                        typeName + "#" + ctor.getNameAsString(),
                        "java",
                        start,
                        end
                ));
            }
        }
    }

    private boolean isJavaFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".java");
    }
}
