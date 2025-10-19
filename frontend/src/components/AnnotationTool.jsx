import { useState } from 'react';
import { MessageSquare, Save, X } from 'lucide-react';
import { toast } from 'react-hot-toast';

/**
 * AnnotationTool Component
 * Tool for adding annotations to visualizations
 * Uses lucide-react for icons
 */
function AnnotationTool({ onSave, onCancel }) {
  const [text, setText] = useState('');

  const handleSave = () => {
    if (!text.trim()) {
      toast.error('Please enter annotation text');
      return;
    }

    onSave({
      text: text.trim(),
      timestamp: new Date().toISOString(),
    });

    setText('');
    toast.success('Annotation saved');
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-4 w-80">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <MessageSquare className="h-5 w-5 text-indigo-600" />
          <h4 className="font-semibold text-gray-900">Add Annotation</h4>
        </div>
        <button onClick={onCancel} className="text-gray-400 hover:text-gray-600">
          <X className="h-5 w-5" />
        </button>
      </div>

      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Enter annotation text..."
        className="w-full px-3 py-2 border border-gray-300 rounded-lg mb-4 h-24 resize-none"
      />

      <div className="flex justify-end space-x-2">
        <button onClick={onCancel} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">
          Cancel
        </button>
        <button
          onClick={handleSave}
          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 flex items-center space-x-2"
        >
          <Save className="h-4 w-4" />
          <span>Save</span>
        </button>
      </div>
    </div>
  );
}

export default AnnotationTool;
