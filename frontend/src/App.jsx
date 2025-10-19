import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { useAuth } from './hooks/useAuth';
import { useMbti } from './hooks/useMbti';

// Pages
import Home from './pages/Home';
import Analyze from './pages/Analyze';
import Explore from './pages/Explore';
import Troubleshoot from './pages/Troubleshoot';
import Collaborate from './pages/Collaborate';

// Components
import Navbar from './components/Navbar';
import AuthForm from './components/AuthForm';
import ErrorBoundary from './components/ErrorBoundary';

// Protected Route Component
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="spinner"></div>
      </div>
    );
  }

  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

function AppContent() {
  const { isAuthenticated } = useAuth();
  const { theme } = useMbti();

  return (
    <ErrorBoundary>
      <div className="min-h-screen" style={{ backgroundColor: theme.background }}>
        {isAuthenticated && <Navbar />}

        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<AuthForm />} />
          <Route path="/register" element={<AuthForm isRegister />} />

          {/* Protected Routes */}
          <Route
            path="/analyze"
            element={
              <ProtectedRoute>
                <Analyze />
              </ProtectedRoute>
            }
          />
          <Route
            path="/explore"
            element={
              <ProtectedRoute>
                <Explore />
              </ProtectedRoute>
            }
          />
          <Route
            path="/troubleshoot"
            element={
              <ProtectedRoute>
                <Troubleshoot />
              </ProtectedRoute>
            }
          />
          <Route
            path="/collaborate"
            element={
              <ProtectedRoute>
                <Collaborate />
              </ProtectedRoute>
            }
          />

          {/* 404 Not Found */}
          <Route
            path="*"
            element={
              <div className="min-h-screen flex items-center justify-center">
                <div className="text-center">
                  <h1 className="text-6xl font-bold text-gray-800 mb-4">404</h1>
                  <p className="text-xl text-gray-600 mb-8">Page not found</p>
                  <a href="/" className="btn-primary">
                    Go Home
                  </a>
                </div>
              </div>
            }
          />
        </Routes>

        {/* Toast Notifications */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 5000,
            style: {
              background: '#363636',
              color: '#fff',
            },
            success: {
              duration: 3000,
              iconTheme: {
                primary: '#4ade80',
                secondary: '#fff',
              },
            },
            error: {
              duration: 4000,
              iconTheme: {
                primary: '#ef4444',
                secondary: '#fff',
              },
            },
          }}
        />
      </div>
    </ErrorBoundary>
  );
}

function App() {
  return <AppContent />;
}

export default App;
