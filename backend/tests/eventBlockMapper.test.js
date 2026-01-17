/**
 * EPIK 3.1: Testovi za eventBlockMapper
 * 
 * Unit testovi za validaciju i mapiranje Event ↔ Block.Data
 */

const eventBlockMapper = require('../services/eventBlockMapper');
const mongoose = require('mongoose');

// Helper funkcija za kreiranje validnog Event-a
function createValidEvent() {
    return {
        _id: new mongoose.Types.ObjectId(),
        topic: 'Parking Full',
        message: 'Parking lot at location X is full',
        timestamp: new Date('2024-01-15T10:30:00Z'),
        location: '46.0569,14.5058',
        eventType: 'PARKING_FULL',
        status: 'PENDING'
    };
}

// Test validacije Event-a
console.log('=== Test validacije Event-a ===');

const validEvent = createValidEvent();
const validationResult = eventBlockMapper.validateEventForBlockchain(validEvent);
console.log('Valid Event:', validationResult.valid ? '✓ PASS' : '✗ FAIL');
console.log('Errors:', validationResult.errors);

// Test validacije - nedostaje polje
const invalidEvent = { ...createValidEvent() };
delete invalidEvent.topic;
const invalidValidation = eventBlockMapper.validateEventForBlockchain(invalidEvent);
console.log('\nInvalid Event (missing topic):', invalidValidation.valid ? '✗ FAIL' : '✓ PASS');
console.log('Errors:', invalidValidation.errors);

// Test serializacije Event → Block.Data
console.log('\n=== Test serializacije Event → Block.Data ===');

const serializationResult = eventBlockMapper.eventToBlockData(validEvent);
if (serializationResult.success) {
    console.log('✓ PASS - Serializacija uspešna');
    console.log('Block.Data length:', serializationResult.data.length);
    console.log('Block.Data preview:', serializationResult.data.substring(0, 100) + '...');
    
    // Test deserializacije Block.Data → Event
    console.log('\n=== Test deserializacije Block.Data → Event ===');
    const deserializationResult = eventBlockMapper.blockDataToEvent(serializationResult.data);
    
    if (deserializationResult.success) {
        console.log('✓ PASS - Deserializacija uspešna');
        console.log('Event data:', deserializationResult.data);
        
        // Proveri da li se podatci poklapaju
        const dataMatches = 
            deserializationResult.data.eventId === validEvent._id.toString() &&
            deserializationResult.data.topic === validEvent.topic &&
            deserializationResult.data.message === validEvent.message &&
            deserializationResult.data.location === validEvent.location &&
            deserializationResult.data.eventType === validEvent.eventType;
        
        console.log('Podatci se poklapaju:', dataMatches ? '✓ PASS' : '✗ FAIL');
    } else {
        console.log('✗ FAIL - Deserializacija neuspešna');
        console.log('Errors:', deserializationResult.errors);
    }
} else {
    console.log('✗ FAIL - Serializacija neuspešna');
    console.log('Errors:', serializationResult.errors);
}

// Test validacije Block.Data
console.log('\n=== Test validacije Block.Data ===');

const validBlockData = '{"eventId":"64a9b8c2f0a5c1234567890b","topic":"Parking Full","message":"Parking lot full","timestamp":1705315800000,"location":"46.0569,14.5058","eventType":"PARKING_FULL"}';
const blockDataValidation = eventBlockMapper.validateBlockData(validBlockData);
console.log('Valid Block.Data:', blockDataValidation.valid ? '✓ PASS' : '✗ FAIL');
console.log('Errors:', blockDataValidation.errors);

// Test invalid Block.Data
const invalidBlockData = '{"eventId":"invalid"}';
const invalidBlockDataValidation = eventBlockMapper.validateBlockData(invalidBlockData);
console.log('\nInvalid Block.Data:', invalidBlockDataValidation.valid ? '✗ FAIL' : '✓ PASS');
console.log('Errors:', invalidBlockDataValidation.errors);

// Test timestamp konverzije
console.log('\n=== Test timestamp konverzije ===');

const eventDate = new Date('2024-01-15T10:30:00Z');
const blockTimestamp = eventBlockMapper.eventTimestampToBlockTimestamp(eventDate);
const backToEventDate = eventBlockMapper.blockTimestampToEventTimestamp(blockTimestamp);

console.log('Original Date:', eventDate.toISOString());
console.log('Block timestamp (Unix seconds):', blockTimestamp);
console.log('Back to Date:', backToEventDate.toISOString());
console.log('Timestamp konverzija:', eventDate.getTime() === backToEventDate.getTime() ? '✓ PASS' : '✗ FAIL');

// Test sa različitim Event tipovima
console.log('\n=== Test sa različitim Event tipovima ===');

const eventTypes = ['PARKING_FULL', 'PARKING_AVAILABLE', 'LOW_AVAILABILITY'];
eventTypes.forEach(eventType => {
    const event = { ...createValidEvent(), eventType };
    const result = eventBlockMapper.eventToBlockData(event);
    console.log(`${eventType}:`, result.success ? '✓ PASS' : '✗ FAIL');
});

// Test maksimalne velikosti
console.log('\n=== Test maksimalne velikosti Block.Data ===');

const largeEvent = {
    ...createValidEvent(),
    message: 'A'.repeat(2000) // Veoma duga poruka
};
const largeResult = eventBlockMapper.eventToBlockData(largeEvent);
console.log('Large Event:', largeResult.success ? '✗ FAIL (trebalo bi da fail-uje)' : '✓ PASS');
console.log('Errors:', largeResult.errors);

console.log('\n=== Testovi završeni ===');

