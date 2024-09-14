package com.example.chat_project.chat;

import com.example.chat_project.chat_status.ChatStatus;
import com.example.chat_project.message.Message;
import com.example.chat_project.message.MessageRepository;
import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.sql.Timestamp;
import java.time.Instant;

@Controller
public class ChatController {
    
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    @Autowired
    private SimpMessageSendingOperations messageTemplate;
    
    public ChatController(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }
    
    @MessageMapping("/chat.addUser/{id}")
    @SendTo("/topic/userJoined")
    public String userJoined(@Payload ChatMessage message) {
        return message.getSender() + " joined!";
    }
    
    @MessageMapping("/chat.sendMessage/{idFrom}")
    @SendTo("/topic/public/{idFrom}")
    public ChatStatus sendMessage(@Payload ChatMessage message, @DestinationVariable("idFrom") Long id,
                            SimpMessageHeaderAccessor headerAccessor) {
        Long idTo = Long.valueOf(headerAccessor.getNativeHeader("destinationId").get(0));
        messageTemplate.convertAndSend("/topic/public/" + idTo, message);
        Message messageToSave = Message.builder()
                .sender(id)
                .receiver(idTo)
                .content(message.getContent())
                .times(new Timestamp(Instant.now().toEpochMilli()))
                .build();
        messageRepository.save(messageToSave);
        return ChatStatus.MESSAGE_SENT;
    }

}
