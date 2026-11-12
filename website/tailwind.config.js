/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: '#0B0F19',
        surface: '#121826',
        'surface-card': '#182032',
        'surface-hover': '#1F293D',
        border: 'rgba(255, 255, 255, 0.08)',
        health: {
          primary: '#10B981',
          teal: '#14B8A6',
          glow: 'rgba(16, 185, 129, 0.25)',
        },
        planora: {
          primary: '#6366F1',
          violet: '#8B5CF6',
          glow: 'rgba(99, 102, 241, 0.25)',
        },
        gold: {
          500: '#F59E0B',
          glow: 'rgba(245, 158, 11, 0.25)',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      },
      boxShadow: {
        'glow-health': '0 0 25px rgba(16, 185, 129, 0.25)',
        'glow-planora': '0 0 25px rgba(99, 102, 241, 0.25)',
        'glow-gold': '0 0 25px rgba(245, 158, 11, 0.25)',
        'glass': '0 8px 32px 0 rgba(0, 0, 0, 0.37)',
      }
    },
  },
  plugins: [],
}
