# Blockchain REST API Specification

## Overview
This document defines the REST API contract for the ParkingMate Blockchain Service.
The service exposes endpoints for mining blocks, validating the blockchain, and
retrieving the current chain state.

---

## Models (JSON DTOs)

### Block(response model)
```json
{
  "index": 1,
  "data": "MPI-mined block #1",
  "timestamp": 1737000000,
  "previousHash": "00ab...ff",
  "difficulty": 4,
  "nonce": 123456,
  "hash": "0000cafe..."
}
```

### ChainStatus(GET /api/blockchain response)
```json
{
  "length": 12,
  "latestHash": "0000abcd...",
  "latestIndex": 11,
  "cumulativeWeight": "12345678901234567890",
  "chain": [ /* array of Block */ ]
}
```

### ValidateResponse(GET /api/blockchain/validate)
```json
{
  "valid": true
}
```

### MineRequest(POST /mine request)
```json
{
  "data": "event: accident at Koroska 11",
  "timestamp": 1737000000
}
```
### ErrorResponse (standard)
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Request is invalid",
  "details": [
    "data is required",
    "timestamp must be unix seconds"
  ]
}
```
## Endpoints

This section defines the REST endpoints exposed by the Blockchain service,
including their purpose, HTTP status codes, and response formats.

---

### GET /api/blockchain

**Description:**  
Returns the current state of the blockchain (in-memory chain snapshot).

**Response statuses:**

**200 OK**  
Returns a `ChainStatus` object.

```json
{
  "length": 12,
  "latestIndex": 11,
  "latestHash": "0000abcd...",
  "cumulativeWeight": "12345678901234567890",
  "chain": [
    {
      "index": 0,
      "data": "Genesis Block",
      "timestamp": 1736990000,
      "previousHash": "0",
      "difficulty": 1,
      "nonce": 0,
      "hash": "00genesis..."
    }
  ]
}
```

**500 Internal Server Error**  
Returns a standard ErrorResponse.

```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "Failed to retrieve blockchain state"
}
```


### GET /api/blockchain/validate

**Description:**  
Validates the blockchain integrity.

**Response statuses:**

**200 OK**
Returns a ValidateResponse object

```json 
{
  "valid": true
}
```
**500 Internal Server Error**  
Returns a standard ErrorResponse.

```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "Blockchain validation failed"
}
```


### POST /api/blockchain/mine

**Description:**  
Mines a new block using the Proof-of-Work algorithm and appends it to the blockchain.
**Request body:**
```json
{
  "data": "event: accident at Koroska 11",
  "timestamp": 1737000000
}
```

**Response statuses:**

**201 Created**  
Successfully mined block (MineResponse).

```json
{
  "index": 12,
  "data": "event: accident at Koroska 11",
  "timestamp": 1737000000,
  "previousHash": "0000abcd...",
  "difficulty": 4,
  "nonce": 982341,
  "hash": "0000cafe..."
}

```

**400 Bad Request**  
Invalid request (e.g. missing data or invalid timestamp).

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid request body",
  "details": [
    "data is required",
    "timestamp must be unix seconds"
  ]
}

```


**409 Conflict**  
A mining operation is already in progress and parallel mining requests are not allowed.

```json
{
  "errorCode": "MINING_IN_PROGRESS",
  "message": "Mining operation already in progress"
}
```

**500 Internal Server Error**  
Mining failed due to an internal server error.

```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "Mining failed due to server error"
}

```