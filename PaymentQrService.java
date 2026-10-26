package com.example.movie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

/** Builds a VietQR image URL without changing the booking flow. */
@Service
public class PaymentQrService {

    private final String bankBin;
    private final String accountNumber;
    private final String accountName;
    private final String template;

    public PaymentQrService(
            @Value("${payment.qr.bank-bin:}") String bankBin,
            @Value("${payment.qr.account-number:}") String accountNumber,
            @Value("${payment.qr.account-name:}") String accountName,
            @Value("${payment.qr.template:compact2}") String template) {
        this.bankBin = bankBin;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.template = template;
    }

    public boolean isConfigured() {
        return !bankBin.isBlank() && !accountNumber.isBlank();
    }

    public String createQrUrl(Long bookingId, BigDecimal amount) {
        if (!isConfigured()) {
            return null;
        }

        return UriComponentsBuilder
                .fromUriString("https://img.vietqr.io/image/{bank}-{account}-{template}.png")
                .queryParam("amount", amount.toBigInteger())
                .queryParam("addInfo", "CineTicket " + bookingId)
                .queryParam("accountName", accountName)
                .buildAndExpand(bankBin, accountNumber, template)
                .toUriString();
    }
}
