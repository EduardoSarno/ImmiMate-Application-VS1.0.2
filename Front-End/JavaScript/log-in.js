document.addEventListener("DOMContentLoaded", () => {
  localStorage.clear(); // Clear localStorage at the start of login process
  const signinForm = document.getElementById("login-form");

  if (signinForm) {
    signinForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const email = document.getElementById("login-email").value;
      const password = document.getElementById("login-password").value;

      console.log("Login form submitted"); // Log form submission
      console.log("Email:", email);
      console.log("Password:", password);

      const formData = new URLSearchParams();
      formData.append("email", email);
      formData.append("password", password);
      console.log("Form Data:", formData.toString());

      try {
        const response = await fetch("/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: formData,
        });

        console.log("Fetch response:", response); // Log fetch response
        const data = await response.json();
        console.log("Response data:", data); // Log response data

        if (response.ok) {
          localStorage.setItem("access_token", data.token);
          localStorage.setItem("user_email", email);
          console.log("Access token stored:", localStorage.getItem("access_token")); // Verify token storage
          console.log("User email stored:", localStorage.getItem("user_email"));   // Verify email storage
          alert("Sign-in successful!");
          window.location.href = "form.html";
        } else {
          alert("Sign-in failed: " + data.message);
        }
      } catch (error) {
        console.error("Error during sign-in:", error);
        alert("Sign-in failed due to a network error.");
      }
    });
  }

  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token");
  const email = urlParams.get("email");

  console.log("URL token parameter (log-in.js):", token); // Log token from URL in log-in.js
  console.log("URL email parameter (log-in.js):", email); // Log email from URL in log-in.js

  if (token) {
    localStorage.setItem("access_token", token);
    console.log("Token stored from URL (log-in.js):", localStorage.getItem("access_token")); // Verify token storage from URL in log-in.js
  } else {
    console.log("No token found in URL (log-in.js)"); // Log if no token in URL
  }

  if (email) {
    localStorage.setItem("user_email", email);
    console.log("User email stored from URL (log-in.js):", localStorage.getItem("user_email")); // Verify email storage from URL in log-in.js
  } else {
    console.log("No email found in URL (log-in.js)"); // Log if no email in URL
  }

  if (token && email) {
    console.log("Redirecting to form.html (log-in.js)"); // Log before redirect in log-in.js
    window.location.href = "form.html";
  } else {
    console.log("Token or email missing, not redirecting from log-in.js"); // Log if not redirecting
  }
});

async function signIn(email, password) {
  try {
    const response = await fetch("/auth/login", {
      // Update URL if deployed
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });

    const data = await response.json();
    if (response.ok) {
      console.log("log-in successful:", data.token);
      localStorage.setItem("access_token", data.token); // Store token
      window.location.href = "form.html"; // Redirect to form.html
      console.log("User email:", email); // Log user email to console
    } else {
      console.error("Sign-in failed:", data.message);
      alert("Sign-in failed: " + data.message);
    }
  } catch (error) {
    console.error("Error during sign-in:", error);
    alert("Sign-in failed due to a network error.");
  }
}