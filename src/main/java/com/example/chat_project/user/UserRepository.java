package com.example.chat_project.user;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<ChatUser, Long> {
    ChatUser findUserById(Long id);
    ChatUser findUserByUsername(String username);
}