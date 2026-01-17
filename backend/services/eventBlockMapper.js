/**
 * EPIK 3.1: Zasnova zapisa dogodkov v blockchain
 * 
 * Helper funkcije za mapiranje Event dokumentov (MongoDB) ↔ Block.Data (Blockchain)
 * 
 * @module eventBlockMapper
 */

const mongoose = require('mongoose');

// Dozvoljene vrednosti za eventType
const VALID_EVENT_TYPES = ['PARKING_FULL', 'PARKING_AVAILABLE', 'LOW_AVAILABILITY'];

// Maksimalna velikost Block.Data stringa (definisano v blockchain servisu)
const MAX_BLOCK_DATA_LENGTH = 1024;

/**
 * Validira Event dokument za zapisovanje v blockchain.
 * 
 * @param {Object} event - Event dokument iz MongoDB
 * @returns {Object} { valid: boolean, errors: string[] }
 */
function validateEventForBlockchain(event) {
    const errors = [];

    if (!event) {
        errors.push('Event je obavezan');
        return { valid: false, errors };
    }

    // Validacija _id
    if (!event._id) {
        errors.push('Event mora imati _id');
    } else {
        // Proveri da li je validan ObjectId
        if (!mongoose.Types.ObjectId.isValid(event._id)) {
            errors.push('Event._id mora biti validan MongoDB ObjectId');
        }
    }

    // Validacija topic
    if (!event.topic || typeof event.topic !== 'string' || event.topic.trim().length === 0) {
        errors.push('Event.topic mora biti non-empty string');
    }

    // Validacija message
    if (!event.message || typeof event.message !== 'string' || event.message.trim().length === 0) {
        errors.push('Event.message mora biti non-empty string');
    }

    // Validacija timestamp
    if (!event.timestamp) {
        errors.push('Event.timestamp je obavezan');
    } else {
        // Proveri da li je validan Date
        const timestamp = event.timestamp instanceof Date ? event.timestamp : new Date(event.timestamp);
        if (isNaN(timestamp.getTime())) {
            errors.push('Event.timestamp mora biti validan Date objekat');
        }
    }

    // Validacija location
    if (!event.location || typeof event.location !== 'string') {
        errors.push('Event.location mora biti string');
    } else {
        // Format: "latitude,longitude"
        const locationPattern = /^-?\d+\.?\d*,-?\d+\.?\d*$/;
        if (!locationPattern.test(event.location.trim())) {
            errors.push('Event.location mora biti u formatu "latitude,longitude"');
        }
    }

    // Validacija eventType
    if (!event.eventType || typeof event.eventType !== 'string') {
        errors.push('Event.eventType je obavezan');
    } else {
        if (!VALID_EVENT_TYPES.includes(event.eventType)) {
            errors.push(`Event.eventType mora biti jedna od: ${VALID_EVENT_TYPES.join(', ')}`);
        }
    }

    return {
        valid: errors.length === 0,
        errors
    };
}

/**
 * Validira Block.Data string.
 * 
 * @param {string} blockData - JSON string koji predstavlja Block.Data
 * @returns {Object} { valid: boolean, errors: string[] }
 */
function validateBlockData(blockData) {
    const errors = [];

    if (!blockData || typeof blockData !== 'string') {
        errors.push('Block.Data mora biti string');
        return { valid: false, errors };
    }

    // Validacija velikosti
    if (blockData.length > MAX_BLOCK_DATA_LENGTH) {
        errors.push(`Block.Data mora biti <= ${MAX_BLOCK_DATA_LENGTH} karaktera (trenutno: ${blockData.length})`);
    }

    // Proveri da li je validan JSON
    let parsedData;
    try {
        parsedData = JSON.parse(blockData);
    } catch (e) {
        errors.push(`Block.Data mora biti validan JSON string: ${e.message}`);
        return { valid: false, errors };
    }

    // Validacija obaveznih polja
    const requiredFields = ['eventId', 'topic', 'message', 'timestamp', 'location', 'eventType'];
    for (const field of requiredFields) {
        if (!(field in parsedData)) {
            errors.push(`Block.Data mora sadržati polje "${field}"`);
        }
    }

    // Validacija eventId
    if (parsedData.eventId) {
        if (typeof parsedData.eventId !== 'string') {
            errors.push('Block.Data.eventId mora biti string');
        } else if (!mongoose.Types.ObjectId.isValid(parsedData.eventId)) {
            errors.push('Block.Data.eventId mora biti validan MongoDB ObjectId format');
        }
    }

    // Validacija topic
    if (parsedData.topic && (typeof parsedData.topic !== 'string' || parsedData.topic.trim().length === 0)) {
        errors.push('Block.Data.topic mora biti non-empty string');
    }

    // Validacija message
    if (parsedData.message && (typeof parsedData.message !== 'string' || parsedData.message.trim().length === 0)) {
        errors.push('Block.Data.message mora biti non-empty string');
    }

    // Validacija timestamp
    if (parsedData.timestamp !== undefined) {
        if (typeof parsedData.timestamp !== 'number' || parsedData.timestamp < 0) {
            errors.push('Block.Data.timestamp mora biti validan Unix timestamp (number)');
        }
    }

    // Validacija location
    if (parsedData.location) {
        if (typeof parsedData.location !== 'string') {
            errors.push('Block.Data.location mora biti string');
        } else {
            const locationPattern = /^-?\d+\.?\d*,-?\d+\.?\d*$/;
            if (!locationPattern.test(parsedData.location.trim())) {
                errors.push('Block.Data.location mora biti u formatu "latitude,longitude"');
            }
        }
    }

    // Validacija eventType
    if (parsedData.eventType) {
        if (typeof parsedData.eventType !== 'string') {
            errors.push('Block.Data.eventType mora biti string');
        } else if (!VALID_EVENT_TYPES.includes(parsedData.eventType)) {
            errors.push(`Block.Data.eventType mora biti jedna od: ${VALID_EVENT_TYPES.join(', ')}`);
        }
    }

    return {
        valid: errors.length === 0,
        errors
    };
}

/**
 * Serializuje Event dokument u Block.Data JSON string.
 * 
 * @param {Object} event - Event dokument iz MongoDB
 * @returns {Object} { success: boolean, data: string, errors: string[] }
 */
function eventToBlockData(event) {
    // Validacija
    const validation = validateEventForBlockchain(event);
    if (!validation.valid) {
        return {
            success: false,
            data: null,
            errors: validation.errors
        };
    }

    try {
        // Konvertuj Event u Block.Data format
        const eventTimestamp = event.timestamp instanceof Date 
            ? event.timestamp.getTime() 
            : new Date(event.timestamp).getTime();

        const blockDataObj = {
            eventId: event._id.toString(), // ObjectId → string
            topic: event.topic.trim(),
            message: event.message.trim(),
            timestamp: eventTimestamp, // Date → Unix timestamp (ms)
            location: event.location.trim(),
            eventType: event.eventType
        };

        // Serializuj u JSON string
        const blockDataString = JSON.stringify(blockDataObj);

        // Proveri velikost
        if (blockDataString.length > MAX_BLOCK_DATA_LENGTH) {
            return {
                success: false,
                data: null,
                errors: [`Block.Data prelazi maksimalnu velikost (${MAX_BLOCK_DATA_LENGTH} karaktera). Trenutno: ${blockDataString.length} karaktera`]
            };
        }

        return {
            success: true,
            data: blockDataString,
            errors: []
        };
    } catch (error) {
        return {
            success: false,
            data: null,
            errors: [`Greška pri serializaciji: ${error.message}`]
        };
    }
}

/**
 * Deserializuje Block.Data JSON string u Event podatke.
 * 
 * @param {string} blockData - JSON string koji predstavlja Block.Data
 * @returns {Object} { success: boolean, data: Object, errors: string[] }
 */
function blockDataToEvent(blockData) {
    // Validacija
    const validation = validateBlockData(blockData);
    if (!validation.valid) {
        return {
            success: false,
            data: null,
            errors: validation.errors
        };
    }

    try {
        // Deserializuj JSON string
        const eventData = JSON.parse(blockData);

        // Vrati Event podatke
        return {
            success: true,
            data: {
                eventId: eventData.eventId,
                topic: eventData.topic,
                message: eventData.message,
                timestamp: new Date(eventData.timestamp), // Unix timestamp (ms) → Date
                location: eventData.location,
                eventType: eventData.eventType
            },
            errors: []
        };
    } catch (error) {
        return {
            success: false,
            data: null,
            errors: [`Greška pri deserializaciji: ${error.message}`]
        };
    }
}

/**
 * Konvertuje Block.timestamp (Unix seconds) u Event.timestamp (Date).
 * 
 * @param {number} blockTimestamp - Unix timestamp u sekundama
 * @returns {Date} JavaScript Date objekat
 */
function blockTimestampToEventTimestamp(blockTimestamp) {
    return new Date(blockTimestamp * 1000);
}

/**
 * Konvertuje Event.timestamp (Date) u Block.timestamp (Unix seconds).
 * 
 * @param {Date} eventTimestamp - JavaScript Date objekat
 * @returns {number} Unix timestamp u sekundama
 */
function eventTimestampToBlockTimestamp(eventTimestamp) {
    const date = eventTimestamp instanceof Date ? eventTimestamp : new Date(eventTimestamp);
    return Math.floor(date.getTime() / 1000);
}

module.exports = {
    eventToBlockData,
    blockDataToEvent,
    validateEventForBlockchain,
    validateBlockData,
    blockTimestampToEventTimestamp,
    eventTimestampToBlockTimestamp,
    VALID_EVENT_TYPES,
    MAX_BLOCK_DATA_LENGTH
};

