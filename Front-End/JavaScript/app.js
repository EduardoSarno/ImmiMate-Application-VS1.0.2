document.addEventListener("DOMContentLoaded", () => {
  // Chat Elements
  const sendBtn = document.getElementById("send-btn");
  const userInput = document.getElementById("user-input");
  const chatWindow = document.getElementById("chat-window");

  // Send message when the user clicks "Send"
  if (sendBtn && userInput && chatWindow) 
    {
      sendBtn.addEventListener("click", sendMessage);

      // Also send message on Enter key press
      userInput.addEventListener("keypress", (e) => 
        {
          if (e.key === "Enter") 
            {
              sendMessage();
          }
      });

      function sendMessage() 
      {
          const messageText = userInput.value.trim();
          if (!messageText) return;

          // Create user's message bubble
          const userMessageDiv = document.createElement("div");
          userMessageDiv.classList.add("message", "user-message");
          userMessageDiv.innerText = messageText;
          chatWindow.appendChild(userMessageDiv);

          // Clear input
          userInput.value = "";

          // Simulate a bot response (replace this with real API call if needed)
          setTimeout(() => 
            {
              const botMessageDiv = document.createElement("div");
              botMessageDiv.classList.add("message", "bot-message");
              botMessageDiv.innerText = "Thanks for your message! This is a placeholder bot response.";
              chatWindow.appendChild(botMessageDiv);

              // Scroll to bottom
              chatWindow.scrollTop = chatWindow.scrollHeight;
          }, 600);

          // Keep chat scrolled to the bottom
          chatWindow.scrollTop = chatWindow.scrollHeight;
      }
  }

  // FAQ Section Toggle
  const faqItems = document.querySelectorAll(".faq-item");

  if (faqItems.length > 0) {
      faqItems.forEach((item) => {
          const question = item.querySelector(".faq-question");
          const icon = item.querySelector(".faq-icon");

          question.addEventListener("click", () => {
              // Close other open items
              faqItems.forEach((otherItem) => {
                  if (otherItem !== item) {
                      otherItem.classList.remove("active");
                      otherItem.querySelector(".faq-icon").textContent = "+";
                  }
              });

              // Toggle current item
              item.classList.toggle("active");
              icon.textContent = item.classList.contains("active") ? "−" : "+";
          });
      });
  }
});