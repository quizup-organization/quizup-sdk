package io.github.quizup.microservice.core.domain.model.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.quizup.microservice.core.domain.exception.LanguageProblems;

import java.util.Arrays;

/**
 * Langue supportée par QuizUp (code ISO 639-1).
 *
 * <p>Sérialisée par son code ({@code fr}, {@code en}) et désérialisée strictement : un code
 * inconnu est rejeté (400 côté surface REST).</p>
 */
public enum Language {

    FR("fr"),
    EN("en");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static Language fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new LanguageProblems.UnsupportedLanguageProblem(code));
    }
}
