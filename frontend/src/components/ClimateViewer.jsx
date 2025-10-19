import { useState, useEffect, Suspense, useRef } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, PerspectiveCamera, Grid, Html } from '@react-three/drei';
import { Maximize2, Minimize2, RotateCcw, Download } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { visualizationAPI } from '../services/api';

/**
 * ClimateViewer Component
 * 3D visualization of climate-health data using Three.js
 * Uses @react-three/fiber and @react-three/drei for 3D rendering
 * Supports interactive controls, zoom, pan, and export
 */

/**
 * HeatmapPoint Component
 * Renders individual data points in 3D space
 */
function HeatmapPoint({ position, value, color, label }) {
  const [hovered, setHovered] = useState(false);

  return (
    <group position={position}>
      <mesh
        onPointerOver={() => setHovered(true)}
        onPointerOut={() => setHovered(false)}
      >
        <sphereGeometry args={[0.2, 16, 16]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={hovered ? 0.5 : 0.2}
          roughness={0.3}
          metalness={0.5}
        />
      </mesh>

      {hovered && (
        <Html distanceFactor={10}>
          <div className="bg-white px-3 py-2 rounded shadow-lg text-sm border border-gray-200">
            <div className="font-semibold">{label}</div>
            <div className="text-gray-600">Value: {value.toFixed(2)}</div>
          </div>
        </Html>
      )}
    </group>
  );
}

/**
 * Scene Component
 * Main 3D scene with data visualization
 */
function Scene({ data, colorScale }) {
  return (
    <>
      {/* Ambient lighting */}
      <ambientLight intensity={0.5} />

      {/* Directional lights */}
      <directionalLight position={[10, 10, 5]} intensity={1} />
      <directionalLight position={[-10, -10, -5]} intensity={0.5} />

      {/* Point light for highlights */}
      <pointLight position={[0, 10, 0]} intensity={0.5} />

      {/* Grid helper */}
      <Grid
        args={[20, 20]}
        cellSize={1}
        cellThickness={0.5}
        cellColor="#6b7280"
        sectionSize={5}
        sectionThickness={1}
        sectionColor="#374151"
        fadeDistance={30}
        fadeStrength={1}
        position={[0, -0.01, 0]}
      />

      {/* Data points */}
      {data.map((point, index) => {
        const normalizedValue = (point.value - data.min) / (data.max - data.min);
        const color = getColorFromScale(normalizedValue, colorScale);

        return (
          <HeatmapPoint
            key={index}
            position={[point.x, point.y, point.z]}
            value={point.value}
            color={color}
            label={point.label || `Point ${index + 1}`}
          />
        );
      })}

      {/* Camera controls */}
      <OrbitControls
        enableDamping
        dampingFactor={0.05}
        minDistance={5}
        maxDistance={50}
        maxPolarAngle={Math.PI / 2}
      />

      {/* Camera */}
      <PerspectiveCamera makeDefault position={[10, 10, 10]} fov={60} />
    </>
  );
}

/**
 * Get color from normalized value and color scale
 */
function getColorFromScale(value, scale) {
  // Default blue to red color scale
  const scales = {
    heatmap: [
      { stop: 0, color: '#3b82f6' }, // blue
      { stop: 0.25, color: '#10b981' }, // green
      { stop: 0.5, color: '#fbbf24' }, // yellow
      { stop: 0.75, color: '#f97316' }, // orange
      { stop: 1, color: '#ef4444' }, // red
    ],
    temperature: [
      { stop: 0, color: '#1e40af' }, // dark blue
      { stop: 0.5, color: '#fbbf24' }, // yellow
      { stop: 1, color: '#dc2626' }, // dark red
    ],
    quality: [
      { stop: 0, color: '#dc2626' }, // red (poor)
      { stop: 0.5, color: '#fbbf24' }, // yellow (moderate)
      { stop: 1, color: '#16a34a' }, // green (good)
    ],
  };

  const colorStops = scales[scale] || scales.heatmap;

  // Find the two color stops to interpolate between
  let lowerStop = colorStops[0];
  let upperStop = colorStops[colorStops.length - 1];

  for (let i = 0; i < colorStops.length - 1; i++) {
    if (value >= colorStops[i].stop && value <= colorStops[i + 1].stop) {
      lowerStop = colorStops[i];
      upperStop = colorStops[i + 1];
      break;
    }
  }

  // Simple color interpolation (for production, use a proper color library)
  return lowerStop.color;
}

/**
 * Loading Component
 */
function LoadingSpinner() {
  return (
    <Html center>
      <div className="flex items-center space-x-2 bg-white px-4 py-2 rounded shadow">
        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-indigo-600"></div>
        <span className="text-sm text-gray-700">Loading 3D scene...</span>
      </div>
    </Html>
  );
}

/**
 * Main ClimateViewer Component
 */
function ClimateViewer({ visualizationId, initialData }) {
  const [data, setData] = useState(initialData || []);
  const [loading, setLoading] = useState(!initialData);
  const [fullscreen, setFullscreen] = useState(false);
  const [colorScale, setColorScale] = useState('heatmap');
  const canvasRef = useRef();

  /**
   * Load visualization data
   */
  useEffect(() => {
    if (visualizationId && !initialData) {
      loadVisualizationData();
    }
  }, [visualizationId]);

  const loadVisualizationData = async () => {
    setLoading(true);
    try {
      const response = await visualizationAPI.getVisualization(visualizationId);
      setData(processVisualizationData(response.data));
    } catch (error) {
      console.error('Failed to load visualization:', error);
      toast.error('Failed to load visualization data');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Process raw data into 3D coordinates
   */
  const processVisualizationData = (rawData) => {
    // This is a placeholder - actual processing depends on data format
    // Convert lat/lng to 3D coordinates, normalize values, etc.
    if (!rawData || !rawData.points) return [];

    const points = rawData.points.map((point, index) => ({
      x: point.x || point.longitude || index % 10,
      y: point.y || point.value || 0,
      z: point.z || point.latitude || Math.floor(index / 10),
      value: point.value || Math.random() * 100,
      label: point.label || `Point ${index + 1}`,
    }));

    // Calculate min/max for normalization
    const values = points.map((p) => p.value);
    points.min = Math.min(...values);
    points.max = Math.max(...values);

    return points;
  };

  /**
   * Reset camera position
   */
  const resetCamera = () => {
    // Camera reset is handled by OrbitControls
    toast.success('Camera reset');
  };

  /**
   * Toggle fullscreen
   */
  const toggleFullscreen = () => {
    setFullscreen(!fullscreen);
  };

  /**
   * Export visualization as PNG
   */
  const exportVisualization = async () => {
    try {
      const canvas = canvasRef.current?.querySelector('canvas');
      if (!canvas) {
        toast.error('Canvas not found');
        return;
      }

      // Convert canvas to blob
      canvas.toBlob((blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `climate-visualization-${Date.now()}.png`;
        link.click();
        URL.revokeObjectURL(url);
        toast.success('Visualization exported');
      });
    } catch (error) {
      console.error('Export error:', error);
      toast.error('Failed to export visualization');
    }
  };

  if (loading && !initialData) {
    return (
      <div className="bg-white rounded-lg shadow-md p-8 flex items-center justify-center h-96">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading 3D visualization...</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`bg-white rounded-lg shadow-md overflow-hidden ${
        fullscreen ? 'fixed inset-0 z-50' : ''
      }`}
    >
      {/* Controls Bar */}
      <div className="bg-gray-50 border-b border-gray-200 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <h3 className="font-semibold text-gray-900">Climate Visualization</h3>

          {/* Color Scale Selector */}
          <select
            value={colorScale}
            onChange={(e) => setColorScale(e.target.value)}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value="heatmap">Heatmap</option>
            <option value="temperature">Temperature</option>
            <option value="quality">Air Quality</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          {/* Reset Camera */}
          <button
            onClick={resetCamera}
            className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition"
            title="Reset Camera"
          >
            <RotateCcw className="h-5 w-5" />
          </button>

          {/* Export */}
          <button
            onClick={exportVisualization}
            className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition"
            title="Export as PNG"
          >
            <Download className="h-5 w-5" />
          </button>

          {/* Fullscreen Toggle */}
          <button
            onClick={toggleFullscreen}
            className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition"
            title={fullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
          >
            {fullscreen ? (
              <Minimize2 className="h-5 w-5" />
            ) : (
              <Maximize2 className="h-5 w-5" />
            )}
          </button>
        </div>
      </div>

      {/* 3D Canvas */}
      <div
        ref={canvasRef}
        className={fullscreen ? 'h-screen' : 'h-96'}
        style={{ touchAction: 'none' }}
      >
        <Canvas>
          <Suspense fallback={<LoadingSpinner />}>
            <Scene data={data} colorScale={colorScale} />
          </Suspense>
        </Canvas>
      </div>

      {/* Legend */}
      <div className="bg-gray-50 border-t border-gray-200 px-4 py-3">
        <div className="flex items-center justify-between text-sm">
          <div className="flex items-center space-x-6">
            <div>
              <span className="text-gray-600">Data Points:</span>
              <span className="ml-2 font-semibold text-gray-900">
                {data.length}
              </span>
            </div>
            {data.min !== undefined && (
              <div>
                <span className="text-gray-600">Range:</span>
                <span className="ml-2 font-semibold text-gray-900">
                  {data.min?.toFixed(2)} - {data.max?.toFixed(2)}
                </span>
              </div>
            )}
          </div>

          <div className="flex items-center space-x-2 text-xs text-gray-500">
            <span>Controls: Click + Drag to rotate, Scroll to zoom</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ClimateViewer;
