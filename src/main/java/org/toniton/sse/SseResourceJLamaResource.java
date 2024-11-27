package org.toniton.sse;

import com.github.tjake.jlama.safetensors.DType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.jlama.JlamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Path("/events")
public class SseResourceJLamaResource {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamEvents(@Context SseEventSink eventSink, @Context Sse sse) throws IOException {
//        String modelName = "tjake/Llama-3.2-1B-Instruct-JQ4";
        String modelName = "tjake/TinyLlama-1.1B-Chat-v1.0-Jlama-Q4";

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = JlamaEmbeddingModel.builder()
                .modelName("intfloat/e5-small-v2")
                .build();

        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("Number of Card payments: 600, Cash payments: 25, Others: 700");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);

        JlamaChatModel m = JlamaChatModel.builder().modelName(modelName)
                .temperature(0.2f)
                .maxTokens(2048)
                .workingQuantizedType(DType.I8)
                .build();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(m)
                .contentRetriever(contentRetriever)
                .build();

        // Open a new thread to handle the event stream
        executor.submit(() -> {
            try (eventSink) {
                // Create an SSE event
                OutboundSseEvent kk = sse.newEventBuilder()
                        .name("message")
                        .data(String.class, assistant.chat("Tell me a pirate joke!"))
                        .build();

                // Send the event
                eventSink.send(kk);
                // Send periodic events
                for (int i = 0; i < 5; i++) {
                    // Create an SSE event
                    OutboundSseEvent event = sse.newEventBuilder()
                            .name("message")
                            .data(String.class, "Event " + i + assistant.chat("Explain amoeba?"))
                            .build();

                    // Send the event
                    eventSink.send(event);

                    // Simulate a delay
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

