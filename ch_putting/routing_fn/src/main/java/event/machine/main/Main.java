package event.machine.main;

import event.machine.ContractRegistry;
import event.machine.TransferStateRepository;
import lombok.val;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        /*
        val DEV_ACCOUNT = "0x00a329c0648769a73afac7f9381e08fb43dbea72";

        val web3j = Admin.build(new HttpService());
        val personalUnlockAccount = web3j.personalUnlockAccount(
                DEV_ACCOUNT,
                "").send();
        */

        val tsr = ContractRegistry.unlock((web3j, tm) ->
                TransferStateRepository.load(
                        "0xee35211c4d9126d520bbfeaf3cfee5fe7b86f221",
                        web3j, tm, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT));

        try {
            tsr.start(args[0]).send();
            tsr.pending(args[0]).send();
            tsr.complete(args[0]).send();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


        /*
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

            val tsr = TransferStateRepository.load(
                    "0xee35211c4d9126d520bbfeaf3cfee5fe7b86f221",
                    web3j, tm, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
            try {
                tsr.complete("5").send();
                tsr.pending("5").send();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }


            /*
            RegistrationRepository r = RegistrationRepository.load(
                "0x62d69f6867a0a084c6d313943dc22023bc263691",
                web3j, tm, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);

            TransactionReceipt tr = r.register("0830000000", "scb", "000000").send();

            for (RegistrationRepository.RegisteredEventResponse event : r.getRegisteredEvents(tr)) {
                System.out.println("Registered: " + event);
            }
            for (RegistrationRepository.AlreadyExistedEventResponse event : r.getAlreadyExistedEvents(tr)) {
                System.out.println("Tel no. " + event.telNo + " already existed.");
            }
            */

    }

}
