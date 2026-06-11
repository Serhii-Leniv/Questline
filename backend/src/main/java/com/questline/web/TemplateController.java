package com.questline.web;

import com.questline.service.TemplateService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public roadmap templates: publish your goal, browse others', import into your own account.
 * Templates are intentionally public (any authenticated user can list/import); publishing and
 * importing are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping("/goals/{goalId}/publish")
    public TemplateSummaryResponse publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID goalId) {
        return TemplateSummaryResponse.from(templateService.publish(userId(jwt), goalId));
    }

    @GetMapping("/templates")
    public List<TemplateSummaryResponse> list() {
        return templateService.list().stream().map(TemplateSummaryResponse::from).toList();
    }

    @GetMapping("/templates/{id}")
    public TemplateDetailResponse get(@PathVariable UUID id) {
        return TemplateDetailResponse.from(templateService.get(id));
    }

    @PostMapping("/templates/{id}/import")
    public GoalTreeResponse importTemplate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return GoalTreeResponse.from(templateService.importTemplate(userId(jwt), id));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
