package io.github.quizup.microservice.core.infrastructure.axon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.axonframework.messaging.responsetypes.MultipleInstancesResponseType;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.common.TypeReflectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.concurrent.Future;

import static org.axonframework.common.ReflectionUtils.unwrapIfType;

/**
 * Variante de {@link MultipleInstancesResponseType} qui matche le type de retour d'un
 * {@code @QueryHandler} en comparant le <b>type brut</b> de l'argument de collection.
 *
 * <p>Axon n'accepte comme argument qu'un {@link Class} (ou une variable/wildcard) : un élément
 * générique du type {@code List<Enveloppe<Payload>>} n'est donc jamais reconnu, et le handler est
 * introuvable ({@code NoHandlerForQueryException}). Cette implémentation lève la limite en
 * s'appuyant sur le type brut ({@code Enveloppe}), tout en réutilisant la conversion Axon.</p>
 */
public class RawMultipleInstancesResponseType<R> extends MultipleInstancesResponseType<R> {

    @JsonCreator
    public RawMultipleInstancesResponseType(
            @JsonProperty("expectedResponseType") Class<R> expectedResponseType) {
        super(expectedResponseType);
    }

    @Override
    public boolean matches(Type responseType) {
        return matchRank(responseType) > ResponseType.NO_MATCH;
    }

    @Override
    public Integer matchRank(Type responseType) {
        Type unwrapped = unwrapIfType(responseType, Future.class);
        Type iterableType = TypeReflectionUtils.getExactSuperType(unwrapped, Iterable.class);

        if (iterableType instanceof ParameterizedType parameterizedType) {
            Type[] arguments = parameterizedType.getActualTypeArguments();
            if (arguments.length == 1 && isRawAssignable(arguments[0])) {
                return ITERABLE_MATCH;
            }
            return ResponseType.NO_MATCH;
        }

        if (iterableType instanceof Class<?> rawClass) {
            return expectedResponseType.isAssignableFrom(rawClass)
                    ? ITERABLE_MATCH
                    : ResponseType.NO_MATCH;
        }

        return ResponseType.NO_MATCH;
    }

    private boolean isRawAssignable(Type type) {
        if (type instanceof Class<?> clazz) {
            return expectedResponseType.isAssignableFrom(clazz);
        }
        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawClass) {
            return expectedResponseType.isAssignableFrom(rawClass);
        }
        if (type instanceof WildcardType wildcardType) {
            return Arrays.stream(wildcardType.getUpperBounds()).anyMatch(this::isRawAssignable);
        }
        if (type instanceof TypeVariable<?> typeVariable) {
            return Arrays.stream(typeVariable.getBounds()).anyMatch(this::isRawAssignable);
        }
        return false;
    }
}
