package com.github.mwiede.feign.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
 * validate the response with all annotations of the validation api (JSR 349). After invocation it validates the response
 * when {@link Valid} is present on the interface method. If {@link ConstraintViolation}s are found,
 * a {@link ConstraintViolationException} is thrown.
 *
 * @author Matthias Wiedemann (mwiede at gmx.de)
 */
public class ValidationInvocationHandlerFactoryDecorator implements InvocationHandlerFactory {

    private final InvocationHandlerFactory original;
    private final Validator validator;
    private Object targetTypeInstance;

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
        public Object invoke(final Object[] argv) throws Throwable {
            final Object response = methodHandler.invoke(argv);

            Set<ConstraintViolation<Object>> constraintViolationSet =
                    validator.forExecutables().validateReturnValue(targetTypeInstance, method, response);
            if (!constraintViolationSet.isEmpty()) {
                throw new ConstraintViolationException(constraintViolationSet);
            }

            return response;
        }
    }

    @Override
    public InvocationHandler create(final Target target, final Map<Method, MethodHandler> dispatch) {
        try {
            this.targetTypeInstance = createTargetInstance(target.type());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        for (final Map.Entry<Method, MethodHandler> entry : dispatch.entrySet()) {
            entry.setValue(new MethodHandlerDecorator(entry.getKey(), entry.getValue()));
        }
        return original.create(target, dispatch);
    }

    private Object createTargetInstance(final Class type) throws InstantiationException, IllegalAccessException {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (o, method, objects) -> {
            if ("hashCode".equals(method.getName())) {
                return hashCode();
            } else if ("toString".equals(method.getName())) {
                return toString();
            } else {
                return null;
            }
        });
    }

}
