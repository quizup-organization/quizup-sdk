package io.github.quizup.microservice.core.infrastructure.axon;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import org.axonframework.messaging.responsetypes.AbstractResponseType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

import static org.axonframework.common.ReflectionUtils.unwrapIfType;

public class PageResultResponseType<R> extends AbstractResponseType<PageResult<R>> {

    public PageResultResponseType(Class<R> expectedResponseType) {
        super(expectedResponseType);
    }

    @Override
    public boolean matches(Type responseType) {
        Type unwrapped = unwrapIfType(responseType, Future.class);

        if (unwrapped instanceof ParameterizedType parameterized) {
            Type rawType = parameterized.getRawType();
            if (rawType instanceof Class<?> rawClass && PageResult.class.isAssignableFrom(rawClass)) {
                Type[] args = parameterized.getActualTypeArguments();
                return args.length == 1 && isAssignableFrom(args[0]);
            }
        }

        // Accepte aussi PageResult brut (sans paramètre générique)
        if (unwrapped instanceof Class<?> rawClass) {
            return PageResult.class.isAssignableFrom(rawClass);
        }

        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class responseMessagePayloadType() {
        return PageResult.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<R> convert(Object response) {
        return (PageResult<R>) response;
    }

}