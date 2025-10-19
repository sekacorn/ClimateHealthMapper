import { useState, useEffect } from 'react';
import { Users, UserPlus, Video, MessageSquare, Share2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { collaborationAPI } from '../services/api';
import wsService from '../services/websocket';

/**
 * CollabPanel Component
 * Real-time collaboration interface for team sessions
 * Uses socket.io-client for WebSocket connections, lucide-react for icons
 */
function CollabPanel({ sessionId }) {
  const [session, setSession] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (sessionId) {
      loadSession();
      joinSession();
    }

    return () => {
      if (sessionId) {
        leaveSession();
      }
    };
  }, [sessionId]);

  const loadSession = async () => {
    try {
      const response = await collaborationAPI.getSession(sessionId);
      setSession(response.data);
      setParticipants(response.data.participants || []);
    } catch (error) {
      console.error('Failed to load session:', error);
      toast.error('Failed to load collaboration session');
    }
  };

  const joinSession = async () => {
    try {
      await collaborationAPI.joinSession(sessionId);
      wsService.joinRoom(sessionId);

      wsService.on('participant-joined', handleParticipantJoined);
      wsService.on('participant-left', handleParticipantLeft);
      wsService.on('message', handleMessage);

      toast.success('Joined collaboration session');
    } catch (error) {
      console.error('Failed to join session:', error);
      toast.error('Failed to join session');
    }
  };

  const leaveSession = async () => {
    try {
      await collaborationAPI.leaveSession(sessionId);
      wsService.leaveRoom(sessionId);
    } catch (error) {
      console.error('Failed to leave session:', error);
    }
  };

  const handleParticipantJoined = (participant) => {
    setParticipants((prev) => [...prev, participant]);
    toast.success(`${participant.name} joined the session`);
  };

  const handleParticipantLeft = (participantId) => {
    setParticipants((prev) => prev.filter((p) => p.id !== participantId));
  };

  const handleMessage = (msg) => {
    setMessages((prev) => [...prev, msg]);
  };

  const sendMessage = () => {
    if (!message.trim()) return;

    wsService.emit('send-message', {
      sessionId,
      message: message.trim(),
    });

    setMessage('');
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <Users className="h-6 w-6 text-indigo-600" />
          <h3 className="text-xl font-bold text-gray-900">Collaboration</h3>
        </div>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-600">
            {participants.length} participants
          </span>
        </div>
      </div>

      <div className="space-y-4">
        <div>
          <h4 className="font-semibold text-gray-700 mb-3">Participants</h4>
          <div className="space-y-2">
            {participants.map((p) => (
              <div key={p.id} className="flex items-center space-x-3 p-2 bg-gray-50 rounded">
                <div className="w-8 h-8 rounded-full bg-indigo-600 text-white flex items-center justify-center font-semibold">
                  {p.name?.charAt(0) || 'U'}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">{p.name}</p>
                  <p className="text-xs text-gray-500">{p.role}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <h4 className="font-semibold text-gray-700 mb-3">Chat</h4>
          <div className="bg-gray-50 rounded-lg p-4 h-64 overflow-y-auto mb-3">
            {messages.map((msg, index) => (
              <div key={index} className="mb-2">
                <p className="text-xs text-gray-500">{msg.userName}</p>
                <p className="text-sm text-gray-800">{msg.content}</p>
              </div>
            ))}
          </div>
          <div className="flex space-x-2">
            <input
              type="text"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
              placeholder="Type a message..."
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg"
            />
            <button
              onClick={sendMessage}
              className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
            >
              Send
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CollabPanel;
