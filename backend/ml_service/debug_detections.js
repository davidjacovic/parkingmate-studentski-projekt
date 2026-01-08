/**
 * Debug skripta - prikazuje sve detekcije sa vrlo niskim threshold-om
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

async function debugDetections() {
    console.log('=== Debug: Sve detekcije sa niskim threshold-om ===\n');

    const testImagePath = path.join(__dirname, '..', 'uploads', 'test_slika1.png');
    
    // Testiraj sa vrlo niskim threshold-om da vidimo sve
    const options = {
        confidenceThreshold: 0.1, // Vrlo nizak da vidimo sve
        iouThreshold: 0.3
    };
    
    try {
        const result = await mlInferenceService.analyzeImage(testImagePath, options);
        
        if (result.success) {
            console.log('Sve detekcije (confidence >= 0.1):\n');
            
            const cars = result.all_detections.filter(d => d.class_id === 0);
            const spots = result.all_detections.filter(d => d.class_id === 1);
            
            console.log(`Automobili (${cars.length}):`);
            cars.forEach((car, idx) => {
                console.log(`  ${idx + 1}. Confidence: ${(car.confidence * 100).toFixed(1)}%`);
                console.log(`     Koordinate: x=${car.bbox.x_center.toFixed(3)}, y=${car.bbox.y_center.toFixed(3)}, w=${car.bbox.width.toFixed(3)}, h=${car.bbox.height.toFixed(3)}`);
            });
            
            console.log(`\nParking mesta (${spots.length}):`);
            spots.forEach((spot, idx) => {
                console.log(`  ${idx + 1}. Confidence: ${(spot.confidence * 100).toFixed(1)}%`);
                console.log(`     Koordinate: x=${spot.bbox.x_center.toFixed(3)}, y=${spot.bbox.y_center.toFixed(3)}, w=${spot.bbox.width.toFixed(3)}, h=${spot.bbox.height.toFixed(3)}`);
            });
            
            console.log('\n--- Preporuke ---');
            console.log('Za 3 parking mesta, probaj threshold:');
            spots.sort((a, b) => b.confidence - a.confidence);
            if (spots.length >= 3) {
                console.log(`  - Parking threshold: ${spots[2].confidence.toFixed(2)} (3. najbolji)`);
            }
            if (spots.length >= 4) {
                console.log(`  - Parking threshold: ${spots[3].confidence.toFixed(2)} (4. najbolji - isključi)`);
            }
            
            console.log('\nZa 2 automobila, probaj threshold:');
            cars.sort((a, b) => b.confidence - a.confidence);
            if (cars.length >= 2) {
                console.log(`  - Car threshold: ${cars[1].confidence.toFixed(2)} (2. najbolji)`);
            }
            if (cars.length >= 1) {
                console.log(`  - Car threshold: ${cars[0].confidence.toFixed(2)} (najbolji)`);
            }
        }
    } catch (error) {
        console.error('✗ Greška:', error.message);
    }
}

debugDetections().catch(console.error);

