import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.jsx';
import { useMbti } from '../hooks/useMbti.jsx';
import { Menu, X, User, LogOut, Settings, Shield, Users } from 'lucide-react';
import { useState } from 'react';
import { mbtiThemes } from '../styles/mbti-themes';

/**
 * Navbar Component
 * Navigation bar with role-based access control
 * Shows different menu items based on user role (user, moderator, admin, enterprise)
 * Uses lucide-react for icons
 */
function Navbar() {
  const { user, logout, isAuthenticated, updateMBTI, role, isModerator, isAdmin, isEnterprise } = useAuth();
  const { mbtiType, changeMbtiType } = useMbti();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [showMbtiSelector, setShowMbtiSelector] = useState(false);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const handleMbtiChange = async (newType) => {
    changeMbtiType(newType);
    if (user) {
      await updateMBTI(newType);
    }
    setShowMbtiSelector(false);
  };

  if (!isAuthenticated) {
    return null;
  }

  // Define navigation links with role-based access
  const navLinks = [
    { to: '/analyze', label: 'Analyze', roles: ['user', 'moderator', 'admin', 'enterprise'] },
    { to: '/explore', label: 'Explore', roles: ['user', 'moderator', 'admin', 'enterprise'] },
    { to: '/collaborate', label: 'Collaborate', roles: ['user', 'moderator', 'admin', 'enterprise'] },
    { to: '/troubleshoot', label: 'Troubleshoot', roles: ['user', 'moderator', 'admin', 'enterprise'] },
  ];

  // Admin-only links
  const adminLinks = [
    { to: '/admin/dashboard', label: 'Admin Dashboard', roles: ['admin'] },
    { to: '/admin/users', label: 'User Management', roles: ['admin', 'moderator'] },
  ];

  // Filter links based on user role
  const filteredNavLinks = navLinks.filter(link =>
    !link.roles || link.roles.includes(role)
  );

  const filteredAdminLinks = adminLinks.filter(link =>
    !link.roles || link.roles.includes(role)
  );

  const showAdminSection = isAdmin() || isModerator();

  return (
    <nav className="bg-white shadow-lg sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="flex items-center">
              <span className="text-xl font-bold text-indigo-600">
                ClimateHealthMapper
              </span>
            </Link>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-4">
            {filteredNavLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-indigo-600 hover:bg-gray-100 transition"
              >
                {link.label}
              </Link>
            ))}

            {/* Admin/Moderator Links */}
            {showAdminSection && filteredAdminLinks.length > 0 && (
              <div className="relative group">
                <button className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:text-indigo-600 hover:bg-gray-100 transition flex items-center">
                  <Shield className="h-4 w-4 mr-1" />
                  Admin
                </button>
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-10 hidden group-hover:block">
                  {filteredAdminLinks.map((link) => (
                    <Link
                      key={link.to}
                      to={link.to}
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      {link.label}
                    </Link>
                  ))}
                </div>
              </div>
            )}

            {/* MBTI Selector */}
            <div className="relative">
              <button
                onClick={() => setShowMbtiSelector(!showMbtiSelector)}
                className="px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100"
              >
                MBTI: {mbtiType}
              </button>
              {showMbtiSelector && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-10">
                  {Object.keys(mbtiThemes).filter(t => t !== 'DEFAULT').map((type) => (
                    <button
                      key={type}
                      onClick={() => handleMbtiChange(type)}
                      className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      {type}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* User Menu */}
            <div className="flex items-center space-x-2 border-l pl-4">
              <div className="flex items-center space-x-2">
                <div className="text-right">
                  <p className="text-sm font-medium text-gray-700">{user?.fullName || user?.email}</p>
                  <p className="text-xs text-gray-500 capitalize">{role}</p>
                </div>
                {isEnterprise() && (
                  <span className="px-2 py-1 text-xs font-semibold text-purple-700 bg-purple-100 rounded-full">
                    Enterprise
                  </span>
                )}
              </div>
              <button
                onClick={handleLogout}
                className="p-2 rounded-md text-gray-700 hover:bg-gray-100"
                title="Logout"
              >
                <LogOut className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Mobile menu button */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              className="p-2 rounded-md text-gray-700 hover:bg-gray-100"
            >
              {isMenuOpen ? (
                <X className="h-6 w-6" />
              ) : (
                <Menu className="h-6 w-6" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isMenuOpen && (
        <div className="md:hidden">
          <div className="px-2 pt-2 pb-3 space-y-1">
            {filteredNavLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:text-indigo-600 hover:bg-gray-100"
                onClick={() => setIsMenuOpen(false)}
              >
                {link.label}
              </Link>
            ))}

            {/* Admin Links - Mobile */}
            {showAdminSection && filteredAdminLinks.length > 0 && (
              <>
                <div className="border-t pt-2 mt-2">
                  <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase">
                    Admin Section
                  </div>
                  {filteredAdminLinks.map((link) => (
                    <Link
                      key={link.to}
                      to={link.to}
                      className="block px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:text-indigo-600 hover:bg-gray-100"
                      onClick={() => setIsMenuOpen(false)}
                    >
                      {link.label}
                    </Link>
                  ))}
                </div>
              </>
            )}

            <div className="border-t pt-2 mt-2">
              <div className="px-3 py-2">
                <span className="text-sm text-gray-500">MBTI: {mbtiType}</span>
              </div>
              <div className="px-3 py-2">
                <p className="text-sm font-medium text-gray-700">{user?.fullName || user?.email}</p>
                <p className="text-xs text-gray-500 capitalize">{role}</p>
              </div>
              <button
                onClick={handleLogout}
                className="w-full text-left px-3 py-2 rounded-md text-base font-medium text-gray-700 hover:bg-gray-100 flex items-center"
              >
                <LogOut className="h-5 w-5 mr-2" />
                Logout
              </button>
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}

export default Navbar;
