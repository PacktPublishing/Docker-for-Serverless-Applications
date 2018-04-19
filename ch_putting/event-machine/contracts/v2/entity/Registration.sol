pragma solidity ^0.4.13;

contract Registration {

	bytes32 public telNo;
	bytes32 public bank;
	bytes32 public accNo;

    function s2b32(string memory source) constant private returns (bytes32 result) {
        bytes memory tempEmptyStringTest = bytes(source);
        if (tempEmptyStringTest.length == 0) {
            return 0x0;
        }

        assembly {
            result := mload(add(source, 32))
        }
    }

	function Registration(string _telNo, string _bank, string _accNo) public {
		telNo = s2b32(_telNo);
		bank  = s2b32(_bank);
		accNo = s2b32(_accNo);
	}

}