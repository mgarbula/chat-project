package com.example.chat_project.chat;

import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/register")
public class AddUserController {
    
    private final UserRepository userRepository;
    private List<Long> ids = new ArrayList<>();
    
    public AddUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    
    @PostMapping
    private ResponseEntity<Long> addUser(@RequestBody ChatMessage message) {
        String sender = message.getSender();
        if (userRepository.findUserByUsername(sender) == null) {
            Long id = generateId();
            userRepository.save(new ChatUser(sender, id));
            return ResponseEntity.ok(id);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    
    private Long generateId() {
        Random rand = new Random();
        Long number;
        do {
            number = (rand.nextLong(10000) + 12345) * 31;
        } while (ids.contains(number));
        ids.add(number);
        return number;
    }
    
}
