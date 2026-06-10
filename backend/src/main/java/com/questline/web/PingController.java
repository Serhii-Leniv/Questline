package com.questline.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public liveness probe used by the frontend and smoke tests.
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public PingResponse ping() {
        return PingResponse.ok();
    }
}
