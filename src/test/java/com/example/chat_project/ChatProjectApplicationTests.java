package com.example.chat_project;

import com.example.chat_project.chat.ChatMessage;
import com.example.chat_project.chat.MessageType;
import com.example.chat_project.chat_status.ChatStatus;
import com.example.chat_project.user.ChatUser;
import com.example.chat_project.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatProjectApplicationTests {

	@LocalServerPort
	private Integer port;
	private WebSocketStompClient stompClient;
	private String url;
	private BlockingQueue<ChatStatus> queue = new ArrayBlockingQueue<>(1);
	private final String topicPublic = "/topic/public";
	private final String addUser = "/app/chat.addUser";
	private final String sendMessage = "/app/chat.sendMessage";
	private BlockingQueue<ChatMessage> messagesQueue = new ArrayBlockingQueue<>(1);
	
	@Autowired
	private UserRepository repository;
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
		ChatUser chatUser = new ChatUser(1L, "user");
		assertThat(chatUser).isEqualTo(repository.findUserById(1L));
		
		ChatUser chatUser2 = new ChatUser("newUser");
		repository.save(chatUser2);
		assertThat(chatUser2).isEqualTo(repository.findUserById(2L));
	}
	
	@Test
	void shouldReturnId() {
		ResponseEntity<Long> response = restTemplate
				.getForEntity("/", Long.class);
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void shouldConnect() throws ExecutionException, InterruptedException, TimeoutException {
		StompSession session = createSession();
		assertTrue(session.isConnected());
	}
	
	
	@Test
	@DirtiesContext
	void shouldAddUserToSession() throws ExecutionException, InterruptedException, TimeoutException {
		String newUsername = "NewUser";
		ResponseEntity<Long> response = restTemplate
				.getForEntity("/", Long.class);
		Long newUserId = response.getBody();
		
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.JOIN)
				.build();
		
		this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		StompSession session = createSession();
		StompSession.Subscription subscription = session.subscribe(topicPublic + "/" + newUserId, 
				new ChatStatusStompFrameHandler());
		assertNotNull(subscription);
		
		session.send(addUser + "/" + newUserId, newUserMessage);
		
		// maybe not the best approach. I sleep for 1 second
		// because I need to wait for addUser() method to execute.
		// await() waits for execution, but poll() executes immediately
		Thread.sleep(1000);
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals(ChatStatus.USER_ADDED, queue.poll()));
		
		assertThat(repository.findUserByUsername("NewUser")).isNotNull();
	}
	
	@Test
	@DirtiesContext
	void shouldNotAddUserWithExistingUsername() throws ExecutionException, InterruptedException, TimeoutException {
		String newUsername = "user";
		ResponseEntity<Long> response = restTemplate
				.getForEntity("/", Long.class);
		Long newUserId = response.getBody();
		
		ChatMessage newUserMessage = ChatMessage.builder()
				.sender(newUsername)
				.type(MessageType.JOIN)
				.build();
		
		this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
		StompSession session = createSession();
		StompSession.Subscription subscription = session.subscribe(topicPublic + "/" + newUserId, new ChatStatusStompFrameHandler());
		
		session.send(addUser + "/" + newUserId, newUserMessage);
		Thread.sleep(1000);
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals(ChatStatus.USER_NAME_INVALID, queue.poll()));
	}
	
	@Test
	@DirtiesContext
	void shouldSendMessage() throws ExecutionException, InterruptedException, TimeoutException {
		String sender = "sender";
		ResponseEntity<Long> response = restTemplate
				.getForEntity("/", Long.class);
		Long senderId = response.getBody();
		
		String receiver = "receiver";
		response = restTemplate
				.getForEntity("/", Long.class);
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
		
		Thread.sleep(1000);
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals("Hello!", messagesQueue.poll().getContent()));
		
		await()
				.atMost(1, TimeUnit.SECONDS)
				.untilAsserted(() -> assertEquals(ChatStatus.MESSAGE_SENT, queue.poll()));
	}

	private StompSession createSession() throws ExecutionException, InterruptedException, TimeoutException {
		return this.stompClient
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

}
