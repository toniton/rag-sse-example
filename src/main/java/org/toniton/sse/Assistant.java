package org.toniton.sse;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

//    @SystemMessage("You are my pirate chatbot friend, who always responds in pirate speak!")
//    @UserMessage("You are a good friend of mine. Answer using slang. {{it}}")
    @SystemMessage("As a good friend of mine, answer using pirate slang. You can only answer questions that relate to payment context, else say 'I don't know!'.")
    String chat(String userMessage);
}
