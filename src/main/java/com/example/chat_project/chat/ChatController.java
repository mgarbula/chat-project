package com.example.chat_project.chat;

import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    
    private final UserRepository userRepository;
    
    public ChatController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        userRepository.save(new ChatUser(message.getSender()));
        return message;
    }

}
