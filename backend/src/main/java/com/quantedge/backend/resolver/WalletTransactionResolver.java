package com.quantedge.backend.resolver;

import java.util.List;

import com.quantedge.backend.dto.response.WalletTransactionResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.WalletTransactionMapper;
import com.quantedge.backend.service.WalletService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class WalletTransactionResolver {

    private final WalletService walletService;
    private final WalletTransactionMapper walletTransactionMapper;

    public WalletTransactionResolver(WalletService walletService, WalletTransactionMapper walletTransactionMapper) {
        this.walletService = walletService;
        this.walletTransactionMapper = walletTransactionMapper;
    }

    @QueryMapping
    public List<WalletTransactionResponse> walletTransactions(@AuthenticationPrincipal User user) {
        return walletService.getTransactionHistory(user).stream()
                .map(walletTransactionMapper::toResponse)
                .toList();
    }
}
