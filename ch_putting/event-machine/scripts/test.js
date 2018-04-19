var RegistrationRepository = artifacts.require("./v2/repository/RegistrationRepository.sol");

module.exports = function(callback) {
  RegistrationRepository.deployed().then(function(repo) {
  	repo.findByTelNo("+661234567").then(function(r){
  		console.log(r);
  	})
  })
}
