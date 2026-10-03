package com.hiretrack.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hiretrack.common.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final String apiKey;
    private final String model;
    private final String visionModel;
    private final double temperature;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqClient(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.model}") String model,
            @Value("${groq.vision-model:}") String visionModel,
            @Value("${groq.temperature:0.4}") double temperature
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.visionModel = visionModel;
        this.temperature = temperature;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();

        log.info("Groq AI Client initialized with model: '{}', temperature: {}", model, temperature);
        if (isSmallModel(model)) {
            log.warn("Configured Groq model '{}' is a small model. For higher quality career coaching responses, consider using a larger model such as 'llama-3.3-70b-versatile' or 'llama3-70b-8192'.", model);
        }
    }

    private boolean isSmallModel(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("8b") || lower.contains("7b") || lower.contains("9b") || lower.contains("instant") || lower.contains("small");
    }

    public String generateResponse(String systemPrompt, String userMessage, java.util.List<com.hiretrack.ai.dto.ChatAttachmentDto> attachments) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("GROQ_API_KEY environment variable is missing");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new AiServiceException("GROQ_MODEL environment variable is missing");
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            
            boolean hasImages = attachments != null && attachments.stream().anyMatch(a -> "image".equals(a.getType()));
            if (hasImages) {
                if (visionModel == null || visionModel.trim().isEmpty()) {
                    throw new AiServiceException("Image analysis is not enabled");
                }
                root.put("model", visionModel);
            } else {
                root.put("model", model);
            }

            ArrayNode messages = root.putArray("messages");

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }

            StringBuilder combinedText = new StringBuilder(userMessage);
            if (attachments != null) {
                for (com.hiretrack.ai.dto.ChatAttachmentDto att : attachments) {
                    if ("text".equals(att.getType())) {
                        combinedText.append("\n\n--- Attachment: ").append(att.getName()).append(" ---\n");
                        combinedText.append(att.getContent());
                    }
                }
            }
            String finalUserMessage = combinedText.toString();

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            
            if (hasImages) {
                ArrayNode contentArray = userMsg.putArray("content");
                
                ObjectNode textContent = contentArray.addObject();
                textContent.put("type", "text");
                textContent.put("text", finalUserMessage);
                
                for (com.hiretrack.ai.dto.ChatAttachmentDto att : attachments) {
                    if ("image".equals(att.getType())) {
                        ObjectNode imgContent = contentArray.addObject();
                        imgContent.put("type", "image_url");
                        ObjectNode imgUrl = imgContent.putObject("image_url");
                        imgUrl.put("url", "data:" + att.getMimeType() + ";base64," + att.getData());
                    }
                }
            } else {
                userMsg.put("content", finalUserMessage);
            }

            root.put("temperature", temperature);

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

    public void streamResponse(String systemPrompt, String userMessage, java.util.List<com.hiretrack.ai.dto.ChatAttachmentDto> attachments, java.util.function.Consumer<String> onToken, java.util.function.Consumer<String> onReasoning) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiServiceException("GROQ_API_KEY environment variable is missing");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new AiServiceException("GROQ_MODEL environment variable is missing");
        }

        try {
            ObjectNode root = objectMapper.createObjectNode();
            
            boolean hasImages = attachments != null && attachments.stream().anyMatch(a -> "image".equals(a.getType()));
            if (hasImages) {
                if (visionModel == null || visionModel.trim().isEmpty()) {
                    throw new AiServiceException("Image analysis is not enabled");
                }
                root.put("model", visionModel);
            } else {
                root.put("model", model);
            }
            
            root.put("stream", true);

            ArrayNode messages = root.putArray("messages");

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                ObjectNode sysMsg = messages.addObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
            }

            StringBuilder combinedText = new StringBuilder(userMessage);
            if (attachments != null) {
                for (com.hiretrack.ai.dto.ChatAttachmentDto att : attachments) {
                    if ("text".equals(att.getType())) {
                        combinedText.append("\n\n--- Attachment: ").append(att.getName()).append(" ---\n");
                        combinedText.append(att.getContent());
                    }
                }
            }
            String finalUserMessage = combinedText.toString();

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            
            if (hasImages) {
                ArrayNode contentArray = userMsg.putArray("content");
                
                ObjectNode textContent = contentArray.addObject();
                textContent.put("type", "text");
                textContent.put("text", finalUserMessage);
                
                for (com.hiretrack.ai.dto.ChatAttachmentDto att : attachments) {
                    if ("image".equals(att.getType())) {
                        ObjectNode imgContent = contentArray.addObject();
                        imgContent.put("type", "image_url");
                        ObjectNode imgUrl = imgContent.putObject("image_url");
                        imgUrl.put("url", "data:" + att.getMimeType() + ";base64," + att.getData());
                    }
                }
            } else {
                userMsg.put("content", finalUserMessage);
            }

            root.put("temperature", temperature);

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
                    if (line.startsWith("data: ")) {
                        String dataStr = line.substring(6);
                        if ("[DONE]".equals(dataStr.trim())) {
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
                        } catch (Exception ex) {
                            log.warn("Failed to parse Groq SSE JSON payload: '{}'", dataStr, ex);
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

