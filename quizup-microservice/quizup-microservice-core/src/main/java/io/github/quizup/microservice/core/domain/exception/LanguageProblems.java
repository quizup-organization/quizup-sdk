package io.github.quizup.microservice.core.domain.exception;

import java.util.Map;

/**
 * Problèmes de validation liés à la langue (code ISO 639-1 non supporté).
 */
public interface LanguageProblems {

    class UnsupportedLanguageProblem extends LanguageProblem {
        public UnsupportedLanguageProblem(String code) {
            super(
                    "urn:quizup:language:unsupported",
                    "Unsupported language",
                    "The language '" + code + "' is not supported",
                    Map.of("language", String.valueOf(code))
            );
        }
    }
}
