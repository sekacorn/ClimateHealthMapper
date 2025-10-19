import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Loader, Trash2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { llmAPI } from '../services/api';
import { useMbti } from '../hooks/useMbti';
import DOMPurify from 'dompurify';

/**
 * LLMChat Component
 * Natural language interface for querying climate-health data
 * MBTI-tailored responses for personalized user experience
 * Uses lucide-react for icons, dompurify for sanitization
 */
function LLMChat({ context }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const { mbtiType } = useMbti();

  // Scroll to bottom when messages change
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  /**
   * Send message to LLM
   */
  const handleSend = async (e) => {
    e.preventDefault();

    if (!input.trim() || loading) return;

    const userMessage = {
      role: 'user',
      content: DOMPurify.sanitize(input.trim()),
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      // Send query with MBTI context for personalized responses
      const response = await llmAPI.query(userMessage.content, {
        ...context,
        mbtiType,
      });

      const assistantMessage = {
        role: 'assistant',
        content: response.data.response,
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMessage]);
    } catch (error) {
      console.error('LLM query error:', error);
      toast.error('Failed to get response from AI assistant');

      // Add error message
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Sorry, I encountered an error processing your request. Please try again.',
          timestamp: new Date().toISOString(),
          error: true,
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Clear chat history
   */
  const handleClear = async () => {
    try {
      await llmAPI.clearChatHistory();
      setMessages([]);
      toast.success('Chat history cleared');
    } catch (error) {
      console.error('Clear history error:', error);
      toast.error('Failed to clear chat history');
    }
  };

  /**
   * Suggested prompts based on MBTI
   */
  const getSuggestedPrompts = () => {
    const prompts = {
      ENTJ: [
        'What are the strategic health risks in my area?',
        'Create a mitigation plan for climate health impacts',
      ],
      INFP: [
        'How can I help my community with climate health issues?',
        'Tell me about the emotional impact of climate change',
      ],
      ESTP: [
        'Quick tips for reducing my health risks today',
        'What immediate actions should I take?',
      ],
      INTJ: [
        'Analyze the long-term health trends in my region',
        'What data-driven strategies work best?',
      ],
    };

    return prompts[mbtiType] || [
      'What health risks am I exposed to?',
      'How can I improve my health outcomes?',
    ];
  };

  return (
    <div className="bg-white rounded-lg shadow-md flex flex-col h-[600px]">
      {/* Header */}
      <div className="bg-indigo-600 text-white px-6 py-4 rounded-t-lg flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Bot className="h-6 w-6" />
          <div>
            <h3 className="font-semibold">Climate Health AI Assistant</h3>
            <p className="text-xs text-indigo-200">
              Personalized for {mbtiType} personality
            </p>
          </div>
        </div>
        <button
          onClick={handleClear}
          className="p-2 hover:bg-indigo-700 rounded transition"
          title="Clear chat"
        >
          <Trash2 className="h-5 w-5" />
        </button>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messages.length === 0 && (
          <div className="text-center text-gray-500 mt-8">
            <Bot className="h-16 w-16 mx-auto mb-4 text-gray-300" />
            <p className="mb-4">Ask me anything about climate and health!</p>
            <div className="space-y-2">
              <p className="text-sm font-semibold text-gray-600">Suggested questions:</p>
              {getSuggestedPrompts().map((prompt, index) => (
                <button
                  key={index}
                  onClick={() => setInput(prompt)}
                  className="block w-full max-w-md mx-auto px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg text-sm text-gray-700 transition"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message, index) => (
          <div
            key={index}
            className={`flex items-start space-x-3 ${
              message.role === 'user' ? 'flex-row-reverse space-x-reverse' : ''
            }`}
          >
            {/* Avatar */}
            <div
              className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
                message.role === 'user'
                  ? 'bg-indigo-600 text-white'
                  : message.error
                  ? 'bg-red-100 text-red-600'
                  : 'bg-gray-200 text-gray-600'
              }`}
            >
              {message.role === 'user' ? (
                <User className="h-4 w-4" />
              ) : (
                <Bot className="h-4 w-4" />
              )}
            </div>

            {/* Message bubble */}
            <div
              className={`flex-1 max-w-3xl ${
                message.role === 'user' ? 'text-right' : ''
              }`}
            >
              <div
                className={`inline-block px-4 py-2 rounded-lg ${
                  message.role === 'user'
                    ? 'bg-indigo-600 text-white'
                    : message.error
                    ? 'bg-red-50 text-red-900 border border-red-200'
                    : 'bg-gray-100 text-gray-900'
                }`}
              >
                <p className="whitespace-pre-wrap">{message.content}</p>
              </div>
              <p className="text-xs text-gray-400 mt-1">
                {new Date(message.timestamp).toLocaleTimeString()}
              </p>
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0 w-8 h-8 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center">
              <Bot className="h-4 w-4" />
            </div>
            <div className="flex-1">
              <div className="inline-block px-4 py-2 rounded-lg bg-gray-100">
                <Loader className="h-5 w-5 animate-spin text-gray-600" />
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSend} className="border-t border-gray-200 p-4">
        <div className="flex items-center space-x-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about climate health risks, genomic data, or recommendations..."
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            disabled={loading}
          />
          <button
            type="submit"
            disabled={loading || !input.trim()}
            className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center space-x-2"
          >
            {loading ? (
              <Loader className="h-5 w-5 animate-spin" />
            ) : (
              <Send className="h-5 w-5" />
            )}
            <span>Send</span>
          </button>
        </div>
      </form>
    </div>
  );
}

export default LLMChat;
