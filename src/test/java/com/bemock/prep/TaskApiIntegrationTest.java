package com.bemock.prep;

import com.bemock.prep.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Full CRUD + filter flow against real PostgreSQL. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void taskLifecycle() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        ResponseEntity<TaskResponse> created = restTemplate.postForEntity("/api/tasks",
                Map.of("title", "Prepare demo", "description", "Record video", "dueDate", tomorrow.toString()),
                TaskResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TaskResponse task = created.getBody();
        assertThat(task.id()).isNotNull();
        assertThat(task.status()).hasToString("TODO");
        assertThat(task.createdAt()).isNotNull();
        String url = "/api/tasks/" + task.id();

        restTemplate.put(url, Map.of("title", "Prepare demo", "status", "DONE", "dueDate", tomorrow.toString()));
        TaskResponse updated = restTemplate.getForObject(url, TaskResponse.class);
        assertThat(updated.status()).hasToString("DONE");
        assertThat(updated.description()).isNull();

        TaskResponse[] done = restTemplate.getForObject("/api/tasks?status=DONE", TaskResponse[].class);
        TaskResponse[] todo = restTemplate.getForObject("/api/tasks?status=TODO", TaskResponse[].class);
        assertThat(done).extracting(TaskResponse::id).contains(task.id());
        assertThat(todo).extracting(TaskResponse::id).doesNotContain(task.id());

        ResponseEntity<Void> deleted = restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restTemplate.getForEntity(url, Map.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUnknownTaskReturns404() {
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/999999", HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "x")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
