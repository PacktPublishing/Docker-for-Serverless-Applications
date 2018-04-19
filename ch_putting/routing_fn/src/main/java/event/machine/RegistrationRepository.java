package event.machine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.2.0.
 */
public class RegistrationRepository extends Contract {
    private static final String BINARY = null;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<>();
        _addresses.put("17", "0xee35211c4d9126d520bbfeaf3cfee5fe7b86f221");
    }

    protected RegistrationRepository(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RegistrationRepository(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public List<RegisteredEventResponse> getRegisteredEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Registered", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>() {}));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<RegisteredEventResponse> responses = new ArrayList<RegisteredEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            RegisteredEventResponse typedResponse = new RegisteredEventResponse();
            typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.registration = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RegisteredEventResponse> registeredEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Registered", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>() {}));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, RegisteredEventResponse>() {
            @Override
            public RegisteredEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                RegisteredEventResponse typedResponse = new RegisteredEventResponse();
                typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.registration = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public List<RegistrationFoundEventResponse> getRegistrationFoundEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("RegistrationFound", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<RegistrationFoundEventResponse> responses = new ArrayList<RegistrationFoundEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            RegistrationFoundEventResponse typedResponse = new RegistrationFoundEventResponse();
            typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.bank = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.accNo = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RegistrationFoundEventResponse> registrationFoundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("RegistrationFound", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, RegistrationFoundEventResponse>() {
            @Override
            public RegistrationFoundEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                RegistrationFoundEventResponse typedResponse = new RegistrationFoundEventResponse();
                typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.bank = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.accNo = (String) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public List<RegistrationNotFoundEventResponse> getRegistrationNotFoundEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("RegistrationNotFound", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<RegistrationNotFoundEventResponse> responses = new ArrayList<RegistrationNotFoundEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            RegistrationNotFoundEventResponse typedResponse = new RegistrationNotFoundEventResponse();
            typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RegistrationNotFoundEventResponse> registrationNotFoundEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("RegistrationNotFound", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, RegistrationNotFoundEventResponse>() {
            @Override
            public RegistrationNotFoundEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                RegistrationNotFoundEventResponse typedResponse = new RegistrationNotFoundEventResponse();
                typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public List<AlreadyExistedEventResponse> getAlreadyExistedEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("AlreadyExisted", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<AlreadyExistedEventResponse> responses = new ArrayList<AlreadyExistedEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            AlreadyExistedEventResponse typedResponse = new AlreadyExistedEventResponse();
            typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<AlreadyExistedEventResponse> alreadyExistedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("AlreadyExisted", 
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, AlreadyExistedEventResponse>() {
            @Override
            public AlreadyExistedEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                AlreadyExistedEventResponse typedResponse = new AlreadyExistedEventResponse();
                typedResponse.telNo = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public RemoteCall<TransactionReceipt> findByTelNo(String telNo) {
        Function function = new Function(
                "findByTelNo", 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(telNo)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> register(String telNo, String bank, String accNo) {
        Function function = new Function(
                "register", 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(telNo), 
                new org.web3j.abi.datatypes.Utf8String(bank), 
                new org.web3j.abi.datatypes.Utf8String(accNo)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<RegistrationRepository> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RegistrationRepository.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<RegistrationRepository> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RegistrationRepository.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static RegistrationRepository load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RegistrationRepository(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static RegistrationRepository load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RegistrationRepository(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class RegisteredEventResponse {
        public String telNo;

        public String registration;
    }

    public static class RegistrationFoundEventResponse {
        public String telNo;

        public String bank;

        public String accNo;
    }

    public static class RegistrationNotFoundEventResponse {
        public String telNo;
    }

    public static class AlreadyExistedEventResponse {
        public String telNo;
    }
}
