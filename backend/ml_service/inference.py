#!/usr/bin/env python3
"""
ML Inference Service for Parking Detection - Task 3.3
Uses YOLO model to detect empty and occupied parking spots in images
"""

import sys
import json
import os
import warnings
from ultralytics import YOLO

# Suppress YOLO warnings to stdout
warnings.filterwarnings('ignore')

# Class mapping for new model
CLASS_MAPPING = {
    "0": "empty",
    "1": "occupied"
}

def apply_nms(detections, iou_threshold=0.5):
    """
    Apply Non-Maximum Suppression to remove duplicate detections.
    detections: list of dicts with 'bbox' and 'confidence'
    Returns: filtered list of detections
    """
    if len(detections) == 0:
        return []
    
    def calculate_iou(box1, box2):
        """Calculate IoU between two bounding boxes in format [x_center, y_center, width, height]"""
        def to_corners(box):
            x_center, y_center, width, height = box
            x1 = x_center - width / 2
            y1 = y_center - height / 2
            x2 = x_center + width / 2
            y2 = y_center + height / 2
            return [x1, y1, x2, y2]
        
        box1_corners = to_corners(box1)
        box2_corners = to_corners(box2)
        
        x1_inter = max(box1_corners[0], box2_corners[0])
        y1_inter = max(box1_corners[1], box2_corners[1])
        x2_inter = min(box1_corners[2], box2_corners[2])
        y2_inter = min(box1_corners[3], box2_corners[3])
        
        if x2_inter < x1_inter or y2_inter < y1_inter:
            return 0.0
        
        intersection = (x2_inter - x1_inter) * (y2_inter - y1_inter)
        area1 = (box1_corners[2] - box1_corners[0]) * (box1_corners[3] - box1_corners[1])
        area2 = (box2_corners[2] - box2_corners[0]) * (box2_corners[3] - box2_corners[1])
        union = area1 + area2 - intersection
        
        if union == 0:
            return 0.0
        
        return intersection / union
    
    # Sort by confidence (highest first)
    sorted_detections = sorted(detections, key=lambda x: x['confidence'], reverse=True)
    
    filtered = []
    while sorted_detections:
        # Take the highest confidence detection
        best = sorted_detections.pop(0)
        filtered.append(best)
        
        # Remove overlapping detections
        sorted_detections = [
            det for det in sorted_detections
            if calculate_iou(best['bbox'], det['bbox']) < iou_threshold
        ]
    
    return filtered

def analyze_parking(image_path, model_path, confidence_threshold=0.5, 
                   empty_confidence_threshold=None, occupied_confidence_threshold=None):
    """
    Analyze parking image using YOLO model.
    New model directly detects parking spots with status (empty/occupied).
    
    Args:
        image_path: Path to the image file
        model_path: Path to the YOLO model file (last.pt)
        confidence_threshold: Minimum confidence for detections (default for all classes)
        empty_confidence_threshold: Specific threshold for empty spots (overrides confidence_threshold)
        occupied_confidence_threshold: Specific threshold for occupied spots (overrides confidence_threshold)
    
    Returns:
        Dictionary with analysis results
    """
    try:
        # Load the model (suppress verbose output)
        model = YOLO(model_path)
        
        # Use lowest threshold to get all possible detections, then filter by class
        min_threshold = min(
            confidence_threshold,
            empty_confidence_threshold if empty_confidence_threshold else confidence_threshold,
            occupied_confidence_threshold if occupied_confidence_threshold else confidence_threshold
        )
        
        # Run inference with minimum threshold to get all detections
        results = model(image_path, conf=min_threshold, verbose=False)
        
        # Get class-specific thresholds
        empty_threshold = empty_confidence_threshold if empty_confidence_threshold is not None else confidence_threshold
        occupied_threshold = occupied_confidence_threshold if occupied_confidence_threshold is not None else confidence_threshold
        
        # Parse results
        detections = []
        empty_spots = []
        occupied_spots = []
        
        for result in results:
            boxes = result.boxes
            for box in boxes:
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                
                # Filter by class-specific threshold
                if class_id == 0:  # empty
                    if confidence < empty_threshold:
                        continue
                elif class_id == 1:  # occupied
                    if confidence < occupied_threshold:
                        continue
                else:
                    if confidence < confidence_threshold:
                        continue
                
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
                
                spot_data = {
                    "bbox": [float(x_center), float(y_center), float(width), float(height)],
                    "confidence": confidence
                }
                
                if class_id == 0:  # empty
                    empty_spots.append(spot_data)
                elif class_id == 1:  # occupied
                    occupied_spots.append(spot_data)
        
        # Apply NMS to remove duplicate/overlapping detections (separately for empty and occupied)
        empty_spots = apply_nms(empty_spots, iou_threshold=0.5)
        occupied_spots = apply_nms(occupied_spots, iou_threshold=0.5)
        
        # Task 3.3.2: Count free and occupied spaces
        # Task 3.3.3: Prepare coordinates for parking spots
        total_spots = len(empty_spots) + len(occupied_spots)
        free_spaces = len(empty_spots)
        occupied_spaces = len(occupied_spots)
        
        # Task 3.3.3: Store all spots with coordinates and status
        spots_with_status = []
        
        # Add empty spots
        for spot_idx, spot in enumerate(empty_spots):
            spot_info = {
                "index": spot_idx,
                "coordinates": spot["bbox"],  # [x_center, y_center, width, height]
                "is_occupied": False,
                "confidence": spot["confidence"]
            }
            spots_with_status.append(spot_info)
        
        # Add occupied spots
        for spot_idx, spot in enumerate(occupied_spots):
            spot_info = {
                "index": len(empty_spots) + spot_idx,
                "coordinates": spot["bbox"],  # [x_center, y_center, width, height]
                "is_occupied": True,
                "confidence": spot["confidence"]
            }
            spots_with_status.append(spot_info)
        
        # Prepare result
        result = {
            "success": True,
            "total_spots": total_spots,
            "free_spaces": free_spaces,
            "occupied_spaces": occupied_spaces,
            "total_cars": occupied_spaces,  # For backward compatibility: occupied spots = cars
            "all_detections": detections,
            "empty_spots": empty_spots,
            "occupied_spots": occupied_spots,
            "free_spot_indices": list(range(free_spaces)),
            "occupied_spot_indices": list(range(free_spaces, total_spots)),
            "spots_with_coordinates": spots_with_status,  # Task 3.3.3: Coordinates with status
            "analysis_metadata": {
                "confidence_threshold": confidence_threshold,
                "empty_threshold": empty_threshold,
                "occupied_threshold": occupied_threshold,
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
    Reads arguments from command line: 
    image_path model_path [confidence_threshold] [empty_threshold] [occupied_threshold]
    """
    if len(sys.argv) < 3:
        print(json.dumps({
            "success": False,
            "error": "Usage: inference.py <image_path> <model_path> [confidence_threshold] [empty_threshold] [occupied_threshold]"
        }))
        sys.exit(1)
    
    # Handle Windows paths with quotes
    image_path = sys.argv[1].strip('"')
    model_path = sys.argv[2].strip('"')
    confidence_threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 0.5
    empty_threshold = float(sys.argv[4]) if len(sys.argv) > 4 else None
    occupied_threshold = float(sys.argv[5]) if len(sys.argv) > 5 else None
    
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
    result = analyze_parking(image_path, model_path, confidence_threshold, 
                            empty_threshold, occupied_threshold)
    
    # Output JSON result (only JSON, no other output)
    # Use sys.stdout.write to avoid extra newlines and ensure clean output
    sys.stdout.write(json.dumps(result))
    sys.stdout.flush()
    
    if not result.get("success", False):
        sys.exit(1)

if __name__ == "__main__":
    main()
