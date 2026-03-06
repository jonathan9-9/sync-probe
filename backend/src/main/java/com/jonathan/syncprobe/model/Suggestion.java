package com.jonathan.syncprobe.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Suggestion {
    private String text;

    public Suggestion() {}

    public Suggestion(String text) {
        this.text = text;
    }


}
