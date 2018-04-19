pragma solidity ^0.4.13;

import "./BankAccount.sol";

contract Bank {

	string public name;
	mapping(bytes32 => address) accounts;
	event BankAccountCreated(address bank, address bankAccount);
	event BankAccountFound(address bank, address bankAccount);

	function Bank(string _name) public {
		name = _name;
	}

	function createAccount(string id, uint initAmount) public returns (address) {
		address ba = new BankAccount(id, initAmount);
		accounts[sha3(id)] = ba;
		BankAccountCreated(this, ba);
		return ba;
	}

	function findAccountByNumber(string name) public returns (BankAccount) {
		BankAccount ba = BankAccount(accounts[sha3(name)]);
		BankAccountFound(this, ba);
		return ba;
	}

}