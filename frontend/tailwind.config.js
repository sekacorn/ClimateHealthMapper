/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // MBTI Type Colors
        intj: { primary: '#4A148C', secondary: '#7B1FA2', accent: '#9C27B0' },
        intp: { primary: '#01579B', secondary: '#0277BD', accent: '#0288D1' },
        entj: { primary: '#B71C1C', secondary: '#C62828', accent: '#D32F2F' },
        entp: { primary: '#E65100', secondary: '#EF6C00', accent: '#F57C00' },
        infj: { primary: '#1A237E', secondary: '#283593', accent: '#3949AB' },
        infp: { primary: '#4A148C', secondary: '#6A1B9A', accent: '#7B1FA2' },
        enfj: { primary: '#004D40', secondary: '#00695C', accent: '#00796B' },
        enfp: { primary: '#F57F17', secondary: '#F9A825', accent: '#FBC02D' },
        istj: { primary: '#263238', secondary: '#37474F', accent: '#455A64' },
        isfj: { primary: '#3E2723', secondary: '#4E342E', accent: '#5D4037' },
        estj: { primary: '#1B5E20', secondary: '#2E7D32', accent: '#388E3C' },
        esfj: { primary: '#880E4F', secondary: '#AD1457', accent: '#C2185B' },
        istp: { primary: '#BF360C', secondary: '#D84315', accent: '#E64A19' },
        isfp: { primary: '#4A148C', secondary: '#6A1B9A', accent: '#8E24AA' },
        estp: { primary: '#FF6F00', secondary: '#FF8F00', accent: '#FFA000' },
        esfp: { primary: '#D81B60', secondary: '#EC407A', accent: '#F06292' },
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-in-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [],
  safelist: [
    {
      pattern: /(bg|text|border)-(intj|intp|entj|entp|infj|infp|enfj|enfp|istj|isfj|estj|esfj|istp|isfp|estp|esfp)-(primary|secondary|accent)/,
    },
  ],
};
