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

### ValidateResponse(GET /validate response)
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
