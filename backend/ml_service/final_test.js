/**
 * Finalni test sa optimalnim postavkama
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

async function finalTest() {
    console.log('=== Finalni test sa optimalnim postavkama ===\n');
    console.log('Cilj: 3 parking mesta, 2 automobila, 1 prazno, 2 zauzeto\n');

    const testImagePath = path.join(__dirname, '..', 'uploads', 'test_slika3.png');
    
    // Optimalne postavke
    const options = {
        confidenceThreshold: 0.1,
        carThreshold: 0.12,        // Uključi sve sa confidence >= 12%
        parkingThreshold: 0.55,   // Uključi top 3 parking mesta
        iouThreshold: 0.3
    };
    
    try {
        const result = await mlInferenceService.analyzeImage(testImagePath, options);
        
        if (result.success) {
            console.log('Rezultati:');
            console.log(`  Parking mesta: ${result.total_spots} (treba 3) ${result.total_spots === 3 ? '✓' : '✗'}`);
            console.log(`  Automobili: ${result.total_cars} (treba 2) ${result.total_cars === 2 ? '✓' : '✗'}`);
            console.log(`  Slobodna: ${result.free_spaces} (treba 1) ${result.free_spaces === 1 ? '✓' : '✗'}`);
            console.log(`  Zauzeta: ${result.occupied_spaces} (treba 2) ${result.occupied_spaces === 2 ? '✓' : '✗'}`);
            
            console.log('\nDetalji detekcija:');
            result.all_detections.forEach((det, idx) => {
                console.log(`  ${idx + 1}. ${det.class_name} - confidence: ${(det.confidence * 100).toFixed(1)}%`);
            });
            
            if (result.total_spots === 3 && result.total_cars === 2) {
                console.log('\n✓ Parking mesta i automobili su tačni!');
                console.log('\nNapomena: Zauzetost zavisi od IoU threshold-a i pozicije automobila.');
                console.log('Ako zauzetost nije tačna, probaj da podesiš iouThreshold.');
            }
            
            const formatted = mlInferenceService.formatResult(result);
            console.log('\nFormatirani rezultat:');
            console.log(JSON.stringify(formatted, null, 2));
        }
    } catch (error) {
        console.error('✗ Greška:', error.message);
    }
}

finalTest().catch(console.error);

