pragma solidity ^0.4.13;

import "../entity/Bank.sol";
import "../entity/BankAccount.sol";
import "../entity/MobileNumber.sol";
import "../entity/MobileOperator.sol";

import "./BankRepository.sol";

contract RegistrationRepository {

	mapping(bytes32 => address) private registrations;
	event Registered(address mobileNumber, address bankAccount);
	event BankAccountFound(string telNo, address bankAccount);

	BankRepository bankRepository;
	MobileOperator mobileOperator;

	function RegistrationRepository(address _br, address _mo) {
		bankRepository = BankRepository(_br);
		mobileOperator = MobileOperator(_mo);
	}

	function register(string telNo, string bankName, string accNo) public {
		Bank bank = bankRepository.findByName(bankName);
		BankAccount bankAccount = bank.findAccountByNumber(accNo);
		MobileNumber mobileNumber = mobileOperator.findByNumber(telNo);

		registrations[sha3(telNo)] = bankAccount;
		Registered(mobileNumber, bankAccount);
	}

	function findBankAccountByTelNo(string telNo) public returns (address) {
		BankAccount ba = BankAccount(registrations[sha3(telNo)]);
		BankAccountFound(telNo, ba);
		return ba;
	}

}