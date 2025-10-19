import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { useMbti } from '@hooks/useMbti';
import { collaborationAPI } from '@services/api';
import wsService from '@services/websocket';
import CollabPanel from '@components/CollabPanel';
import { Users, Plus, Video, MessageSquare } from 'lucide-react';

function Collaborate() {
  const { theme } = useMbti();
  const [sessions, setSessions] = useState([]);
  const [activeSession, setActiveSession] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newSession, setNewSession] = useState({
    name: '',
    description: '',
    max_participants: 10,
  });

  useEffect(() => {
    loadSessions();

    // Subscribe to collaboration updates
    wsService.on('collaborationUpdate', handleCollaborationUpdate);

    return () => {
      wsService.off('collaborationUpdate', handleCollaborationUpdate);
      if (activeSession) {
        wsService.leaveCollaborationSession(activeSession.id);
      }
    };
  }, []);

  const loadSessions = async () => {
    try {
      const response = await collaborationAPI.getSessions();
      setSessions(response.data);
    } catch (error) {
      console.error('Failed to load sessions:', error);
      toast.error('Failed to load collaboration sessions');
    }
  };

  const handleCollaborationUpdate = (data) => {
    // Handle real-time collaboration updates
    if (data.session_id === activeSession?.id) {
      setActiveSession((prev) => ({
        ...prev,
        participants: data.participants,
        annotations: data.annotations,
      }));
    }
  };

  const handleCreateSession = async (e) => {
    e.preventDefault();
    try {
      const response = await collaborationAPI.createSession(newSession);
      toast.success('Session created successfully!');
      setShowCreateModal(false);
      setNewSession({ name: '', description: '', max_participants: 10 });
      loadSessions();
      handleJoinSession(response.data);
    } catch (error) {
      console.error('Failed to create session:', error);
      toast.error('Failed to create session');
    }
  };

  const handleJoinSession = async (session) => {
    try {
      if (activeSession) {
        await collaborationAPI.leaveSession(activeSession.id);
        wsService.leaveCollaborationSession(activeSession.id);
      }

      await collaborationAPI.joinSession(session.id);
      wsService.joinCollaborationSession(session.id);

      const response = await collaborationAPI.getSession(session.id);
      setActiveSession(response.data);
      toast.success(`Joined session: ${session.name}`);
    } catch (error) {
      console.error('Failed to join session:', error);
      toast.error('Failed to join session');
    }
  };

  const handleLeaveSession = async () => {
    if (!activeSession) return;

    try {
      await collaborationAPI.leaveSession(activeSession.id);
      wsService.leaveCollaborationSession(activeSession.id);
      setActiveSession(null);
      toast.info('Left collaboration session');
      loadSessions();
    } catch (error) {
      console.error('Failed to leave session:', error);
      toast.error('Failed to leave session');
    }
  };

  return (
    <div className="min-h-screen py-8 px-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 flex justify-between items-center">
          <div>
            <h1 className="text-4xl font-bold mb-2" style={{ color: theme.primary }}>
              Collaboration
            </h1>
            <p className="text-gray-600">
              Work together in real-time with your team
            </p>
          </div>
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-2 px-4 py-2 rounded-lg text-white font-medium transition-colors"
            style={{ backgroundColor: theme.primary }}
          >
            <Plus className="w-5 h-5" />
            New Session
          </button>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left Sidebar - Sessions List */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-8">
              <h2
                className="text-xl font-semibold mb-4 flex items-center gap-2"
                style={{ color: theme.primary }}
              >
                <Users className="w-5 h-5" />
                Active Sessions
              </h2>
              <div className="space-y-2 max-h-[600px] overflow-y-auto">
                {sessions.length === 0 ? (
                  <p className="text-gray-500 text-sm">No active sessions</p>
                ) : (
                  sessions.map((session) => (
                    <div
                      key={session.id}
                      className={`p-4 rounded-lg border transition-colors cursor-pointer ${
                        activeSession?.id === session.id
                          ? 'border-2'
                          : 'border-gray-200 hover:bg-gray-50'
                      }`}
                      style={{
                        borderColor:
                          activeSession?.id === session.id ? theme.primary : undefined,
                      }}
                      onClick={() => handleJoinSession(session)}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <h3 className="font-semibold text-gray-800">{session.name}</h3>
                        <span className="flex items-center gap-1 text-sm text-gray-600">
                          <Users className="w-4 h-4" />
                          {session.participant_count}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-2">{session.description}</p>
                      <div className="flex items-center gap-2 text-xs text-gray-500">
                        <span>Created {new Date(session.created_at).toLocaleDateString()}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Main Content - Active Session */}
          <div className="lg:col-span-2">
            {activeSession ? (
              <div className="space-y-6">
                {/* Session Header */}
                <div className="bg-white rounded-lg shadow-md p-6">
                  <div className="flex justify-between items-start mb-4">
                    <div>
                      <h2 className="text-2xl font-bold mb-2" style={{ color: theme.primary }}>
                        {activeSession.name}
                      </h2>
                      <p className="text-gray-600">{activeSession.description}</p>
                    </div>
                    <button
                      onClick={handleLeaveSession}
                      className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                    >
                      Leave Session
                    </button>
                  </div>

                  {/* Participants */}
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-medium text-gray-700">Participants:</span>
                    {activeSession.participants?.map((participant) => (
                      <div
                        key={participant.id}
                        className="flex items-center gap-2 px-3 py-1 bg-gray-100 rounded-full text-sm"
                      >
                        <div
                          className="w-2 h-2 rounded-full bg-green-500"
                          title="Online"
                        />
                        {participant.name}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Collaboration Panel */}
                <CollabPanel session={activeSession} />

                {/* Quick Actions */}
                <div className="grid md:grid-cols-2 gap-4">
                  <button
                    className="flex items-center justify-center gap-3 p-6 bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
                    style={{ borderLeft: `4px solid ${theme.primary}` }}
                  >
                    <Video className="w-6 h-6" style={{ color: theme.primary }} />
                    <span className="font-medium text-gray-800">Start Video Call</span>
                  </button>
                  <button
                    className="flex items-center justify-center gap-3 p-6 bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
                    style={{ borderLeft: `4px solid ${theme.secondary}` }}
                  >
                    <MessageSquare className="w-6 h-6" style={{ color: theme.secondary }} />
                    <span className="font-medium text-gray-800">Open Chat</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="bg-white rounded-lg shadow-md p-12 text-center">
                <Users className="w-16 h-16 mx-auto mb-4 opacity-30" style={{ color: theme.primary }} />
                <h3 className="text-xl font-semibold mb-2 text-gray-800">
                  No Active Session
                </h3>
                <p className="text-gray-600 mb-6">
                  Join an existing session or create a new one to start collaborating
                </p>
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="px-6 py-3 rounded-lg text-white font-medium transition-colors"
                  style={{ backgroundColor: theme.primary }}
                >
                  Create New Session
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Create Session Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h2 className="text-2xl font-bold mb-4" style={{ color: theme.primary }}>
              Create Collaboration Session
            </h2>
            <form onSubmit={handleCreateSession} className="space-y-4">
              <div>
                <label className="label">Session Name</label>
                <input
                  type="text"
                  value={newSession.name}
                  onChange={(e) =>
                    setNewSession({ ...newSession, name: e.target.value })
                  }
                  className="input-field"
                  required
                />
              </div>
              <div>
                <label className="label">Description</label>
                <textarea
                  value={newSession.description}
                  onChange={(e) =>
                    setNewSession({ ...newSession, description: e.target.value })
                  }
                  className="input-field"
                  rows="3"
                />
              </div>
              <div>
                <label className="label">Max Participants</label>
                <input
                  type="number"
                  value={newSession.max_participants}
                  onChange={(e) =>
                    setNewSession({
                      ...newSession,
                      max_participants: parseInt(e.target.value),
                    })
                  }
                  className="input-field"
                  min="2"
                  max="50"
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 rounded-lg text-white font-medium transition-colors"
                  style={{ backgroundColor: theme.primary }}
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Collaborate;
