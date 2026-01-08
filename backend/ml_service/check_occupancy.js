/**
 * Proverava IoU između automobila i parking mesta
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

// IoU funkcija
function calculateIoU(box1, box2) {
    function toCorners(box) {
        const [x_center, y_center, width, height] = box;
        return {
            x1: x_center - width / 2,
            y1: y_center - height / 2,
            x2: x_center + width / 2,
            y2: y_center + height / 2
        };
    }
    
    const b1 = toCorners(box1);
    const b2 = toCorners(box2);
    
    const x1_inter = Math.max(b1.x1, b2.x1);
    const y1_inter = Math.max(b1.y1, b2.y1);
    const x2_inter = Math.min(b1.x2, b2.x2);
    const y2_inter = Math.min(b1.y2, b2.y2);
    
    if (x2_inter < x1_inter || y2_inter < y1_inter) return 0;
    
    const intersection = (x2_inter - x1_inter) * (y2_inter - y1_inter);
    const area1 = (b1.x2 - b1.x1) * (b1.y2 - b1.y1);
    const area2 = (b2.x2 - b2.x1) * (b2.y2 - b2.y1);
    const union = area1 + area2 - intersection;
    
    return union === 0 ? 0 : intersection / union;
}

async function checkOccupancy() {
    const testImagePath = path.join(__dirname, '..', 'uploads', 'test_slika1.png');
    
    const options = {
        confidenceThreshold: 0.1,
        carThreshold: 0.12,
        parkingThreshold: 0.55,
        iouThreshold: 0.3
    };
    
    const result = await mlInferenceService.analyzeImage(testImagePath, options);
    
    console.log('=== Provera zauzetosti ===\n');
    console.log(`Parking mesta: ${result.total_spots}`);
    console.log(`Automobili: ${result.total_cars}\n`);
    
    const cars = result.cars;
    const spots = result.parking_spots;
    
    console.log('IoU između automobila i parking mesta:\n');
    
    spots.forEach((spot, spotIdx) => {
        console.log(`Parking mesto ${spotIdx + 1} (x=${spot.bbox[0].toFixed(3)}, y=${spot.bbox[1].toFixed(3)}):`);
        let maxIoU = 0;
        let bestCar = -1;
        
        cars.forEach((car, carIdx) => {
            const iou = calculateIoU(spot.bbox, car.bbox);
            console.log(`  Automobil ${carIdx + 1} (x=${car.bbox[0].toFixed(3)}, y=${car.bbox[1].toFixed(3)}): IoU = ${iou.toFixed(3)}`);
            if (iou > maxIoU) {
                maxIoU = iou;
                bestCar = carIdx;
            }
        });
        
        const isOccupied = maxIoU >= 0.3;
        console.log(`  → Max IoU: ${maxIoU.toFixed(3)} ${isOccupied ? '(ZAUZETO)' : '(SLOBODNO)'}\n`);
    });
    
    console.log('\nPreporuka:');
    console.log('Ako je IoU < 0.3, smanji iouThreshold na 0.2 ili 0.15');
}

checkOccupancy().catch(console.error);

