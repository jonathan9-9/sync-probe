package com.jonathan.syncprobe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = {
        "langchain4j.google-ai-gemini.chat-model.api-key=dummy"
})
class SyncProbeApplicationTests {

    @Test
    void contextLoads() {
    }

}