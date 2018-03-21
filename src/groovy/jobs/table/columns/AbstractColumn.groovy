package jobs.table.columns

import com.google.common.base.Objects
import groovy.transform.CompileStatic
import jobs.table.BackingMap
import jobs.table.Column
import jobs.table.MissingValueAction

@CompileStatic
abstract class AbstractColumn implements Column {

	String header

	MissingValueAction missingValueAction = new MissingValueAction.DropRowMissingValueAction()

	void onDataSourceDepleted(String dataSourceName, Iterable dataSource) {
		// override to do something here
	}

	void beforeDataSourceIteration(String dataSourceName, Iterable dataSource) {
		// override to do something here
	}

	void onAllDataSourcesDepleted(int columnNumber, BackingMap backingMap) {
		// override to do something here
	}

	Closure getValueTransformer() {}

	String toString() {
		Objects.toStringHelper(this).add('header', header).toString()
	}
}
