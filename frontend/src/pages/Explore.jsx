import { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { useMbti } from '@hooks/useMbti';
import { visualizationAPI, dataAPI } from '@services/api';
import ClimateViewer from '@components/ClimateViewer';
import AnnotationTool from '@components/AnnotationTool';
import ExportTool from '@components/ExportTool';
import { Loader2, Layers, Download } from 'lucide-react';

function Explore() {
  const { theme } = useMbti();
  const [datasets, setDatasets] = useState([]);
  const [selectedDataset, setSelectedDataset] = useState(null);
  const [visualizationData, setVisualizationData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [annotations, setAnnotations] = useState([]);
  const [showExport, setShowExport] = useState(false);

  useEffect(() => {
    loadDatasets();
  }, []);

  const loadDatasets = async () => {
    try {
      const response = await dataAPI.getDatasets();
      setDatasets(response.data);
    } catch (error) {
      console.error('Failed to load datasets:', error);
      toast.error('Failed to load datasets');
    }
  };

  const handleDatasetSelect = async (dataset) => {
    setSelectedDataset(dataset);
    setLoading(true);

    try {
      const response = await visualizationAPI.generate3DScene({
        dataset_id: dataset.id,
      });
      setVisualizationData(response.data);
      toast.success('Visualization loaded successfully!');
    } catch (error) {
      console.error('Failed to generate visualization:', error);
      toast.error('Failed to generate visualization');
    } finally {
      setLoading(false);
    }
  };

  const handleAnnotationAdd = (annotation) => {
    setAnnotations([...annotations, annotation]);
    toast.success('Annotation added');
  };

  const handleAnnotationDelete = (index) => {
    const newAnnotations = annotations.filter((_, i) => i !== index);
    setAnnotations(newAnnotations);
    toast.info('Annotation removed');
  };

  return (
    <div className="min-h-screen py-8 px-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 flex justify-between items-center">
          <div>
            <h1
              className="text-4xl font-bold mb-2"
              style={{ color: theme.primary }}
            >
              3D Climate-Health Visualization
            </h1>
            <p className="text-gray-600">
              Explore interactive 3D visualizations of climate-health correlations
            </p>
          </div>
          {visualizationData && (
            <button
              onClick={() => setShowExport(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-lg text-white font-medium transition-colors"
              style={{ backgroundColor: theme.primary }}
            >
              <Download className="w-5 h-5" />
              Export
            </button>
          )}
        </div>

        <div className="grid lg:grid-cols-4 gap-6">
          {/* Left Sidebar - Dataset Selection */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-8">
              <h2
                className="text-xl font-semibold mb-4 flex items-center gap-2"
                style={{ color: theme.primary }}
              >
                <Layers className="w-5 h-5" />
                Datasets
              </h2>
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {datasets.length === 0 ? (
                  <p className="text-gray-500 text-sm">No datasets available</p>
                ) : (
                  datasets.map((dataset) => (
                    <button
                      key={dataset.id}
                      onClick={() => handleDatasetSelect(dataset)}
                      className={`w-full text-left p-3 rounded-lg transition-colors border ${
                        selectedDataset?.id === dataset.id
                          ? 'border-2'
                          : 'border-gray-200 hover:bg-gray-50'
                      }`}
                      style={{
                        borderColor:
                          selectedDataset?.id === dataset.id
                            ? theme.primary
                            : undefined,
                      }}
                    >
                      <div className="font-medium text-sm text-gray-800">
                        {dataset.name}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {dataset.type} • {dataset.records} records
                      </div>
                    </button>
                  ))
                )}
              </div>

              {/* Annotation Controls */}
              {visualizationData && (
                <div className="mt-6 pt-6 border-t border-gray-200">
                  <h3
                    className="text-lg font-semibold mb-4"
                    style={{ color: theme.primary }}
                  >
                    Annotations
                  </h3>
                  <AnnotationTool
                    annotations={annotations}
                    onAdd={handleAnnotationAdd}
                    onDelete={handleAnnotationDelete}
                  />
                </div>
              )}
            </div>
          </div>

          {/* Main Content - 3D Visualization */}
          <div className="lg:col-span-3">
            <div className="bg-white rounded-lg shadow-md overflow-hidden">
              {loading ? (
                <div className="flex flex-col items-center justify-center h-[600px]">
                  <Loader2
                    className="w-12 h-12 animate-spin mb-4"
                    style={{ color: theme.primary }}
                  />
                  <p className="text-gray-600">Loading visualization...</p>
                </div>
              ) : visualizationData ? (
                <div className="h-[600px]">
                  <ClimateViewer
                    data={visualizationData}
                    annotations={annotations}
                  />
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-[600px] text-gray-500">
                  <Layers className="w-16 h-16 mb-4 opacity-30" />
                  <p className="text-lg mb-2">No visualization loaded</p>
                  <p className="text-sm">Select a dataset to begin</p>
                </div>
              )}
            </div>

            {/* Visualization Info */}
            {visualizationData && (
              <div className="mt-6 bg-white rounded-lg shadow-md p-6">
                <h2
                  className="text-xl font-semibold mb-4"
                  style={{ color: theme.primary }}
                >
                  Visualization Details
                </h2>
                <div className="grid md:grid-cols-3 gap-4">
                  <div>
                    <p className="text-sm text-gray-500 mb-1">Data Points</p>
                    <p className="text-2xl font-bold" style={{ color: theme.primary }}>
                      {visualizationData.metadata?.total_points || 0}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 mb-1">Risk Level</p>
                    <p className="text-2xl font-bold" style={{ color: theme.primary }}>
                      {visualizationData.metadata?.risk_level || 'N/A'}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-500 mb-1">Annotations</p>
                    <p className="text-2xl font-bold" style={{ color: theme.primary }}>
                      {annotations.length}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Export Modal */}
      {showExport && (
        <ExportTool
          visualizationId={visualizationData?.id}
          onClose={() => setShowExport(false)}
        />
      )}
    </div>
  );
}

export default Explore;
