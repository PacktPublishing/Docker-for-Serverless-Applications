// var Registration = artifacts.require("./v2/entity/Registration.sol");
var RegistrationRepository = artifacts.require("./v2/repository/RegistrationRepository.sol");
var TransferStateRepository = artifacts.require("./v2/repository/TransferStateRepository.sol");

module.exports = function(deployer) {
  deployer.deploy(RegistrationRepository);
  deployer.deploy(TransferStateRepository);
  //deployer.deploy(BankRepository).then(function(){
  //  deployer.deploy(MobileOperator).then(function() {
  //		deployer.deploy(RegistrationRepository,
  //			BankRepository.address,
  //			MobileOperator.address);
  //	})
  //})
};
