package br.com.erpsystem.mssales.enums;

import lombok.Getter;

@Getter
public enum PaymentTypeEnum {

    CARTAO_DEBITO, CARTAO_CREDITO, PIX, DINHEIRO, BOLETO;
}
