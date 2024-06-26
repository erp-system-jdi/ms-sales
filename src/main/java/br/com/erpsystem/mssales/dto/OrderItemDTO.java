package br.com.erpsystem.mssales.dto;


import br.com.erpsystem.mssales.enums.PaymentTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    @JsonProperty("id")
    private UUID id;
    @JsonProperty("product_id")
    private String productId;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;
}
