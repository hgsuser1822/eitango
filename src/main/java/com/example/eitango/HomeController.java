package com.example.eitango;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {

        String word = "apple";

        model.addAttribute("word", word);

        return "index";
    }
}
