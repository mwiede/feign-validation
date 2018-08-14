package com.github.mwiede.feign.validation;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import feign.InvocationHandlerFactory;

@RunWith(MockitoJUnitRunner.class)
public class ValidationInvocationHandlerFactoryDecoratorTest {

    @Mock
    InvocationHandlerFactory invocationHandlerFactory;

    @Mock
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

    @Before
    public void setup() {

        for (Method method : SomeInterface.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            } else {
                methodToHandler.put(method, mock(InvocationHandlerFactory.MethodHandler.class));
            }
        }

        // Fake the Feign handling:
        when(invocationHandlerFactory.create(any(), any())).thenReturn(new InvocationHandler() {
            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                return methodToHandler.get(method).invoke(objects);
            }
        });
    }

    @Test
    public void decorateAndInvoke() throws Throwable {
        ValidationInvocationHandlerFactoryDecorator validationInvocationHandlerFactoryDecorator =
                new ValidationInvocationHandlerFactoryDecorator(invocationHandlerFactory, validator);
        InvocationHandler invocationHandler = validationInvocationHandlerFactoryDecorator.create(null, methodToHandler);
        Method method = SomeInterface.class.getDeclaredMethod("someMethodWithAnnotation");
        Object[] parameters = {null};
        invocationHandler.invoke(someInterface, method, parameters);
        verify(validator).validate(any(), any());
    }

    @Test
    public void decorateAndInvokeWithout() throws Throwable {
        ValidationInvocationHandlerFactoryDecorator validationInvocationHandlerFactoryDecorator =
                new ValidationInvocationHandlerFactoryDecorator(invocationHandlerFactory, validator);
        InvocationHandler invocationHandler = validationInvocationHandlerFactoryDecorator.create(null, methodToHandler);
        Method method = SomeInterface.class.getDeclaredMethod("someMethodWithoutAnnotation");
        Object[] parameters = {null};
        invocationHandler.invoke(someInterface, method, parameters);
        verify(validator, never()).validate(any(), any());
    }
}