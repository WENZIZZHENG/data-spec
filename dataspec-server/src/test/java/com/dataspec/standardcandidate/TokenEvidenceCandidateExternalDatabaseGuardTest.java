package com.dataspec.standardcandidate;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenEvidenceCandidateExternalDatabaseGuardTest {

    @Test
    void userObjectQueryCoversDatabaseLevelAndNamespacedObjects() {
        assertThat(TokenEvidenceCandidateConcurrencyIT.externalDatabaseUserObjectsSql()).contains(
                "pg_publication",
                "pg_subscription",
                "pg_event_trigger",
                "pg_foreign_data_wrapper",
                "pg_foreign_server",
                "pg_user_mapping",
                "pg_largeobject_metadata",
                "pg_default_acl",
                "pg_ts_config",
                "pg_ts_dict",
                "pg_ts_parser",
                "pg_ts_template");
        assertThat(TokenEvidenceCandidateConcurrencyIT.externalDatabaseUserObjectsSql()).contains(
                "WHERE s.subdbid = (SELECT oid FROM pg_database WHERE datname = current_database())");
    }

    @Test
    void guardRejectsDatabaseNotOwnedByConfiguredUser() throws Exception {
        Connection connection = connection("other_owner", "dataspec_owner", 0);

        assertThatThrownBy(() -> TokenEvidenceCandidateConcurrencyIT
                .assertExternalDatabaseIsOwnedAndEmpty(connection, "dataspec_owner"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("owner");
    }

    @Test
    void guardRejectsAnyDetectedUserObject() throws Exception {
        Connection connection = connection("dataspec_owner", "dataspec_owner", 1);

        assertThatThrownBy(() -> TokenEvidenceCandidateConcurrencyIT
                .assertExternalDatabaseIsOwnedAndEmpty(connection, "dataspec_owner"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("no user objects");
    }

    private Connection connection(String ownerName, String currentUser, int userObjectCount) throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet owner = mock(ResultSet.class);
        ResultSet objects = mock(ResultSet.class);
        when(connection.getCatalog()).thenReturn("dataspec_candidate_it");
        when(connection.getSchema()).thenReturn("public");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(owner, objects);
        when(owner.next()).thenReturn(true);
        when(owner.getString(1)).thenReturn(ownerName);
        when(owner.getString(2)).thenReturn(currentUser);
        when(objects.next()).thenReturn(true);
        when(objects.getInt(1)).thenReturn(userObjectCount);
        return connection;
    }
}
