package com.aiservice.service;

import org.springframework.security.core.Authentication;

public interface AiChatService {
    String chat(Authentication authentication, String userMessage);
}
