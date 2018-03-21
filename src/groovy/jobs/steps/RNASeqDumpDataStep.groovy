package jobs.steps

import com.google.common.collect.Lists
import org.transmartproject.core.dataquery.DataRow
import org.transmartproject.core.dataquery.highdim.AssayColumn
import org.transmartproject.core.dataquery.highdim.chromoregion.RegionRow
import org.transmartproject.core.dataquery.highdim.rnaseq.RnaSeqValues

class RNASeqDumpDataStep extends AbstractDumpHighDimensionalDataStep {

	private static final Map PER_ASSAY_COLUMNS = [readcount: { RnaSeqValues v -> v.getReadcount() }]

	int rowNumber = 1

	RNASeqDumpDataStep() {
		callPerColumn = false
	}

	@Override
	protected computeCsvRow(String subsetName, String seriesName, DataRow genericRow, AssayColumn column /* null */, cell /* null */) {
		RegionRow<RnaSeqValues> row = genericRow
		// +1 because the first column has no header
		List<String> line = Lists.newArrayListWithCapacity(csvHeader.size() + 1)
		line << rowNumber++ as String
		line << row.name
		line << row.chromosome
		line << row.start as String
		line << row.end as String
		line << row.numberOfProbes as String
		line << row.cytoband
		line << row.bioMarker as String

		int j = 8

		PER_ASSAY_COLUMNS.each {k, Closure<RnaSeqValues> value ->
			for (AssayColumn assay in assays) {
				line[j++] = value(row.getAt(assay)) as String
			}
		}

		line
	}

	@Lazy List<String> csvHeader = {
		List<String> r = ['regionname', 'chromosome', 'start', 'end', 'num.probes', 'cytoband', 'genesymbol']

		for (String head in PER_ASSAY_COLUMNS.keySet()) {
			for (AssayColumn assay in assays) {
				r << head + '.' + assay.patientInTrialId
			}
		}

		r
	}()

	@Lazy List<AssayColumn> assays = {
		results.values().iterator().next().indicesList
	}()
}
