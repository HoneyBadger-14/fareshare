package com.fareshare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:fareshare_api_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void groupExpensesInvitationsAndConfirmedSettlement() throws Exception {
        List<Map<String, Object>> countries = JsonPath.read(mvc.perform(get("/api/v1/catalog/countries"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$[*]");
        assertEquals(22, countries.size());
        assertEquals(18, countries.stream().map(country -> country.get("defaultCurrencyCode")).distinct().count());

        String alice = "alice@example.com", bob = "bob@example.com", carol = "carol@example.com";
        String a = field(mvc.perform(get("/api/v1/me").header("X-Dev-Email", alice))
                .andExpect(status().isOk()).andReturn(), "$.userId");
        String b = field(mvc.perform(get("/api/v1/me").header("X-Dev-Email", bob))
                .andExpect(status().isOk()).andReturn(), "$.userId");
        String c = field(mvc.perform(get("/api/v1/me").header("X-Dev-Email", carol))
                .andExpect(status().isOk()).andReturn(), "$.userId");

        String group = field(mvc.perform(post("/api/v1/groups").header("X-Dev-Email", alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Trip\",\"currencyCode\":\"INR\"}"))
                .andExpect(status().isCreated()).andReturn(), "$.id");
        invite(group, alice, bob);
        invite(group, alice, carol);

        String dinner = expenseJson("Dinner", 90000, a, List.of(a, b, c));
        MvcResult first = mvc.perform(post("/api/v1/groups/{id}/expenses", group)
                .header("X-Dev-Email", alice).header("Idempotency-Key", "dinner-1")
                .contentType(MediaType.APPLICATION_JSON).content(dinner))
                .andExpect(status().isCreated()).andReturn();
        assertUtcTimestamp(first, "$.createdAt");
        String expenseId = field(first, "$.id");
        assertEquals(expenseId, field(mvc.perform(post("/api/v1/groups/{id}/expenses", group)
                .header("X-Dev-Email", alice).header("Idempotency-Key", "dinner-1")
                .contentType(MediaType.APPLICATION_JSON).content(dinner))
                .andExpect(status().isCreated()).andReturn(), "$.id"));
        mvc.perform(post("/api/v1/groups/{id}/expenses", group)
                .header("X-Dev-Email", alice).header("Idempotency-Key", "dinner-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expenseJson("Different", 90000, a, List.of(a, b, c))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/groups/{id}/expenses", group)
                .header("X-Dev-Email", bob).header("Idempotency-Key", "taxi-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(expenseJson("Taxi", 60000, b, List.of(a, b, c))))
                .andExpect(status().isCreated());

        MvcResult balance = mvc.perform(get("/api/v1/groups/{id}/balances", group)
                .header("X-Dev-Email", alice)).andExpect(status().isOk()).andReturn();
        Map<String, Integer> amounts = JsonPath.read(balance.getResponse().getContentAsString(), "$.balances");
        assertEquals(40000, amounts.get(a));
        assertEquals(10000, amounts.get(b));
        assertEquals(-50000, amounts.get(c));
        List<?> suggestions = JsonPath.read(balance.getResponse().getContentAsString(), "$.suggestedPayments");
        assertEquals(2, suggestions.size());

        MvcResult requestedSettlement = mvc.perform(post("/api/v1/groups/{id}/settlements", group)
                .header("X-Dev-Email", carol).header("Idempotency-Key", "pay-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"senderId\":\"" + c + "\",\"recipientId\":\"" + a
                        + "\",\"amountMinor\":40000}"))
                .andExpect(status().isCreated()).andReturn();
        assertUtcTimestamp(requestedSettlement, "$.createdAt");
        String settlement = field(requestedSettlement, "$.id");
        mvc.perform(post("/api/v1/settlements/{id}/confirm", settlement)
                .header("X-Dev-Email", carol)).andExpect(status().isForbidden());
        assertUtcTimestamp(mvc.perform(post("/api/v1/settlements/{id}/confirm", settlement)
                .header("X-Dev-Email", alice)).andExpect(status().isOk()).andReturn(), "$.decidedAt");
        MvcResult after = mvc.perform(get("/api/v1/groups/{id}/balances", group)
                .header("X-Dev-Email", alice)).andExpect(status().isOk()).andReturn();
        Map<String, Integer> afterAmounts = JsonPath.read(after.getResponse().getContentAsString(), "$.balances");
        assertEquals(0, afterAmounts.get(a));
        assertEquals(10000, afterAmounts.get(b));
        assertEquals(-10000, afterAmounts.get(c));

        mvc.perform(patch("/api/v1/groups/{id}/expenses/{expenseId}", group, expenseId)
                .header("X-Dev-Email", bob).contentType(MediaType.APPLICATION_JSON)
                .content(editJson(0, "Dinner corrected", 120000, a, List.of(a, b, c))))
                .andExpect(status().isForbidden());
        MvcResult edited = mvc.perform(patch("/api/v1/groups/{id}/expenses/{expenseId}", group, expenseId)
                .header("X-Dev-Email", alice).contentType(MediaType.APPLICATION_JSON)
                .content(editJson(0, "Dinner corrected", 120000, a, List.of(a, b, c))))
                .andExpect(status().isOk()).andReturn();
        assertEquals(120000, ((Number) JsonPath.read(edited.getResponse().getContentAsString(), "$.amountMinor")).intValue());
        mvc.perform(patch("/api/v1/groups/{id}/expenses/{expenseId}", group, expenseId)
                .header("X-Dev-Email", alice).contentType(MediaType.APPLICATION_JSON)
                .content(editJson(0, "Stale", 120000, a, List.of(a, b, c))))
                .andExpect(status().isConflict());
        MvcResult activity = mvc.perform(get("/api/v1/groups/{id}/activity", group)
                .header("X-Dev-Email", alice)).andExpect(status().isOk()).andReturn();
        assertTrue(activity.getResponse().getContentAsString().contains("EXPENSE_EDITED"));
        assertUtcTimestamp(activity, "$[0].createdAt");
    }

    private void invite(String group, String inviter, String invited) throws Exception {
        MvcResult invitation = mvc.perform(post("/api/v1/groups/{id}/invites", group)
                .header("X-Dev-Email", inviter).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + invited + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        assertUtcTimestamp(invitation, "$.expiresAt");
        String link = field(invitation, "$.localJoinLink");
        String token = link.substring(link.indexOf("token=") + 6);
        mvc.perform(post("/api/v1/invites/accept").header("X-Dev-Email", invited)
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }

    private String field(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private void assertUtcTimestamp(MvcResult result, String path) throws Exception {
        assertTrue(field(result, path).endsWith("Z"), path + " must be serialized in UTC");
    }

    private String expenseJson(String description, int amount, String payer, List<String> people) {
        return "{\"description\":\"" + description + "\",\"amountMinor\":" + amount
                + ",\"payerId\":\"" + payer + "\",\"spentOn\":\"2026-10-04\",\"splitMethod\":\"EQUAL\",\"participantIds\":[\""
                + String.join("\",\"", people) + "\"]}";
    }

    private String editJson(int version, String description, int amount, String payer, List<String> people) {
        return expenseJson(description, amount, payer, people).replaceFirst("\\{", "{\"expectedVersion\":" + version + ",");
    }
}
