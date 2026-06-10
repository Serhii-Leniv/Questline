package com.questline.web;

public record PingResponse(String status) {

    public static PingResponse ok() {
        return new PingResponse("ok");
    }
}
