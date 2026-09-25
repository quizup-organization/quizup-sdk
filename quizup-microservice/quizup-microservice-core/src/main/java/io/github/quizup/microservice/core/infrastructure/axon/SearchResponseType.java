package io.github.quizup.microservice.core.infrastructure.axon;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import org.axonframework.messaging.responsetypes.AbstractResponseType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

import static org.axonframework.common.ReflectionUtils.unwrapIfType;

public class SearchResponseType<R> extends AbstractResponseType<SearchResponse<R>> {

    public SearchResponseType(Class<R> expectedResponseType) {
        super(expectedResponseType);
    }

    @Override
    public boolean matches(Type responseType) {
        Type unwrapped = unwrapIfType(responseType, Future.class);

        if (unwrapped instanceof ParameterizedType parameterized) {
            Type rawType = parameterized.getRawType();
            if (rawType instanceof Class<?> rawClass
                    && SearchResponse.class.isAssignableFrom(rawClass)) {
                Type[] args = parameterized.getActualTypeArguments();
                return args.length == 1 && isAssignableFrom(args[0]);
            }
        }

        if (unwrapped instanceof Class<?> rawClass) {
            return SearchResponse.class.isAssignableFrom(rawClass);
        }

        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class responseMessagePayloadType() {
        return SearchResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SearchResponse<R> convert(Object response) {
        return (SearchResponse<R>) response;
    }
}