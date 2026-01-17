/**
 * EPIK 3.2: Implementacija zapisa dogodkov v blockchain
 * 
 * Servis za komunikacijo sa blockchain API-jem (ASP.NET Core Web API)
 * 
 * @module blockchainService
 */

const axios = require('axios');
const eventBlockMapper = require('./eventBlockMapper');

// URL blockchain servisa (može se postaviti preko environment varijable)
const BLOCKCHAIN_SERVICE_URL = process.env.BLOCKCHAIN_SERVICE_URL || 'http://localhost:5024';

// Timeout za HTTP zahteve (milisekunde)
const HTTP_TIMEOUT = 30000; // 30 sekundi

/**
 * Zabeleži Event v blockchain preko blockchain servisa.
 * 
 * @param {Object} event - Event dokument iz MongoDB
 * @returns {Promise<Object>} { success: boolean, blockchainHash: string, blockchainTimestamp: Date, blockIndex: number, errors: string[] }
 */
async function recordEventInBlockchain(event) {
    try {
        // Validacija Event-a za blockchain
        const validation = eventBlockMapper.validateEventForBlockchain(event);
        if (!validation.valid) {
            return {
                success: false,
                blockchainHash: null,
                blockchainTimestamp: null,
                blockIndex: null,
                errors: validation.errors
            };
        }

        // Serializuj Event u Block.Data format
        const blockDataResult = eventBlockMapper.eventToBlockData(event);
        if (!blockDataResult.success) {
            return {
                success: false,
                blockchainHash: null,
                blockchainTimestamp: null,
                blockIndex: null,
                errors: blockDataResult.errors
            };
        }

        // Konvertuj Event.timestamp (Date) u Block.timestamp (Unix seconds)
        const blockTimestamp = eventBlockMapper.eventTimestampToBlockTimestamp(event.timestamp);

        // Pozovi blockchain servis
        const response = await axios.post(
            `${BLOCKCHAIN_SERVICE_URL}/api/blockchain/mine`,
            {
                data: blockDataResult.data,
                timestamp: blockTimestamp
            },
            {
                timeout: HTTP_TIMEOUT,
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        );

        // Ekstraktuj podatke iz odgovora
        const minedBlock = response.data;

        // Konvertuj Block.timestamp (Unix seconds) u Date
        const blockchainTimestamp = eventBlockMapper.blockTimestampToEventTimestamp(minedBlock.timestamp);

        return {
            success: true,
            blockchainHash: minedBlock.hash,
            blockchainTimestamp: blockchainTimestamp,
            blockIndex: minedBlock.index,
            errors: []
        };
    } catch (error) {
        // Obravnava različite tipove napak
        let errorMessage = 'Unknown error';
        
        if (error.response) {
            // Server je odgovorio sa status kodom van opsega 2xx
            const statusCode = error.response.status;
            const statusText = error.response.statusText;
            const responseData = error.response.data;

            if (statusCode === 409) {
                // Mining je već u toku (Conflict)
                errorMessage = `Mining operation already in progress (${statusCode} ${statusText})`;
            } else if (statusCode === 400) {
                // Validaciona greška
                errorMessage = `Validation error: ${JSON.stringify(responseData)}`;
            } else {
                errorMessage = `Blockchain service error: ${statusCode} ${statusText} - ${JSON.stringify(responseData)}`;
            }
        } else if (error.request) {
            // Zahtev je poslat ali nema odgovora
            errorMessage = `Blockchain service unavailable (no response from ${BLOCKCHAIN_SERVICE_URL})`;
        } else {
            // Greška pri postavljanju zahteva
            errorMessage = `Error setting up request: ${error.message}`;
        }

        console.error('Error recording event in blockchain:', {
            eventId: event._id?.toString(),
            error: errorMessage,
            fullError: error
        });

        return {
            success: false,
            blockchainHash: null,
            blockchainTimestamp: null,
            blockIndex: null,
            errors: [errorMessage]
        };
    }
}

/**
 * Proverava da li je blockchain servis dostupan (health check).
 * 
 * @returns {Promise<Object>} { available: boolean, message: string }
 */
async function checkBlockchainServiceHealth() {
    try {
        const response = await axios.get(`${BLOCKCHAIN_SERVICE_URL}/health`, {
            timeout: 5000 // Kratak timeout za health check
        });

        if (response.status === 200 && response.data.status === 'ok') {
            return {
                available: true,
                message: 'Blockchain service is available'
            };
        } else {
            return {
                available: false,
                message: `Blockchain service returned unexpected response: ${response.status}`
            };
        }
    } catch (error) {
        return {
            available: false,
            message: `Blockchain service unavailable: ${error.message}`
        };
    }
}

/**
 * Validira blockchain lanac.
 * 
 * @returns {Promise<Object>} { valid: boolean, message: string }
 */
async function validateBlockchain() {
    try {
        const response = await axios.get(`${BLOCKCHAIN_SERVICE_URL}/api/blockchain/validate`, {
            timeout: HTTP_TIMEOUT
        });

        if (response.status === 200) {
            return {
                valid: response.data.valid === true,
                message: response.data.valid ? 'Blockchain is valid' : 'Blockchain validation failed'
            };
        } else {
            return {
                valid: false,
                message: `Blockchain validation returned unexpected response: ${response.status}`
            };
        }
    } catch (error) {
        return {
            valid: false,
            message: `Error validating blockchain: ${error.message}`
        };
    }
}

/**
 * Dohvata trenutno stanje blockchain lanca.
 * 
 * @returns {Promise<Object>} { success: boolean, chain: Array, errors: string[] }
 */
async function getBlockchainState() {
    try {
        const response = await axios.get(`${BLOCKCHAIN_SERVICE_URL}/api/blockchain`, {
            timeout: HTTP_TIMEOUT
        });

        if (response.status === 200) {
            return {
                success: true,
                chain: response.data,
                errors: []
            };
        } else {
            return {
                success: false,
                chain: null,
                errors: [`Unexpected response status: ${response.status}`]
            };
        }
    } catch (error) {
        return {
            success: false,
            chain: null,
            errors: [error.message]
        };
    }
}

/**
 * Verifikuje da li je blok sa određenim hash-om validan i prisutan u lancu.
 * 
 * @param {string} hash - Hash bloka za verifikaciju
 * @returns {Promise<Object>} { verified: boolean, found: boolean, integrityValid: boolean, chainValid: boolean, message: string, block: Object|null }
 */
async function verifyBlock(hash) {
    try {
        if (!hash || typeof hash !== 'string' || hash.trim().length === 0) {
            return {
                verified: false,
                found: false,
                integrityValid: false,
                chainValid: false,
                message: 'Hash is required',
                block: null
            };
        }

        const response = await axios.get(`${BLOCKCHAIN_SERVICE_URL}/api/blockchain/verify/${encodeURIComponent(hash)}`, {
            timeout: HTTP_TIMEOUT
        });

        if (response.status === 200) {
            return {
                verified: response.data.verified === true,
                found: response.data.found === true,
                integrityValid: response.data.integrityValid === true,
                chainValid: response.data.chainValid === true,
                message: response.data.message || '',
                block: response.data.block || null
            };
        } else {
            return {
                verified: false,
                found: false,
                integrityValid: false,
                chainValid: false,
                message: `Unexpected response status: ${response.status}`,
                block: null
            };
        }
    } catch (error) {
        let errorMessage = 'Unknown error';
        
        if (error.response) {
            const statusCode = error.response.status;
            errorMessage = `Blockchain service error: ${statusCode} ${error.response.statusText}`;
        } else if (error.request) {
            errorMessage = `Blockchain service unavailable (no response from ${BLOCKCHAIN_SERVICE_URL})`;
        } else {
            errorMessage = `Error setting up request: ${error.message}`;
        }

        return {
            verified: false,
            found: false,
            integrityValid: false,
            chainValid: false,
            message: errorMessage,
            block: null
        };
    }
}

/**
 * Pretražuje blokove po sadržaju (data).
 * 
 * @param {string} data - Podaci za pretragu
 * @returns {Promise<Object>} { success: boolean, blocks: Array, errors: string[] }
 */
async function searchBlocks(data) {
    try {
        if (!data || typeof data !== 'string' || data.trim().length === 0) {
            return {
                success: false,
                blocks: [],
                errors: ['Search data parameter is required']
            };
        }

        const response = await axios.get(`${BLOCKCHAIN_SERVICE_URL}/api/blockchain/search`, {
            params: { data },
            timeout: HTTP_TIMEOUT
        });

        if (response.status === 200) {
            return {
                success: true,
                blocks: Array.isArray(response.data) ? response.data : [],
                errors: []
            };
        } else {
            return {
                success: false,
                blocks: [],
                errors: [`Unexpected response status: ${response.status}`]
            };
        }
    } catch (error) {
        let errorMessage = 'Unknown error';
        
        if (error.response) {
            const statusCode = error.response.status;
            errorMessage = `Blockchain service error: ${statusCode} ${error.response.statusText}`;
        } else if (error.request) {
            errorMessage = `Blockchain service unavailable (no response from ${BLOCKCHAIN_SERVICE_URL})`;
        } else {
            errorMessage = `Error setting up request: ${error.message}`;
        }

        return {
            success: false,
            blocks: [],
            errors: [errorMessage]
        };
    }
}

module.exports = {
    recordEventInBlockchain,
    checkBlockchainServiceHealth,
    validateBlockchain,
    getBlockchainState,
    verifyBlock,
    searchBlocks,
    BLOCKCHAIN_SERVICE_URL
};

