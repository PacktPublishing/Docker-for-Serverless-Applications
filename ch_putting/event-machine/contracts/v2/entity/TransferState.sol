pragma solidity ^0.4.13;

contract TransferState {

	enum State { NONE, STARTED, PENDING, COMPLETED }

	string txId;
	State state;

	function TransferState(string _txId) {
		state = State.NONE;
		txId = _txId;
	}

	function start() public {
		require(state == State.NONE);
		state = State.STARTED;
	}

	function pending() public {
		require(state == State.STARTED);
		state = State.PENDING;
	}

	function complete() public {
		require(state == State.PENDING);
		state = State.COMPLETED;
	}

	function currentState() public constant returns (uint8) {
		return uint8(state);
	}

}