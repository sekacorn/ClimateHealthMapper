# Climate Visualizer API Examples

Complete examples for testing the Climate Visualizer microservice.

## Base URL
```
http://localhost:8083/api/visualizer
```

## 1. Generate 3D Climate Map

### Request
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "INTJ",
    "quality": "medium"
  }'
```

### Response
```json
{
  "visualizationId": 1,
  "data": {
    "type": "Scene",
    "geometries": [...],
    "materials": {...},
    "camera": {...},
    "lights": [...]
  },
  "processingTime": 1234,
  "dataPoints": 2500
}
```

## 2. Generate Health Heatmap

### Request
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/health-heatmap \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user456",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "radius": 50.0,
    "healthFactors": ["heat_stress", "respiratory", "cardiovascular"],
    "mbtiType": "ENFP",
    "quality": "high"
  }'
```

### Response
```json
{
  "visualizationId": 2,
  "data": {
    "type": "Heatmap",
    "grid": {
      "width": 100,
      "height": 100,
      "grid": [[...]]
    },
    "colorScheme": {
      "gradient": ["#F39C12", "#E74C3C", ...],
      "interpolation": "linear"
    },
    "statistics": {
      "averageRisk": 45.2,
      "maxRisk": 87.3,
      "riskDistribution": {...}
    }
  },
  "processingTime": 2345,
  "riskPoints": 10000
}
```

## 3. Get User Visualizations

### Request - All Visualizations
```bash
curl -X GET http://localhost:8083/api/visualizer/visualizations/user123
```

### Request - Filter by Type
```bash
curl -X GET "http://localhost:8083/api/visualizer/visualizations/user123?type=CLIMATE_MAP_3D"
```

### Response
```json
[
  {
    "id": 1,
    "userId": "user123",
    "title": "Climate Map - 40.7128, -74.006",
    "visualizationType": "CLIMATE_MAP_3D",
    "mbtiType": "INTJ",
    "processingTimeMs": 1234,
    "createdAt": "2024-01-15T10:30:00",
    "isPublic": false
  },
  {
    "id": 3,
    "userId": "user123",
    "title": "Climate Map - 51.5074, -0.1278",
    "visualizationType": "CLIMATE_MAP_3D",
    "mbtiType": "INTJ",
    "processingTimeMs": 1567,
    "createdAt": "2024-01-16T14:20:00",
    "isPublic": false
  }
]
```

## 4. Get Specific Visualization

### Request
```bash
curl -X GET http://localhost:8083/api/visualizer/visualization/1
```

### Response
```json
{
  "id": 1,
  "userId": "user123",
  "title": "Climate Map - 40.7128, -74.006",
  "description": null,
  "visualizationType": "CLIMATE_MAP_3D",
  "mbtiType": "INTJ",
  "visualizationData": "{\"type\":\"Scene\",...}",
  "cameraSettings": null,
  "lightingSettings": null,
  "processingTimeMs": 1234,
  "renderQuality": "medium",
  "isPublic": false,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## 5. Export Visualization to PNG

### Request
```bash
curl -X POST http://localhost:8083/api/visualizer/export/1 \
  -H "Content-Type: application/json" \
  -d '{
    "format": "PNG",
    "options": {
      "width": 1920,
      "height": 1080
    }
  }'
```

### Response
```json
{
  "success": true,
  "format": "PNG",
  "filePath": "./exports/Climate_Map_-_40_7128__-74_006_1_20240115_103000.png",
  "fileSize": 2457600,
  "exportedAt": "2024-01-15T10:35:00"
}
```

## 6. Export to SVG

### Request
```bash
curl -X POST http://localhost:8083/api/visualizer/export/2 \
  -H "Content-Type: application/json" \
  -d '{
    "format": "SVG",
    "options": {
      "width": 2560,
      "height": 1440
    }
  }'
```

## 7. Export to STL (3D Models Only)

### Request
```bash
curl -X POST http://localhost:8083/api/visualizer/export/1 \
  -H "Content-Type: application/json" \
  -d '{
    "format": "STL",
    "options": {}
  }'
```

### Response
```json
{
  "success": true,
  "format": "STL",
  "filePath": "./exports/Climate_Map_-_40_7128__-74_006_1_20240115_103500.stl",
  "fileSize": 1234567,
  "exportedAt": "2024-01-15T10:35:00"
}
```

## 8. Check System Resources

### Request
```bash
curl -X GET http://localhost:8083/api/visualizer/resources/check
```

### Response
```json
{
  "cpu": {
    "model": "Intel(R) Core(TM) i7-9750H CPU @ 2.60GHz",
    "cores": 12,
    "physicalCores": 6,
    "usage": 23.5,
    "maxFrequency": 2.6
  },
  "memory": {
    "total": "16.00 GB",
    "used": "8.50 GB",
    "available": "7.50 GB",
    "usagePercent": 53.125
  },
  "gpu": {
    "name": "NVIDIA GeForce GTX 1660 Ti",
    "vendor": "NVIDIA",
    "vram": "6.00 GB",
    "available": true
  },
  "loadAverage": {
    "1min": 2.5,
    "5min": 2.1,
    "15min": 1.8
  },
  "status": "GOOD"
}
```

## 9. Delete Visualization

### Request
```bash
curl -X DELETE http://localhost:8083/api/visualizer/visualization/1
```

### Response
```json
{
  "message": "Visualization deleted successfully"
}
```

## MBTI Type Examples

### All 16 MBTI Types with Color Schemes

```bash
# INTJ - Deep blue-gray theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "INTJ", "quality": "medium"}'

# INTP - Teal theme with wireframe
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user2", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "INTP", "quality": "medium"}'

# ENTJ - Bold red theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user3", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ENTJ", "quality": "medium"}'

# ENTP - Purple theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user4", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ENTP", "quality": "medium"}'

# INFJ - Soft gray-blue theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user5", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "INFJ", "quality": "medium"}'

# INFP - Lavender theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user6", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "INFP", "quality": "medium"}'

# ENFJ - Warm green theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user7", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ENFJ", "quality": "medium"}'

# ENFP - Vibrant orange theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user8", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ENFP", "quality": "medium"}'

# ISTJ - Navy blue theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user9", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ISTJ", "quality": "medium"}'

# ISFJ - Warm brown theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user10", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ISFJ", "quality": "medium"}'

# ESTJ - Dark red theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user11", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ESTJ", "quality": "medium"}'

# ESFJ - Burnt orange theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user12", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ESFJ", "quality": "medium"}'

# ISTP - Cool gray theme with wireframe
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user13", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ISTP", "quality": "medium"}'

# ISFP - Rose theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user14", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ISFP", "quality": "medium"}'

# ESTP - Bright red theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user15", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ESTP", "quality": "medium"}'

# ESFP - Bright yellow theme
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{"userId": "user16", "latitude": 40.7128, "longitude": -74.0060, "radius": 50.0, "mbtiType": "ESFP", "quality": "medium"}'
```

## Quality Level Examples

### Low Quality (Fast Processing)
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "INTJ",
    "quality": "low"
  }'
```

### Medium Quality (Balanced)
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "INTJ",
    "quality": "medium"
  }'
```

### High Quality (Detailed)
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "INTJ",
    "quality": "high"
  }'
```

## Error Handling Examples

### Invalid Coordinates
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 100.0,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "INTJ",
    "quality": "medium"
  }'
```

Response:
```json
{
  "error": "Latitude must be between -90 and 90"
}
```

### Invalid MBTI Type
```bash
curl -X POST http://localhost:8083/api/visualizer/generate/climate-map \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "radius": 50.0,
    "mbtiType": "XXXX",
    "quality": "medium"
  }'
```

Response:
```json
{
  "error": "Invalid MBTI type: XXXX"
}
```

### Unsupported Export Format
```bash
curl -X POST http://localhost:8083/api/visualizer/export/1 \
  -H "Content-Type: application/json" \
  -d '{
    "format": "PDF",
    "options": {}
  }'
```

Response:
```json
{
  "error": "Unsupported format: PDF"
}
```

## Health Check

```bash
curl -X GET http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

## Metrics

```bash
curl -X GET http://localhost:8083/actuator/metrics
```

## Testing Tips

1. **Start with low quality** for faster testing
2. **Use different MBTI types** to see styling variations
3. **Monitor resources** during generation with `/resources/check`
4. **Export in multiple formats** to test all export functionality
5. **Test edge cases** like boundary coordinates
6. **Check logs** for detailed processing information

## Postman Collection

Import these examples into Postman for easier testing. Create a collection with:
- Base URL variable: `{{base_url}}` = `http://localhost:8083/api/visualizer`
- Environment variables for common test data
