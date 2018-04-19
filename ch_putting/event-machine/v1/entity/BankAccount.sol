pragma solidity ^0.4.13;

contract BankAccount {

	string id;
	uint balance;

	function BankAccount(string _id, uint initAmount) {
		id = _id;
		balance = initAmount;
	}

}