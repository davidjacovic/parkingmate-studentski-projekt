/**
 * Test sa optimalnim threshold-ima za test_slika1.png
 * Treba: 3 parking mesta, 2 automobila, 1 prazno mesto
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

async function testOptimal() {
    console.log('=== Test sa optimalnim threshold-ima ===\n');
    console.log('Cilj: 3 parking mesta, 2 automobila, 1 prazno mesto\n');

    const testImagePath = path.join(__dirname, '..', 'uploads', 'test_slika1.png');
    
    if (!fs.existsSync(testImagePath)) {
        console.error(`✗ Slika nije pronađena: ${testImagePath}`);
        return;
    }

    // Testiraj različite kombinacije
    const tests = [
        { car: 0.2, parking: 0.5, desc: 'Niži za automobile (0.2), viši za parking mesta (0.5)' },
        { car: 0.15, parking: 0.6, desc: 'Još niži za automobile (0.15), još viši za parking (0.6)' },
        { car: 0.25, parking: 0.5, desc: 'Srednji za automobile (0.25), viši za parking (0.5)' },
    ];

    for (const test of tests) {
        console.log(`\n--- Test: ${test.desc} ---`);
        try {
            const options = {
                confidenceThreshold: 0.2, // Base threshold
                iouThreshold: 0.3,
                carThreshold: test.car,
                parkingThreshold: test.parking
            };
            
            const result = await mlInferenceService.analyzeImage(testImagePath, options);
            
            if (result.success) {
                console.log(`Parking mesta: ${result.total_spots} (treba 3)`);
                console.log(`Automobili: ${result.total_cars} (treba 2)`);
                console.log(`Slobodna: ${result.free_spaces}, Zauzeta: ${result.occupied_spaces}`);
                
                if (result.total_spots === 3 && result.total_cars === 2) {
                    console.log('✓ PERFEKTNO! Pronađeno tačno 3 mesta i 2 automobila!');
                    const formatted = mlInferenceService.formatResult(result);
                    console.log('\nDetalji:');
                    console.log(JSON.stringify(formatted, null, 2));
                    break;
                }
            }
        } catch (error) {
            console.error('✗ Greška:', error.message);
        }
    }
}

testOptimal().catch(console.error);

