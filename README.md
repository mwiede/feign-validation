# feign-validation [![travis status](https://travis-ci.org/mwiede/feign-validation.svg?branch=master)](https://travis-ci.org/mwiede/feign-validation)
A library for Feign containing a decorator which makes it possible to validate reponses using bean validation api 1.1 (JSR 349).

While Jersey as the JAX-RS implementation provides [Bean Validation Support](https://jersey.github.io/documentation/latest/bean-validation.html), [Feign](https://github.com/OpenFeign/feign) acts as a client and til now only supports a limited set of annotations
in it's [JAX-RS module](https://github.com/OpenFeign/feign/tree/master/jaxrs2), meaning `@javax.validation.Valid` or `@javax.validation.groups.ConvertGroup` are not supported.

This library contains a decorator class, which can be setup within invocationHandlerFactory to retrieve additional validation.

## Motivation

Sometimes it is required, that a client also validates the response it got from the providing service/endpoint. With JSR-349 [bean validation](https://beanvalidation.org/), Java provides a nice api for validation purposes. Just by adding annotations like `@NotNull` or `@Email`, contraints can be validated very easily.

## Example

Here is how you can activate bean validation within Feign wrapper client:
```java

interface GitHub {
    @RequestLine("GET /repos/{owner}/{repo}/contributors")
    @Size(min = 1)
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

```
