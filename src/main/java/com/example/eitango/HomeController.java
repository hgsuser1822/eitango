package com.example.eitango;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class HomeController {

  private final ListeningService listeningService;
  private final WordService wordService;

  public HomeController(
      ListeningService listeningService,
      WordService wordService) {

    this.listeningService = listeningService;
    this.wordService = wordService;
  }

  // =========================
  // CSVから単語を読み込む
  // =========================

  @GetMapping("/words")
  public String wordsPage(Model model) {

    List<Integer> scores = wordService.getScores();

    model.addAttribute("scores", scores);
    model.addAttribute("page", "words");

    return "index";
  }

  // =========================
  // TOEICスコア → ステージ一覧
  // =========================
  @GetMapping("/words/score/{score}")
  public String stages(
      @PathVariable int score,
      Model model) {

    List<Integer> stages = wordService.getStages(score);

    model.addAttribute("score", score);
    model.addAttribute("stages", stages);
    model.addAttribute("page", "stages");

    return "index";
  }

  // =========================
  // ホーム
  // =========================
  @GetMapping("/")
  public String home(Model model) {

    model.addAttribute("page", "home");

    return "index";
  }

  // =========================
  // 単語クイズ
  // =========================
  @GetMapping("/quiz")
  public String quiz(
      @RequestParam int score,
      @RequestParam int stage,
      Model model,
      HttpSession session) {

    List<Word> quizWords = (List<Word>) session.getAttribute("quizWords");

    Integer quizIndex = (Integer) session.getAttribute("quizIndex");

    // まだクイズが存在しない場合だけ新しく作る
    if (quizWords == null ||
        quizIndex == null ||
        quizIndex >= quizWords.size()) {

      List<Word> stageWords = wordService.getWordsByStage(score, stage);

      quizWords = new ArrayList<>(stageWords);

      Collections.shuffle(quizWords);

      quizWords = new ArrayList<>(
          quizWords.stream()
              .limit(10)
              .toList());

      session.setAttribute(
          "quizWords",
          quizWords);

      session.setAttribute(
          "quizIndex",
          0);

      session.setAttribute(
          "quizScore",
          score);

      session.setAttribute(
          "quizStage",
          stage);

      session.setAttribute(
          "correctCount",
          0);

      quizIndex = 0;
    }

    // =========================
    // 現在の問題
    // =========================

    Word word = quizWords.get(quizIndex);

    model.addAttribute(
        "word",
        word.getWord());

    model.addAttribute(
        "meaning",
        word.getMeaning());

    // =========================
    // 4択を作成
    // =========================

    List<String> choices = new ArrayList<>();

    // 正解を追加
    choices.add(
        word.getMeaning());

    // Groqから誤答3つを取得
    List<String> wrongChoices = wordService.generateWrongChoices(word);

    // 誤答を追加
    choices.addAll(wrongChoices);

    // 4択をランダムに並べる
    Collections.shuffle(choices);

    // Thymeleafに渡す
    model.addAttribute(
        "choices",
        choices);

    // =========================
    // 問題情報
    // =========================

    model.addAttribute(
        "score",
        score);

    model.addAttribute(
        "stage",
        stage);

    model.addAttribute(
        "questionNumber",
        quizIndex + 1);

    model.addAttribute(
        "totalQuestions",
        quizWords.size());

    model.addAttribute(
        "page",
        "quiz");

    return "index";
  }

  // =========================
  // 学習
  // =========================
  @GetMapping("/study")
  public String study(Model model) {

    model.addAttribute("page", "study");

    return "index";
  }

  // =========================
  // 履歴
  // =========================
  @GetMapping("/history")
  public String history(Model model) {

    model.addAttribute("page", "history");

    return "index";
  }

  // =========================
  // 設定
  // =========================
  @GetMapping("/settings")
  public String settings(Model model) {

    model.addAttribute("page", "settings");

    return "index";
  }

  // =========================
  // 回答チェック
  // =========================
  @PostMapping("/answer")
  @ResponseBody
  public String answer(
      @RequestParam String choice,
      HttpSession session) {

    // セッションからクイズ情報を取得
    List<Word> quizWords = (List<Word>) session.getAttribute("quizWords");

    Integer quizIndex = (Integer) session.getAttribute("quizIndex");

    Integer correctCount = (Integer) session.getAttribute("correctCount");

    // クイズ情報がない場合
    if (quizWords == null || quizIndex == null) {
      return "error";
    }

    // 現在の問題
    Word currentWord = quizWords.get(quizIndex);

    // 正解判定
    boolean correct = choice.equals(currentWord.getMeaning());

    if (correct) {
      correctCount++;
      session.setAttribute("correctCount", correctCount);
    }

    // 次の問題へ
    quizIndex++;
    session.setAttribute("quizIndex", quizIndex);

    // 10問終了
    if (quizIndex >= quizWords.size()) {

      return correct
          ? "correct_finished"
          : "incorrect_finished";
    }

    // まだ問題が残っている
    return correct
        ? "correct"
        : "incorrect";
  }

  // =========================
  // Google TTSテスト
  // =========================
  @GetMapping("/tts-test")
  @ResponseBody
  public String ttsTest() {

    String text = "Hello. This is a test of Google Cloud Text to Speech.";

    listeningService.createSpeech(text);

    return "TTS成功！ output.mp3 を作成しました。";
  }

  // =========================
  // Groq → Google TTS
  // =========================
  @GetMapping("/generate-listening")
  @ResponseBody
  public String generateListening(
      @RequestParam String situation,
      @RequestParam String level,
      @RequestParam String length,
      @RequestParam String freedom) {

    try {

      String result = listeningService.generateListening(
          situation,
          level,
          length,
          freedom);

      System.out.println("generateListening result:");
      System.out.println(result);

      return result;

    } catch (Exception e) {

      e.printStackTrace();

      return "リスニング生成エラー: " + e.getMessage();
    }
  }
}
