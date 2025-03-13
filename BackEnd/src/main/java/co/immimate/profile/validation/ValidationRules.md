# ImmiMate Profile Validation Rules

This document outlines the validation rules applied to user immigration profiles in the ImmiMate application.

## Overview

Profile validation occurs at two levels:
1. **Bean Validation**: Annotations on the `ProfileSubmissionRequest` class that are automatically validated
2. **Service-Level Validation**: Additional business rules validated in the `ProfileService.validateProfileRequest()` method

## Basic Field Validations

### Personal Information
- **Email**: Must be provided and in valid format
- **Applicant Name**: Cannot be null or empty
- **Age**: Must be at least 16 and not more than 120
- **Citizenship**: Cannot be null or empty
- **Country of Residence**: Cannot be null or empty
- **Marital Status**: Must be one of: Single, Married, Common-Law, Separated, Divorced, Widowed

### Education
- **Education Level**: Cannot be null or empty
- **Canadian Education Level**: Optional, but if provided when education completed in Canada is true, must be valid

### Language Tests
- **Primary Language Test Type**: Must be one of: IELTS, CELPIP, TEF, TCF, PTE
- **Primary Test Scores**: All scores (Speaking, Listening, Reading, Writing) must be between 0 and 10
- **Secondary Language Test**: If indicated (tookSecondaryLanguageTest = true), then:
  - Secondary test type must be provided
  - All secondary scores must be provided and be between 0 and 10

### Work Experience
- **Canadian Work Experience**: Cannot be negative
- **Foreign Work Experience**: Cannot be negative
- **NOC Codes**: If work experience years > 0, corresponding NOC code must be provided

### Provincial Information
- **Province of Interest**: Cannot be null or empty
- **Provincial Nomination**: If true, province of interest must be specified

### Financial Information
- **Settlement Funds**: Cannot be negative

### Job Offer
- **Job Offer**: If true, then:
  - NOC code must be provided
  - Wage must be provided and positive

### Partner Information
- **Partner Data Consistency**: If marital status is "Single", then:
  - Partner education level should not be provided
  - Partner language test information should not be provided
  - Partner test scores should not be provided
  - Partner work experience should not be provided
- **Partner Test Scores**: If provided, must be between 0 and 9

## Logical Consistency Rules

The following rules ensure logical consistency across related fields:

1. **Secondary Language Test Consistency**:
   ```
   IF tookSecondaryLanguageTest = true
   THEN secondaryTestType, secondaryTestSpeakingScore, secondaryTestListeningScore, 
        secondaryTestReadingScore, secondaryTestWritingScore must all be provided
   ```

2. **Work Experience Consistency**:
   ```
   IF foreignWorkExperienceYears > 0
   THEN nocCodeForeign must be provided
   
   IF canadianWorkExperienceYears > 0
   THEN nocCodeCanadian should be provided
   ```

3. **Job Offer Consistency**:
   ```
   IF hasJobOffer = true
   THEN jobOfferNocCode must be provided
        jobOfferWageCAD must be provided and > 0
   ```

4. **Partner Data Consistency**:
   ```
   IF applicantMaritalStatus = "Single"
   THEN partnerEducationLevel, partnerLanguageTestType, partnerTestScores, 
        partnerCanadianWorkExperienceYears should all be null
   ```

5. **Provincial Nomination Consistency**:
   ```
   IF hasProvincialNomination = true
   THEN provinceOfInterest must be provided
   ```

## Error Handling

When validation fails:
1. Bean validation errors are returned as HTTP 400 Bad Request with details about which fields failed validation
2. Service-level validation returns a `ProfileSubmissionResponse` with:
   - `success` set to `false`
   - `message` containing a descriptive error message
   - `profileId` set to `null`

## Example Validation Errors

| Scenario | Error Message |
|----------|---------------|
| Missing applicant name | "Applicant name is required" |
| Negative age | "Invalid age: Age must be a positive number" |
| Invalid language score | "Invalid primary reading score: Must be between 0 and 9" |
| Missing job offer details | "Job offer NOC code is required when job offer is indicated" |
| Negative settlement funds | "Settlement funds cannot be negative" |
| Missing NOC code | "Foreign NOC code is required when foreign work experience is indicated" |
| Missing secondary language scores | "Secondary language speaking score is required when secondary test is indicated" |
| Partner data for single applicant | "Partner education level should not be provided for single applicants" |
| Missing province for nomination | "Province of interest is required when provincial nomination is indicated" |

## Implementation Notes

- Bean validation is enforced by the `@Valid` annotation on the controller method parameter
- Service-level validation is performed in the `ProfileService.validateProfileRequest()` method
- All validation errors are logged for debugging purposes

## Future Enhancements

- Consider adding cross-field validation using custom validation annotations
- Implement more specific validation for NOC codes based on the official classification
- Add validation for province names against a list of valid Canadian provinces 