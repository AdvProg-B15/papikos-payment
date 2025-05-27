package id.ac.ui.cs.advprog.papikos.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class RentalResponseWrapper<T> {
    private int status;
    private String message;
    private T data;
    private long timestamp;
}