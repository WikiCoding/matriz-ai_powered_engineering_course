package com.wikicoding.matrizapigateway.controllers;

public record LogoutRequest(String refresh_token) {
    @Override
    public String toString() {
        return "LogoutRequest[refresh_token=REDACTED]";
    }
}
