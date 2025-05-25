package id.ac.ui.cs.advprog.papikos.payment.dto;

import lombok.*;

@lombok.Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyTokenResponse {
    public int status;
    public String message;
    public Data data;
    public long timestamp;

    @lombok.Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {
        public String userId;
        public String email;
        public String role;
        public String status;
    }
}
