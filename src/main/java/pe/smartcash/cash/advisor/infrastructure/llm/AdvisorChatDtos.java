package pe.smartcash.cash.advisor.infrastructure.llm;

import java.util.List;

/**
 * DTOs de cable para el dialecto Chat Completions (mismo dialecto que usa {@code
 * transactions.infrastructure.llm}, pero sin {@code response_format}: acá la salida es texto
 * libre para el usuario, no un JSON estructurado). Propios de este contexto a propósito: son
 * package-private en {@code transactions...llm} y no hay razón de dominio para compartirlos
 * entre bounded contexts, solo son forma de cable.
 */
record ChatCompletionRequest(String model, List<ChatMessage> messages, Double temperature) {}

record ChatMessage(String role, String content) {}

record ChatCompletionResponse(List<Choice> choices) {}

record Choice(ChatMessage message) {}
