module.exports = {
  networks: {
    development: {
      host: "127.0.0.1",
      port: 8545,
      network_id: "*", // Match any network id
      from: "0x00a329c0648769A73afAc7F9381E08FB43dBEA72"
    }
  },
  solc: {
  	optimizer: {
    	enabled: true,
    	runs: 500
  	}
	}
};
