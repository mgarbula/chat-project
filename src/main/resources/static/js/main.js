'use-strict';

var registerPage = document.querySelector('#register-page');
var registerForm = document.querySelector('#register-form');
var registerText = document.querySelector('#name');
var chatPage = document.querySelector('#chat-page');
var connectedUser = document.querySelector('#connected-user');
var logout = document.querySelector('#logout');

var username = null;
const urlRegister = 'http://localhost:8080/register';
const urlLogout = 'http://localhost:8080/logout';

async function connect() {
    var uniqueId = null;
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
                uniqueId = json.id;
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
            registerPage.classList.remove('hidden');
            alert('Logout successful');
            chatPage.classList.add('hidden');
        }
    } catch (error) {
        console.error('Error: ', error);
    }
}

logout.addEventListener('click', onLogout, true);