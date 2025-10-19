import { useState, useEffect, createContext, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { authAPI, userAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import wsService from '../services/websocket';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const token = localStorage.getItem('authToken');
      if (token) {
        const response = await userAPI.getProfile();
        setUser(response.data);
        setIsAuthenticated(true);
        wsService.connect();
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      localStorage.removeItem('authToken');
    } finally {
      setLoading(false);
    }
  };

  const login = async (credentials) => {
    try {
      const response = await authAPI.login(credentials);
      const { token, user: userData, requires_mfa } = response.data;

      if (requires_mfa) {
        return { requiresMFA: true, tempToken: token };
      }

      localStorage.setItem('authToken', token);
      setUser(userData);
      setIsAuthenticated(true);
      wsService.connect();
      toast.success('Login successful!');
      navigate('/analyze');
      return { success: true };
    } catch (error) {
      toast.error(error.response?.data?.message || 'Login failed');
      throw error;
    }
  };

  const verifyMFA = async (code, tempToken) => {
    try {
      const response = await authAPI.verifyMFA(code);
      const { token, user: userData } = response.data;

      localStorage.setItem('authToken', token);
      setUser(userData);
      setIsAuthenticated(true);
      wsService.connect();
      toast.success('MFA verification successful!');
      navigate('/analyze');
      return { success: true };
    } catch (error) {
      toast.error('Invalid MFA code');
      throw error;
    }
  };

  const register = async (userData) => {
    try {
      const response = await authAPI.register(userData);
      const { token, user: newUser } = response.data;

      localStorage.setItem('authToken', token);
      setUser(newUser);
      setIsAuthenticated(true);
      wsService.connect();
      toast.success('Registration successful!');
      navigate('/analyze');
      return { success: true };
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed');
      throw error;
    }
  };

  const logout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('authToken');
      setUser(null);
      setIsAuthenticated(false);
      wsService.disconnect();
      toast.info('Logged out successfully');
      navigate('/login');
    }
  };

  const oauthLogin = async (provider, token) => {
    try {
      let response;
      switch (provider) {
        case 'google':
          response = await authAPI.googleOAuth(token);
          break;
        case 'azure':
          response = await authAPI.azureOAuth(token);
          break;
        case 'okta':
          response = await authAPI.oktaOAuth(token);
          break;
        default:
          throw new Error('Unsupported OAuth provider');
      }

      const { token: authToken, user: userData } = response.data;
      localStorage.setItem('authToken', authToken);
      setUser(userData);
      setIsAuthenticated(true);
      wsService.connect();
      toast.success(`${provider} login successful!`);
      navigate('/analyze');
      return { success: true };
    } catch (error) {
      toast.error(`${provider} login failed`);
      throw error;
    }
  };

  const updateProfile = async (data) => {
    try {
      const response = await userAPI.updateProfile(data);
      setUser(response.data);
      toast.success('Profile updated successfully');
      return { success: true };
    } catch (error) {
      toast.error('Failed to update profile');
      throw error;
    }
  };

  const updateMBTI = async (mbtiType) => {
    try {
      const response = await userAPI.updateMBTI(mbtiType);
      setUser(response.data);
      toast.success('MBTI type updated successfully');
      return { success: true };
    } catch (error) {
      toast.error('Failed to update MBTI type');
      throw error;
    }
  };

  const samlLogin = async (assertion) => {
    try {
      const response = await authAPI.samlAuth(assertion);
      const { token: authToken, user: userData } = response.data;

      localStorage.setItem('authToken', authToken);
      setUser(userData);
      setIsAuthenticated(true);
      wsService.connect();
      toast.success('SAML login successful!');
      navigate('/analyze');
      return { success: true };
    } catch (error) {
      toast.error('SAML login failed');
      throw error;
    }
  };

  const setupMFA = async () => {
    try {
      const response = await authAPI.setupMFA();
      return response.data;
    } catch (error) {
      toast.error('Failed to setup MFA');
      throw error;
    }
  };

  const refreshToken = async () => {
    try {
      const response = await authAPI.refreshToken();
      const { token, user: userData } = response.data;

      localStorage.setItem('authToken', token);
      setUser(userData);
      return { success: true };
    } catch (error) {
      console.error('Token refresh failed:', error);
      logout();
      throw error;
    }
  };

  // Role-Based Access Control (RBAC) functions
  const hasRole = (requiredRole) => {
    if (!user?.role) return false;

    const roleHierarchy = {
      user: 0,
      moderator: 1,
      admin: 2,
      enterprise: 2,
    };

    return roleHierarchy[user.role] >= roleHierarchy[requiredRole];
  };

  const hasPermission = (requiredPermission) => {
    return user?.permissions?.includes(requiredPermission) || false;
  };

  const hasAnyPermission = (requiredPermissions) => {
    if (!user?.permissions) return false;
    return requiredPermissions.some((perm) => user.permissions.includes(perm));
  };

  const hasAllPermissions = (requiredPermissions) => {
    if (!user?.permissions) return false;
    return requiredPermissions.every((perm) => user.permissions.includes(perm));
  };

  const isEnterprise = () => {
    return user?.role === 'enterprise' || user?.role === 'admin';
  };

  const isModerator = () => {
    return hasRole('moderator');
  };

  const isAdmin = () => {
    return user?.role === 'admin';
  };

  const isUser = () => {
    return user?.role === 'user';
  };

  const value = {
    user,
    loading,
    isAuthenticated,
    role: user?.role,
    permissions: user?.permissions || [],

    // Auth methods
    login,
    logout,
    register,
    verifyMFA,
    setupMFA,
    refreshToken,

    // SSO methods
    oauthLogin,
    samlLogin,

    // Profile methods
    updateProfile,
    updateMBTI,

    // RBAC methods
    hasRole,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isEnterprise,
    isModerator,
    isAdmin,
    isUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
