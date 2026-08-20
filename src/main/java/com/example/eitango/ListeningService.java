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
    // Groq → 英語会話スクリプト生成
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
                      "content": "You create English listening conversations for Japanese learners."
                    },
                    {
                      "role": "user",
                      "content": "Create a short English conversation about checking into a hotel.Make it suitable for an intermediate English learner. Keep it around 30 seconds when spoken.Use exactly two speakers.Label them only as A and B.Start each line with either A: or B:.Do not use names, job titles, or role names.Alternate between the two speakers naturally. Do not include any other speakers. Do not include a title, explanation, Markdown, Japanese, or pronunciation notes."
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
    // Google TTS → 男女の会話音声
    // =========================

    public String createSpeech(String text) {

        try (TextToSpeechClient textToSpeechClient =
                    TextToSpeechClient.create()) {

            String ssml = createSsml(text);

            SynthesisInput input =
                    SynthesisInput.newBuilder()
                            .setSsml(ssml)
                            .build();

            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("en-US")
                            .setName("en-US-Neural2-D")
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

            // 毎回違うファイル名を作る
            String fileName =
                    "output_" + System.currentTimeMillis() + ".mp3";

            Path outputPath =
                    Path.of(
                        "src/main/resources/static/" + fileName
                    );

            Files.write(
                    outputPath,
                    response.getAudioContent().toByteArray()
            );

            // 作ったファイル名を返す
            return fileName;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Google TTSで音声生成に失敗しました: "
                            + e.getMessage()
            );
        }
    }


    // =========================
    // 会話 → SSML変換
    // =========================

    private String createSsml(String text) {

        StringBuilder ssml = new StringBuilder();

        ssml.append("<speak>");

        String[] lines = text.split("\\R");

        for (String line : lines) {

            line = line.trim();

            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("B:")) {

                String speech =
                        line.substring("B:".length()).trim();

                ssml.append(
                        "<voice name=\"en-US-Neural2-C\">"
                );

                ssml.append(escapeSsml(speech));

                ssml.append("</voice>");

                // 少し間を入れる
                ssml.append("<break time=\"400ms\"/>");

            } else if (line.startsWith("A:")) {

                String speech =
                        line.substring("A:".length()).trim();

                ssml.append(
                        "<voice name=\"en-US-Neural2-D\">"
                );

                ssml.append(escapeSsml(speech));

                ssml.append("</voice>");

                ssml.append("<break time=\"400ms\"/>");
            }
        }

        ssml.append("</speak>");

        return ssml.toString();
    }


    // =========================
    // SSML用エスケープ
    // =========================

    private String escapeSsml(String text) {

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }


    // =========================
    // Groq → TTS
    // =========================

    public String generateListening() {

        // ① Groqで会話を作る
        String script = generateScript();
        // ② そのscriptから音声を作る
        String audioFileName = createSpeech(script);
        // ③ スクリプトと音声ファイル名をセットで返す
        return """
                {
                    "script": %s,
                    "audio": "/%s"
                }
                """.formatted(
                    toJsonString(script),
                    audioFileName
                );
    }
    private String toJsonString(String text) {

    try {
        return objectMapper.writeValueAsString(text);

    } catch (Exception e) {

        throw new RuntimeException(
                "JSON変換に失敗しました",
                e
        );
    }
    }


}
