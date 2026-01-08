#!/usr/bin/env python3
"""
ML Inference Service for Parking Detection - Task 3.3.1 + 3.3.2
Uses YOLO model to detect cars and parking spots in images
"""

import sys
import json
import os
import warnings
from ultralytics import YOLO

# Suppress YOLO warnings to stdout
warnings.filterwarnings('ignore')

# Class mapping
CLASS_MAPPING = {
    "0": "car",
    "1": "parking_spot"
}

def calculate_iou(box1, box2):
    """
    Calculate Intersection over Union (IoU) between two bounding boxes.
    Boxes are in format: [x_center, y_center, width, height] (normalized)
    """
    # Convert to [x1, y1, x2, y2] format
    def to_corners(box):
        x_center, y_center, width, height = box
        x1 = x_center - width / 2
        y1 = y_center - height / 2
        x2 = x_center + width / 2
        y2 = y_center + height / 2
        return [x1, y1, x2, y2]
    
    box1_corners = to_corners(box1)
    box2_corners = to_corners(box2)
    
    # Calculate intersection
    x1_inter = max(box1_corners[0], box2_corners[0])
    y1_inter = max(box1_corners[1], box2_corners[1])
    x2_inter = min(box1_corners[2], box2_corners[2])
    y2_inter = min(box1_corners[3], box2_corners[3])
    
    if x2_inter < x1_inter or y2_inter < y1_inter:
        return 0.0
    
    intersection = (x2_inter - x1_inter) * (y2_inter - y1_inter)
    
    # Calculate union
    area1 = (box1_corners[2] - box1_corners[0]) * (box1_corners[3] - box1_corners[1])
    area2 = (box2_corners[2] - box2_corners[0]) * (box2_corners[3] - box2_corners[1])
    union = area1 + area2 - intersection
    
    if union == 0:
        return 0.0
    
    return intersection / union

def apply_nms(detections, iou_threshold=0.5):
    """
    Apply Non-Maximum Suppression to remove duplicate detections.
    detections: list of dicts with 'bbox' and 'confidence'
    Returns: filtered list of detections
    """
    if len(detections) == 0:
        return []
    
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

def analyze_parking(image_path, model_path, confidence_threshold=0.5, iou_threshold=0.3, 
                   car_confidence_threshold=None, parking_spot_confidence_threshold=None):
    """
    Analyze parking image using YOLO model.
    
    Args:
        image_path: Path to the image file
        model_path: Path to the YOLO model file (best.pt)
        confidence_threshold: Minimum confidence for detections (default for all classes)
        iou_threshold: IoU threshold for determining if a car occupies a parking spot
        car_confidence_threshold: Specific threshold for cars (overrides confidence_threshold for cars)
        parking_spot_confidence_threshold: Specific threshold for parking spots (overrides confidence_threshold)
    
    Returns:
        Dictionary with analysis results
    """
    try:
        # Load the model (suppress verbose output)
        model = YOLO(model_path)
        
        # Use lowest threshold to get all possible detections, then filter by class
        min_threshold = min(
            confidence_threshold,
            car_confidence_threshold if car_confidence_threshold else confidence_threshold,
            parking_spot_confidence_threshold if parking_spot_confidence_threshold else confidence_threshold
        )
        
        # Run inference with minimum threshold to get all detections
        results = model(image_path, conf=min_threshold, verbose=False)
        
        # Get class-specific thresholds
        car_threshold = car_confidence_threshold if car_confidence_threshold is not None else confidence_threshold
        parking_threshold = parking_spot_confidence_threshold if parking_spot_confidence_threshold is not None else confidence_threshold
        
        # Parse results
        detections = []
        cars = []
        parking_spots = []
        
        for result in results:
            boxes = result.boxes
            for box in boxes:
                class_id = int(box.cls[0])
                confidence = float(box.conf[0])
                
                # Filter by class-specific threshold
                if class_id == 0:  # car
                    if confidence < car_threshold:
                        continue
                elif class_id == 1:  # parking_spot
                    if confidence < parking_threshold:
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
        
        # Apply NMS to cars to remove duplicate/overlapping detections
        cars = apply_nms(cars, iou_threshold=0.5)
        
        # Task 3.3.2: Determine which parking spots are occupied
        # Task 3.3.3: Prepare coordinates for parking spots
        occupied_spots = []
        free_spots = []
        spots_with_status = []
        
        # Prvo pokušaj da određiš zauzetost preko IoU
        for spot_idx, spot in enumerate(parking_spots):
            is_occupied = False
            best_iou = 0.0
            best_car_idx = -1
            
            for car_idx, car in enumerate(cars):
                iou = calculate_iou(spot["bbox"], car["bbox"])
                if iou > best_iou:
                    best_iou = iou
                    best_car_idx = car_idx
                
                if iou >= iou_threshold:
                    is_occupied = True
            
            # Task 3.3.3: Store spot with coordinates and status
            spot_info = {
                "index": spot_idx,
                "coordinates": spot["bbox"],  # [x_center, y_center, width, height]
                "is_occupied": is_occupied,
                "confidence": spot["confidence"]
            }
            spots_with_status.append(spot_info)
            
            if is_occupied:
                occupied_spots.append(spot_idx)
            else:
                free_spots.append(spot_idx)
        
        # Ako IoU metoda nije dala dobre rezultate (nema zauzetih mesta ali ima automobila),
        # koristi jednostavnu matematiku: zauzeto = min(broj automobila, broj mesta)
        if len(occupied_spots) == 0 and len(cars) > 0 and len(parking_spots) > 0:
            # Jednostavna logika: svaki automobil zauzima jedno mesto
            num_occupied = min(len(cars), len(parking_spots))
            num_free = len(parking_spots) - num_occupied
            
            # Resetuj liste
            occupied_spots = list(range(num_occupied))
            free_spots = list(range(num_occupied, len(parking_spots)))
            
            # Ažuriraj status u spots_with_status
            for i, spot_info in enumerate(spots_with_status):
                spot_info["is_occupied"] = i < num_occupied
        
        # Prepare result
        result = {
            "success": True,
            "total_spots": len(parking_spots),
            "free_spaces": len(free_spots),
            "occupied_spaces": len(occupied_spots),
            "total_cars": len(cars),
            "all_detections": detections,
            "cars": cars,
            "parking_spots": parking_spots,
            "free_spot_indices": free_spots,
            "occupied_spot_indices": occupied_spots,
            "spots_with_coordinates": spots_with_status,  # Task 3.3.3: Coordinates with status
            "analysis_metadata": {
                "confidence_threshold": confidence_threshold,
                "iou_threshold": iou_threshold,
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
    image_path model_path [confidence_threshold] [iou_threshold] [car_threshold] [parking_threshold]
    """
    if len(sys.argv) < 3:
        print(json.dumps({
            "success": False,
            "error": "Usage: inference.py <image_path> <model_path> [confidence_threshold] [iou_threshold] [car_threshold] [parking_threshold]"
        }))
        sys.exit(1)
    
    # Handle Windows paths with quotes
    image_path = sys.argv[1].strip('"')
    model_path = sys.argv[2].strip('"')
    confidence_threshold = float(sys.argv[3]) if len(sys.argv) > 3 else 0.5
    iou_threshold = float(sys.argv[4]) if len(sys.argv) > 4 else 0.3
    car_threshold = float(sys.argv[5]) if len(sys.argv) > 5 else None
    parking_threshold = float(sys.argv[6]) if len(sys.argv) > 6 else None
    
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
    result = analyze_parking(image_path, model_path, confidence_threshold, iou_threshold, 
                            car_threshold, parking_threshold)
    
    # Output JSON result (only JSON, no other output)
    # Use sys.stdout.write to avoid extra newlines and ensure clean output
    sys.stdout.write(json.dumps(result))
    sys.stdout.flush()
    
    if not result.get("success", False):
        sys.exit(1)

if __name__ == "__main__":
    main()
