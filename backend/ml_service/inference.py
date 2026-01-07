#!/usr/bin/env python3
"""
ML Inference Service for Parking Detection - Task 3.3.1
Uses YOLO model to detect cars and parking spots in images
"""

import sys
import json
import os
from ultralytics import YOLO

# Class mapping
CLASS_MAPPING = {
    "0": "car",
    "1": "parking_spot"
}

def analyze_parking(image_path, model_path, confidence_threshold=0.5):
    """
    Analyze parking image using YOLO model.
    
    Args:
        image_path: Path to the image file
        model_path: Path to the YOLO model file (best.pt)
        confidence_threshold: Minimum confidence for detections
    
    Returns:
        Dictionary with analysis results
    """
    try:
        # Load the model
        model = YOLO(model_path)
        
        # Run inference
        results = model(image_path, conf=confidence_threshold)
        
        # Parse results
        detections = []
        cars = []
        parking_spots = []
        
        for result in results:
            boxes = result.boxes
            for box in boxes:
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                # Get normalized coordinates: x_center, y_center, width, height
                x_center, y_center, width, height = box.xywhn[0].cpu().numpy()
                
                detection = {
                    "class_id": class_id,
                    "class_name": CLASS_MAPPING.get(str(class_id), "unknown"),
                    "confidence": confidence,
                    "bbox": {
                        "x_center": float(x_center),
                        "y_center": float(y_center),
                        "width": float(width),
                        "height": float(height)
                    }
                }
                detections.append(detection)
                
                if class_id == 0:  # car
                    cars.append({
                        "bbox": [float(x_center), float(y_center), float(width), float(height)],
                        "confidence": confidence
                    })
                elif class_id == 1:  # parking_spot
                    parking_spots.append({
                        "bbox": [float(x_center), float(y_center), float(width), float(height)],
                        "confidence": confidence
                    })
        
        # Prepare result - basic detection results only
        result = {
            "success": True,
            "total_spots": len(parking_spots),
            "total_cars": len(cars),
            "all_detections": detections,
            "cars": cars,
            "parking_spots": parking_spots,
            "analysis_metadata": {
                "confidence_threshold": confidence_threshold,
                "image_path": image_path
            }
        }
        
        return result
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "error_type": type(e).__name__
        }

def main():
    """
    Main entry point for the inference service.
    Reads arguments from command line: image_path model_path [confidence_threshold]
    """
    if len(sys.argv) < 3:
        print(json.dumps({
            "success": False,
            "error": "Usage: inference.py <image_path> <model_path> [confidence_threshold]"
        }))
        sys.exit(1)
    
    # Handle Windows paths with quotes
    image_path = sys.argv[1].strip('"')
    model_path = sys.argv[2].strip('"')
    confidence_threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 0.5
    
    # Normalize paths for cross-platform compatibility
    image_path = os.path.normpath(image_path)
    model_path = os.path.normpath(model_path)
    
    # Validate paths
    if not os.path.exists(image_path):
        print(json.dumps({
            "success": False,
            "error": f"Image file not found: {image_path}"
        }))
        sys.exit(1)
    
    if not os.path.exists(model_path):
        print(json.dumps({
            "success": False,
            "error": f"Model file not found: {model_path}"
        }))
        sys.exit(1)
    
    # Run analysis
    result = analyze_parking(image_path, model_path, confidence_threshold)
    
    # Output JSON result
    print(json.dumps(result, indent=2))
    
    if not result.get("success", False):
        sys.exit(1)

if __name__ == "__main__":
    main()
