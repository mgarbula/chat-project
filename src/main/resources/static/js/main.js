'use-strict';

var registerPage = document.querySelector('#register-page');
var registerForm = document.querySelector('#register-form');
var registerText = document.querySelector('#name');
var chatPage = document.querySelector('#chat-page');
var connectedUser = document.querySelector('#connected-user');
var logout = document.querySelector('#logout');
var chatUsers = document.querySelector('#chat-users');
var messageArea = document.querySelector('#message-area');

const urlRegister = 'http://localhost:8080/register';
const urlLogout = 'http://localhost:8080/logout';
const urlGetId = 'http://localhost:8080/getUser?';
const urlGetUsers = 'http://localhost:8080/getUsers'

var username = null;
var uniqueId = null;
var stompClient = null;

async function connect() {
    username = registerText.value.trim();
    
    if (username.length < 3) {
        console.log('Username must be at least 3 characters long!');
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
                registerText.classList.remove('register-text-basic');
                registerText.classList.add('register-text-error');
                alert('Username already in use!');
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

async function onUserJoined(payload) {
    if (payload.body !== username) {
        //alert(payload.body, ' joined!');
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
            messageArea.innerHTML = '';
        }
    } catch (error) {
        console.error('Error: ', error);
    }
}

function appendUserElement(username, userId, connectedUsersList) {
    const listItem = document.createElement('li');
    listItem.classList.add('user-item');
    listItem.id = userId;

    const usernameSpan = document.createElement('span');
    usernameSpan.textContent = username;

    const receivedMsg = document.createElement('span');
    receivedMsg.textContent = '0';
    receivedMsg.classList.add('nbr-msg', 'hidden');

    listItem.appendChild(usernameSpan);
    listItem.appendChild(receivedMsg);

    //listItem.addEventListener('click', userItemClick);

    connectedUsersList.appendChild(listItem);
}

logout.addEventListener('click', onLogout, true);