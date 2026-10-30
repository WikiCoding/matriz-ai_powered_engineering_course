package com.wikicoding.matrizapigateway.controllers;

import java.util.UUID;

record UserCreateRequest(String email, String role, UUID teamId) { }
