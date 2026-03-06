package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Chunk {
    private String id;
    private String content;
    private String type; // code or doc
    private String symbol; // class or method name
    private String path;

    public Chunk() {}


    public Chunk(String id, String content, String type, String symbol, String path) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.symbol = symbol;
        this.path = path;
    }
}

