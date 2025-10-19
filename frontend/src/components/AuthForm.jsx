import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { authAPI } from '../services/api';
import { Mail, Lock, User, Building, Shield, KeyRound } from 'lucide-react';
import QRCode from 'qrcode';
import DOMPurify from 'dompurify';

const AuthForm = ({ isRegister = false }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  // Form state
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    organization: '',
    role: 'user', // user, moderator, admin, enterprise
    mbtiType: '',
  });

  // UI state
  const [loading, setLoading] = useState(false);
  const [showMFA, setShowMFA] = useState(false);
  const [mfaCode, setMfaCode] = useState('');
  const [mfaSetup, setMfaSetup] = useState(null);
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  const [authMethod, setAuthMethod] = useState('email'); // email, google, azure, okta, saml

  const mbtiTypes = [
    'ENTJ', 'INTJ', 'ENTP', 'INTP',
    'ENFJ', 'INFJ', 'ENFP', 'INFP',
    'ESTJ', 'ISTJ', 'ESTP', 'ISTP',
    'ESFJ', 'ISFJ', 'ESFP', 'ISFP'
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // Email/Password Authentication
  const handleEmailAuth = async (e) => {
    e.preventDefault();

    if (isRegister) {
      // Validation
      if (formData.password !== formData.confirmPassword) {
        toast.error('Passwords do not match');
        return;
      }
      if (formData.password.length < 8) {
        toast.error('Password must be at least 8 characters');
        return;
      }
      if (!formData.fullName.trim()) {
        toast.error('Full name is required');
        return;
      }
    }

    setLoading(true);
    try {
      const sanitizedData = {
        email: DOMPurify.sanitize(formData.email),
        password: formData.password,
        ...(isRegister && {
          fullName: DOMPurify.sanitize(formData.fullName),
          organization: DOMPurify.sanitize(formData.organization),
          role: formData.role,
          mbtiType: formData.mbtiType,
        })
      };

      const response = isRegister
        ? await authAPI.register(sanitizedData)
        : await authAPI.login(sanitizedData);

      if (response.data.requiresMFA) {
        setShowMFA(true);
        toast.info('Please enter your MFA code');
      } else if (response.data.requiresMFASetup) {
        // New user needs to set up MFA
        await setupMFA();
      } else {
        handleAuthSuccess(response.data);
      }
    } catch (error) {
      console.error('Authentication error:', error);
      toast.error(error.response?.data?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  // MFA Setup
  const setupMFA = async () => {
    try {
      const response = await authAPI.setupMFA();
      setMfaSetup(response.data);

      // Generate QR code
      const qrUrl = await QRCode.toDataURL(response.data.otpAuthUrl);
      setQrCodeUrl(qrUrl);
      setShowMFA(true);

      toast.success('Scan QR code with your authenticator app');
    } catch (error) {
      console.error('MFA setup error:', error);
      toast.error('Failed to setup MFA');
    }
  };

  // Verify MFA
  const handleMFAVerify = async (e) => {
    e.preventDefault();

    if (mfaCode.length !== 6) {
      toast.error('MFA code must be 6 digits');
      return;
    }

    setLoading(true);
    try {
      const response = await authAPI.verifyMFA(mfaCode);
      handleAuthSuccess(response.data);
    } catch (error) {
      console.error('MFA verification error:', error);
      toast.error('Invalid MFA code');
      setMfaCode('');
    } finally {
      setLoading(false);
    }
  };

  // SSO Authentication Handlers
  const handleGoogleSSO = async () => {
    setLoading(true);
    try {
      // In production, this would use Google OAuth2
      const googleAuthUrl = `${import.meta.env.VITE_API_URL}/api/auth/oauth/google`;
      window.location.href = googleAuthUrl;
    } catch (error) {
      console.error('Google SSO error:', error);
      toast.error('Failed to initiate Google SSO');
      setLoading(false);
    }
  };

  const handleAzureSSO = async () => {
    setLoading(true);
    try {
      // In production, this would use Azure AD OAuth2
      const azureAuthUrl = `${import.meta.env.VITE_API_URL}/api/auth/oauth/azure`;
      window.location.href = azureAuthUrl;
    } catch (error) {
      console.error('Azure SSO error:', error);
      toast.error('Failed to initiate Azure AD SSO');
      setLoading(false);
    }
  };

  const handleOktaSSO = async () => {
    setLoading(true);
    try {
      // In production, this would use Okta OAuth2
      const oktaAuthUrl = `${import.meta.env.VITE_API_URL}/api/auth/oauth/okta`;
      window.location.href = oktaAuthUrl;
    } catch (error) {
      console.error('Okta SSO error:', error);
      toast.error('Failed to initiate Okta SSO');
      setLoading(false);
    }
  };

  const handleSAMLAuth = async () => {
    setLoading(true);
    try {
      // In production, this would redirect to SAML IdP
      const samlAuthUrl = `${import.meta.env.VITE_API_URL}/api/auth/saml`;
      window.location.href = samlAuthUrl;
    } catch (error) {
      console.error('SAML SSO error:', error);
      toast.error('Failed to initiate SAML SSO');
      setLoading(false);
    }
  };

  // Success handler
  const handleAuthSuccess = (data) => {
    localStorage.setItem('authToken', data.token);
    localStorage.setItem('userRole', data.user.role);
    localStorage.setItem('mbtiType', data.user.mbtiType);

    toast.success(isRegister ? 'Registration successful!' : 'Login successful!');
    navigate(from, { replace: true });
  };

  // MFA Form
  if (showMFA) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <Shield className="mx-auto h-12 w-12 text-indigo-600 mb-4" />
            <h2 className="text-3xl font-bold text-gray-900">Two-Factor Authentication</h2>
            <p className="text-gray-600 mt-2">
              {mfaSetup ? 'Scan the QR code and enter the code' : 'Enter your 6-digit code'}
            </p>
          </div>

          {qrCodeUrl && (
            <div className="mb-6">
              <div className="flex justify-center mb-4">
                <img src={qrCodeUrl} alt="MFA QR Code" className="w-48 h-48" />
              </div>
              <div className="bg-gray-50 rounded-lg p-4">
                <p className="text-sm font-medium text-gray-700 mb-2">Manual Entry:</p>
                <p className="text-xs font-mono text-gray-600 break-all">
                  {mfaSetup?.secret}
                </p>
              </div>
            </div>
          )}

          <form onSubmit={handleMFAVerify} className="space-y-6">
            <div>
              <label htmlFor="mfaCode" className="block text-sm font-medium text-gray-700 mb-2">
                6-Digit Code
              </label>
              <input
                type="text"
                id="mfaCode"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-center text-2xl tracking-widest"
                placeholder="000000"
                maxLength="6"
                required
                autoFocus
              />
            </div>

            <button
              type="submit"
              disabled={loading || mfaCode.length !== 6}
              className="w-full bg-indigo-600 text-white py-3 px-4 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
            >
              {loading ? 'Verifying...' : 'Verify Code'}
            </button>

            <button
              type="button"
              onClick={() => {
                setShowMFA(false);
                setMfaCode('');
                setQrCodeUrl('');
                setMfaSetup(null);
              }}
              className="w-full text-gray-600 hover:text-gray-800 text-sm"
            >
              Back to login
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Main Auth Form
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 py-12">
      <div className="max-w-6xl w-full grid md:grid-cols-2 gap-8">
        {/* Left Panel - Info */}
        <div className="hidden md:flex flex-col justify-center text-white bg-gradient-to-br from-indigo-600 to-purple-700 rounded-2xl p-12">
          <h1 className="text-4xl font-bold mb-6">ClimateHealthMapper</h1>
          <p className="text-lg mb-8 opacity-90">
            AI-powered climate and health analysis platform. Integrating environmental,
            health, and genomic data for personalized risk assessments.
          </p>
          <div className="space-y-4">
            <div className="flex items-start">
              <div className="bg-white/20 rounded-lg p-3 mr-4">
                <Shield className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold mb-1">Enterprise-Grade Security</h3>
                <p className="text-sm opacity-80">SSO, MFA, and role-based access control</p>
              </div>
            </div>
            <div className="flex items-start">
              <div className="bg-white/20 rounded-lg p-3 mr-4">
                <Building className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-semibold mb-1">Multi-Tenant Support</h3>
                <p className="text-sm opacity-80">Individual, organizational, and enterprise plans</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Panel - Form */}
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900">
              {isRegister ? 'Create Account' : 'Welcome Back'}
            </h2>
            <p className="text-gray-600 mt-2">
              {isRegister ? 'Join ClimateHealthMapper today' : 'Sign in to your account'}
            </p>
          </div>

          {/* SSO Buttons */}
          <div className="space-y-3 mb-6">
            <button
              onClick={handleGoogleSSO}
              disabled={loading}
              className="w-full flex items-center justify-center px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              <svg className="h-5 w-5 mr-2" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Continue with Google
            </button>

            <button
              onClick={handleAzureSSO}
              disabled={loading}
              className="w-full flex items-center justify-center px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              <svg className="h-5 w-5 mr-2" viewBox="0 0 24 24">
                <path fill="#00A4EF" d="M11.4 0H0v11.4h11.4V0z"/>
                <path fill="#FFB900" d="M24 0H12.6v11.4H24V0z"/>
                <path fill="#00A4EF" d="M11.4 12.6H0V24h11.4V12.6z"/>
                <path fill="#00A4EF" d="M24 12.6H12.6V24H24V12.6z"/>
              </svg>
              Continue with Microsoft
            </button>

            <button
              onClick={handleOktaSSO}
              disabled={loading}
              className="w-full flex items-center justify-center px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              <KeyRound className="h-5 w-5 mr-2 text-blue-600" />
              Continue with Okta
            </button>

            {formData.role === 'enterprise' && (
              <button
                onClick={handleSAMLAuth}
                disabled={loading}
                className="w-full flex items-center justify-center px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors"
              >
                <Building className="h-5 w-5 mr-2 text-purple-600" />
                Enterprise SAML SSO
              </button>
            )}
          </div>

          <div className="relative mb-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-white text-gray-500">Or continue with email</span>
            </div>
          </div>

          {/* Email/Password Form */}
          <form onSubmit={handleEmailAuth} className="space-y-4">
            {isRegister && (
              <>
                <div>
                  <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-1">
                    Full Name
                  </label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                      type="text"
                      id="fullName"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleInputChange}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                      required
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="organization" className="block text-sm font-medium text-gray-700 mb-1">
                    Organization (Optional)
                  </label>
                  <div className="relative">
                    <Building className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                      type="text"
                      id="organization"
                      name="organization"
                      value={formData.organization}
                      onChange={handleInputChange}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="role" className="block text-sm font-medium text-gray-700 mb-1">
                    Account Type
                  </label>
                  <select
                    id="role"
                    name="role"
                    value={formData.role}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                    required
                  >
                    <option value="user">Individual User</option>
                    <option value="moderator">Healthcare Provider</option>
                    <option value="admin">Administrator</option>
                    <option value="enterprise">Enterprise</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="mbtiType" className="block text-sm font-medium text-gray-700 mb-1">
                    MBTI Type (Optional)
                  </label>
                  <select
                    id="mbtiType"
                    name="mbtiType"
                    value={formData.mbtiType}
                    onChange={handleInputChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  >
                    <option value="">Select MBTI Type</option>
                    {mbtiTypes.map(type => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                  <p className="text-xs text-gray-500 mt-1">
                    We'll personalize your experience based on your personality type
                  </p>
                </div>
              </>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                <input
                  type="password"
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  minLength="8"
                  required
                />
              </div>
            </div>

            {isRegister && (
              <div>
                <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
                  Confirm Password
                </label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="password"
                    id="confirmPassword"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleInputChange}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                    minLength="8"
                    required
                  />
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-indigo-600 text-white py-3 px-4 rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
            >
              {loading ? 'Processing...' : (isRegister ? 'Create Account' : 'Sign In')}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
              <a
                href={isRegister ? '/login' : '/register'}
                className="text-indigo-600 hover:text-indigo-700 font-medium"
              >
                {isRegister ? 'Sign in' : 'Sign up'}
              </a>
            </p>
          </div>

          {isRegister && (
            <p className="text-xs text-gray-500 text-center mt-4">
              By creating an account, you agree to our Terms of Service and Privacy Policy.
              MFA will be required for all accounts.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default AuthForm;
