package com.example.chat_project.user;

import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface UserRepository extends CrudRepository<ChatUser, Long> {
    ChatUser findUserById(Long id);
    ChatUser findUserByUsername(String username);
    ArrayList<ChatUser> findAll();
}