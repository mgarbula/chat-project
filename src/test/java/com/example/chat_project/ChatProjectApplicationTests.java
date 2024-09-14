package com.example.chat_project;

import com.example.chat_project.chat.ChatMessage;
import com.example.chat_project.chat.MessageType;
import com.example.chat_project.chat_status.ChatStatus;
import com.example.chat_project.message.MessageRepository;
import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatProjectApplicationTests {

	@LocalServerPort
	private Integer port;
	private WebSocketStompClient stompClient;
	private WebSocketStompClient stompStringClient;
	private String url;
	private BlockingQueue<ChatStatus> queue = new ArrayBlockingQueue<>(1);
	private final String topicPublic = "/topic/public";
	private final String addUser = "/app/chat.addUser";
	private final String sendMessage = "/app/chat.sendMessage";
	private BlockingQueue<ChatMessage> messagesQueue = new ArrayBlockingQueue<>(1);
	private BlockingQueue<String> userJoinedQueue = new ArrayBlockingQueue<>(1);
	
	@Autowired
	private UserRepository repository;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	TestRestTemplate restTemplate;
	
	@BeforeEach
	void setup() {
		this.stompClient = new WebSocketStompClient( new SockJsClient(
				List.of(new WebSocketTransport(new StandardWebSocketClient()))
		));
		this.url = String.format("ws://localhost:%d/ws", this.port);
	}
	
	@Test
	@DirtiesContext
	void testDatabase() {
		ChatUser chatUser = new ChatUser(1L, "user", 1234L);
		assertThat(chatUser).isEqualTo(repository.findUserById(1L));
		
		ChatUser chatUser2 = new ChatUser("newUser", 5678L);
		repository.save(chatUser2);
		assertThat(chatUser2).isEqualTo(repository.findUserById(2L));
	}
	
	@Test
	void shouldReturnId() {
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender("newUser")
				.type(MessageType.JOIN)
				.build();
		ResponseEntity<Long> response = restTemplate
				.postForEntity("/register", newUserMessage, Long.class);
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void shouldConnect() throws ExecutionException, InterruptedException, TimeoutException {
		StompSession session = createSession();
		assertTrue(session.isConnected());
	}
	
	
	@Test
	@DirtiesContext
	void shouldAddUserToSession() {
		String newUsername = "NewUser";
		
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.JOIN)
				.build();
		
		ResponseEntity<Long> response = restTemplate
				.postForEntity("/register", newUserMessage, Long.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
	}
	
	@Test
	@DirtiesContext
	void shouldInformAboutJoin() throws ExecutionException, InterruptedException, TimeoutException {
		this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		
		this.stompStringClient = new WebSocketStompClient( new SockJsClient(
				List.of(new WebSocketTransport(new StandardWebSocketClient()))
		));
		this.stompStringClient.setMessageConverter(new StringMessageConverter());
		
		StompSession session = createStringSession();
		StompSession.Subscription subs = session.subscribe("/topic/userJoined", new UserJoinedStompFrameHandler());
		System.out.println(subs.getSubscriptionHeaders());
		
		String newUsername2 = "NewUser2";
		ChatMessage newUserMessage2 = ChatMessage.builder()
				.sender(newUsername2)
				.type(MessageType.JOIN)
				.build();
		
		ResponseEntity<Long> response = restTemplate.
				postForEntity("/register", newUserMessage2, Long.class);
		Long id = response.getBody();
		
		StompSession session2 = createSession();
		StompHeaders headers = new StompHeaders();
		headers.setDestination("/app/chat.addUser/" + id);
		session2.send(headers, newUserMessage2);
		
		await()
				.atMost(5, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals("NewUser2", userJoinedQueue.poll()));
	}
	
	@Test
	@DirtiesContext
	void shouldRemoveUserFromSession() {
		String newUsername = "NewUser";
		
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.JOIN)
				.build();
		ResponseEntity<Long> response = restTemplate
				.postForEntity("/register", newUserMessage, Long.class);
		
		newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.LEAVE)
				.build();
		ResponseEntity<Void> responseEntity = restTemplate
				.postForEntity("/logout", newUserMessage, Void.class);
		
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(repository.findUserByUsername("NewUser")).isNull();
	}
	
	@Test
	@DirtiesContext
	void shouldNotAddUserWithExistingUsername() {
		String newUsername = "user";
		
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.JOIN)
				.build();
		
		ResponseEntity<Long> response = restTemplate
				.postForEntity("/register", newUserMessage, Long.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}
	
	@Test
	@DirtiesContext
	void shouldSendMessage() throws ExecutionException, InterruptedException, TimeoutException {
		String sender = "sender";
		ChatMessage senderMessage = ChatMessage.builder()
				.sender(sender)
				.type(MessageType.JOIN)
				.build();
		
		ResponseEntity<Long> response = restTemplate
				.postForEntity("/register", senderMessage, Long.class);
		Long senderId = response.getBody();
		
		String receiver = "receiver";
		ChatMessage receiverMessage = ChatMessage.builder()
				.sender(receiver)
				.type(MessageType.JOIN)
				.build();
		response = restTemplate
				.postForEntity("/register", receiverMessage, Long.class);
		Long receiverId = response.getBody();
		
		this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		StompSession session = createSession();
		
		session.subscribe(topicPublic + "/" + senderId, new ChatStatusStompFrameHandler());
		session.subscribe(topicPublic + "/" + receiverId,
				new ChatStompFrameHandler());
		
		ChatMessage message = ChatMessage.builder()
				.sender(sender)
				.type(MessageType.CHAT)
				.content("Hello!")
				.build();
		
		StompHeaders headers = new StompHeaders();
		headers.setDestination(sendMessage + "/" + senderId);
		headers.add("destinationId", Long.toString(receiverId));
		session.send(headers, message);
		
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals("Hello!", messagesQueue.poll().getContent()));
		
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals(ChatStatus.MESSAGE_SENT, queue.poll()));
		
		assertThat(messageRepository.findAllBySenderAndReceiver(senderId, receiverId).size()).isEqualTo(1);
	}

	private StompSession createSession() throws ExecutionException, InterruptedException, TimeoutException {
		return this.stompClient
				.connectAsync(url, new StompSessionHandlerAdapter() {
				})
				.get(1, TimeUnit.SECONDS);
	}
	
	private StompSession createStringSession() throws ExecutionException, InterruptedException, TimeoutException {
		return this.stompStringClient
				.connectAsync(url, new StompSessionHandlerAdapter() {
				})
				.get(1, TimeUnit.SECONDS);
	}

	private class ChatStatusStompFrameHandler implements StompFrameHandler {

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return ChatStatus.class;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			queue.add((ChatStatus) payload);
		}
	}
	
	private class ChatStompFrameHandler implements StompFrameHandler {
		
		@Override
		public Type getPayloadType(StompHeaders headers) {
			return ChatMessage.class;
		}
		
		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			messagesQueue.add((ChatMessage) payload);
		}
	}
	
	private class UserJoinedStompFrameHandler implements StompFrameHandler {
		
		@Override
		public Type getPayloadType(StompHeaders headers) {
			return String.class;
		}
		
		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			userJoinedQueue.add((String) payload);
		}
	}

}
