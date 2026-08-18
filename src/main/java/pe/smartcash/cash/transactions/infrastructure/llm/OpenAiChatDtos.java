package pe.smartcash.cash.transactions.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** DTOs de cable para el dialecto Chat Completions (OpenAI / Gemini en modo OpenAI-compat). */
record ChatCompletionRequest(
    String model,
    List<ChatMessage> messages,
    @JsonProperty("response_format") ResponseFormat responseFormat,
    Double temperature) {}

record ChatMessage(String role, String content) {}

record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchemaSpec jsonSchema) {}

record JsonSchemaSpec(String name, JsonNode schema, boolean strict) {}

record ChatCompletionResponse(List<Choice> choices) {}

record Choice(ChatMessage message) {}
