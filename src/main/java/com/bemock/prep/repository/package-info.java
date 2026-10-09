/**
 * Persistence layer: Spring Data JPA repositories over PostgreSQL.
 * <p>
 * Responsibilities: queries only (derived queries, {@code @Query}, Specifications, locking).
 * Schema is owned by Flyway migrations in {@code src/main/resources/db/migration}.
 */
package com.bemock.prep.repository;
