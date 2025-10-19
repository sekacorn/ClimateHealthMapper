import { io } from 'socket.io-client';
import { toast } from 'react-hot-toast';

const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8000';

class WebSocketService {
  constructor() {
    this.socket = null;
    this.listeners = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    if (this.socket?.connected) {
      console.log('WebSocket already connected');
      return;
    }

    const token = localStorage.getItem('authToken');

    this.socket = io(WS_URL, {
      auth: {
        token,
      },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 5000,
      reconnectionAttempts: this.maxReconnectAttempts,
    });

    this.setupEventHandlers();
  }

  setupEventHandlers() {
    // Connection events
    this.socket.on('connect', () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      toast.success('Connected to server');
    });

    this.socket.on('disconnect', (reason) => {
      console.log('WebSocket disconnected:', reason);
      if (reason === 'io server disconnect') {
        // Server disconnected the socket, need to reconnect manually
        this.socket.connect();
      }
    });

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error);
      this.reconnectAttempts++;

      if (this.reconnectAttempts >= this.maxReconnectAttempts) {
        toast.error('Failed to connect to server. Please refresh the page.');
      }
    });

    this.socket.on('reconnect', (attemptNumber) => {
      console.log('WebSocket reconnected after', attemptNumber, 'attempts');
      toast.info('Reconnected to server');
    });

    this.socket.on('reconnect_failed', () => {
      console.error('WebSocket reconnection failed');
      toast.error('Connection lost. Please refresh the page.');
    });

    // Error handling
    this.socket.on('error', (error) => {
      console.error('WebSocket error:', error);
      toast.error(error.message || 'WebSocket error occurred');
    });

    // Custom event handlers
    this.socket.on('notification', (data) => {
      this.handleNotification(data);
    });

    this.socket.on('analysis_progress', (data) => {
      this.emit('analysisProgress', data);
    });

    this.socket.on('collaboration_update', (data) => {
      this.emit('collaborationUpdate', data);
    });

    this.socket.on('resource_update', (data) => {
      this.emit('resourceUpdate', data);
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
      console.log('WebSocket disconnected');
    }
  }

  // Event emitter pattern for components to subscribe
  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event);
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => callback(data));
    }
  }

  // Send messages to server
  send(event, data) {
    if (this.socket?.connected) {
      this.socket.emit(event, data);
    } else {
      console.error('WebSocket not connected');
      toast.error('Not connected to server');
    }
  }

  // Specific message handlers
  handleNotification(data) {
    const { type, message, level } = data;

    switch (level) {
      case 'success':
        toast.success(message);
        break;
      case 'error':
        toast.error(message);
        break;
      case 'warning':
        toast(message);
        break;
      case 'info':
      default:
        toast.info(message);
        break;
    }

    this.emit('notification', data);
  }

  // Collaboration methods
  joinCollaborationSession(sessionId) {
    this.send('join_session', { session_id: sessionId });
  }

  leaveCollaborationSession(sessionId) {
    this.send('leave_session', { session_id: sessionId });
  }

  sendAnnotation(sessionId, annotation) {
    this.send('share_annotation', {
      session_id: sessionId,
      annotation
    });
  }

  sendCursorPosition(sessionId, position) {
    this.send('cursor_position', {
      session_id: sessionId,
      position,
    });
  }

  // Analysis methods
  subscribeToAnalysis(analysisId) {
    this.send('subscribe_analysis', { analysis_id: analysisId });
  }

  unsubscribeFromAnalysis(analysisId) {
    this.send('unsubscribe_analysis', { analysis_id: analysisId });
  }

  // Resource monitoring
  subscribeToResourceUpdates() {
    this.send('subscribe_resources', {});
  }

  unsubscribeFromResourceUpdates() {
    this.send('unsubscribe_resources', {});
  }
}

// Singleton instance
const wsService = new WebSocketService();

export default wsService;
