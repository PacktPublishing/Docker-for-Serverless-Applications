package event.listener;

import event.machine.ContractRegistry;
import event.machine.TransferStateRepository;
import lombok.val;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;

import java.io.IOException;
import java.math.BigInteger;

public class Main {

    public static void main(String[] args) throws Exception {

        val tsrContract = ContractRegistry.unlock((web3j, tm) -> {
            return TransferStateRepository.load(
                        "0x62d69f6867a0a084c6d313943dc22023bc263691",
                        web3j, tm, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        });

        tsrContract.transferCompletedEventObservable(
            DefaultBlockParameterName.LATEST,
            DefaultBlockParameterName.LATEST).subscribe(event -> {

            System.out.printf("Transfer completed: %s\n", event.txId );

        });
    }

}