import { useState, useEffect } from 'react';
import { Activity, AlertTriangle, TrendingUp, Heart, Wind, Thermometer } from 'lucide-react';
import Plot from 'react-plotly.js';
import { analysisAPI } from '../services/api';
import { toast } from 'react-hot-toast';
import { useMbti } from '../hooks/useMbti';

/**
 * HealthDetails Component
 * Displays AI-powered health risk predictions based on climate, genomic, and clinical data
 * Uses react-plotly.js for data visualization, lucide-react for icons
 * MBTI-tailored presentations for different personality types
 */
function HealthDetails({ analysisId, data }) {
  const [healthData, setHealthData] = useState(data || null);
  const [loading, setLoading] = useState(!data);
  const { mbtiType, theme } = useMbti();

  useEffect(() => {
    if (analysisId && !data) {
      loadHealthAnalysis();
    }
  }, [analysisId]);

  /**
   * Load health analysis data from API
   */
  const loadHealthAnalysis = async () => {
    setLoading(true);
    try {
      const response = await analysisAPI.getAnalysis(analysisId);
      setHealthData(response.data);
    } catch (error) {
      console.error('Failed to load health analysis:', error);
      toast.error('Failed to load health analysis');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Get risk level color
   */
  const getRiskColor = (level) => {
    const colors = {
      low: '#10b981', // green
      moderate: '#fbbf24', // yellow
      high: '#f97316', // orange
      critical: '#ef4444', // red
    };
    return colors[level?.toLowerCase()] || colors.moderate;
  };

  /**
   * Get risk level badge
   */
  const getRiskBadge = (level) => {
    const badges = {
      low: 'bg-green-100 text-green-800',
      moderate: 'bg-yellow-100 text-yellow-800',
      high: 'bg-orange-100 text-orange-800',
      critical: 'bg-red-100 text-red-800',
    };
    return badges[level?.toLowerCase()] || badges.moderate;
  };

  /**
   * Get MBTI-tailored recommendation format
   */
  const getMbtiRecommendation = (recommendation) => {
    // Analytical types (INTJ, INTP, ENTJ, ENTP) get detailed, strategic advice
    const analyticalTypes = ['INTJ', 'INTP', 'ENTJ', 'ENTP'];
    // Feeling types (INFJ, INFP, ENFJ, ENFP) get empathetic, value-driven advice
    const feelingTypes = ['INFJ', 'INFP', 'ENFJ', 'ENFP'];
    // Action-oriented types (ESTP, ESFP, ISTP, ISFP) get quick, practical tips
    const actionTypes = ['ESTP', 'ESFP', 'ISTP', 'ISFP'];

    if (analyticalTypes.includes(mbtiType)) {
      return {
        ...recommendation,
        presentation: 'detailed',
        icon: <TrendingUp className="h-5 w-5" />,
      };
    } else if (feelingTypes.includes(mbtiType)) {
      return {
        ...recommendation,
        presentation: 'empathetic',
        icon: <Heart className="h-5 w-5" />,
      };
    } else if (actionTypes.includes(mbtiType)) {
      return {
        ...recommendation,
        presentation: 'actionable',
        icon: <Activity className="h-5 w-5" />,
      };
    } else {
      return {
        ...recommendation,
        presentation: 'structured',
        icon: <AlertTriangle className="h-5 w-5" />,
      };
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-8">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Analyzing health data...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!healthData) {
    return (
      <div className="bg-white rounded-lg shadow-md p-8">
        <div className="text-center text-gray-600">
          <AlertTriangle className="h-12 w-12 mx-auto mb-4 text-gray-400" />
          <p>No health analysis data available</p>
        </div>
      </div>
    );
  }

  // Calculate overall risk score
  const overallRisk = healthData.overallRisk || 'moderate';
  const riskScore = healthData.riskScore || 0;

  return (
    <div className="space-y-6">
      {/* Overall Risk Summary */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl font-bold text-gray-900">Health Risk Analysis</h2>
          <span className={`px-4 py-2 rounded-full font-semibold ${getRiskBadge(overallRisk)}`}>
            {overallRisk.toUpperCase()} RISK
          </span>
        </div>

        <div className="grid md:grid-cols-3 gap-6 mb-6">
          {/* Risk Score */}
          <div className="text-center">
            <div
              className="text-5xl font-bold mb-2"
              style={{ color: getRiskColor(overallRisk) }}
            >
              {riskScore.toFixed(1)}
            </div>
            <p className="text-gray-600">Risk Score (0-100)</p>
          </div>

          {/* Analyzed Factors */}
          <div className="text-center">
            <div className="text-5xl font-bold text-indigo-600 mb-2">
              {healthData.analyzedFactors || 0}
            </div>
            <p className="text-gray-600">Factors Analyzed</p>
          </div>

          {/* Confidence */}
          <div className="text-center">
            <div className="text-5xl font-bold text-green-600 mb-2">
              {healthData.confidence || 0}%
            </div>
            <p className="text-gray-600">Prediction Confidence</p>
          </div>
        </div>

        {/* Risk Breakdown */}
        {healthData.riskFactors && healthData.riskFactors.length > 0 && (
          <div className="mt-6">
            <h3 className="font-semibold text-gray-900 mb-3">Risk Factors</h3>
            <div className="space-y-3">
              {healthData.riskFactors.map((factor, index) => (
                <div key={index} className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    {factor.category === 'climate' && <Wind className="h-5 w-5 text-blue-600" />}
                    {factor.category === 'genomic' && <Activity className="h-5 w-5 text-purple-600" />}
                    {factor.category === 'environmental' && <Thermometer className="h-5 w-5 text-orange-600" />}
                    <span className="text-gray-700">{factor.name}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <div className="w-32 bg-gray-200 rounded-full h-2">
                      <div
                        className="h-2 rounded-full transition-all"
                        style={{
                          width: `${factor.impact}%`,
                          backgroundColor: getRiskColor(factor.level),
                        }}
                      ></div>
                    </div>
                    <span className="text-sm text-gray-600 w-12 text-right">
                      {factor.impact}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Risk Trends Chart */}
      {healthData.trends && (
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-xl font-bold text-gray-900 mb-4">Risk Trends</h3>
          <Plot
            data={[
              {
                x: healthData.trends.dates || [],
                y: healthData.trends.values || [],
                type: 'scatter',
                mode: 'lines+markers',
                marker: { color: theme.primary },
                line: { color: theme.primary, width: 2 },
                name: 'Risk Score',
              },
            ]}
            layout={{
              autosize: true,
              margin: { l: 50, r: 20, t: 20, b: 50 },
              xaxis: { title: 'Date' },
              yaxis: { title: 'Risk Score', range: [0, 100] },
              hovermode: 'closest',
            }}
            config={{ responsive: true, displayModeBar: false }}
            style={{ width: '100%', height: '300px' }}
          />
        </div>
      )}

      {/* Recommendations */}
      {healthData.recommendations && healthData.recommendations.length > 0 && (
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-xl font-bold text-gray-900 mb-4">
            Personalized Recommendations
          </h3>
          <p className="text-sm text-gray-600 mb-4">
            Tailored for {mbtiType} personality type
          </p>

          <div className="space-y-4">
            {healthData.recommendations.map((rec, index) => {
              const mbtiRec = getMbtiRecommendation(rec);
              return (
                <div
                  key={index}
                  className="border-l-4 pl-4 py-2"
                  style={{ borderColor: theme.primary }}
                >
                  <div className="flex items-start space-x-3">
                    <div className="mt-1" style={{ color: theme.primary }}>
                      {mbtiRec.icon}
                    </div>
                    <div className="flex-1">
                      <h4 className="font-semibold text-gray-900 mb-1">
                        {rec.title}
                      </h4>
                      <p className="text-gray-700 mb-2">{rec.description}</p>
                      {rec.actions && rec.actions.length > 0 && (
                        <ul className="text-sm text-gray-600 space-y-1">
                          {rec.actions.map((action, idx) => (
                            <li key={idx} className="flex items-start">
                              <span className="mr-2">"</span>
                              <span>{action}</span>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Disclaimer */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <AlertTriangle className="h-5 w-5 text-yellow-600 mt-0.5" />
          <div className="text-sm text-yellow-800">
            <p className="font-semibold mb-1">Medical Disclaimer</p>
            <p>
              This analysis is for informational purposes only and should not replace
              professional medical advice. Please consult with a healthcare provider
              for medical decisions.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default HealthDetails;
