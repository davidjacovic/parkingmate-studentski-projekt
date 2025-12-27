//
// Created by Windows11 on 27. 12. 2025.
//

#include "blockchain.h"

Block::Block(uint32_t index,
             const string& data,
             int64_t timestamp,
             const string& previousHash,
             uint32_t difficulty,
             uint64_t nonce)
    : index(index),
      data(data),
      timestamp(timestamp),
      previousHash(previousHash),
      difficulty(difficulty),
      nonce(nonce),
      hash("")
{

}
string Block::serialize() const {
    std::ostringstream ss;
    ss << index
       << data
       << timestamp
       << previousHash
       << difficulty
       << nonce;
    return ss.str();
}