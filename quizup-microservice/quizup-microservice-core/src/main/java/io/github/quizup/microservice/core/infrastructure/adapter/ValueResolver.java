package io.github.quizup.microservice.core.infrastructure.adapter;

import io.github.quizup.microservice.core.domain.model.search.FieldType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

/**
 * Convertit les valeurs brutes (issues de la désérialisation JSON) vers le type Java
 * attendu par les prédicats JPA, en fonction du {@link FieldType} cible.
 */
public final class ValueResolver {

    private ValueResolver() {
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,              // 2024-12-03T10:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2024-12-03 10:00:00
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"), // 03-12-2024 10:00:00
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"), // 03/12/2024 10:00:00
            DateTimeFormatter.ISO_LOCAL_DATE                    // 2024-12-03 (sera converti en LocalDateTime à minuit)
    );

    /**
     * Résout une valeur brute vers le type Java correspondant au {@link FieldType}.
     *
     * @param rawValue   la valeur brute (String, Number, Boolean, etc.)
     * @param targetType le type cible défini par le champ searchable
     * @return la valeur convertie, ou {@code null} si rawValue est null
     */
    public static Object resolve(Object rawValue, FieldType targetType) {
        if (rawValue == null) {
            return null;
        }

        return switch (targetType) {
            case STRING -> resolveString(rawValue);
            case NUMBER -> resolveNumber(rawValue);
            case DATE -> resolveDate(rawValue);
            case BOOLEAN -> resolveBoolean(rawValue);
        };
    }

    /**
     * Résout une liste de valeurs brutes vers le type Java correspondant.
     * Utilisé pour les opérateurs IN et NOT_IN.
     */
    public static List<Object> resolveAll(List<Object> rawValues, FieldType targetType) {
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyList();
        }

        return rawValues.stream()
                .map(v -> resolve(v, targetType))
                .toList();
    }

    private static String resolveString(Object rawValue) {
        return String.valueOf(rawValue);
    }

    private static Number resolveNumber(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number;
        }

        String str = String.valueOf(rawValue).trim();

        if (str.contains(".") || str.contains(",")) {
            return Double.parseDouble(str.replace(",", "."));
        }

        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return Double.parseDouble(str);
        }
    }

    private static Comparable<?> resolveDate(Object rawValue) {
        if (rawValue instanceof Instant instant) {
            return instant;
        }

        if (rawValue instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        if (rawValue instanceof LocalDate localDate) {
            return localDate;
        }


        String str = String.valueOf(rawValue).trim();

        // Tenter Instant d'abord (format ISO-8601 avec Z ou offset)
        try {
            return Instant.parse(str);
        } catch (DateTimeParseException ignored) {
            // fallback
        }

        // Tenter LocalDateTime ensuite
        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException ignored) {

        }
        // Tenter LocalDate ensuite
        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private static Boolean resolveBoolean(Object rawValue) {
        if (rawValue instanceof Boolean bool) {
            return bool;
        }

        return Boolean.valueOf(String.valueOf(rawValue).trim());
    }
}

