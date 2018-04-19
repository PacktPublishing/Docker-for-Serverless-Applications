pragma solidity ^0.4.13;

import "./MobileNumber.sol";

contract MobileOperator {

	function findByNumber(string telNo) public returns(MobileNumber) {
		return MobileNumber(address(0x0));
	}

}