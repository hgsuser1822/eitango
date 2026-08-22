//以下はWord.java
package com.example.eitango;

public class Word {

    private int score;
    private int stage;
    private String word;
    private String meaning;

    public Word(int score, int stage, String word, String meaning) {
        this.score = score;
        this.stage = stage;
        this.word = word;
        this.meaning = meaning;
    }

    public int getScore() {
        return score;
    }

    public int getStage() {
        return stage;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }
}
