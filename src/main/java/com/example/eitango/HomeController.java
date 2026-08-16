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
            "apple", "りんご",
            "book", "本",
            "cat", "猫"
    );

    @GetMapping("/")
    public String home(Model model) {

        String word = "apple";
        String meaning = words.get(word);

        List<String> choices = List.of(
                "りんご",
                "本",
                "猫",
                "学校"
        );

        model.addAttribute("word", word);
        model.addAttribute("meaning", meaning);
        model.addAttribute("choices", choices);

        return "index";
    }

    @PostMapping("/answer")
    @ResponseBody
    public String answer(@RequestParam String choice) {

        String word = "apple";

        String correctAnswer = words.get(word);

        if (choice.equals(correctAnswer)) {
            return "correct";
        } else {
            return "incorrect";
        }
    }


}
