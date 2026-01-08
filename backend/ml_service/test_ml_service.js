/**
 * Test script za ML servis
 * Usage: node ml_service/test_ml_service.js
 */

const mlInferenceService = require('./mlInferenceService');
const path = require('path');
const fs = require('fs');

async function testMLService() {
    console.log('=== Test ML Servisa ===\n');

    // Test 1: Health Check
    console.log('1. Testiranje health check...');
    try {
        await mlInferenceService.checkDependencies();
        console.log('✓ Python zavisnosti su instalirane\n');
    } catch (error) {
        console.error('✗ Greška:', error.message);
        console.log('\nInstaliraj zavisnosti sa: pip install -r backend/ml_service/requirements.txt\n');
        return;
    }

    // Test 2: Provera modela
    console.log('2. Provera YOLO modela...');
    const modelPath = mlInferenceService.modelPath;
    if (fs.existsSync(modelPath)) {
        console.log(`✓ Model pronađen: ${modelPath}\n`);
    } else {
        console.error(`✗ Model nije pronađen: ${modelPath}`);
        console.log('Kopiraj best.pt u backend/models/best.pt\n');
        return;
    }

    // Test 3: Testiranje analize slike
    console.log('3. Testiranje analize slike...');
    const testImagePath = path.join(__dirname, '..', 'uploads', 'test_slika3.png');
    
    if (!fs.existsSync(testImagePath)) {
        console.error(`✗ Test slika nije pronađena: ${testImagePath}`);
        console.log('Koristi neku drugu sliku iz backend/uploads/\n');
        return;
    }

    try {
        console.log(`Analiziram sliku: ${testImagePath}`);
        
        // Test sa nižim confidence threshold za bolje prepoznavanje
        // Možeš eksperimentisati sa različitim vrednostima:
        // - 0.3 = više detekcija (ali možda i lažnih)
        // - 0.5 = default (balans)
        // - 0.7 = manje detekcija (ali sigurnije)
        const options = {
            confidenceThreshold: 0.3,  // Niži threshold za više detekcija
            iouThreshold: 0.3
        };
        
        console.log(`Koristim confidence threshold: ${options.confidenceThreshold}`);
        const result = await mlInferenceService.analyzeImage(testImagePath, options);
        
        if (result.success) {
            console.log('✓ Analiza uspešna!\n');
            console.log('Rezultati:');
            console.log(`  - Ukupno parking mesta: ${result.total_spots}`);
            console.log(`  - Slobodna mesta: ${result.free_spaces}`);
            console.log(`  - Zauzeta mesta: ${result.occupied_spaces}`);
            console.log(`  - Ukupno automobila: ${result.total_cars}`);
            
            const formatted = mlInferenceService.formatResult(result);
            console.log('\nFormatirani rezultat:');
            console.log(JSON.stringify(formatted, null, 2));
            
            // Test logovanja
            console.log('\n4. Testiranje logovanja...');
            await mlInferenceService.logAnalysis(testImagePath, formatted, result);
            console.log('✓ Logovanje uspešno! Proveri backend/logs/ml_analysis.log\n');
            
        } else {
            console.error('✗ Analiza nije uspela:', result.error);
        }
    } catch (error) {
        console.error('✗ Greška pri analizi:', error.message);
        if (error.stack) {
            console.error('Stack trace:', error.stack);
        }
    }

    console.log('\n=== Test završen ===');
}

// Pokreni test
testMLService().catch(console.error);

