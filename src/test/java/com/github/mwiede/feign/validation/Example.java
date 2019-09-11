package com.github.mwiede.feign.validation;

import com.google.gson.*;
import feign.Param;
import feign.RequestLine;
import feign.gson.GsonDecoder;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Matthias Wiedemann (mwiede at gmx.de)
 */
public class Example {

    interface GitHub {
        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        @Size(min = 1, max = 3)
        @Valid
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

        @RequestLine("GET /repos/{owner}/{repo}")
        @Valid
        Repo repo(@Param("owner") String owner, @Param("repo") String repo);
    }

    static class Contributor {
        @NotNull
        @Size(min = 10)
        String login;
        int contributions;
    }

    static class Repo {
        @NotNull
        @Future
        ZonedDateTime created_at;
        String name;
    }

    static class DateTimeDeserializer implements JsonDeserializer<ZonedDateTime> {
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString());
        }
    }
    public static void main(String... args) {

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Gson gson = new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new DateTimeDeserializer()).create();

        GitHub github = ExtendedFeign.builder(validator)//
                .decoder(new GsonDecoder(gson)).target(GitHub.class, "https://api.github.com");

        // Fetch and print a list of the contributors to this library.
        try {
            Repo repo = github.repo("OpenFeign", "feign");
        } catch (ConstraintViolationException ex) {
            ex.getConstraintViolations().forEach(System.out::println);
        }

    }
}