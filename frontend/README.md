# ClimateHealthMapper Frontend

Production-ready React frontend for the ClimateHealthMapper application with SSO, MFA, and role-based access control.

## Features

- **Authentication & Authorization**
  - Email/password authentication
  - SSO (Google, Microsoft/Azure AD, Okta, SAML)
  - Multi-factor authentication (MFA) with QR code
  - Role-based access control (User, Moderator, Admin, Enterprise)
  - JWT token management

- **3D Climate Visualization**
  - Interactive 3D visualizations using Three.js
  - Real-time climate-health data rendering
  - Export capabilities (PNG, SVG, STL)

- **AI-Powered Features**
  - Health risk analysis and predictions
  - LLM-based natural language queries
  - MBTI-personalized recommendations

- **Collaboration**
  - Real-time collaboration sessions
  - WebSocket-based communication
  - Live annotations and sharing

## Tech Stack

- **React 18** - UI framework
- **Vite** - Build tool
- **Three.js** - 3D visualization (@react-three/fiber, @react-three/drei)
- **Tailwind CSS** - Styling
- **Plotly.js** - Data visualization
- **Socket.IO** - Real-time communication
- **Axios** - HTTP client
- **React Router** - Routing
- **Zustand** - State management
- **Vitest** - Testing framework

## Prerequisites

- Node.js >= 18.0.0
- npm >= 9.0.0

## Installation

```bash
# Install dependencies
npm install

# Create environment file
cp .env.example .env

# Update .env with your configuration
```

## Environment Variables

Create a `.env` file with the following variables:

```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

## Development

```bash
# Start development server
npm run dev

# Run tests
npm test

# Run tests with UI
npm run test:ui

# Lint code
npm run lint

# Fix linting issues
npm run lint:fix
```

## Build

```bash
# Production build
npm run build

# Preview production build
npm run preview
```

## Docker

```bash
# Build Docker image
docker build -t climatehealthmapper-frontend .

# Run container
docker run -p 80:80 climatehealthmapper-frontend
```

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── components/      # Reusable components
│   │   ├── AuthForm.jsx
│   │   ├── ClimateViewer.jsx
│   │   ├── DataUpload.jsx
│   │   ├── HealthDetails.jsx
│   │   ├── LLMChat.jsx
│   │   ├── Navbar.jsx
│   │   └── ...
│   ├── pages/           # Page components
│   │   ├── Home.jsx
│   │   ├── Analyze.jsx
│   │   ├── Explore.jsx
│   │   └── ...
│   ├── hooks/           # Custom hooks
│   │   ├── useAuth.js
│   │   └── useMbti.js
│   ├── services/        # API services
│   │   ├── api.js
│   │   └── websocket.js
│   ├── styles/          # Style configurations
│   ├── tests/           # Test files
│   └── App.jsx          # Main app component
├── Dockerfile           # Production Docker config
├── nginx.conf           # Nginx configuration
└── vite.config.js       # Vite configuration
```

## Key Components

### Authentication
- **AuthForm**: Login/register with SSO and MFA
- **useAuth**: Authentication hook with RBAC

### Data Visualization
- **ClimateViewer**: 3D climate data visualization
- **HealthDetails**: AI-powered health risk analysis
- **DataUpload**: Multi-format file upload

### Communication
- **LLMChat**: Natural language query interface
- **CollabPanel**: Real-time collaboration

### Utilities
- **ResourceMonitor**: System resource tracking
- **ExportTool**: Visualization export
- **AnnotationTool**: Interactive annotations

## Testing

The project uses Vitest for unit and integration testing.

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

## Security

- Input sanitization with DOMPurify
- HTTPS/TLS for all communications
- JWT token-based authentication
- Role-based access control (RBAC)
- MFA for all accounts
- Content Security Policy (CSP)
- XSS and CSRF protection

## MBTI Personalization

The application provides personalized experiences for all 16 MBTI personality types:
- **Analytical** (INTJ, INTP, ENTJ, ENTP): Detailed, strategic insights
- **Feeling** (INFJ, INFP, ENFJ, ENFP): Empathetic, value-driven recommendations
- **Action-Oriented** (ESTP, ESFP, ISTP, ISFP): Quick, practical tips
- **Others**: Structured, balanced presentations

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the GitHub repository.
