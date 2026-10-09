package com.bemock.prep.dto;

import com.bemock.prep.model.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body for create (POST) and full replace (PUT). A missing status defaults to TODO. */
public record TaskRequest(
        @NotBlank @Size(max = 100) String title,
        String description,
        TaskStatus status,
        @FutureOrPresent LocalDate dueDate) {
}
