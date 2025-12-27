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
    ostringstream ss;
    ss << index
       << data
       << timestamp
       << previousHash
       << difficulty
       << nonce;
    return ss.str();
}
string Block::toString() const {
    ostringstream ss;
    ss << "Block {\n"
       << "  index: " << index << "\n"
       << "  data: " << data << "\n"
       << "  timestamp: " << timestamp << "\n"
       << "  previousHash: " << previousHash << "\n"
       << "  difficulty: " << difficulty << "\n"
       << "  nonce: " << nonce << "\n"
       << "  hash: " << hash << "\n"
       << "}";
    return ss.str();

}
