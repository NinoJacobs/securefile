package com.capitec.securefile.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminCustomerResponse {
    String customerId;
    String customerNumber;
    String username;
    String email;
}
