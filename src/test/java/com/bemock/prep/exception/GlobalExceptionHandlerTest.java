package com.bemock.prep.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the API error contract: every failure returns the same JSON shape
 * ({@code timestamp, status, error, message, path, fieldErrors}) with the right HTTP status.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ErrorScenarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ErrorScenarioController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"quantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/items"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "quantity")));
    }

    @Test
    void invalidRequestParamReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(get("/test/search").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("limit"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void wrongPathVariableTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'id' has invalid value 'abc'"));
    }

    @Test
    void resourceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/items/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Item with id 42 not found"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void conflictReturns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Insufficient stock"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete("/test/conflict"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    record CreateItemRequest(
            @NotBlank @Size(max = 100) String title,
            @NotNull @Positive Integer quantity) {
    }

    @RestController
    @RequestMapping("/test")
    static class ErrorScenarioController {

        @PostMapping("/items")
        String create(@Valid @RequestBody CreateItemRequest request) {
            return "ok";
        }

        @GetMapping("/items/{id}")
        String get(@PathVariable Long id) {
            throw new ResourceNotFoundException("Item", id);
        }

        @GetMapping("/search")
        String search(@RequestParam @Min(1) int limit) {
            return "ok";
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ConflictException("Insufficient stock");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}
