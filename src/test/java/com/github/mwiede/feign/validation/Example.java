package com.github.mwiede.feign.validation;

import java.util.List;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

/**
 * @author Matthias Wiedemann (mwiede at gmx.de)
 */
public class Example {

    interface GitHub {
        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        @Size(min = 1, max = 3)
        @Valid
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

    }

    static class Contributor {
        @NotNull
        @Size(min = 10)
        String login;
        int contributions;
    }

    public static void main(String... args) {

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        GitHub github = ExtendedFeign.builder(validator)//
                .decoder(new GsonDecoder()).target(GitHub.class, "https://api.github.com");

        // Fetch and print a list of the contributors to this library.
        try {
            List<Contributor> contributors = github.contributors("OpenFeign", "feign");
            for (Contributor contributor : contributors) {
                System.out.println(contributor.login + " (" + contributor.contributions + ")");
            }
        } catch (ConstraintViolationException ex) {
            ex.getConstraintViolations().forEach(System.out::println);
        }

    }
}