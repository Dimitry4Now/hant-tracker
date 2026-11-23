package com.hanttracker.api.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Spring test on its own in-memory database with the migrations and no dev
 * seed, so it can count exactly what it creates. Each test rolls back.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
    "app.seed.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:empty-tests;DB_CLOSE_DELAY=-1"
})
@Transactional
public @interface EmptyDatabase {}
