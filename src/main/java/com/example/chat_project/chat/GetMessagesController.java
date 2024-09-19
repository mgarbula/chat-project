package com.example.chat_project.chat;

import com.example.chat_project.message.Message;
import com.example.chat_project.message.MessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping
public class GetMessagesController {
    
    private final MessageRepository messageRepository;
    
    public GetMessagesController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }
    
    @RequestMapping(value="getMessages", method= RequestMethod.GET)
    private ResponseEntity<List<Message>> getMessages(@RequestParam("callerId") Long callerId,
                                                      @RequestParam("secondId") Long secondId) {
        List<Message> messagesFromCaller = messageRepository.findAllBySenderAndReceiver(callerId, secondId);
        List<Message> messagesFromSecond = messageRepository.findAllBySenderAndReceiver(secondId, callerId);
        List<Message> messagesUnmodifiable = Stream.concat(messagesFromCaller.stream(), messagesFromSecond.stream()).toList();
        List<Message> messages = new ArrayList<>(messagesUnmodifiable);
        messages.sort(Comparator.comparingLong(m -> m.getTimes().getTime()));
        return ResponseEntity.ok(messages);
    }
    
}
