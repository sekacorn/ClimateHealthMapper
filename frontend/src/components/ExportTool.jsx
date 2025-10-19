import { Download, FileText, Image } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { visualizationAPI } from '../services/api';
import { saveAs } from 'file-saver';

/**
 * ExportTool Component
 * Tool for exporting visualizations in multiple formats (PNG, SVG, STL)
 * Uses file-saver for downloads, lucide-react for icons
 */
function ExportTool({ visualizationId }) {
  const exportFormats = [
    { id: 'png', label: 'PNG Image', icon: <Image className="h-5 w-5" />, type: 'image' },
    { id: 'svg', label: 'SVG Vector', icon: <FileText className="h-5 w-5" />, type: 'vector' },
    { id: 'stl', label: 'STL 3D Model', icon: <Download className="h-5 w-5" />, type: '3d' },
  ];

  const handleExport = async (format) => {
    try {
      toast.loading(`Exporting as ${format.toUpperCase()}...`, { id: 'export' });

      const response = await visualizationAPI.exportVisualization(visualizationId, format);

      const blob = new Blob([response.data], {
        type: response.headers['content-type'],
      });

      saveAs(blob, `climate-visualization-${Date.now()}.${format}`);

      toast.success(`Exported as ${format.toUpperCase()}`, { id: 'export' });
    } catch (error) {
      console.error('Export error:', error);
      toast.error('Failed to export visualization', { id: 'export' });
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-bold text-gray-900 mb-4">Export Visualization</h3>

      <div className="space-y-3">
        {exportFormats.map((format) => (
          <button
            key={format.id}
            onClick={() => handleExport(format.id)}
            className="w-full flex items-center justify-between px-4 py-3 border border-gray-300 rounded-lg hover:border-indigo-400 hover:bg-indigo-50 transition"
          >
            <div className="flex items-center space-x-3">
              <div className="text-indigo-600">{format.icon}</div>
              <div className="text-left">
                <p className="font-medium text-gray-900">{format.label}</p>
                <p className="text-xs text-gray-500 capitalize">{format.type}</p>
              </div>
            </div>
            <Download className="h-5 w-5 text-gray-400" />
          </button>
        ))}
      </div>

      <p className="text-xs text-gray-500 mt-4">
        Exports include all current data points, annotations, and visual settings
      </p>
    </div>
  );
}

export default ExportTool;
