package com.hiretrack.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hiretrack.common.exception.AiServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GroqClient {

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqClient(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("GROQ_API_KEY environment variable is missing");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new AiServiceException("GROQ_MODEL environment variable is missing");
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);

            ArrayNode messages = root.putArray("messages");

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            root.put("temperature", 0.7);

            String requestBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AiServiceException("Groq API returned HTTP error " + response.statusCode() + ": " + response.body());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText();
                }
            }

            throw new AiServiceException("Groq API response format unexpected");
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Failed to call Groq AI API: " + ex.getMessage(), ex);
        }
    }

    public void streamResponse(String systemPrompt, String userMessage, java.util.function.Consumer<String> onToken, java.util.function.Consumer<String> onReasoning) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("GROQ_API_KEY environment variable is missing");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new AiServiceException("GROQ_MODEL environment variable is missing");
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("stream", true);

            ArrayNode messages = root.putArray("messages");

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            root.put("temperature", 0.7);

            String requestBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                String errorBody = response.body().reduce("", (a, b) -> a + b);
                throw new AiServiceException("Groq API returned HTTP error " + response.statusCode() + ": " + errorBody);
            }

            try (java.util.stream.Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("data: ")) {
                        String dataStr = trimmed.substring(6).trim();
                        if ("[DONE]".equals(dataStr)) {
                            return;
                        }
                        try {
                            JsonNode node = objectMapper.readTree(dataStr);
                            JsonNode choices = node.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null) {
                                    if (delta.has("reasoning") && !delta.get("reasoning").isNull()) {
                                        String rText = delta.get("reasoning").asText();
                                        if (rText != null && !rText.isEmpty()) {
                                            onReasoning.accept(rText);
                                        }
                                    } else if (delta.has("thinking") && !delta.get("thinking").isNull()) {
                                        String tText = delta.get("thinking").asText();
                                        if (tText != null && !tText.isEmpty()) {
                                            onReasoning.accept(tText);
                                        }
                                    }

                                    if (delta.has("content") && !delta.get("content").isNull()) {
                                        String cText = delta.get("content").asText();
                                        if (cText != null && !cText.isEmpty()) {
                                            onToken.accept(cText);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceException("Failed to stream from Groq AI API: " + ex.getMessage(), ex);
        }
    }
}
