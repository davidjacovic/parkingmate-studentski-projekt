#include <iostream>
#include <ctime>

#include "blockchain.h"

using namespace std;

int main() {
    Block testBlock(
        1,
        "Test data",
        time(nullptr),
        "0000000000000000",
        3,
        42
    );

    cout << "Serialized block:" << endl;
    cout << testBlock.serialize() << endl << endl;

    cout << "Debug output:" << endl;
    cout << testBlock.toString() << endl;

    return 0;
}
