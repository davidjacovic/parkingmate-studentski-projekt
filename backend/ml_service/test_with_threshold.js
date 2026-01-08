/**
 * Test script sa prilagodljivim threshold-ima
 * Usage: node ml_service/test_with_threshold.js [confidence] [iou]
 * Example: node ml_service/test_with_threshold.js 0.3 0.3
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

async function testWithThreshold() {
    // Uzmi parametre iz command line ili koristi default
    const confidenceThreshold = parseFloat(process.argv[2]) || 0.3;
    const iouThreshold = parseFloat(process.argv[3]) || 0.3;
    const imageName = process.argv[4] || 'test_slika1.png';
    
    console.log('=== Test ML Servisa sa prilagodljivim threshold-ima ===\n');
    console.log(`Confidence threshold: ${confidenceThreshold}`);
    console.log(`IoU threshold: ${iouThreshold}`);
    console.log(`Slika: ${imageName}\n`);

    const testImagePath = path.join(__dirname, '..', 'uploads', imageName);
    
    if (!fs.existsSync(testImagePath)) {
        console.error(`✗ Slika nije pronađena: ${testImagePath}`);
        console.log('\nDostupne slike u uploads/:');
        const uploadsDir = path.join(__dirname, '..', 'uploads');
        const files = fs.readdirSync(uploadsDir).filter(f => 
            f.match(/\.(jpg|jpeg|png|bmp)$/i)
        );
        files.forEach(f => console.log(`  - ${f}`));
        return;
    }

    try {
        console.log(`Analiziram sliku: ${testImagePath}\n`);
        
        const options = {
            confidenceThreshold: confidenceThreshold,
            iouThreshold: iouThreshold
        };
        
        const result = await mlInferenceService.analyzeImage(testImagePath, options);
        
        if (result.success) {
            console.log('✓ Analiza uspešna!\n');
            console.log('Rezultati:');
            console.log(`  - Ukupno parking mesta: ${result.total_spots}`);
            console.log(`  - Slobodna mesta: ${result.free_spaces}`);
            console.log(`  - Zauzeta mesta: ${result.occupied_spaces}`);
            console.log(`  - Ukupno automobila: ${result.total_cars}`);
            console.log(`  - Ukupno detekcija: ${result.all_detections.length}`);
            
            if (result.all_detections.length > 0) {
                console.log('\nDetalji detekcija:');
                result.all_detections.forEach((det, idx) => {
                    console.log(`  ${idx + 1}. ${det.class_name} (confidence: ${(det.confidence * 100).toFixed(1)}%)`);
                });
            }
            
            const formatted = mlInferenceService.formatResult(result);
            console.log('\nFormatirani rezultat:');
            console.log(JSON.stringify(formatted, null, 2));
            
        } else {
            console.error('✗ Analiza nije uspela:', result.error);
        }
    } catch (error) {
        console.error('✗ Greška pri analizi:', error.message);
    }

    console.log('\n=== Test završen ===');
    console.log('\nSaveti za bolje prepoznavanje:');
    console.log('  - Smanji confidence threshold (0.2-0.3) za više detekcija');
    console.log('  - Povećaj confidence threshold (0.6-0.8) za sigurnije detekcije');
    console.log('  - Smanji IoU threshold (0.2) ako automobili nisu dovoljno dobro detektovani');
    console.log('\nPrimer: node ml_service/test_with_threshold.js 0.25 0.25 test_slika2.png');
}

testWithThreshold().catch(console.error);

