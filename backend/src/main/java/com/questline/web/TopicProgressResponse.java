package com.questline.web;

import com.questline.service.TopicService.TopicProgress;

/** Per-topic completion for the stats view. */
public record TopicProgressResponse(String name, String slug, long total, long done) {

    public static TopicProgressResponse from(TopicProgress progress) {
        return new TopicProgressResponse(progress.name(), progress.slug(),
                progress.total(), progress.done());
    }
}
