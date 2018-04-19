pragma solidity ^0.4.13;

import "../entity/TransferState.sol";

contract TransferStateRepository {

    event TransferStarted(string txId);
    event TransferPending(string txId);
    event TransferCompleted(string txId);

    mapping(bytes32 => address) states;

    function start(string txId) public {
        bytes32 key = sha256(txId);
        require(states[key] == address(0x0));

        TransferState tx = new TransferState(txId);
        states[key] = address(tx);

        tx.start();
        TransferStarted(txId);
    }

    function pending(string txId) public {
        bytes32 key = sha256(txId);
        require(states[key] != address(0x0));
        TransferState tx = TransferState(states[key]);

        tx.pending();
        TransferPending(txId);
    }

    function complete(string txId) public {
        bytes32 key = sha256(txId);
        require(states[key] != address(0x0));
        TransferState tx = TransferState(states[key]);

        tx.complete();
        TransferCompleted(txId);
    }

    function getStateOf(string txId) public constant returns (string) {
        bytes32 key = sha256(txId);
        require(states[key] != address(0x0));
        TransferState tx = TransferState(states[key]);
        uint8 state = tx.currentState();

        if (state == 0) return "NONE";
        else if (state == 1) return "STARTED";
        else if (state == 2) return "PENDING";
        else if (state == 3) return "COMPLETED";
    }


}