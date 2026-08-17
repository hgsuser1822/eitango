package com.example.eitango;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ListeningService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ListeningService() {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .build();

        this.objectMapper = new ObjectMapper();
    }


    // =========================
    // Groq → 英語スクリプト生成
    // =========================

    public String generateScript() {

        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GROQ_API_KEYが設定されていません。");
        }

        String requestBody = """
                {
                  "model": "openai/gpt-oss-120b",
                  "messages": [
                    {
                      "role": "system",
                      "content": "You create English listening scripts for Japanese learners."
                    },
                    {
                      "role": "user",
                      "content": "Create a short English conversation about checking into a hotel. Make it suitable for an intermediate English learner. Keep it around 30 seconds when spoken. Return only the conversation text. Do not include a title, explanation, Markdown formatting, or Japanese translation."
                    }
                  ],
                  "temperature": 0.7
                }
                """;

        try {

            String response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // JSONを解析
            JsonNode root = objectMapper.readTree(response);

            String script = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (script == null || script.isBlank()) {
                throw new RuntimeException("Groqから英文を取得できませんでした。");
            }

            return script.trim();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Groqでスクリプト生成に失敗しました: "
                            + e.getMessage()
            );
        }
    }


    // =========================
    // Google TTS → MP3
    // =========================

    public void createSpeech(String text) {

        try (TextToSpeechClient textToSpeechClient =
                     TextToSpeechClient.create()) {

            SynthesisInput input =
                    SynthesisInput.newBuilder()
                            .setText(text)
                            .build();

            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("en-US")
                            .build();

            AudioConfig audioConfig =
                    AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.MP3)
                            .build();

            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(
                            input,
                            voice,
                            audioConfig
                    );

            Path outputPath = Path.of("output.mp3");

            Files.write(
                    outputPath,
                    response.getAudioContent().toByteArray()
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Google TTSで音声生成に失敗しました: "
                            + e.getMessage()
            );
        }
    }


    // =========================
    // Groq → TTS
    // =========================

    public String generateListening() {

        // ① Groqで英文を作る
        String script = generateScript();

        // ② その英文をGoogle TTSへ渡す
        createSpeech(script);

        // ③ 生成された英文を返す
        return script;
    }
}
