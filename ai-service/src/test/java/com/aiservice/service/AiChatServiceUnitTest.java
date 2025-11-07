package com.aiservice.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
// import dev.langchain4j.data.message.TextContent; // Removed this import
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection; // Added this import
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private ChatLanguageModel mockChatModel;

    @InjectMocks
    private AiChatServiceImpl aiChatService;

    @Captor
    private ArgumentCaptor<List<ChatMessage>> messagesCaptor;

    // We create mock responses that our model will return
    private final Response<AiMessage> firstResponse = Response.from(AiMessage.from("First response"));
    private final Response<AiMessage> secondResponse = Response.from(AiMessage.from("Second response"));
    private final Response<AiMessage> otherUserResponse = Response.from(AiMessage.from("Other user response"));

    @BeforeEach
    void setUp() {
        // This is necessary because @InjectMocks will create the service
        // using its constructor, injecting the mock model.
    }

    /**
     * Helper to create a mock Authentication object.
     */
    private Authentication createMockAuth(String username, String... roles) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);

        Set<GrantedAuthority> authorities = Stream.of(roles)
                .map(role -> {
                    GrantedAuthority authority = mock(GrantedAuthority.class);
                    when(authority.getAuthority()).thenReturn(role);
                    return authority;
                })
                .collect(Collectors.toSet());

        // Use a raw type for the collection to match the Authentication interface
        @SuppressWarnings("rawtypes")
        Collection authoritiesCollection = authorities;
        when(auth.getAuthorities()).thenReturn(authoritiesCollection);

        return auth;
    }

    @Test
    @SuppressWarnings("deprecation") // Suppress warning for .text()
    void testChat_firstMessage_formatsContextAndCreatesMemory() {
        // Arrange
        Authentication mockAuth = createMockAuth("testUser", "ROLE_TECHNICIAN");
        String userMessage = "How do I complete a task?";
        String expectedResponse = "First response";

        // --- FIX: Stub using any(List.class) to resolve ambiguity ---
        when(mockChatModel.generate(any(List.class))).thenReturn(firstResponse);

        // Act
        String actualResponse = aiChatService.chat(mockAuth, userMessage);

        // Assert
        assertEquals(expectedResponse, actualResponse);

        // --- FIX: Capture the argument in the verify call ---
        verify(mockChatModel, times(1)).generate(messagesCaptor.capture());

        // Check the messages sent to the model
        List<ChatMessage> capturedMessages = messagesCaptor.getValue();
        assertEquals(2, capturedMessages.size(), "Should have System prompt and User message");

        // Check the User message for correct context formatting
        assertTrue(capturedMessages.get(1) instanceof UserMessage, "Last message should be a UserMessage");
        // --- FIX: Reverted to .text() ---
        String formattedUserMessage = capturedMessages.get(1).text();

        assertTrue(formattedUserMessage.contains("[My Context: My username is 'testUser'"));
        assertTrue(formattedUserMessage.contains("my roles are 'ROLE_TECHNICIAN'"));
        assertTrue(formattedUserMessage.contains("My question is: How do I complete a task?"));
    }

    @Test
    @SuppressWarnings("deprecation") // Suppress warning for .text()
    void testChat_followUpMessage_usesExistingMemory() {
        // Arrange
        Authentication mockAuth = createMockAuth("testUser", "ROLE_TECHNICIAN");
        String firstUserMessage = "First message";
        String secondUserMessage = "Second message";

        // --- FIX: Stub using any(List.class) to resolve ambiguity ---
        when(mockChatModel.generate(any(List.class)))
                .thenReturn(firstResponse)  // For the first call
                .thenReturn(secondResponse); // For the second call

        // Act
        String response1 = aiChatService.chat(mockAuth, firstUserMessage);
        String response2 = aiChatService.chat(mockAuth, secondUserMessage);

        // Assert
        assertEquals("First response", response1);
        assertEquals("Second response", response2);

        // --- FIX: Capture the argument in the verify call ---
        verify(mockChatModel, times(2)).generate(messagesCaptor.capture());

        // Get the messages from the *second* call
        List<ChatMessage> secondCallMessages = messagesCaptor.getAllValues().get(1);

        // Assert that memory was used.
        // The list should contain: 1. System, 2. UserMsg1, 3. AiResponse1, 4. UserMsg2
        assertEquals(4, secondCallMessages.size(), "Should have full conversation history");

        // Check that the history is correct
        // --- FIX: Reverted to .text() ---
        assertTrue(secondCallMessages.get(1).text().contains(firstUserMessage));
        assertEquals("First response", secondCallMessages.get(2).text());
        assertTrue(secondCallMessages.get(3).text().contains(secondUserMessage));
    }

    @Test
    @SuppressWarnings("deprecation") // Suppress warning for .text()
    void testChat_differentUsers_haveIsolatedMemories() {
        // Arrange
        Authentication userA = createMockAuth("userA", "ROLE_A");
        Authentication userB = createMockAuth("userB", "ROLE_B");

        // --- FIX: Stub using any(List.class) to resolve ambiguity ---
        when(mockChatModel.generate(any(List.class)))
                .thenReturn(firstResponse)      // userA's first message
                .thenReturn(otherUserResponse)  // userB's message
                .thenReturn(secondResponse);    // userA's second message

        // Act
        // 1. User A sends first message
        aiChatService.chat(userA, "Message from A");

        // 2. User B sends a message
        aiChatService.chat(userB, "Message from B");

        // 3. User A sends a second message
        aiChatService.chat(userA, "Second message from A");

        // Assert
        // --- FIX: Capture the argument in the verify call ---
        verify(mockChatModel, times(3)).generate(messagesCaptor.capture());

        List<List<ChatMessage>> allCalls = messagesCaptor.getAllValues();

        // Check call 1 (User A)
        // --- FIX: Reverted to .text() ---
        List<ChatMessage> call1_UserA = allCalls.get(0);
        assertEquals(2, call1_UserA.size(), "User A's first call should have 2 messages (System + User)");
        assertTrue(call1_UserA.get(1).text().contains("userA"));
        assertTrue(call1_UserA.get(1).text().contains("Message from A"));

        // Check call 2 (User B)
        // --- FIX: Reverted to .text() ---
        List<ChatMessage> call2_UserB = allCalls.get(1);
        assertEquals(2, call2_UserB.size(), "User B's call should have 2 messages (System + User)");
        assertTrue(call2_UserB.get(1).text().contains("userB"));
        assertTrue(call2_UserB.get(1).text().contains("Message from B"));
        // Critically, it should NOT contain User A's message
        assertFalse(call2_UserB.get(1).text().contains("Message from A"));

        // Check call 3 (User A again)
        // --- FIX: Reverted to .text() ---
        List<ChatMessage> call3_UserA = allCalls.get(2);
        assertEquals(4, call3_UserA.size(), "User A's second call should have history (Sys + U1 + A1 + U2)");
        assertTrue(call3_UserA.get(1).text().contains("Message from A")); // Original message
        assertEquals("First response", call3_UserA.get(2).text()); // AI's first response
        assertTrue(call3_UserA.get(3).text().contains("Second message from A")); // New message
        // Critically, it should NOT contain User B's message
        assertFalse(call3_UserA.get(3).text().contains("Message from B"));
    }
}