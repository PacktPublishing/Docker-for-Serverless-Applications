package com.example.fn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import event.machine.ContractRegistry;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.ActionsApi;
import io.swagger.client.auth.HttpBasicAuth;
import io.swagger.client.model.KeyValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.HashMap;

public class TransferFunction {

    @Data
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Input {
        private Transfer object;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transfer {
        private String  objectId;
        private String  from;
        private String  to;
        private Double  amount;
        private Boolean sent;
        private Boolean processed;
    }

    @Data
    @AllArgsConstructor
    public static class Success {
        private Transfer success;
    }

    @Data
    @AllArgsConstructor
    public static class Error {
        private String error;
    }

    @Data
    @AllArgsConstructor
    static class RegistrationResult {
        private String bankName;
        private String accountId;
    }

    public RegistrationResult lookup(String telNo) throws Exception {
        val repo = ContractRegistry.registrationRepository();
        val receipt = repo.findByTelNo(telNo).send();
        val foundEvents = repo.getRegistrationFoundEvents(receipt);
        if (foundEvents.isEmpty() == false) {
            val reg = foundEvents.get(0);
            return new RegistrationResult(reg.bank, reg.accNo);
        } else {
            val notFoundEvents = repo.getRegistrationNotFoundEvents(receipt);
            if(notFoundEvents.isEmpty() == false) {
                val reg = notFoundEvents.get(0);
                return null;
            }
        }

        throw new Exception("Lookup does not find any event in receipt.");
    }


    private boolean transferStart(String txId) {
        try {
            val repo = ContractRegistry.transferStateRepository();
            val receipt = repo.start(txId).send();
            val events = repo.getTransferStartedEvents(receipt);
            if (events.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void adjustFromAccountRequest(String txId, String bankName, String accountId, Double amount) throws Exception {
        adjust(txId, bankName, accountId, amount);
    }

    public boolean faasAdjust(String txId, String accountId, Double amount) throws Exception {
        val env = System.getenv("FAAS_GATEWAY_SERVICE");
        val faasGatewayService = (env == null? "http://gateway:8080" : env);

        val JSON = MediaType.parse("application/json; charset=utf-8");
        val client = new OkHttpClient();
        val json = new ObjectMapper().writeValueAsString(new HashMap<String,String>(){{
            put("accountId", accountId);
            put("amount", String.valueOf(amount));
        }});
        System.out.println(json);
        val body = RequestBody.create(JSON, json);
        val request = new Request.Builder()
                .url(faasGatewayService + "/function/hivectl")
                .post(body)
                .build();
        val response = client.newCall(request).execute();
        System.out.println(response);

        if(response.code() == 200) {
            val str = response.body().string();
            return true;
        }

        throw new Exception(response.toString());
    }

    private boolean whiskAdjust(String txId, String accountId, Double amount) throws Exception {
        val env = System.getenv("WHISK_GATEWAY_SERVICE");
        val whiskGatewayService = (env == null? "https://whisk:443" : env);

        val API_KEY="23bc46b1-71f6-4ed5-8c54-816aa4f8c502";
        val API_PASS="123zO3xZCLrMN6v2BKK1dXYFpXlPkccOFqm12CdAsMgRU4VrNZ9lyGVCGuMDGIwP";

        // val auth = new HttpBasicAuth();
        // auth.setUsername(API_KEY);
        // auth.setPassword(API_PASS);

        val client = new ApiClient();
        client.setBasePath(whiskGatewayService + "/api/v1");
        client.setUsername(API_KEY);
        client.setPassword(API_PASS);
        client.setVerifyingSsl(false);
        client.setDebugging(true);

        val actionsApi = new ActionsApi(client);

        val payload = new KeyValue();
        payload.put("accountId", accountId);
        payload.put("amount", amount);

        try {
            val activation = actionsApi.invokeAction("guest",
                    "",
                    "account_ctl",
                    payload,
                    "true",
                    "false",
                    60000);
            if(activation.getResponse().getSuccess() == true) {
                return true;
            }
        } catch(ApiException e) {
            throw new Exception("OpenWhisk Action API: " + e.getMessage());
        }

        return false;
    }

    private boolean transferPending(String txId) {
        try {
            val repo = ContractRegistry.transferStateRepository();
            val receipt = repo.pending(txId).send();
            val events = repo.getTransferPendingEvents(receipt);
            if (events.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void adjustToAccountRequest(String txId, String bankName, String accountId, Double amount) throws Exception {
        adjust(txId, bankName, accountId, amount);
    }

    private void adjust(String txId, String bankName, String accountId, Double amount) throws Exception {
        switch (bankName) {
            case "faas":
                faasAdjust(txId, accountId, amount);
                break;

            case "whisk":
                whiskAdjust(txId, accountId, amount);
                break;
        }
    }

    private boolean transferComplete(String txId) {
        try {
            val repo = ContractRegistry.transferStateRepository();
            val receipt = repo.complete(txId).send();
            val events = repo.getTransferCompletedEvents(receipt);
            if (events.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object handleRequest(String body) {
        if (body == null || body.isEmpty()) {
            body = "{}";
        }

        Input input;
        try {
            val mapper = new ObjectMapper();
            input = mapper.readValue(body, Input.class);
        } catch (IOException e) {
            return new Error(e.getMessage());
        }

        if (input == null) {
            return new Error(body);
        }

        if (input.object == null) {
            return new Error("input object is null");
        }

        if (input.object.sent != Boolean.TRUE) {

            input.object.sent = null;
            input.object.processed = null;
            // return success because we allow object to be saved,
            // but not processed.
            return new Success(input.object);
        }

        val transfer = input.object;
        val amount   = transfer.amount;
        val txId     = transfer.objectId;

        try {
            val from = lookup(transfer.from);
            if (from == null) {
                return new Error("Could not lookup number: " + transfer.from);
            }

            val to = lookup(transfer.to);
            if (to == null) {
                return new Error("Could not lookup number: " + transfer.to);
            }

            val startOK = transferStart(txId);

            adjustFromAccountRequest(txId, from.getBankName(), from.getAccountId(), -amount);
            val pendingOK = transferPending(txId);

            adjustToAccountRequest(txId, to.getBankName(), to.getAccountId(), amount);
            val completeOK = transferComplete(txId);

            input.object.processed = true;
            return new Success(input.object);
        } catch (Exception e) {
            return new Error(e.getMessage());
        }

    }

}