package jobs

import com.recomdata.transmart.util.ZipService
import jobs.steps.AcghRegionDumpDataStep
import jobs.steps.BuildTableResultStep
import jobs.steps.MultiRowAsGroupDumpTableResultsStep
import jobs.steps.OpenHighDimensionalDataStep
import jobs.steps.RCommandsStep
import jobs.steps.Step
import jobs.steps.WriteFileStep
import jobs.steps.ZipResultsStep
import jobs.steps.helpers.CensorColumnConfigurator
import jobs.steps.helpers.NumericColumnConfigurator
import jobs.steps.helpers.SimpleAddColumnConfigurator
import jobs.table.Table
import jobs.table.columns.PrimaryKeyColumn
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.core.dataquery.highdim.HighDimensionResource

import static jobs.steps.AbstractDumpStep.DEFAULT_OUTPUT_FILE_NAME

@Component
@Scope('job')
class AcghSurvivalAnalysis extends AbstractAnalysisJob implements InitializingBean {

	@Autowired
	HighDimensionResource highDimensionResource

	@Autowired
	SimpleAddColumnConfigurator primaryKeyColumnConfigurator

	@Autowired
	NumericColumnConfigurator timeVariableConfigurator

	@Autowired
	CensorColumnConfigurator censoringConfigurator

	@Autowired
	MessageSource messageSource

	@Autowired
	Table table

	@Autowired
	GrailsApplication grailsApplication

	@Autowired
	ZipService zipService

	void afterPropertiesSet() {
		primaryKeyColumnConfigurator.column = new PrimaryKeyColumn(header: 'PATIENT_NUM')

		configureTimeVariableConfigurator()
		configureCensoringVariableConfigurator()
	}

	void configureTimeVariableConfigurator() {
		timeVariableConfigurator.header = 'TIME'
		timeVariableConfigurator.setKeys('time')
		timeVariableConfigurator.alwaysClinical = true
	}

	void configureCensoringVariableConfigurator() {
		censoringConfigurator.header = 'CENSOR'
		censoringConfigurator.keyForConceptPaths = 'censoringVariable'
	}

	protected List<Step> prepareSteps() {
		List<Step> steps = []

		steps << new WriteFileStep(
				temporaryDirectory: temporaryDirectory,
				fileName: 'README.txt',
				fileContent: messageSource.getMessage("jobs.SurvivalAnalysis.readmeFileContent", null, null, null))

		steps << new BuildTableResultStep(
				table: table,
				configurators: [primaryKeyColumnConfigurator,
				                timeVariableConfigurator,
				                censoringConfigurator,
				])

		steps << new MultiRowAsGroupDumpTableResultsStep(
				table: table,
				temporaryDirectory: temporaryDirectory,
				outputFileName: 'phenodata.tsv')

		def openResultSetStep = new OpenHighDimensionalDataStep(
				params: params,
				dataTypeResource: highDimensionResource.getSubResourceForType(analysisConstraints['data_type']),
				analysisConstraints: analysisConstraints)

		steps << openResultSetStep

		steps << createDumpHighDimensionDataStep { -> openResultSetStep.results }

		steps << new RCommandsStep(
				temporaryDirectory: temporaryDirectory,
				scriptsDirectory: scriptsDirectory,
				rStatements: RStatements,
				studyName: studyName,
				params: params,
				extraParams: [inputFileName: DEFAULT_OUTPUT_FILE_NAME])

		steps << new ZipResultsStep(jobName: params.jobName,
				grailsApplication: grailsApplication,
				zipService: zipService)

		steps
	}

	@Override
	protected Step createDumpHighDimensionDataStep(Closure resultsHolder) {
		new AcghRegionDumpDataStep(
				temporaryDirectory: temporaryDirectory,
				resultsHolder: resultsHolder,
				params: params)
	}

	@Override
	protected List<String> getRStatements() {
		[
				'''source('$pluginDirectory/aCGH/acgh-survival-test.R')''',
				'''acgh.survival.test(survival               = 'TIME',
                                      censor                 = 'CENSOR',
                                      number.of.permutations = $numberOfPermutations,
                                      test.aberrations       = '$aberrationType')''',
				'''source('$pluginDirectory/aCGH/acgh-plot-survival.R')''',
				'''acgh.plot.survival(survival             = 'TIME',
                                      censor               = 'CENSOR',
                                      aberrations          = '$aberrationType',
                                      confidence.intervals = '$confidenceIntervals')'''
		]
	}

	@Override
	protected String getForwardPath() {
		"/aCGHSurvivalAnalysis/aCGHSurvivalAnalysisOutput?jobName=${name}"
	}
}
