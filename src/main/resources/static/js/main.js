'use-strict';

var registerPage = document.querySelector('#register-page');
var registerForm = document.querySelector('#register-form');
var registerText = document.querySelector('#name');
var chatPage = document.querySelector('#chat-page');
var connectedUser = document.querySelector('#connected-user');
var logout = document.querySelector('#logout');
var chatUsers = document.querySelector('#chat-users');
var chatMessages = document.querySelector('#chat-messages');
var messageForm = document.querySelector('#message-form');
var message = document.querySelector('#message');
var chatArea = document.querySelector('#chat-area');

const urlRegister = 'http://localhost:8080/register';
const urlLogout = 'http://localhost:8080/logout';
const urlGetId = 'http://localhost:8080/getUser?';
const urlGetUsers = 'http://localhost:8080/getUsers';
const urlGetMessages = 'http://localhost:8080/getMessages?';

var username = null;
var uniqueId = null;
var stompClient = null;
var clickedUserId = null;

function usernameAlert(textAlert) {
    registerText.classList.remove('register-text-basic');
    registerText.classList.add('register-text-error');
    alert(textAlert);
}

async function connect() {
    username = registerText.value.trim();
    
    if (username.length < 3) {
        usernameAlert('Username must be at least 3 characters long!');
    } else {
        try {
            const response = await fetch(urlRegister, {
                method: "POST",
                body: JSON.stringify({
                    sender: username,
                    type: "JOIN"
                }),
                headers: {
                    "Content-type": "application/json; charset:UTF-8"
                }
            });
            
            if (response.ok) {
                const json = await response.json();
                uniqueId = json;

                const socket = new SockJS('/ws');
                stompClient = Stomp.over(socket);
                stompClient.connect({}, onConnected);

                console.log('success ', uniqueId);
                registerPage.classList.add('hidden');
                chatPage.classList.remove('hidden');
                registerText.value = '';
                connectedUser.innerHTML = username;
            } else if (response.status === 409) {
                usernameAlert('Username already in use!');
            } else {
                console.error('Error ', response.statusText);
            }
        } catch (error) {
            console.error('Error: ', error);
        }
    }
}

async function getConnectedUsers() {
    try {
        const response = await fetch(urlGetUsers);
        if (response.ok) {
            const json = await response.json();
            json.forEach(function (user) {
                if (user.username !== username) {
                    appendUserElement(user.username, user.randomId, chatUsers);
                }
            })
        } else {
            console.error('Error ', response.statusText);
        }
    } catch (error) {
        console.error('Error ', error);
    }
}

function onConnected() {
    stompClient.send(`/app/chat.addUser/${uniqueId}`, {}, JSON.stringify({sender: username, type: 'JOIN'}));
    stompClient.subscribe(`/topic/public/${uniqueId}`, onMessageReceived);
    stompClient.subscribe('/topic/userJoined', onUserJoined);
    stompClient.subscribe('/topic/userDisconnected', onUserDisconnected);
    getConnectedUsers();
}

const handleMessageDotInline = (domElement, fun) => domElement.classList[fun]('hidden');

function handleMessageDot(userId, fun) {
    var users = chatUsers.getElementsByTagName("li");
    for (var i = 0; i < users.length; ++i) {
        if (users[i].id === userId) {
            handleMessageDotInline(users[i].lastChild, fun);
            break;
        }
    }
}

function onMessageReceived(payload) {
    const json = JSON.parse(payload.body);
    const message = json.content;
    const sender = json.sender;

    if (clickedUserId === sender) {
        addMessage('receiver', message);
    } else {
        handleMessageDot(sender, 'remove');
    }
}

async function onUserJoined(payload) {
    if (payload.body !== username) {
        try {
            const response = await fetch(urlGetId + new URLSearchParams({
                username: payload.body
            }).toString());
            if (response.ok) {
                const json = await response.json();
                userId = json;
                console.log('userId ', userId);
            } else {
                console.error('Error ', response.statusText);
            }
        } catch (error) {
            console.error('Error ', error);
        }
        appendUserElement(payload.body, userId, chatUsers);
    }
}

function onUserDisconnected(payload) {
    const disconnectedUserID = payload.body;
    if (payload !== uniqueId) {
        var users = chatUsers.getElementsByTagName("li");
        for (var i = 0; i < users.length; ++i) {
            if (users[i].id === disconnectedUserID) {
                chatUsers.removeChild(users[i]);
                break;
            }
        }
    }
}

async function onLogout() {
    try {
        const response = await fetch(urlLogout, {
            method: "POST",
            body: JSON.stringify({
                sender: username,
                type: "LEAVE"
            }),
            headers: {
                "Content-type": "application/json; charset:UTF-8"
            }
        });

        if (response.ok) {
            stompClient.send(`/app/chat.disconnect/${uniqueId}`, {}, JSON.stringify({sender: username, type: 'LEAVE'}));
            stompClient.disconnect();
            registerPage.classList.remove('hidden');
            alert('Logout successful');
            chatPage.classList.add('hidden');
            chatUsers.innerHTML = '';
            chatMessages.innerHTML = '';
        }
    } catch (error) {
        console.error('Error: ', error);
    }
}

function appendUserElement(username, userId, connectedUsersList) {
    const listItem = document.createElement('li');
    listItem.classList.add('user-item');
    listItem.id = userId;
    listItem.setAttribute('username', username);

    const usernameSpan = document.createElement('span');
    usernameSpan.textContent = username;

    const receivedMsg = document.createElement('span');
    receivedMsg.classList.add('nbr-msg', 'hidden');

    listItem.appendChild(usernameSpan);
    listItem.appendChild(receivedMsg);

    listItem.addEventListener('click', userItemClick);

    connectedUsersList.appendChild(listItem);
}

async function getMessages(secondId) {
    try {
        const response = await fetch(urlGetMessages + new URLSearchParams({
            callerId: uniqueId,
            secondId: secondId
        }).toString());
        if (response.ok) {
            const json = await response.json();
            return json;
        } else {
            console.error('Error ', response.statusText);
        }
    } catch (error) {
        console.error('Error ', error);
    }
}

function showMessage(message) {
    const messageContent = message.content;
    if (message.sender === uniqueId) {
        addMessage('sender', messageContent);
    } else {
        addMessage('receiver', messageContent);
    }
}

function scrollToBottom() {
    chatMessages.scrollTo(0, chatMessages.scrollHeight);
}

async function userItemClick(event) {
    const node = document.getElementById('clickedUsername');
    if (node !== null) {
        node.parentNode.removeChild(node);
        chatMessages.innerHTML = '';
    }

    const clickedUser = event.currentTarget;
    clickedUserId = clickedUser.getAttribute('id');
    handleMessageDot(clickedUserId, 'add');

    const clickedUsername = clickedUser.getAttribute('username');
    messageForm.classList.remove('hidden');

    const h2 = document.createElement('h2');
    h2.setAttribute('id', 'clickedUsername');
    h2.textContent = clickedUsername;

    chatArea.insertBefore(h2, chatArea.firstChild);

    const messages = await getMessages(clickedUserId);
    console.log(messages);
    if (messages.length > 0) {
        messages.forEach((message) => showMessage(message));
    }
    scrollToBottom();
}

function addMessage(type, message) {
    const messageDiv = document.createElement('div');
    messageDiv.classList.add('message');
    messageDiv.classList.add(type);

    const messageP = document.createElement('p');
    messageP.textContent = message;

    messageDiv.appendChild(messageP);
    chatMessages.appendChild(messageDiv);
}

function sendMessage(event) {
    const messageText = message.value.trim();
    if (messageText.length > 0) {
        stompClient.send(`/app/chat.sendMessage/${uniqueId}`, {destinationId: clickedUserId}, JSON.stringify({
            sender: uniqueId,
            receiver: clickedUserId,
            content: messageText,
            times: Date.now()
        }));

        addMessage('sender', messageText);
        message.value = '';
    }
    scrollToBottom();

    event.preventDefault();
}

logout.addEventListener('click', onLogout, true);
messageForm.addEventListener('submit', sendMessage, true);