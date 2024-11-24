package br.com.breno.createUrlShortner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();
        Map<String, String> bodyMap;

        try {
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON body" + e.getMessage(), e);
        }

        String originalUrl = bodyMap.get("originalUrl");
        String expirationTime = bodyMap.get("expirationTime");
        String shorUrlCode = UUID.randomUUID().toString().substring(0, 8);
        long expirationTimeInSecond = Long.parseLong(expirationTime) * 3600;

        UrlData urlData = new UrlData(originalUrl, expirationTimeInSecond);

        try {
            String urlObjectJson = objectMapper.writeValueAsString(urlData);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket("aws-bucket-url-shortener")
                    .key(shorUrlCode + "json") // nome do arquivo
                    .build();

            s3Client.putObject(request, RequestBody.fromString(urlObjectJson));
        } catch (Exception e) {
            throw new RuntimeException("Error saving data to S3: " + e.getMessage(), e);
        }

        Map<String, String> response = new HashMap<>();
        response.put("codeId", shorUrlCode);
        return response;
    } 
}