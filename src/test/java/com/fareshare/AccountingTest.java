package com.fareshare;

import com.fareshare.service.Accounting;
import com.fareshare.controller.ApiError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountingTest {
    @Test
    void equalSplitDistributesRemainderDeterministically() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");
        assertEquals(List.of(new Accounting.ShareAmount(a, 4), new Accounting.ShareAmount(b, 3),
                new Accounting.ShareAmount(c, 3)), Accounting.splitEqually(10, List.of(c, a, b)));
    }

    @Test
    void suggestionsClearBalances() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");
        assertEquals(List.of(new Accounting.Payment(c, a, 40), new Accounting.Payment(c, b, 10)),
                Accounting.suggestions(Map.of(a, 40L, b, 10L, c, -50L)));
    }

    @Test
    void rejectsInvalidExactShares() {
        UUID a = UUID.randomUUID();
        assertThrows(ApiError.class, () -> Accounting.validateShares(10,
                List.of(new Accounting.ShareAmount(a, 9))));
    }
}
