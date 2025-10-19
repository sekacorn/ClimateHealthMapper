import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { useMbti } from '@hooks/useMbti';
import { troubleshootAPI, monitoringAPI } from '@services/api';
import ResourceMonitor from '@components/ResourceMonitor';
import { AlertTriangle, CheckCircle, XCircle, Loader2, Play } from 'lucide-react';

function Troubleshoot() {
  const { theme } = useMbti();
  const [diagnosticsRunning, setDiagnosticsRunning] = useState(false);
  const [diagnosticsResult, setDiagnosticsResult] = useState(null);
  const [errorLogs, setErrorLogs] = useState([]);
  const [systemHealth, setSystemHealth] = useState(null);
  const [issueReport, setIssueReport] = useState({
    title: '',
    description: '',
    severity: 'medium',
  });

  useEffect(() => {
    loadErrorLogs();
    loadSystemHealth();
  }, []);

  const loadErrorLogs = async () => {
    try {
      const response = await troubleshootAPI.getErrorLogs({ limit: 50 });
      setErrorLogs(response.data);
    } catch (error) {
      console.error('Failed to load error logs:', error);
    }
  };

  const loadSystemHealth = async () => {
    try {
      const response = await monitoringAPI.getSystemHealth();
      setSystemHealth(response.data);
    } catch (error) {
      console.error('Failed to load system health:', error);
    }
  };

  const runDiagnostics = async () => {
    setDiagnosticsRunning(true);
    try {
      const response = await troubleshootAPI.runDiagnostics();
      setDiagnosticsResult(response.data);
      toast.success('Diagnostics completed successfully');
    } catch (error) {
      console.error('Diagnostics failed:', error);
      toast.error('Failed to run diagnostics');
    } finally {
      setDiagnosticsRunning(false);
    }
  };

  const handleReportIssue = async (e) => {
    e.preventDefault();
    try {
      await troubleshootAPI.reportIssue(issueReport);
      toast.success('Issue reported successfully');
      setIssueReport({ title: '', description: '', severity: 'medium' });
    } catch (error) {
      console.error('Failed to report issue:', error);
      toast.error('Failed to report issue');
    }
  };

  const getHealthStatusIcon = (status) => {
    switch (status) {
      case 'healthy':
        return <CheckCircle className="w-6 h-6 text-green-500" />;
      case 'warning':
        return <AlertTriangle className="w-6 h-6 text-yellow-500" />;
      case 'critical':
        return <XCircle className="w-6 h-6 text-red-500" />;
      default:
        return <AlertTriangle className="w-6 h-6 text-gray-500" />;
    }
  };

  return (
    <div className="min-h-screen py-8 px-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold mb-2" style={{ color: theme.primary }}>
            Troubleshooting & Diagnostics
          </h1>
          <p className="text-gray-600">
            Monitor system health, diagnose issues, and report problems
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* Left Column */}
          <div className="space-y-6">
            {/* System Health */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                System Health
              </h2>
              {systemHealth ? (
                <div className="space-y-4">
                  {Object.entries(systemHealth).map(([service, status]) => (
                    <div
                      key={service}
                      className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
                    >
                      <div className="flex items-center gap-3">
                        {getHealthStatusIcon(status.status)}
                        <div>
                          <p className="font-medium text-gray-800 capitalize">
                            {service.replace('_', ' ')}
                          </p>
                          <p className="text-sm text-gray-500">{status.message}</p>
                        </div>
                      </div>
                      <span
                        className={`px-3 py-1 rounded-full text-sm font-medium ${
                          status.status === 'healthy'
                            ? 'bg-green-100 text-green-800'
                            : status.status === 'warning'
                            ? 'bg-yellow-100 text-yellow-800'
                            : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {status.status}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <Loader2 className="w-8 h-8 animate-spin mx-auto mb-2" />
                  <p>Loading system health...</p>
                </div>
              )}
            </div>

            {/* Resource Monitor */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Resource Usage
              </h2>
              <ResourceMonitor />
            </div>
          </div>

          {/* Right Column */}
          <div className="space-y-6">
            {/* Diagnostics */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Run Diagnostics
              </h2>
              <button
                onClick={runDiagnostics}
                disabled={diagnosticsRunning}
                className="w-full py-3 rounded-lg text-white font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                style={{ backgroundColor: theme.primary }}
              >
                {diagnosticsRunning ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Running Diagnostics...
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5" />
                    Run Full Diagnostics
                  </>
                )}
              </button>

              {diagnosticsResult && (
                <div className="mt-4 space-y-3">
                  <h3 className="font-semibold text-gray-800">Results:</h3>
                  {diagnosticsResult.tests.map((test, index) => (
                    <div
                      key={index}
                      className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg"
                    >
                      {test.passed ? (
                        <CheckCircle className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
                      ) : (
                        <XCircle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
                      )}
                      <div>
                        <p className="font-medium text-gray-800">{test.name}</p>
                        <p className="text-sm text-gray-600">{test.message}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Report Issue */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Report an Issue
              </h2>
              <form onSubmit={handleReportIssue} className="space-y-4">
                <div>
                  <label className="label">Issue Title</label>
                  <input
                    type="text"
                    value={issueReport.title}
                    onChange={(e) =>
                      setIssueReport({ ...issueReport, title: e.target.value })
                    }
                    className="input-field"
                    required
                  />
                </div>
                <div>
                  <label className="label">Description</label>
                  <textarea
                    value={issueReport.description}
                    onChange={(e) =>
                      setIssueReport({ ...issueReport, description: e.target.value })
                    }
                    className="input-field"
                    rows="4"
                    required
                  />
                </div>
                <div>
                  <label className="label">Severity</label>
                  <select
                    value={issueReport.severity}
                    onChange={(e) =>
                      setIssueReport({ ...issueReport, severity: e.target.value })
                    }
                    className="input-field"
                  >
                    <option value="low">Low</option>
                    <option value="medium">Medium</option>
                    <option value="high">High</option>
                    <option value="critical">Critical</option>
                  </select>
                </div>
                <button
                  type="submit"
                  className="w-full py-3 rounded-lg text-white font-medium transition-colors"
                  style={{ backgroundColor: theme.primary }}
                >
                  Submit Report
                </button>
              </form>
            </div>

            {/* Error Logs */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-2xl font-semibold mb-4" style={{ color: theme.primary }}>
                Recent Errors
              </h2>
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {errorLogs.length === 0 ? (
                  <p className="text-gray-500 text-sm">No errors recorded</p>
                ) : (
                  errorLogs.map((log, index) => (
                    <div
                      key={index}
                      className="p-3 bg-red-50 border border-red-200 rounded-lg"
                    >
                      <div className="flex items-start gap-2">
                        <XCircle className="w-4 h-4 text-red-500 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <p className="font-medium text-sm text-red-800">{log.message}</p>
                          <p className="text-xs text-red-600 mt-1">
                            {new Date(log.timestamp).toLocaleString()}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Troubleshoot;
