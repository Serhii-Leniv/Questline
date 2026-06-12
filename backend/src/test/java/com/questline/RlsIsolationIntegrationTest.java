package com.questline;

import static org.assertj.core.api.Assertions.assertThat;

import com.questline.domain.Goal;
import com.questline.domain.User;
import com.questline.repository.GoalRepository;
import com.questline.repository.UserRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves RLS actually isolates rows: connecting as a NON-superuser role (the app would run as one in
 * production), a query only sees rows for the user in {@code app.user_id}, and sees all rows when it
 * is unset (the pass-through that keeps background jobs working). The app's own connection is a
 * superuser in tests and bypasses RLS, so this verifies the policy directly via a restricted role.
 */
class RlsIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    GoalRepository goalRepository;

    @Autowired
    DataSource dataSource;

    @Test
    void policyIsolatesByCurrentUserAndPassesThroughWhenUnset() throws Exception {
        User a = newUser();
        User b = newUser();
        UUID goalA = newGoal(a).getId();
        UUID goalB = newGoal(b).getId();

        String role = "rls_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = dataSource.getConnection(); Statement s = admin.createStatement()) {
            s.execute("create role " + role + " login password 'rls'");
            s.execute("grant usage on schema public to " + role);
            s.execute("grant select on goals to " + role);
        }

        try (Connection conn = DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, "rls")) {
            // As user A: only A's goal is visible.
            setUser(conn, a.getId());
            assertThat(visible(conn, goalA)).isTrue();
            assertThat(visible(conn, goalB)).isFalse();

            // As user B: only B's goal is visible.
            setUser(conn, b.getId());
            assertThat(visible(conn, goalA)).isFalse();
            assertThat(visible(conn, goalB)).isTrue();

            // Unset (background-job / service context): pass-through sees both.
            exec(conn, "reset app.user_id");
            assertThat(visible(conn, goalA)).isTrue();
            assertThat(visible(conn, goalB)).isTrue();
        }
    }

    private static void setUser(Connection conn, UUID userId) throws Exception {
        exec(conn, "set app.user_id = '" + userId + "'");
    }

    private static void exec(Connection conn, String sql) throws Exception {
        try (Statement s = conn.createStatement()) {
            s.execute(sql);
        }
    }

    private static boolean visible(Connection conn, UUID goalId) throws Exception {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("select count(*) from goals where id = '" + goalId + "'")) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }

    private User newUser() {
        User user = new User();
        user.setEmail("rls-" + UUID.randomUUID() + "@questline.test");
        user.setName("RLS");
        return userRepository.save(user);
    }

    private Goal newGoal(User user) {
        Goal goal = new Goal();
        goal.setUser(user);
        goal.setTitle("Goal");
        return goalRepository.save(goal);
    }
}
