package com.questline.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** New ordering for a set of tasks: each id's position becomes its order index. */
public record ReorderTasksRequest(@NotEmpty List<UUID> ids) {
}
