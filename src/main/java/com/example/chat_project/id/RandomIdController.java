package com.example.chat_project.id;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/getId")
public class RandomIdController {
    
    private List<Long> ids = new ArrayList<>();
    
    @GetMapping
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
