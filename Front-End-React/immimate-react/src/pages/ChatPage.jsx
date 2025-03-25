import React, { useState } from 'react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { useAuth } from '../contexts/AuthContext';

const ChatPage = () => {
  const [message, setMessage] = useState('');
  const [chatHistory, setChatHistory] = useState([]);
  const { currentUser } = useAuth();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!message.trim()) return;

    // Add user message to chat
    setChatHistory([...chatHistory, { text: message, isUser: true }]);
    
    // Simulate bot response (in a real app, this would call your API)
    setTimeout(() => {
      setChatHistory(prev => [...prev, { 
        text: "Thanks for your message! I'm your AI immigration assistant. How can I help you today?", 
        isUser: false 
      }]);
    }, 1000);
    
    setMessage('');
  };

  return (
    <>
      <Navbar />
      <div className="chat-container">
        <div className="chat-header">
          <h1>Chat with Your Immigration Assistant</h1>
          <p>Get personalized guidance for your Canadian immigration journey</p>
          {currentUser && <div className="user-badge">{currentUser.email}</div>}
        </div>
        
        <div className="chat-window">
          {chatHistory.length === 0 ? (
            <div className="bot-message">
              Hello{currentUser ? `, ${currentUser.email.split('@')[0]}` : ''}! I'm your AI immigration assistant. How can I help with your Canadian immigration journey today?
            </div>
          ) : (
            chatHistory.map((chat, index) => (
              <div 
                key={index} 
                className={chat.isUser ? 'message user-message' : 'message bot-message'}
              >
                {chat.text}
              </div>
            ))
          )}
        </div>
        
        <div className="input-container">
          <form onSubmit={handleSubmit}>
            <input
              type="text"
              className="chat-input"
              placeholder="Type your message here..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
            />
            <button type="submit" className="send-btn">Send</button>
          </form>
        </div>
      </div>
      <Footer />
    </>
  );
};

export default ChatPage; 