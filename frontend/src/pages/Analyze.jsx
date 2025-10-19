import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { useMbti } from '@hooks/useMbti';
import { analysisAPI } from '@services/api';
import DataUpload from '@components/DataUpload';
import HealthDetails from '@components/HealthDetails';
import LLMChat from '@components/LLMChat';
import { Loader2, FileText, TrendingUp } from 'lucide-react';

function Analyze() {
  const { theme } = useMbti();
  const [uploadedData, setUploadedData] = useState(null);
  const [analysisResult, setAnalysisResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState([]);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const response = await analysisAPI.getAnalysisHistory();
      setHistory(response.data);
    } catch (error) {
      console.error('Failed to load history:', error);
    }
  };

  const handleDataUpload = (data) => {
    setUploadedData(data);
    toast.success('Data uploaded successfully!');
  };

  const handleAnalyze = async () => {
    if (!uploadedData) {
      toast.error('Please upload data first');
      return;
    }

    setLoading(true);
    try {
      const response = await analysisAPI.analyzeHealthRisk({
        dataset_id: uploadedData.id,
      });

      setAnalysisResult(response.data);
      toast.success('Analysis completed successfully!');
      loadHistory(); // Reload history
    } catch (error) {
      console.error('Analysis failed:', error);
      toast.error('Analysis failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectHistory = async (historyItem) => {
    try {
      const response = await analysisAPI.getAnalysis(historyItem.id);
      setAnalysisResult(response.data);
    } catch (error) {
      console.error('Failed to load analysis:', error);
      toast.error('Failed to load analysis');
    }
  };

  return (
    <div className="min-h-screen py-8 px-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1
            className="text-4xl font-bold mb-2"
            style={{ color: theme.primary }}
          >
            Health Risk Analysis
          </h1>
          <p className="text-gray-600">
            Upload clinical, genomic, and environmental data to analyze climate-health risks
          </p>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Left Column - Data Upload & History */}
          <div className="lg:col-span-1 space-y-6">
            {/* Data Upload */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2
                className="text-2xl font-semibold mb-4 flex items-center gap-2"
                style={{ color: theme.primary }}
              >
                <FileText className="w-6 h-6" />
                Upload Data
              </h2>
              <DataUpload onUpload={handleDataUpload} />

              {uploadedData && (
                <div className="mt-4">
                  <button
                    onClick={handleAnalyze}
                    disabled={loading}
                    className="w-full py-3 rounded-lg text-white font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    style={{
                      backgroundColor: loading ? theme.secondary : theme.primary,
                    }}
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-5 h-5 animate-spin" />
                        Analyzing...
                      </>
                    ) : (
                      <>
                        <TrendingUp className="w-5 h-5" />
                        Run Analysis
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>

            {/* Analysis History */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2
                className="text-2xl font-semibold mb-4"
                style={{ color: theme.primary }}
              >
                Recent Analyses
              </h2>
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {history.length === 0 ? (
                  <p className="text-gray-500 text-sm">No analysis history yet</p>
                ) : (
                  history.map((item) => (
                    <button
                      key={item.id}
                      onClick={() => handleSelectHistory(item)}
                      className="w-full text-left p-3 rounded-lg hover:bg-gray-50 transition-colors border border-gray-200"
                    >
                      <div className="font-medium text-sm text-gray-800">
                        {item.name || 'Untitled Analysis'}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {new Date(item.created_at).toLocaleDateString()}
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Middle Column - Analysis Results */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-8">
              <h2
                className="text-2xl font-semibold mb-4"
                style={{ color: theme.primary }}
              >
                Analysis Results
              </h2>
              {loading ? (
                <div className="flex flex-col items-center justify-center py-12">
                  <Loader2 className="w-12 h-12 animate-spin mb-4" style={{ color: theme.primary }} />
                  <p className="text-gray-600">Analyzing your data...</p>
                  <p className="text-sm text-gray-500 mt-2">This may take a few moments</p>
                </div>
              ) : analysisResult ? (
                <HealthDetails data={analysisResult} />
              ) : (
                <div className="text-center py-12 text-gray-500">
                  <TrendingUp className="w-16 h-16 mx-auto mb-4 opacity-30" />
                  <p>Upload data and run analysis to see results</p>
                </div>
              )}
            </div>
          </div>

          {/* Right Column - LLM Chat */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-8">
              <h2
                className="text-2xl font-semibold mb-4"
                style={{ color: theme.primary }}
              >
                AI Assistant
              </h2>
              <LLMChat context={analysisResult} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Analyze;
