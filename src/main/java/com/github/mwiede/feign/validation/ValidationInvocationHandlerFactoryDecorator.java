package com.github.mwiede.feign.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;

import feign.InvocationHandlerFactory;
import feign.Target;

/**
 * This is a decorator for the {@link InvocationHandler} used in {@link feign.Feign}, which makes it possible to
 * validate the response with all annotations of the validation api (JSR303). After invocation it validates the response
 * when {@link Valid} is present on the interface method. If {@link ConstraintViolation}s are found,
 * a {@link ConstraintViolationException} is thrown.
 */
public class ValidationInvocationHandlerFactoryDecorator implements InvocationHandlerFactory {

    private final InvocationHandlerFactory original;
    private final Validator validator;

    public ValidationInvocationHandlerFactoryDecorator(final InvocationHandlerFactory original,
            final Validator validator) {
        this.original = original;
        this.validator = validator;
    }

    /**
     * A decorator, which triggers validation.
     */
    private class MethodHandlerDecorator implements MethodHandler {

        private final Method method;
        private final MethodHandler methodHandler;

        MethodHandlerDecorator(final Method method, final MethodHandler methodHandler) {
            this.method = method;
            this.methodHandler = methodHandler;
        }

        @Override
        public Object invoke(Object[] argv) throws Throwable {
            Object response = methodHandler.invoke(argv);
            if (method.isAnnotationPresent(Valid.class)) {
                Set<ConstraintViolation<Object>> constraintViolations = validator.validate(response);
                if (!constraintViolations.isEmpty()) {
                    throw new ConstraintViolationException(constraintViolations);
                }
            }
            return response;
        }
    }

    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        for (Map.Entry<Method, MethodHandler> entry : dispatch.entrySet()) {
            entry.setValue(new MethodHandlerDecorator(entry.getKey(), entry.getValue()));
        }
        return original.create(target, dispatch);
    }

}
