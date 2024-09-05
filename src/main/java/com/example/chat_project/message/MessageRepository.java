package com.example.chat_project.message;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long> {
    List<Message> findAllBySenderAndReceiver(Long sender, Long receiver);
}
