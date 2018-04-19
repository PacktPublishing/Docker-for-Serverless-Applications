pragma solidity ^0.4.13;

import "../entity/Registration.sol";

contract RegistrationRepository {

    mapping(bytes32 => address) registrations;

    event Registered(string telNo, address registration);
    event RegistrationFound(string telNo, string bank, string accNo);
    event AlreadyExisted(string telNo);

    function register(string telNo, string bank, string accNo) public {
        if (registrations[sha3(telNo)] != address(0x0)) {
            AlreadyExisted(telNo);
            return;
        }

        Registration r = new Registration(telNo, bank, accNo);
        registrations[sha3(telNo)] = address(r);

        Registered(telNo, address(r));
    }

    function to_s(bytes32 x) constant private returns (string) {
        bytes memory bytesString = new bytes(32);
        uint charCount = 0;
        for (uint j = 0; j < 32; j++) {
            byte char = byte(bytes32(uint(x) * 2 ** (8 * j)));
            if (char != 0) {
                bytesString[charCount] = char;
                charCount++;
            }
        }
        bytes memory bytesStringTrimmed = new bytes(charCount);
        for (j = 0; j < charCount; j++) {
            bytesStringTrimmed[j] = bytesString[j];
        }
        return string(bytesStringTrimmed);
    }

    function findByTelNo(string telNo) public returns (address) {
        Registration r = Registration(registrations[sha3(telNo)]);
        RegistrationFound(telNo, to_s(r.bank()), to_s(r.accNo()));

        return address(r);
    }

}