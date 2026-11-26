package app.swarsetu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** End-to-end API checks against a real MySQL (database swarsetu_test is wiped first). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcClient jdbc;
    static String adminA, adminB, studentTok, collegeA, collegeB, courseA, studentA, studentUserId, refreshA;
    static final Pattern CODE = Pattern.compile("is (\\d{6})");

    @BeforeAll
    static void wipe(@Autowired Flyway flyway) { flyway.clean(); flyway.migrate(); }

    JsonNode call(MockHttpServletRequestBuilder req, Object body, String token, int expect) throws Exception {
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        String out = mvc.perform(req).andExpect(status().is(expect)).andReturn().getResponse().getContentAsString();
        return out.isEmpty() ? json.nullNode() : json.readTree(out);
    }

    String code(String recipient) {
        String body = jdbc.sql("SELECT body FROM outbox WHERE recipient = ? ORDER BY id DESC LIMIT 1").param(recipient).query(String.class).single();
        Matcher m = CODE.matcher(body);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    Map<String, Object> college(String name, String user, String email, String mobile) {
        return new HashMap<>(Map.of("collegeName", name, "city", "Pune", "firstName", "Asha", "username", user, "email", email, "mobile", mobile, "password", "Secret123"));
    }

    @Test @Order(1)
    void registerCollegeWithEmailAndSmsCodes() throws Exception {
        JsonNode v = call(post("/api/auth/colleges"), college("Raga Academy of Music", "ragaadmin", "admin@raga.in", "9876500001"), null, 200);
        assertThat(v.get("simulated")).isNotNull(); // no SMTP/SMS configured in tests
        call(post("/api/auth/verify"), Map.of("verificationId", v.get("verificationId").asText(), "emailCode", "000000", "mobileCode", "000000"), null, 400);
        JsonNode r = call(post("/api/auth/verify"), Map.of("verificationId", v.get("verificationId").asText(),
            "emailCode", code("admin@raga.in"), "mobileCode", code("+919876500001")), null, 200);
        collegeA = r.get("collegeCode").asText();
        assertThat(collegeA).matches("RAM-[A-Z0-9]{4}");
        adminA = r.get("accessToken").asText();
        refreshA = r.get("refreshToken").asText();
        assertThat(r.get("user").get("role").asText()).isEqualTo("admin");
    }

    @Test @Order(2)
    void duplicateCollegeNameAndIdsAreRefused() throws Exception {
        call(post("/api/auth/colleges"), college("RAGA academy of music!", "other", "o@x.in", "9876500009"), null, 409);
        call(post("/api/auth/colleges"), college("Another School", "RagaAdmin", "o@x.in", "9876500009"), null, 409);   // username, any case
        call(post("/api/auth/colleges"), college("Another School", "other2", "admin@raga.in", "9876500009"), null, 409); // email
    }

    @Test @Order(3)
    void secondCollegeIsIsolated() throws Exception {
        JsonNode v = call(post("/api/auth/colleges"), college("The Swara Music School", "priya", "priya@gmail.com", "9876500002"), null, 200);
        JsonNode r = call(post("/api/auth/verify"), Map.of("verificationId", v.get("verificationId").asText(),
            "emailCode", code("priya@gmail.com"), "mobileCode", code("+919876500002")), null, 200);
        collegeB = r.get("collegeCode").asText();
        adminB = r.get("accessToken").asText();
        assertThat(collegeB).startsWith("SMS-");
    }

    @Test @Order(4)
    void coursesAndStudentsStayInTheirCollege() throws Exception {
        courseA = call(post("/api/courses"), Map.of("name", "Hindustani Vocal"), adminA, 200).get("id").asText();
        JsonNode s = call(post("/api/students"), Map.of("firstName", "Aarav", "courseId", courseA, "classType", "Group", "mode", "Online",
            "weekdays", "Mon,Wed", "startTime", "17:00", "monthlyFee", 1800), adminA, 200);
        studentA = s.get("id").asText();
        assertThat(studentA).isEqualTo("STU0001-HIN");
        assertThat(call(get("/api/students"), null, adminB, 200).size()).isZero();
        call(put("/api/courses/" + courseA), Map.of("name", "Hacked"), adminB, 404);
        call(post("/api/students"), Map.of("firstName", "X", "courseId", courseA, "classType", "Group", "mode", "Online",
            "weekdays", "Mon", "startTime", "17:00"), adminB, 400); // course belongs to another college
        call(delete("/api/courses/" + courseA), null, adminA, 409);   // in use
    }

    @Test @Order(5)
    void studentJoinsIsApprovedAndSeesOnlyOwnData() throws Exception {
        JsonNode v = call(post("/api/auth/join"), Map.of("collegeCode", collegeA, "role", "student", "firstName", "Aarav",
            "email", "aarav@gmail.com", "password", "Student99"), null, 200);
        JsonNode r = call(post("/api/auth/verify"), Map.of("verificationId", v.get("verificationId").asText(), "emailCode", code("aarav@gmail.com")), null, 200);
        assertThat(r.get("status").asText()).isEqualTo("pending");
        call(post("/api/auth/login"), Map.of("identifier", "aarav@gmail.com", "password", "Student99"), null, 403);
        JsonNode users = call(get("/api/users?status=pending"), null, adminA, 200);
        studentUserId = users.get(0).get("id").asText();
        call(get("/api/users"), null, adminB, 200); // B sees none of A's users
        assertThat(call(get("/api/users?status=pending"), null, adminB, 200).size()).isZero();
        call(post("/api/users/" + studentUserId + "/approve"), Map.of("role", "student", "studentId", studentA), adminA, 200);
        studentTok = call(post("/api/auth/login"), Map.of("identifier", "AARAV@gmail.com", "password", "Student99"), null, 200).get("accessToken").asText();
        assertThat(call(get("/api/students"), null, studentTok, 200).size()).isEqualTo(1);
        call(post("/api/courses"), Map.of("name", "Sitar"), studentTok, 403);
        call(get("/api/users"), null, studentTok, 403);
    }

    @Test @Order(6)
    void attendanceAndSummary() throws Exception {
        call(post("/api/attendance"), Map.of("date", "2026-11-02", "marks", List.of(Map.of("studentId", studentA, "code", "P"))), adminA, 200);
        call(post("/api/attendance"), Map.of("date", "2026-11-02", "marks", List.of(Map.of("studentId", studentA, "code", "L"))), adminA, 200);
        JsonNode a = call(get("/api/attendance?from=2026-11-01&to=2026-11-30"), null, studentTok, 200);
        assertThat(a.get(0).get("code").asText()).isEqualTo("L");
        assertThat(a.get(0).get("previousCode").asText()).isEqualTo("P");
        call(get("/api/attendance?from=2026-11-30&to=2026-11-01"), null, adminA, 400); // To before From
        JsonNode sum = call(get("/api/attendance/summary?fromMonth=2026-11&toMonth=2026-11"), null, adminA, 200);
        assertThat(sum.get(0).get("completed").asInt()).isEqualTo(1);
    }

    @Test @Order(7)
    void paymentsClaimsAndUniqueReference() throws Exception {
        String batch = call(get("/api/students"), null, adminA, 200).get(0).get("batchId").asText();
        call(post("/api/payments"), Map.of("studentId", studentA, "batchId", batch, "months", List.of("2026-11"), "totalFee", 1800, "paid", 2000), adminA, 400);
        call(post("/api/payments"), Map.of("studentId", studentA, "batchId", batch, "months", List.of("2026-11"), "totalFee", 1800, "paid", 500), adminA, 200);
        String today = java.time.LocalDate.now().toString();
        String claim = call(post("/api/claims"), Map.of("month", "2026-11", "amount", 1300, "method", "UPI", "reference", "123456789012", "paidOn", today),
            studentTok, 200).get("id").asText();
        call(post("/api/claims"), Map.of("month", "2026-11", "amount", 100, "method", "UPI", "reference", "123456789012", "paidOn", today), studentTok, 409);
        call(post("/api/claims/" + claim + "/verify"), null, adminB, 404); // other college
        call(post("/api/claims/" + claim + "/verify"), null, adminA, 200);
        JsonNode p = call(get("/api/payments?fromMonth=2026-11&toMonth=2026-11"), null, adminA, 200);
        assertThat(p.get("rows").get(0).get("status").asText()).isEqualTo("Paid");
        call(delete("/api/students/" + studentA), null, adminA, 409); // has payments
    }

    @Test @Order(8)
    void refreshRotationDetectsReuse() throws Exception {
        String next = call(post("/api/auth/refresh"), Map.of("refreshToken", refreshA), null, 200).get("refreshToken").asText();
        call(post("/api/auth/refresh"), Map.of("refreshToken", refreshA), null, 401);  // old token reused
        call(post("/api/auth/refresh"), Map.of("refreshToken", next), null, 401);      // whole family revoked
    }

    @Test @Order(9)
    void lockoutAfterFiveWrongPasswords() throws Exception {
        for (int i = 0; i < 5; i++) call(post("/api/auth/login"), Map.of("identifier", "priya", "password", "wrong-pass1"), null, 401);
        call(post("/api/auth/login"), Map.of("identifier", "priya", "password", "Secret123"), null, 423);
    }

    @Test @Order(10)
    void keepOneActiveAdminAndDeactivatedUsersLoseAccess() throws Exception {
        String adminId = call(get("/api/me"), null, adminA, 200).get("id").asText();
        call(put("/api/users/" + adminId), Map.of("status", "inactive"), adminA, 409);
        call(put("/api/users/" + studentUserId), Map.of("status", "inactive"), adminA, 200);
        call(get("/api/students"), null, studentTok, 401); // token still valid, but the account is inactive
    }

    @Test @Order(11)
    void forgotPasswordDoesNotRevealAccounts() throws Exception {
        JsonNode a = call(post("/api/auth/forgot"), Map.of("identifier", "nobody@nowhere.in"), null, 200);
        JsonNode b = call(post("/api/auth/forgot"), Map.of("identifier", "admin@raga.in"), null, 200);
        assertThat(a.get("message").asText()).isEqualTo(b.get("message").asText());
        call(post("/api/auth/reset"), Map.of("verificationId", b.get("verificationId").asText(), "emailCode", code("admin@raga.in"), "newPassword", "NewSecret456"), null, 200);
        call(post("/api/auth/login"), Map.of("identifier", "ragaadmin", "password", "NewSecret456"), null, 200);
    }
}
