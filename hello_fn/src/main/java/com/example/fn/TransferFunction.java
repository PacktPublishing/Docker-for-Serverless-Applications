package com.example.fn;

/*
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
*/
import java.io.*;
import lombok.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
        private String from;
        private String to;
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

    // private OkHttpClient client = new OkHttpClient();

    public Object handleRequest(String body) {
        if (body == null || body.isEmpty()) {
            body = "{}";
        }

        Input input;
        try {
            ObjectMapper mapper = new ObjectMapper();
            input = mapper.readValue(body, Input.class);
        } catch(IOException e) {
            return new Error(e.getMessage());
        }

        /*
        Request request = new Request.Builder()
            .url("http://test/")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                Success output = new Success();
                output.success = "ok";
                return output;
            } else {
                Error output = new Error();
                output.error = "ok";
                return output;
            }
        }

        Error output = new Error();
        output.error = "doing nothing";
		return error;
        */
        if (input == null) {
            return new Error(body);
        }

        if (input.object == null) {
            return new Transfer();
        }

        input.object.to = "test";
        return new Success(input.object);
    }

}