package com.bemock.prep.service;

import com.bemock.prep.dto.TaskRequest;
import com.bemock.prep.dto.TaskResponse;
import com.bemock.prep.exception.ResourceNotFoundException;
import com.bemock.prep.model.Task;
import com.bemock.prep.model.TaskStatus;
import com.bemock.prep.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null
                ? taskRepository.findAll(NEWEST_FIRST)
                : taskRepository.findByStatus(status, NEWEST_FIRST);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTask(id);
        apply(task, request);
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    /** PUT is a full replace: every field comes from the request. */
    private static void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setDueDate(request.dueDate());
    }
}
