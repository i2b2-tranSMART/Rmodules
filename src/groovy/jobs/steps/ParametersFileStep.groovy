package jobs.steps

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import jobs.UserParameters

@CompileStatic
class ParametersFileStep implements Step {

	File temporaryDirectory
	UserParameters params

	final String statusName = 'Writing parameters'

	void execute() {
		File jobInfoFile = new File(temporaryDirectory, 'jobInfo.txt')

		jobInfoFile.withWriter { BufferedWriter writer ->
			writer.writeLine 'Parameters'
			params.each { key, value -> writer.writeLine "\t$key -> $value" 	}
		}

		new File(temporaryDirectory, 'request.json') << JsonOutput.prettyPrint(params.toJSON())
	}
}
