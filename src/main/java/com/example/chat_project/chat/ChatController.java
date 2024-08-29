package com.example.chat_project.chat;

import com.example.chat_project.chat_status.ChatStatus;
import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
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

    @MessageMapping("/chat.addUser/{id}")
    @SendTo("/topic/public/{id}")
    public ChatStatus addUser(@Payload ChatMessage message, @DestinationVariable("id") Long id, 
                              SimpMessageHeaderAccessor headerAccessor) {
        String sender = message.getSender();
        if (userRepository.findUserByUsername(sender) == null) {
            headerAccessor.getSessionAttributes().put("username", sender);
            userRepository.save(new ChatUser(sender));
            return ChatStatus.USER_ADDED;
        }
        return ChatStatus.USER_NAME_INVALID;
    }

}
