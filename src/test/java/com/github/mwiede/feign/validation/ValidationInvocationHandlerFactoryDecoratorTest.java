package com.github.mwiede.feign.validation;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Constraint;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;

/**
 * @author Matthias Wiedemann (mwiede at gmx.de)
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidationInvocationHandlerFactoryDecoratorTest {

    @Mock
    InvocationHandlerFactory invocationHandlerFactory;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Validator validator;

    private Map<Method, InvocationHandlerFactory.MethodHandler> methodToHandler =
            new HashMap<Method, InvocationHandlerFactory.MethodHandler>();

    interface SomeInterface {
        @Valid
        Object someMethodWithAnnotation();

        Object someMethodWithoutAnnotation();
    }

    @Mock
    SomeInterface someInterface;

    @Mock
    Target<SomeInterface> target;

    @Before
    public void setup() {

        for (Method method : SomeInterface.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            } else {
                methodToHandler.put(method, mock(InvocationHandlerFactory.MethodHandler.class));
            }
        }

        when(target.type()).thenReturn(SomeInterface.class);

        // Fake the Feign handling:
        when(invocationHandlerFactory.create(any(), any()))
                .thenReturn((o, method, objects) -> methodToHandler.get(method).invoke(objects));
    }

    @Test(expected = ConstraintViolationException.class)
    public void decorateAndInvoke() throws Throwable {
        final ValidationInvocationHandlerFactoryDecorator validationInvocationHandlerFactoryDecorator =
                new ValidationInvocationHandlerFactoryDecorator(invocationHandlerFactory, validator);
        final InvocationHandler invocationHandler =
                validationInvocationHandlerFactoryDecorator.create(target, methodToHandler);
        final Method method = SomeInterface.class.getDeclaredMethod("someMethodWithAnnotation");
        final Object[] parameters = {null};
        invocationHandler.invoke(someInterface, method, parameters);
    }

    @Test
    public void decorateAndInvokeWithoutViolation() throws Throwable {
        when(validator.forExecutables().validateReturnValue(any(), any(), any())).thenReturn(Collections.EMPTY_SET);
        final ValidationInvocationHandlerFactoryDecorator validationInvocationHandlerFactoryDecorator =
                new ValidationInvocationHandlerFactoryDecorator(invocationHandlerFactory, validator);
        final InvocationHandler invocationHandler =
                validationInvocationHandlerFactoryDecorator.create(target, methodToHandler);
        final Method method = SomeInterface.class.getDeclaredMethod("someMethodWithoutAnnotation");
        final Object[] parameters = {null};
        invocationHandler.invoke(someInterface, method, parameters);
        verify(validator.forExecutables()).validateReturnValue(any(), any(), any());
    }
}