package com.example.eitango;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    // 単語と意味の辞書
    Map<String, String> words = Map.of(
            "appraisal", "査定",
            "proceeds", "収益",
            "inclement", "荒れ模様の"
    );


    // ホーム
    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute("page", "home");

        return "index";
    }


    // 単語
    @GetMapping("/quiz")
    public String quiz(Model model) {

        String word = "appraisal";
        String meaning = words.get(word);

        List<String> choices = List.of(
                "査定",
                "収益",
                "荒れ模様の",
                "出来事"
        );

        model.addAttribute("page", "quiz");
        model.addAttribute("word", word);
        model.addAttribute("meaning", meaning);
        model.addAttribute("choices", choices);

        return "index";
    }


    // 学習
    @GetMapping("/study")
    public String study(Model model) {

        model.addAttribute("page", "study");

        return "index";
    }


    // 履歴
    @GetMapping("/history")
    public String history(Model model) {

        model.addAttribute("page", "history");

        return "index";
    }


    // 設定
    @GetMapping("/settings")
    public String settings(Model model) {

        model.addAttribute("page", "settings");

        return "index";
    }


    // 回答チェック
    @PostMapping("/answer")
    @ResponseBody
    public String answer(@RequestParam String choice) {

        String word = "appraisal";

        String correctAnswer = words.get(word);

        if (choice.equals(correctAnswer)) {
            return "correct";
        } else {
            return "incorrect";
        }
    }
}
