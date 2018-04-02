package jobs.steps.helpers

import groovy.transform.CompileStatic
import jobs.table.columns.AbstractColumn

@CompileStatic
class StubColumn extends AbstractColumn {
	Map<String, String> data

	@Override
	void onReadRow(String dataSourceName, row) {}

	@Override
	Map<String, String> consumeResultingTableRows() {
		try {
			data
		}
		finally {
			data = null
		}
	}
}
