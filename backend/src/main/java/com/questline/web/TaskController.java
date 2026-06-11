package com.questline.web;

import com.questline.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task CRUD + scheduling/ordering. Thin: resolves the user id from the JWT subject and delegates
 * to {@link TaskService}. The user id is never read from the request.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public TaskResponse create(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody CreateTaskRequest req) {
        return TaskResponse.from(taskService.create(userId(jwt), req.goalId(), req.milestoneId(),
                req.title(), req.description(), req.estimateMinutes(), req.scheduledFor(),
                req.notes(), req.resources()));
    }

    @GetMapping("/today")
    public List<TaskResponse> today(@AuthenticationPrincipal Jwt jwt) {
        return taskService.today(userId(jwt)).stream().map(TaskResponse::from).toList();
    }

    @PatchMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody UpdateTaskRequest req) {
        return TaskResponse.from(taskService.update(userId(jwt), id, req.title(), req.description(),
                req.estimateMinutes(), req.notes(), req.resources()));
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody UpdateTaskStatusRequest req) {
        return TaskResponse.from(taskService.changeStatus(userId(jwt), id, req.status()));
    }

    @PatchMapping("/{id}/schedule")
    public TaskResponse schedule(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody ScheduleTaskRequest req) {
        return TaskResponse.from(taskService.schedule(userId(jwt), id, req.scheduledFor()));
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody ReorderTasksRequest req) {
        taskService.reorder(userId(jwt), req.ids());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        taskService.delete(userId(jwt), id);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
