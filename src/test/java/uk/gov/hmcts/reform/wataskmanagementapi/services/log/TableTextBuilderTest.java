package uk.gov.hmcts.reform.wataskmanagementapi.services.log;

import dnl.utils.text.table.TextTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.CANCELLED_STATE;

class TableTextBuilderTest {

    @Test
    void shouldCreateTableText() {
        final TaskCancellationView taskCancellationView = new TaskCancellationView("123", CANCELLED_STATE, "456");

        final TextTable textTable = new TableTextBuilder().buildTextTable(List.of(taskCancellationView));

        assertThat(textTable).isNotNull();
        assertThat(textTable.getTableModel().getColumnName(0)).isEqualTo("Task id");
        assertThat(textTable.getTableModel().getColumnName(1)).isEqualTo("Cancellation State");
        assertThat(textTable.getTableModel().getColumnName(2)).isEqualTo("Case id");
        assertThat(textTable.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(textTable.getTableModel().getValueAt(0, 0)).isEqualTo("123");
        assertThat(textTable.getTableModel().getValueAt(0, 1)).isEqualTo(CANCELLED_STATE);
        assertThat(textTable.getTableModel().getValueAt(0, 2)).isEqualTo("456");
    }

}