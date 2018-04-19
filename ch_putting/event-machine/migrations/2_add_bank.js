// var Registration = artifacts.require("./v2/entity/Registration.sol");
var RegistrationRepository = artifacts.require("./v2/repository/RegistrationRepository.sol");
var TransferStateRepository = artifacts.require("./v2/repository/TransferStateRepository.sol");

module.exports = function(deployer) {
  deployer.deploy(TransferStateRepository);
  deployer.deploy(RegistrationRepository).then(function() {
  	RegistrationRepository.deployed().then(function(repo){
  		repo.register("+661234567", "faas",  "55700").then();
  		repo.register("+661111111", "whisk", "A1234").then();
  	});
  	/*repo.then(function(r){
  		r.register("+661234567", "faas",  "55700").then();
  		r.register("+661111111", "whisk", "A1234").then();
  	});*/
  });

  //deployer.deploy(BankRepository).then(function(){
  //  deployer.deploy(MobileOperator).then(function() {
  //		deployer.deploy(RegistrationRepository,
  //			BankRepository.address,
  //			MobileOperator.address);
  //	})
  //})
};
