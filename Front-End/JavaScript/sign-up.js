document.addEventListener("DOMContentLoaded", () => {
  const signupForm = document.getElementById("signup-form");
  const errorMessage = document.getElementById("error-message");

  if (signupForm) {
    signupForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const email = document.getElementById("signup-email").value;
      const password = document.getElementById("signup-password").value;
      const confirmPassword = document.getElementById("confirm-password").value;

      if (password !== confirmPassword) {
          alert("Passwords do not match!");
          return;
      }

      const formData = new URLSearchParams();
      formData.append("email", email);
      formData.append("password", password);

      try {
        const response = await fetch("/auth/signup", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: formData
        });

        if (response.ok) {
          const data = await response.json();
          localStorage.setItem("access_token", data.token);
          alert("Sign-up successful!");
          window.location.href = "form.html";  // Redirect to form.html
        } else if (response.status === 400) {
          const data = await response.json();
          errorMessage.textContent = data.message;  // Display the error message
          errorMessage.style.color = "red";  // Set the text color to red
        }
      } catch (error) {
        console.error("Error during sign-up:", error);
        alert("Sign-up failed due to a network error.");
      }
    });
  }
});

async function signUp(email, password) {
  try {
    const response = await fetch('/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await response.json();
    if (response.ok) {
      console.log('Sign-up successful:', data.token);
      localStorage.setItem('access_token', data.token);
      window.location.href = 'chat.html';
    } else {
      console.error('Sign-up failed:', data.message);
      alert('Sign-up failed: ' + data.message);
    }
  } catch (error) {
    console.error('Error during sign-up:', error);
    alert('Sign-up failed due to a network error.');
  }
}