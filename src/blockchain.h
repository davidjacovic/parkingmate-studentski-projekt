//
// Created by Windows11 on 27. 12. 2025.
//

#ifndef PARKINGMATE_STUDENTSKI_PROJEKT_BLOCKCHAIN_H
#define PARKINGMATE_STUDENTSKI_PROJEKT_BLOCKCHAIN_H


#include <string>
#include <cstdint>
#include <sstream>
using namespace std;

class Block {
public:
    uint32_t index;
    string data;
    int64_t timestamp;
    string previousHash;
    uint32_t difficulty;
    uint64_t nonce;
    string hash;

    Block(uint32_t index,
          const std::string& data,
          int64_t timestamp,
          const std::string& previousHash,
          uint32_t difficulty,
          uint64_t nonce);

    string serialize() const;
};


#endif //PARKINGMATE_STUDENTSKI_PROJEKT_BLOCKCHAIN_H