package com.fareshare.service;

import com.fareshare.controller.ApiError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Accounting {
    public record ShareAmount(UUID userId, long amountMinor) {}
    public record Payment(UUID fromUserId, UUID toUserId, long amountMinor) {}

    private Accounting() {}

    public static List<ShareAmount> splitEqually(long amountMinor, List<UUID> participants) {
        if (amountMinor <= 0 || participants == null || participants.isEmpty()
                || participants.stream().distinct().count() != participants.size()) {
            throw ApiError.bad("Expense requires a positive amount and distinct participants");
        }
        List<UUID> ordered = participants.stream().sorted().toList();
        long base = amountMinor / ordered.size();
        long remainder = amountMinor % ordered.size();
        List<ShareAmount> shares = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            shares.add(new ShareAmount(ordered.get(i), base + (i < remainder ? 1 : 0)));
        }
        return shares;
    }

    public static void validateShares(long amountMinor, List<ShareAmount> shares) {
        if (amountMinor <= 0 || shares == null || shares.isEmpty()
                || shares.stream().anyMatch(s -> s.userId() == null || s.amountMinor() < 0)
                || shares.stream().map(ShareAmount::userId).distinct().count() != shares.size()) {
            throw ApiError.bad("Invalid expense shares");
        }
        long sum;
        try {
            sum = shares.stream().mapToLong(ShareAmount::amountMinor).reduce(0, Math::addExact);
        } catch (ArithmeticException ex) {
            throw ApiError.bad("Expense shares overflow");
        }
        if (sum != amountMinor) throw ApiError.bad("Expense shares must total the expense amount");
    }

    public static List<Payment> suggestions(Map<UUID, Long> balances) {
        List<Map.Entry<UUID, Long>> debtors = balances.entrySet().stream()
                .filter(e -> e.getValue() < 0).sorted(Map.Entry.comparingByKey()).toList();
        List<Map.Entry<UUID, Long>> creditors = balances.entrySet().stream()
                .filter(e -> e.getValue() > 0).sorted(Map.Entry.comparingByKey()).toList();
        List<Payment> payments = new ArrayList<>();
        int i = 0, j = 0;
        long owing = 0, owed = 0;
        while (i < debtors.size() && j < creditors.size()) {
            if (owing == 0) owing = Math.negateExact(debtors.get(i).getValue());
            if (owed == 0) owed = creditors.get(j).getValue();
            long amount = Math.min(owing, owed);
            payments.add(new Payment(debtors.get(i).getKey(), creditors.get(j).getKey(), amount));
            owing -= amount; owed -= amount;
            if (owing == 0) i++;
            if (owed == 0) j++;
        }
        if (i != debtors.size() || j != creditors.size()) throw new IllegalStateException("Balances do not sum to zero");
        return payments;
    }

    public static Map<UUID, Long> zeroBalances(List<UUID> users) {
        Map<UUID, Long> balances = new LinkedHashMap<>();
        users.stream().sorted(Comparator.naturalOrder()).forEach(id -> balances.put(id, 0L));
        return balances;
    }
}
