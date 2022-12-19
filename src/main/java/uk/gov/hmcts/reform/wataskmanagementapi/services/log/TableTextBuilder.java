package uk.gov.hmcts.reform.wataskmanagementapi.services.log;

import dnl.utils.text.table.TextTable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.COLUMN_NAMES;

@Service
public class TableTextBuilder {

    public TextTable buildTextTable(final List<TaskCancellationView> taskCancellationViews) {
        final Object[][] data = new Object[taskCancellationViews.size()][COLUMN_NAMES.size()];
        final List<Object[]> rowData = getRowData(taskCancellationViews);

        for (int i = 0; i < taskCancellationViews.size(); i++) {
            for (int j = 0; j < COLUMN_NAMES.size(); j++) {
                data[i][j] = rowData.get(i)[j];
            }
        }
        final TextTable textTable = new TextTable(COLUMN_NAMES.toArray(new String[0]), data);
        textTable.setSort(0);

        return textTable;
    }

    private List<Object[]> getRowData(final List<TaskCancellationView> taskCancellationViews) {
        final List<Object[]> rowData = new ArrayList<>();

        taskCancellationViews.forEach(taskCancellationView -> {
            final Object[] caseDataFields = {taskCancellationView.getTaskId(), taskCancellationView.getState(),
                    taskCancellationView.getCaseId()};
            rowData.add(caseDataFields);
        });
        return rowData;
    }
}
