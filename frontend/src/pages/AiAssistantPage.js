import React, { useState, useRef, useEffect } from "react";
import ReactMarkdown from "react-markdown";
import { aiAPI } from "../api";

const QUICK_PROMPTS = [
  "How can I reduce my monthly expenses?",
  "What is a good savings rate?",
  "How should I allocate my budget?",
  "Give me tips to avoid overspending",
  "How to build an emergency fund?",
  "Explain the 50/30/20 rule",
];

function ChatMessage({ msg }) {
  const isUser = msg.role === "user";

  return (
      <div
          className={`flex gap-3 ${
              isUser ? "justify-end" : "justify-start"
          }`}
      >
        {!isUser && (
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-r from-purple-500 to-indigo-500 flex items-center justify-center text-white flex-shrink-0 shadow-md">
              🤖
            </div>
        )}

        <div
            className={`max-w-[80%] rounded-3xl px-5 py-4 shadow-sm ${
                isUser
                    ? "bg-gradient-to-r from-indigo-500 to-purple-500 text-white rounded-br-md"
                    : "bg-white border border-gray-100 text-gray-800 rounded-bl-md"
            }`}
        >
          {isUser ? (
              <p className="whitespace-pre-wrap text-sm leading-7">
                {msg.content}
              </p>
          ) : (
              <div className="prose prose-sm max-w-none">
                <ReactMarkdown>{msg.content}</ReactMarkdown>
              </div>
          )}

          <p
              className={`text-xs mt-3 ${
                  isUser ? "text-white/70" : "text-gray-400"
              }`}
          >
            {new Date(msg.timestamp).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
        </div>

        {isUser && (
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-r from-indigo-500 to-purple-500 flex items-center justify-center text-white flex-shrink-0 shadow-md">
              👤
            </div>
        )}
      </div>
  );
}

export default function AiAssistantPage() {
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      content:
          "👋 Hi! I’m your **AI Finance Assistant**.\n\nI can help you with:\n\n- Budget planning\n- Saving strategies\n- Expense analysis\n- Financial advice\n- Smart spending habits\n\nHow can I help you today?",
      timestamp: Date.now(),
    },
  ]);

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages]);

  const sendMessage = async (text) => {
    const message = text || input.trim();

    if (!message || loading) return;

    setInput("");

    const userMessage = {
      role: "user",
      content: message,
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMessage]);

    setLoading(true);

    try {
      const response = await aiAPI.chat({
        message,
      });

      const aiMessage = {
        role: "assistant",
        content: response.data.data,
        timestamp: Date.now(),
      };

      setMessages((prev) => [...prev, aiMessage]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content:
              "⚠️ Unable to connect to AI service.\n\nPlease try again later.",
          timestamp: Date.now(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="h-screen overflow-hidden bg-gradient-to-br from-gray-50 to-purple-50 flex flex-col">

        {/* Header */}
        <div className="px-6 py-5 border-b bg-white/80 backdrop-blur-md flex-shrink-0">
          <h1 className="text-3xl font-bold text-gray-900">
            AI Finance Assistant 🤖
          </h1>

          <p className="text-gray-500 mt-1">
            Smart financial guidance powered by AI
          </p>
        </div>

        {/* Main Layout */}
        <div className="flex-1 overflow-hidden">
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 h-full p-6">

            {/* Chat Section */}
            <div className="lg:col-span-3 h-full overflow-hidden">

              <div className="bg-white rounded-3xl shadow-xl border border-gray-100 h-full flex flex-col overflow-hidden">

                {/* Chat Top */}
                <div className="px-6 py-4 border-b bg-white flex items-center justify-between flex-shrink-0">
                  <div>
                    <h2 className="font-bold text-gray-800">
                      Personal Finance Chat
                    </h2>

                    <p className="text-xs text-gray-500">
                      Ask anything about money & savings
                    </p>
                  </div>

                  <div className="text-xs bg-green-100 text-green-700 px-3 py-1 rounded-full">
                    Online
                  </div>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6 min-h-0">

                  {messages.map((msg, index) => (
                      <ChatMessage key={index} msg={msg} />
                  ))}

                  {loading && (
                      <div className="flex gap-3">

                        <div className="w-10 h-10 rounded-2xl bg-gradient-to-r from-purple-500 to-indigo-500 flex items-center justify-center text-white flex-shrink-0 shadow-md">
                          🤖
                        </div>

                        <div className="bg-white border border-gray-100 rounded-3xl rounded-bl-md px-5 py-4 flex gap-1 shadow-sm">
                          {[0, 1, 2].map((i) => (
                              <div
                                  key={i}
                                  className="w-2 h-2 bg-purple-400 rounded-full animate-bounce"
                                  style={{
                                    animationDelay: `${i * 0.15}s`,
                                  }}
                              ></div>
                          ))}
                        </div>
                      </div>
                  )}

                  <div ref={messagesEndRef}></div>
                </div>

                {/* Input */}
                <div className="border-t bg-white p-4 flex-shrink-0">

                  <div className="flex gap-3">

                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={(e) =>
                            e.key === "Enter" &&
                            !e.shiftKey &&
                            sendMessage()
                        }
                        placeholder="Ask about budgeting, saving, investing..."
                        className="flex-1 border border-gray-200 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-purple-400 focus:border-transparent bg-gray-50"
                    />

                    <button
                        onClick={() => sendMessage()}
                        disabled={loading || !input.trim()}
                        className="px-6 rounded-2xl bg-gradient-to-r from-indigo-500 to-purple-500 text-white font-medium shadow-md hover:opacity-90 disabled:opacity-50"
                    >
                      {loading ? "..." : "➤"}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Sidebar */}
            <div className="space-y-4 overflow-y-auto pr-1">

              {/* Quick Prompts */}
              <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-5">

                <h3 className="font-semibold text-gray-800 mb-4">
                  💡 Quick Questions
                </h3>

                <div className="space-y-3">
                  {QUICK_PROMPTS.map((prompt, index) => (
                      <button
                          key={index}
                          onClick={() => sendMessage(prompt)}
                          disabled={loading}
                          className="w-full text-left text-sm px-4 py-3 rounded-2xl bg-gray-50 hover:bg-purple-50 hover:text-purple-700 transition-all text-gray-600"
                      >
                        {prompt}
                      </button>
                  ))}
                </div>
              </div>

              {/* AI Capabilities */}
              <div className="bg-gradient-to-br from-purple-100 to-pink-100 rounded-3xl shadow-lg p-5 border border-purple-200">

                <p className="text-xs font-bold text-purple-700 uppercase tracking-wider mb-4">
                  AI Capabilities
                </p>

                <ul className="space-y-3 text-sm text-gray-700">
                  {[
                    "Expense analysis",
                    "Budget planning",
                    "Savings advice",
                    "Debt management",
                    "Investment basics",
                    "Emergency fund guidance",
                  ].map((item) => (
                      <li
                          key={item}
                          className="flex items-center gap-2"
                      >
                        <span className="text-purple-600">✓</span>
                        {item}
                      </li>
                  ))}
                </ul>

                <div className="mt-5 bg-white/70 rounded-2xl p-3 text-xs text-purple-700 font-medium border border-purple-200">
                  🔒 Your financial data is secure & private
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}
