package com.wikicoding.matrizbackend.user.dtos;

import java.util.List;

public record UserPage(List<UserItem> content, long totalElements, int totalPages, int number, int size) {}
