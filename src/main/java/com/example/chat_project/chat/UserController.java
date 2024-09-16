package com.example.chat_project.chat;

import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping
public class UserController {
    
    private final UserRepository userRepository;
    private List<Long> ids = new ArrayList<>();
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @PostMapping("/register")
    private ResponseEntity<Long> addUser(@RequestBody ChatMessage message) {
        String sender = message.getSender();
        if (userRepository.findUserByUsername(sender) == null) {
            Long id = generateId();
            userRepository.save(new ChatUser(sender, id));
            return ResponseEntity.ok(id);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    
    @PostMapping("/logout")
    private ResponseEntity<Void> logout(@RequestBody ChatMessage message) {
        String sender = message.getSender();
        ChatUser user = userRepository.findUserByUsername(sender);
        if (user != null) {
            userRepository.delete(user);
            ids.remove(user.getId());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @RequestMapping(value="getUser", method= RequestMethod.GET)
    private ResponseEntity<Long> getUserId(@RequestParam("username") String username) {
        ChatUser user = userRepository.findUserByUsername(username);
        return ResponseEntity.ok(user.getRandomId());
    }
    
    @GetMapping("/getUsers")
    private ResponseEntity<ArrayList<ChatUser>> getUsers() {
        ArrayList<ChatUser> users = userRepository.findAll();
        return ResponseEntity.ok(users);
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
