package event.machine;

import lombok.experimental.var;
import lombok.val;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import org.web3j.tx.TransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.function.BiFunction;

public class ContractRegistry {

    public static <T> T unlock(BiFunction<Web3j, TransactionManager, T> callable) throws Exception {
        val DEV_ACCOUNT = "0x00a329c0648769a73afac7f9381e08fb43dbea72";

        val env = System.getenv("BLOCKCHAIN_SERVICE");
        // val blockchainService = (env == null? "http://blockchain:8545/" : env );
        val blockchainService = (env == null? "http://172.17.0.1:8545/" : env );

        val web3j = Admin.build(new HttpService(blockchainService));
        val personalUnlockAccount = web3j.personalUnlockAccount(
                DEV_ACCOUNT, "").send();

        if (personalUnlockAccount.accountUnlocked()) {
            val tm = new ClientTransactionManager(web3j, DEV_ACCOUNT) {
                @Override
                public EthSendTransaction sendTransaction(BigInteger gasPrice,
                                                          BigInteger gasLimit, String to, String data, BigInteger value) throws IOException {

                    val transaction = new Transaction(getFromAddress(), null, gasPrice, gasLimit, to, value, data);
                    val estimate = web3j.ethEstimateGas(transaction).send();
                    val error = estimate.getError();
                    if (error != null && error.getCode() == -32015) {
                        throw new IOException("The transaction will throw an exception with the current values.");
                    }

                    return web3j.ethSendTransaction(transaction).send();
                }
            };

            return callable.apply(web3j, tm);
        }

        return null;
    }

    public static RegistrationRepository registrationRepository() throws Exception {
        return unlock((web3j, transactionManager) ->
                RegistrationRepository.load(
                        "0xee35211c4d9126d520bbfeaf3cfee5fe7b86f221",
                        web3j, transactionManager, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT));
    }


    public static TransferStateRepository transferStateRepository() throws Exception {
        return unlock((web3j, transactionManager) ->
                TransferStateRepository.load(
                        "0x62d69f6867a0a084c6d313943dc22023bc263691",
                        web3j, transactionManager, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT));
    }
}
