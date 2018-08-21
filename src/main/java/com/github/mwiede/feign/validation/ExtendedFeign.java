package com.github.mwiede.feign.validation;

import javax.validation.Validator;

import feign.InvocationHandlerFactory;

/**
 * Extension of {@link feign.Feign} which includes a builder which decorates some of the default
 * implementations. {@link InvocationHandlerFactory.Default} is decorated with
 * {@link ValidationInvocationHandlerFactoryDecorator}.
 */
public abstract class ExtendedFeign {

    /**
     * Convenience method to instantiate a {@link feign.Feign.Builder} including the classes to
     * configure validator instance.
     *
     * @param validator
     *         validator instance
     * @return the builder
     */
    public static feign.Feign.Builder builder(final Validator validator) {
        return new ExtendedFeign.Builder(validator).invocationHandlerFactory(new InvocationHandlerFactory.Default());
    }

    public static class Builder extends feign.Feign.Builder {

        private final Validator validator;

        public Builder(final Validator validator) {
            super();
            this.validator = validator;
        }

        @Override
        public feign.Feign.Builder invocationHandlerFactory(final InvocationHandlerFactory invocationHandlerFactory) {
            return super.invocationHandlerFactory(
                    new ValidationInvocationHandlerFactoryDecorator(invocationHandlerFactory, validator));
        }

    }

}
