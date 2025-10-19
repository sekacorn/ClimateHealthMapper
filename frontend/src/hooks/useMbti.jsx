import { useState, useEffect, createContext, useContext } from 'react';
import { getMBTITheme } from '@styles/mbti-themes';
import { useAuth } from './useAuth';

const MbtiContext = createContext(null);

export const MbtiProvider = ({ children }) => {
  const { user } = useAuth();
  const [mbtiType, setMbtiType] = useState('DEFAULT');
  const [theme, setTheme] = useState(getMBTITheme('DEFAULT'));

  useEffect(() => {
    if (user?.mbti_type) {
      setMbtiType(user.mbti_type);
      setTheme(getMBTITheme(user.mbti_type));
    } else {
      setMbtiType('DEFAULT');
      setTheme(getMBTITheme('DEFAULT'));
    }
  }, [user]);

  const changeMbtiType = (newType) => {
    setMbtiType(newType);
    setTheme(getMBTITheme(newType));
  };

  const getButtonClass = () => {
    return theme.buttonStyle;
  };

  const getCardClass = () => {
    return theme.cardStyle;
  };

  const getPrimaryColor = () => {
    return theme.primary;
  };

  const getSecondaryColor = () => {
    return theme.secondary;
  };

  const getAccentColor = () => {
    return theme.accent;
  };

  const getBackgroundColor = () => {
    return theme.background;
  };

  const getTextColor = () => {
    return theme.text;
  };

  const value = {
    mbtiType,
    theme,
    changeMbtiType,
    getButtonClass,
    getCardClass,
    getPrimaryColor,
    getSecondaryColor,
    getAccentColor,
    getBackgroundColor,
    getTextColor,
  };

  return <MbtiContext.Provider value={value}>{children}</MbtiContext.Provider>;
};

export const useMbti = () => {
  const context = useContext(MbtiContext);
  if (!context) {
    throw new Error('useMbti must be used within an MbtiProvider');
  }
  return context;
};
