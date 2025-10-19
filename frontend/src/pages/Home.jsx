import { Link } from 'react-router-dom';
import { useAuth } from '@hooks/useAuth';
import { useMbti } from '@hooks/useMbti';
import { Globe, Activity, Users, TrendingUp } from 'lucide-react';

function Home() {
  const { isAuthenticated } = useAuth();
  const { theme } = useMbti();

  const features = [
    {
      icon: <Globe className="w-12 h-12" />,
      title: '3D Climate Visualization',
      description: 'Interactive 3D visualizations of climate-health risk correlations using advanced Three.js rendering.',
    },
    {
      icon: <Activity className="w-12 h-12" />,
      title: 'Health Risk Analysis',
      description: 'AI-powered analysis of genomic, clinical, and environmental data to predict health risks.',
    },
    {
      icon: <Users className="w-12 h-12" />,
      title: 'Real-time Collaboration',
      description: 'Work together with teams in real-time, share annotations, and collaborate on insights.',
    },
    {
      icon: <TrendingUp className="w-12 h-12" />,
      title: 'Predictive Analytics',
      description: 'Machine learning models that predict climate-health outcomes with high accuracy.',
    },
  ];

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <div
        className="relative py-20 px-6"
        style={{
          background: `linear-gradient(135deg, ${theme.primary} 0%, ${theme.secondary} 100%)`,
        }}
      >
        <div className="max-w-6xl mx-auto text-center text-white">
          <h1 className="text-5xl md:text-6xl font-bold mb-6 animate-fade-in">
            ClimateHealthMapper
          </h1>
          <p className="text-xl md:text-2xl mb-8 opacity-90">
            Interactive 3D Climate-Health Risk Visualization Platform
          </p>
          <p className="text-lg mb-12 max-w-3xl mx-auto opacity-80">
            Analyze climate impacts on health outcomes using genomic data, clinical records,
            and environmental sensors. Powered by AI and advanced 3D visualization.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            {isAuthenticated ? (
              <Link
                to="/analyze"
                className="px-8 py-4 bg-white text-gray-900 rounded-lg hover:bg-gray-100 transition-all transform hover:scale-105 font-semibold text-lg shadow-lg"
              >
                Start Analyzing
              </Link>
            ) : (
              <>
                <Link
                  to="/register"
                  className="px-8 py-4 bg-white text-gray-900 rounded-lg hover:bg-gray-100 transition-all transform hover:scale-105 font-semibold text-lg shadow-lg"
                >
                  Get Started
                </Link>
                <Link
                  to="/login"
                  className="px-8 py-4 bg-transparent border-2 border-white text-white rounded-lg hover:bg-white hover:text-gray-900 transition-all transform hover:scale-105 font-semibold text-lg"
                >
                  Sign In
                </Link>
              </>
            )}
          </div>
        </div>
      </div>

      {/* Features Section */}
      <div className="py-20 px-6 bg-white">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-4xl font-bold text-center mb-4" style={{ color: theme.primary }}>
            Powerful Features
          </h2>
          <p className="text-center text-gray-600 mb-16 max-w-2xl mx-auto">
            Everything you need to analyze and visualize climate-health correlations
          </p>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="text-center p-6 rounded-lg hover:shadow-xl transition-shadow duration-300 animate-slide-up"
                style={{ animationDelay: `${index * 100}ms` }}
              >
                <div
                  className="inline-flex items-center justify-center w-20 h-20 rounded-full mb-4"
                  style={{ backgroundColor: theme.background, color: theme.primary }}
                >
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold mb-3" style={{ color: theme.primary }}>
                  {feature.title}
                </h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Data Sources Section */}
      <div className="py-20 px-6" style={{ backgroundColor: theme.background }}>
        <div className="max-w-6xl mx-auto">
          <h2 className="text-4xl font-bold text-center mb-4" style={{ color: theme.primary }}>
            Supported Data Sources
          </h2>
          <p className="text-center text-gray-600 mb-16 max-w-2xl mx-auto">
            Import and analyze data from multiple healthcare and environmental sources
          </p>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="bg-white p-8 rounded-lg shadow-md">
              <h3 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Clinical Data
              </h3>
              <ul className="space-y-2 text-gray-700">
                <li>FHIR (HL7 Fast Healthcare Interoperability Resources)</li>
                <li>Electronic Health Records (EHR)</li>
                <li>Clinical trial data</li>
                <li>Patient demographics</li>
              </ul>
            </div>

            <div className="bg-white p-8 rounded-lg shadow-md">
              <h3 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Genomic Data
              </h3>
              <ul className="space-y-2 text-gray-700">
                <li>VCF (Variant Call Format)</li>
                <li>DNA sequencing data</li>
                <li>SNP analysis</li>
                <li>Gene expression data</li>
              </ul>
            </div>

            <div className="bg-white p-8 rounded-lg shadow-md">
              <h3 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Environmental Data
              </h3>
              <ul className="space-y-2 text-gray-700">
                <li>Climate sensor data (CSV/JSON)</li>
                <li>Air quality measurements</li>
                <li>Temperature and humidity</li>
                <li>Geospatial data</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* CTA Section */}
      {!isAuthenticated && (
        <div
          className="py-20 px-6 text-center text-white"
          style={{
            background: `linear-gradient(135deg, ${theme.secondary} 0%, ${theme.accent} 100%)`,
          }}
        >
          <div className="max-w-4xl mx-auto">
            <h2 className="text-4xl font-bold mb-6">Ready to Get Started?</h2>
            <p className="text-xl mb-8 opacity-90">
              Join researchers and healthcare professionals using ClimateHealthMapper
            </p>
            <Link
              to="/register"
              className="inline-block px-8 py-4 bg-white text-gray-900 rounded-lg hover:bg-gray-100 transition-all transform hover:scale-105 font-semibold text-lg shadow-lg"
            >
              Create Free Account
            </Link>
          </div>
        </div>
      )}

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-8 px-6">
        <div className="max-w-6xl mx-auto text-center">
          <p className="text-gray-400">
            &copy; 2025 ClimateHealthMapper. All rights reserved.
          </p>
          <div className="mt-4 space-x-6">
            <a href="#" className="text-gray-400 hover:text-white transition">
              Privacy Policy
            </a>
            <a href="#" className="text-gray-400 hover:text-white transition">
              Terms of Service
            </a>
            <a href="#" className="text-gray-400 hover:text-white transition">
              Contact
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default Home;
