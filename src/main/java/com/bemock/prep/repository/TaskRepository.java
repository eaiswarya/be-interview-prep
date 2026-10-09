package com.bemock.prep.repository;

import com.bemock.prep.model.Task;
import com.bemock.prep.model.TaskStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status, Sort sort);
}
