pragma solidity ^0.4.13;

import "../entity/Bank.sol";

contract BankRepository {

	mapping(bytes32 => address) private banks;

	event BankCreated(address bank, string name);
	event BankFound(address bank, string name);

	function create(string name) public returns (address) {
		address b = new Bank(name);
		banks[sha3(name)] = b;
		BankCreated(b, name);
		return b;
	}

	function findByName(string name) public returns (Bank) {
		address b = banks[sha3(name)];
		BankFound(b, name);
		return Bank(b);
	}

}