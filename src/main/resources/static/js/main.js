'use-strict';

var registerPage = document.querySelector('#register-page');
var registerForm = document.querySelector('#register-form');
var registerText = document.querySelector('#name');

var username = null;

async function connect() {
    var uniqueId = null;
    username = registerText.value.trim();
    
    try {
        const response = await fetch('http://localhost:8080/register', {
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