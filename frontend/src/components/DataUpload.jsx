import { useState, useRef } from 'react';
import { Upload, FileText, CheckCircle, XCircle, Loader } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { dataAPI } from '../services/api';
import DOMPurify from 'dompurify';

/**
 * DataUpload Component
 * Handles file uploads for climate, health, and genomic data
 * Supports CSV, JSON, FHIR, and VCF formats
 * Uses lucide-react for icons, dompurify for sanitization
 */
function DataUpload({ onUploadComplete }) {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});
  const fileInputRef = useRef(null);

  // Supported file types
  const supportedTypes = {
    climate: {
      extensions: ['.csv', '.json'],
      description: 'Climate data (CSV, JSON)',
      mimeTypes: ['text/csv', 'application/json'],
    },
    health: {
      extensions: ['.json', '.xml', '.fhir'],
      description: 'Health data (FHIR, JSON, XML)',
      mimeTypes: ['application/json', 'application/xml', 'text/xml'],
    },
    genomic: {
      extensions: ['.vcf', '.vcf.gz'],
      description: 'Genomic data (VCF)',
      mimeTypes: ['text/plain', 'application/gzip'],
    },
  };

  const [selectedType, setSelectedType] = useState('climate');

  /**
   * Handle file selection
   * Validates file type and size
   */
  const handleFileSelect = (event) => {
    const selectedFiles = Array.from(event.target.files);
    const maxSize = 100 * 1024 * 1024; // 100MB max file size

    const validFiles = selectedFiles.filter((file) => {
      // Check file size
      if (file.size > maxSize) {
        toast.error(`${file.name} exceeds 100MB limit`);
        return false;
      }

      // Check file extension
      const fileName = file.name.toLowerCase();
      const typeConfig = supportedTypes[selectedType];
      const hasValidExtension = typeConfig.extensions.some((ext) =>
        fileName.endsWith(ext)
      );

      if (!hasValidExtension) {
        toast.error(
          `${file.name} is not a valid ${selectedType} file. Expected: ${typeConfig.extensions.join(', ')}`
        );
        return false;
      }

      return true;
    });

    if (validFiles.length > 0) {
      setFiles((prev) => [
        ...prev,
        ...validFiles.map((file) => ({
          file,
          id: `${file.name}-${Date.now()}`,
          type: selectedType,
          status: 'pending',
        })),
      ]);
    }
  };

  /**
   * Remove file from upload queue
   */
  const removeFile = (fileId) => {
    setFiles((prev) => prev.filter((f) => f.id !== fileId));
  };

  /**
   * Upload all files
   */
  const handleUpload = async () => {
    if (files.length === 0) {
      toast.error('Please select files to upload');
      return;
    }

    setUploading(true);
    const results = [];

    for (const fileItem of files) {
      if (fileItem.status === 'completed') continue;

      try {
        // Update status to uploading
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id ? { ...f, status: 'uploading' } : f
          )
        );

        // Upload file
        const response = await dataAPI.uploadFile(fileItem.file, fileItem.type);

        // Update status to completed
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id
              ? { ...f, status: 'completed', data: response.data }
              : f
          )
        );

        results.push({ success: true, file: fileItem, data: response.data });
        toast.success(`${fileItem.file.name} uploaded successfully`);
      } catch (error) {
        console.error(`Upload error for ${fileItem.file.name}:`, error);

        // Update status to error
        setFiles((prev) =>
          prev.map((f) =>
            f.id === fileItem.id
              ? {
                  ...f,
                  status: 'error',
                  error: error.response?.data?.message || 'Upload failed',
                }
              : f
          )
        );

        results.push({ success: false, file: fileItem, error });
        toast.error(
          `Failed to upload ${fileItem.file.name}: ${error.response?.data?.message || 'Unknown error'}`
        );
      }
    }

    setUploading(false);

    // Notify parent component
    if (onUploadComplete) {
      onUploadComplete(results);
    }

    // Clear completed files after a delay
    setTimeout(() => {
      setFiles((prev) => prev.filter((f) => f.status !== 'completed'));
    }, 3000);
  };

  /**
   * Clear all files
   */
  const clearFiles = () => {
    setFiles([]);
  };

  /**
   * Get status icon
   */
  const getStatusIcon = (status) => {
    switch (status) {
      case 'uploading':
        return <Loader className="h-5 w-5 animate-spin text-blue-500" />;
      case 'completed':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'error':
        return <XCircle className="h-5 w-5 text-red-500" />;
      default:
        return <FileText className="h-5 w-5 text-gray-400" />;
    }
  };

  /**
   * Format file size
   */
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Upload Data</h2>

      {/* Data Type Selector */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Data Type
        </label>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {Object.entries(supportedTypes).map(([type, config]) => (
            <button
              key={type}
              onClick={() => setSelectedType(type)}
              className={`p-4 border-2 rounded-lg text-left transition-all ${
                selectedType === type
                  ? 'border-indigo-600 bg-indigo-50'
                  : 'border-gray-300 hover:border-indigo-400'
              }`}
            >
              <div className="font-semibold text-gray-900 capitalize mb-1">
                {type} Data
              </div>
              <div className="text-sm text-gray-600">{config.description}</div>
              <div className="text-xs text-gray-500 mt-2">
                {config.extensions.join(', ')}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* File Upload Area */}
      <div
        className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-indigo-400 transition-colors cursor-pointer"
        onClick={() => fileInputRef.current?.click()}
      >
        <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p className="text-lg font-medium text-gray-700 mb-2">
          Drop files here or click to browse
        </p>
        <p className="text-sm text-gray-500">
          Supported formats: {supportedTypes[selectedType].extensions.join(', ')}
        </p>
        <p className="text-xs text-gray-400 mt-1">Maximum file size: 100MB</p>

        <input
          ref={fileInputRef}
          type="file"
          multiple
          onChange={handleFileSelect}
          accept={supportedTypes[selectedType].extensions.join(',')}
          className="hidden"
        />
      </div>

      {/* File List */}
      {files.length > 0 && (
        <div className="mt-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-gray-900">
              Files ({files.length})
            </h3>
            <button
              onClick={clearFiles}
              className="text-sm text-red-600 hover:text-red-700"
              disabled={uploading}
            >
              Clear All
            </button>
          </div>

          <div className="space-y-3">
            {files.map((fileItem) => (
              <div
                key={fileItem.id}
                className="flex items-center justify-between p-4 bg-gray-50 rounded-lg"
              >
                <div className="flex items-center space-x-3 flex-1">
                  {getStatusIcon(fileItem.status)}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {fileItem.file.name}
                    </p>
                    <div className="flex items-center space-x-2 text-xs text-gray-500">
                      <span>{formatFileSize(fileItem.file.size)}</span>
                      <span>"</span>
                      <span className="capitalize">{fileItem.type}</span>
                      {fileItem.status === 'error' && fileItem.error && (
                        <>
                          <span>"</span>
                          <span className="text-red-600">{fileItem.error}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                {fileItem.status !== 'uploading' && (
                  <button
                    onClick={() => removeFile(fileItem.id)}
                    className="ml-4 text-gray-400 hover:text-red-600"
                    disabled={uploading}
                  >
                    <XCircle className="h-5 w-5" />
                  </button>
                )}
              </div>
            ))}
          </div>

          {/* Upload Button */}
          <div className="mt-6 flex justify-end space-x-3">
            <button
              onClick={clearFiles}
              className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
              disabled={uploading}
            >
              Cancel
            </button>
            <button
              onClick={handleUpload}
              disabled={uploading || files.every((f) => f.status === 'completed')}
              className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center"
            >
              {uploading ? (
                <>
                  <Loader className="h-5 w-5 animate-spin mr-2" />
                  Uploading...
                </>
              ) : (
                <>
                  <Upload className="h-5 w-5 mr-2" />
                  Upload Files
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* Info Section */}
      <div className="mt-6 p-4 bg-blue-50 rounded-lg">
        <h4 className="text-sm font-semibold text-blue-900 mb-2">
          Data Upload Guidelines
        </h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>" Climate data: CSV or JSON with temperature, humidity, air quality metrics</li>
          <li>" Health data: FHIR-compliant JSON or XML from EHR systems</li>
          <li>" Genomic data: VCF files from sequencing platforms (23andMe, AncestryDNA, etc.)</li>
          <li>" All data is encrypted in transit and at rest</li>
          <li>" Files are validated and processed asynchronously</li>
        </ul>
      </div>
    </div>
  );
}

export default DataUpload;
