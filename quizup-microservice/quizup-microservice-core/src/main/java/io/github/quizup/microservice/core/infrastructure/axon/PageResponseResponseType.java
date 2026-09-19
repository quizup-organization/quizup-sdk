package io.github.quizup.microservice.core.infrastructure.axon;

import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import org.axonframework.messaging.responsetypes.AbstractResponseType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

import static org.axonframework.common.ReflectionUtils.unwrapIfType;

public class PageResponseResponseType<R> extends AbstractResponseType<PageResponse<R>> {

    public PageResponseResponseType(Class<R> expectedResponseType) {
        super(expectedResponseType);
    }

    @Override
    public boolean matches(Type responseType) {
        Type unwrapped = unwrapIfType(responseType, Future.class);

        if (unwrapped instanceof ParameterizedType parameterized) {
            Type rawType = parameterized.getRawType();
            if (rawType instanceof Class<?> rawClass
                    && PageResponse.class.isAssignableFrom(rawClass)) {
                Type[] args = parameterized.getActualTypeArguments();
                return args.length == 1 && isAssignableFrom(args[0]);
            }
        }

        if (unwrapped instanceof Class<?> rawClass) {
            return PageResponse.class.isAssignableFrom(rawClass);
        }

        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class responseMessagePayloadType() {
        return PageResponse.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResponse<R> convert(Object response) {
        return (PageResponse<R>) response;
    }
}