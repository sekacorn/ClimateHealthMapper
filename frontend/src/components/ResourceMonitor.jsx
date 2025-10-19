import { useState, useEffect } from 'react';
import { Cpu, HardDrive, Activity } from 'lucide-react';
import { monitoringAPI } from '../services/api';

/**
 * ResourceMonitor Component  
 * Displays system resource usage for 3D rendering and AI predictions
 * Uses lucide-react for icons
 */
function ResourceMonitor() {
  const [resources, setResources] = useState({ cpu: 0, memory: 0, gpu: 0 });

  useEffect(() => {
    loadResourceUsage();
    const interval = setInterval(loadResourceUsage, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadResourceUsage = async () => {
    try {
      const response = await monitoringAPI.getResourceUsage();
      setResources(response.data);
    } catch (error) {
      console.error('Failed to load resource usage:', error);
    }
  };

  const getColor = (value) => {
    if (value < 50) return 'bg-green-500';
    if (value < 75) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-bold text-gray-900 mb-4">System Resources</h3>
      <div className="space-y-4">
        {['cpu', 'memory', 'gpu'].map((type) => (
          <div key={type}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center space-x-2">
                {type === 'cpu' && <Cpu className="h-5 w-5 text-blue-600" />}
                {type === 'memory' && <HardDrive className="h-5 w-5 text-purple-600" />}
                {type === 'gpu' && <Activity className="h-5 w-5 text-green-600" />}
                <span className="text-sm font-medium text-gray-700 capitalize">{type}</span>
              </div>
              <span className="text-sm text-gray-600">{resources[type]}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${getColor(resources[type])}`}
                style={{ width: `${resources[type]}%` }}
              ></div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ResourceMonitor;
