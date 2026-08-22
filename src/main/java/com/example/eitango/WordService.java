//以下はWordService.java
package com.example.eitango;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

@Service
public class WordService {

  private final List<Word> words = new ArrayList<>();

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public WordService() {

    this.restClient = RestClient.builder()
        .baseUrl("https://api.groq.com/openai/v1")
        .build();

    this.objectMapper = new ObjectMapper();

    loadWords();
  }

  // =========================
  // CSV読み込み
  // =========================

  private void loadWords() {

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            getClass()
                .getClassLoader()
                .getResourceAsStream("words.csv"),
            StandardCharsets.UTF_8))) {

      // ヘッダーを読み飛ばす
      reader.readLine();

      String line;

      while ((line = reader.readLine()) != null) {

        String[] parts = line.split(",", 4);

        if (parts.length == 4) {

          int score = Integer.parseInt(parts[0].trim());
          int stage = Integer.parseInt(parts[1].trim());
          String word = parts[2].trim();
          String meaning = parts[3].trim();

          words.add(
              new Word(
                  score,
                  stage,
                  word,
                  meaning));
        }
      }

      System.out.println(
          "WordService: "
              + words.size()
              + "語読み込みました。");

    } catch (Exception e) {

      e.printStackTrace();

      throw new RuntimeException(
          "words.csvの読み込みに失敗しました。",
          e);
    }
  }
  // =========================
  // Groq → 誤選択肢生成
  // =========================

  public List<String> generateWrongChoices(
      Word word) {

    String apiKey = System.getenv("GROQ_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      throw new RuntimeException(
          "GROQ_API_KEYが設定されていません。");
    }

    String prompt = """
        Create exactly 3 incorrect answer choices for an English vocabulary
        multiple-choice question.

        Target word: %s
        Correct Japanese meaning: %s

        Choose 3 English words whose SPELLINGS are similar to the target word
        but whose meanings are different.

        The words should be easy for Japanese learners to confuse with the
        target word because they look similar when written.

        Example:

        Target: react

        Similar words:
        read
        rest
        reach

        Japanese meanings:
        読む
        休む
        届く

        For this task:

        - Generate exactly 3 choices.
        - The choices must have different meanings from the target.
        - Do not use the target word.
        - Prefer common English words.
        - Output Japanese meanings only.
        - Do not output the English words.
        - Do not output explanations.
        - Do not output numbering.
        - Return valid JSON.

        Return exactly this JSON format:
        {"choices":["meaning1","meaning2","meaning3"]}

        The response must contain only JSON.
        """.formatted(
        word.getWord(),
        word.getMeaning());

    String requestBody = """
        {
          "model": "openai/gpt-oss-120b",
          "messages": [
            {
              "role": "system",
              "content": "You create English vocabulary multiple-choice questions."
            },
            {
              "role": "user",
              "content": %s
            }
          ],
          "temperature": 0.7,
          "response_format": {
            "type": "json_object"
          }
        }
        """.formatted(
        toJsonString(prompt));

    try {

      String response = restClient.post()
          .uri("/chat/completions")
          .header(
              "Authorization",
              "Bearer " + apiKey)
          .header(
              "Content-Type",
              "application/json")
          .body(requestBody)
          .retrieve()
          .body(String.class);

      JsonNode root = objectMapper.readTree(response);

      String content = root
          .path("choices")
          .get(0)
          .path("message")
          .path("content")
          .asText();

      JsonNode json = objectMapper.readTree(content);

      List<String> wrongChoices = new ArrayList<>();

      JsonNode choices = json.path("choices");

      if (choices.isArray()) {

        for (JsonNode choice : choices) {

          String text = choice.asText().trim();

          if (!text.isBlank()
              && !text.equals(word.getMeaning())) {

            wrongChoices.add(text);
          }
        }
      }

      if (wrongChoices.size() != 3) {

        throw new RuntimeException(
            "Groqから誤選択肢を3つ取得できませんでした。");
      }

      return wrongChoices;

    } catch (Exception e) {

      e.printStackTrace();

      throw new RuntimeException(
          "Groqで誤選択肢の生成に失敗しました: "
              + e.getMessage());
    }
  }

  // =========================
  // 全単語
  // =========================

  public List<Word> getWords() {

    return words;
  }

  // =========================
  // TOEICスコア一覧
  // =========================

  public List<Integer> getScores() {

    return words.stream()
        .map(Word::getScore)
        .distinct()
        .sorted()
        .toList();
  }

  // =========================
  // ステージ一覧
  // =========================

  public List<Integer> getStages(int score) {

    return words.stream()
        .filter(w -> w.getScore() == score)
        .map(Word::getStage)
        .distinct()
        .sorted()
        .toList();
  }

  // =========================
  // 指定されたスコア・ステージの単語
  // =========================

  public List<Word> getWordsByStage(
      int score,
      int stage) {

    return words.stream()
        .filter(w -> w.getScore() == score)
        .filter(w -> w.getStage() == stage)
        .toList();
  }

  private String toJsonString(String text) {

    try {

      return objectMapper.writeValueAsString(text);

    } catch (Exception e) {

      throw new RuntimeException(
          "JSON変換に失敗しました",
          e);
    }
  }

}
