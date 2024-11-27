package org.toniton.sse;

import com.github.tjake.jlama.safetensors.DType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.glassfish.jersey.media.sse.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Path("/event-new")
public class SseResource {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamEvents(@Context SseEventSink eventSink, @Context Sse sse) throws IOException {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        TextSegment segment1 = TextSegment.from("I like football.");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("The weather is good today.");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);

        ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(embeddingStore, embeddingModel);
        // The model name to use (e.g., "orca-mini", "mistral", "llama2", "codellama", "phi", or
        // "tinyllama")
        String modelName = "orca-mini";

        // Create and start the Ollama container
        OllamaContainer ollama =
                new OllamaContainer(DockerImageName.parse("langchain4j/ollama-" + modelName + ":latest")
                        .asCompatibleSubstituteFor("ollama/ollama"));
        ollama.start();

        System.out.println(baseUrl(ollama));

        // Build the ChatLanguageModel
        ChatLanguageModel model =
                OllamaChatModel.builder().baseUrl(baseUrl(ollama)).modelName(modelName).build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .build();

        // Open a new thread to handle the event stream
        executor.submit(() -> {
            try (eventSink) {
                // Send periodic events
                for (int i = 0; i < 5; i++) {
                    // Create an SSE event
                    OutboundSseEvent event = sse.newEventBuilder()
                            .name("message")
                            .data(String.class, "Event " + i + assistant.chat("What is my fav sport?"))
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
        ollama.stop();
    }

    private static String baseUrl(GenericContainer<?> ollama) {
        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
    }
}

