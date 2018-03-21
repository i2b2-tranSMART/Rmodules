package jobs.table.columns.binning

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class NumericBinRange {
	BigDecimal from
	BigDecimal to
}
