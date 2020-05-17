# MeasureReport to CSV
A Coping Mechanism to convert a MeasureReport resource to a CSV File

As a user with a measure collection process that has MeasureReport resource, but must report using a spreadsheet or CSV file,
I want to convert the FHIR MeasureReport resource spreadsheet to to a CSV file
so that I can automate the transmission of measures to a CSV based endpoint.

## Acceptance Criteria
Given a {MeasureReport} 

And a {Measure} resource

And {mappings} from the MeasureReport data elements to CSV Columns

When I process the {MeasureReport} using the service 

Then I have a {CSV} file which contains the data in the {MeasureReport}

And it has all {required components}

And the CSV file is valid according to [RFC4180](https://tools.ietf.org/html/rfc4180)

And the Column heads match those in the example CSV given by the {Measure} (e.g., in Measure.relatedArtifact.where(type.code='documentation'))
