package com.bemock.prep.controller;

import com.bemock.prep.dto.TaskResponse;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.model.TaskStatus;
import com.bemock.prep.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(taskService.create(any())).thenReturn(new TaskResponse(
                1L, "Write tests", null, TaskStatus.TODO, null, Instant.now()));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Write tests\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tasks/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createWithBlankTitleAndPastDueDateReturns400WithFieldErrors() throws Exception {
        String body = "{\"title\":\"\",\"dueDate\":\"%s\"}".formatted(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "dueDate")));
        verifyNoInteractions(taskService);
    }

    @Test
    void createWithTitleOver100CharsReturns400() throws Exception {
        String body = "{\"title\":\"%s\"}".formatted("a".repeat(101));

        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void createWithUnknownStatusReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"status\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be one of [TODO, IN_PROGRESS, DONE]"));
    }

    @Test
    void createWithUnparseableDueDateReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"dueDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));
    }

    @Test
    void getUnknownTaskReturns404() throws Exception {
        when(taskService.get(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task with id 99 not found"));
    }

    @Test
    void listWithUnknownStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
