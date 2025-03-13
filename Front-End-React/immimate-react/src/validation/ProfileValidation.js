/**
 * Profile Validation Functions for ImmiMate Frontend
 * 
 * This file contains validation functions that mirror the backend validation rules
 * for the profile submission form. These functions can be used with form libraries
 * like Formik or React Hook Form.
 */

/**
 * Validates the entire profile form
 * @param {Object} values - The form values
 * @returns {Object} - Validation errors object
 */
export const validateProfileForm = (values) => {
  const errors = {};
  
  // Basic field validations
  validatePersonalInfo(values, errors);
  validateEducation(values, errors);
  validateLanguageTests(values, errors);
  validateWorkExperience(values, errors);
  validateProvincialInfo(values, errors);
  validateFinancialInfo(values, errors);
  validateJobOffer(values, errors);
  validatePartnerInfo(values, errors);
  
  return errors;
};

/**
 * Validates personal information fields
 */
const validatePersonalInfo = (values, errors) => {
  // Email validation
  if (!values.userEmail) {
    errors.userEmail = 'Email is required';
  } else if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i.test(values.userEmail)) {
    errors.userEmail = 'Invalid email format';
  }
  
  // Applicant name validation
  if (!values.applicantName) {
    errors.applicantName = 'Applicant name is required';
  }
  
  // Age validation
  if (!values.applicantAge) {
    errors.applicantAge = 'Age is required';
  } else if (values.applicantAge < 16) {
    errors.applicantAge = 'Age must be at least 16';
  } else if (values.applicantAge > 120) {
    errors.applicantAge = 'Age must be realistic (maximum 120)';
  }
  
  // Citizenship validation
  if (!values.applicantCitizenship) {
    errors.applicantCitizenship = 'Citizenship is required';
  }
  
  // Country of residence validation
  if (!values.applicantResidence) {
    errors.applicantResidence = 'Country of residence is required';
  }
  
  // Marital status validation
  const validMaritalStatuses = ['Single', 'Married', 'Common-Law', 'Separated', 'Divorced', 'Widowed'];
  if (!values.applicantMaritalStatus) {
    errors.applicantMaritalStatus = 'Marital status is required';
  } else if (!validMaritalStatuses.includes(values.applicantMaritalStatus)) {
    errors.applicantMaritalStatus = 'Marital status must be one of: Single, Married, Common-Law, Separated, Divorced, Widowed';
  }
};

/**
 * Validates education fields
 */
const validateEducation = (values, errors) => {
  // Education level validation
  if (!values.applicantEducationLevel) {
    errors.applicantEducationLevel = 'Education level is required';
  }
  
  // Canadian education level validation
  if (values.educationCompletedInCanada && !values.canadianEducationLevel) {
    errors.canadianEducationLevel = 'Canadian education level is required when education completed in Canada is selected';
  }
};

/**
 * Validates language test fields
 */
const validateLanguageTests = (values, errors) => {
  // Primary language test type validation
  const validTestTypes = ['IELTS', 'CELPIP', 'TEF', 'TCF', 'PTE'];
  if (!values.primaryLanguageTestType) {
    errors.primaryLanguageTestType = 'Primary language test type is required';
  } else if (!validTestTypes.includes(values.primaryLanguageTestType)) {
    errors.primaryLanguageTestType = `Language test type must be one of: ${validTestTypes.join(', ')}`;
  }
  
  // Primary test scores validation - now using CLB values
  const validateCLBScore = (score, fieldName) => {
    if (score === undefined || score === null) {
      errors[fieldName] = `${fieldName.replace('primaryTest', '').replace('Test', '')} score is required`;
    } else if (score < 1 || score > 12) {
      errors[fieldName] = `${fieldName.replace('primaryTest', '').replace('Test', '')} CLB score must be between 1 and 12`;
    }
  };
  
  validateCLBScore(values.primaryTestSpeakingScore, 'primaryTestSpeakingScore');
  validateCLBScore(values.primaryTestListeningScore, 'primaryTestListeningScore');
  validateCLBScore(values.primaryTestReadingScore, 'primaryTestReadingScore');
  validateCLBScore(values.primaryTestWritingScore, 'primaryTestWritingScore');
  
  // Secondary language test validation
  if (values.tookSecondaryLanguageTest) {
    if (!values.secondaryTestType) {
      errors.secondaryTestType = 'Secondary language test type is required when secondary test is indicated';
    } else if (!validTestTypes.includes(values.secondaryTestType)) {
      errors.secondaryTestType = `Secondary language test type must be one of: ${validTestTypes.join(', ')}`;
    }
    
    // Secondary test scores validation - using CLB values
    if (values.secondaryTestSpeakingScore === undefined || values.secondaryTestSpeakingScore === null) {
      errors.secondaryTestSpeakingScore = 'Secondary language speaking score is required when secondary test is indicated';
    } else if (values.secondaryTestSpeakingScore < 1 || values.secondaryTestSpeakingScore > 12) {
      errors.secondaryTestSpeakingScore = 'Secondary speaking CLB score must be between 1 and 12';
    }
    
    if (values.secondaryTestListeningScore === undefined || values.secondaryTestListeningScore === null) {
      errors.secondaryTestListeningScore = 'Secondary language listening score is required when secondary test is indicated';
    } else if (values.secondaryTestListeningScore < 1 || values.secondaryTestListeningScore > 12) {
      errors.secondaryTestListeningScore = 'Secondary listening CLB score must be between 1 and 12';
    }
    
    if (values.secondaryTestReadingScore === undefined || values.secondaryTestReadingScore === null) {
      errors.secondaryTestReadingScore = 'Secondary language reading score is required when secondary test is indicated';
    } else if (values.secondaryTestReadingScore < 1 || values.secondaryTestReadingScore > 12) {
      errors.secondaryTestReadingScore = 'Secondary reading CLB score must be between 1 and 12';
    }
    
    if (values.secondaryTestWritingScore === undefined || values.secondaryTestWritingScore === null) {
      errors.secondaryTestWritingScore = 'Secondary language writing score is required when secondary test is indicated';
    } else if (values.secondaryTestWritingScore < 1 || values.secondaryTestWritingScore > 12) {
      errors.secondaryTestWritingScore = 'Secondary writing CLB score must be between 1 and 12';
    }
  }
  
  // Partner language test validation (if applicable)
  if (values.partnerLanguageTestType) {
    if (!validTestTypes.includes(values.partnerLanguageTestType)) {
      errors.partnerLanguageTestType = `Partner language test type must be one of: ${validTestTypes.join(', ')}`;
    }
    
    // Partner test scores validation - using CLB values
    if (values.partnerTestSpeakingScore !== undefined && values.partnerTestSpeakingScore !== null) {
      if (values.partnerTestSpeakingScore < 1 || values.partnerTestSpeakingScore > 12) {
        errors.partnerTestSpeakingScore = 'Partner speaking CLB score must be between 1 and 12';
      }
    }
    
    if (values.partnerTestListeningScore !== undefined && values.partnerTestListeningScore !== null) {
      if (values.partnerTestListeningScore < 1 || values.partnerTestListeningScore > 12) {
        errors.partnerTestListeningScore = 'Partner listening CLB score must be between 1 and 12';
      }
    }
    
    if (values.partnerTestReadingScore !== undefined && values.partnerTestReadingScore !== null) {
      if (values.partnerTestReadingScore < 1 || values.partnerTestReadingScore > 12) {
        errors.partnerTestReadingScore = 'Partner reading CLB score must be between 1 and 12';
      }
    }
    
    if (values.partnerTestWritingScore !== undefined && values.partnerTestWritingScore !== null) {
      if (values.partnerTestWritingScore < 1 || values.partnerTestWritingScore > 12) {
        errors.partnerTestWritingScore = 'Partner writing CLB score must be between 1 and 12';
      }
    }
  }
};

/**
 * Validates work experience fields
 */
const validateWorkExperience = (values, errors) => {
  // Canadian work experience validation
  if (values.canadianWorkExperienceYears === undefined || values.canadianWorkExperienceYears === null) {
    errors.canadianWorkExperienceYears = 'Canadian work experience is required';
  } else if (values.canadianWorkExperienceYears < 0) {
    errors.canadianWorkExperienceYears = 'Canadian work experience years cannot be negative';
  }
  
  // Canadian NOC code validation
  if (values.canadianWorkExperienceYears > 0 && !values.nocCodeCanadian) {
    errors.nocCodeCanadian = 'Canadian NOC code is required when Canadian work experience is indicated';
  }
  
  // Foreign work experience validation
  if (values.foreignWorkExperienceYears === undefined || values.foreignWorkExperienceYears === null) {
    errors.foreignWorkExperienceYears = 'Foreign work experience is required';
  } else if (values.foreignWorkExperienceYears < 0) {
    errors.foreignWorkExperienceYears = 'Foreign work experience years cannot be negative';
  }
  
  // Foreign NOC code validation
  if (values.foreignWorkExperienceYears > 0 && !values.nocCodeForeign) {
    errors.nocCodeForeign = 'Foreign NOC code is required when foreign work experience is indicated';
  }
};

/**
 * Validates provincial information fields
 */
const validateProvincialInfo = (values, errors) => {
  // Province of interest validation
  if (!values.provinceOfInterest) {
    errors.provinceOfInterest = 'Province of interest is required';
  }
  
  // Provincial nomination validation
  if (values.hasProvincialNomination && !values.provinceOfInterest) {
    errors.provinceOfInterest = 'Province of interest is required when provincial nomination is indicated';
  }
  
  // Canadian relatives validation
  if (values.hasCanadianRelatives && !values.relationshipWithCanadianRelative) {
    errors.relationshipWithCanadianRelative = 'Relationship with Canadian relative is required when Canadian relatives is indicated';
  }
};

/**
 * Validates financial information fields
 */
const validateFinancialInfo = (values, errors) => {
  // Settlement funds validation
  if (values.settlementFundsCAD === undefined || values.settlementFundsCAD === null) {
    errors.settlementFundsCAD = 'Settlement funds are required';
  } else if (values.settlementFundsCAD < 0) {
    errors.settlementFundsCAD = 'Settlement funds cannot be negative';
  }
  
  // Preferred city validation
  if (!values.preferredCity) {
    errors.preferredCity = 'Preferred city is required';
  }
  
  // Preferred destination province validation
  if (!values.preferredDestinationProvince) {
    errors.preferredDestinationProvince = 'Preferred destination province is required';
  }
};

/**
 * Validates job offer fields
 */
const validateJobOffer = (values, errors) => {
  // Job offer validation
  if (values.hasJobOffer) {
    if (!values.jobOfferNocCode) {
      errors.jobOfferNocCode = 'Job offer NOC code is required when job offer is indicated';
    }
    
    if (values.jobOfferWageCAD === undefined || values.jobOfferWageCAD === null) {
      errors.jobOfferWageCAD = 'Job offer wage is required when job offer is indicated';
    } else if (values.jobOfferWageCAD < 0) {
      errors.jobOfferWageCAD = 'Job offer wage cannot be negative';
    }
  }
};

/**
 * Validates partner information fields
 */
const validatePartnerInfo = (values, errors) => {
  // Partner data consistency validation
  if (values.applicantMaritalStatus === 'Single') {
    if (values.partnerEducationLevel) {
      errors.partnerEducationLevel = 'Partner education level should not be provided for single applicants';
    }
    
    if (values.partnerLanguageTestType) {
      errors.partnerLanguageTestType = 'Partner language test information should not be provided for single applicants';
    }
    
    if (values.partnerTestSpeakingScore !== undefined && values.partnerTestSpeakingScore !== null) {
      errors.partnerTestSpeakingScore = 'Partner language scores should not be provided for single applicants';
    }
    
    if (values.partnerTestListeningScore !== undefined && values.partnerTestListeningScore !== null) {
      errors.partnerTestListeningScore = 'Partner language scores should not be provided for single applicants';
    }
    
    if (values.partnerTestReadingScore !== undefined && values.partnerTestReadingScore !== null) {
      errors.partnerTestReadingScore = 'Partner language scores should not be provided for single applicants';
    }
    
    if (values.partnerTestWritingScore !== undefined && values.partnerTestWritingScore !== null) {
      errors.partnerTestWritingScore = 'Partner language scores should not be provided for single applicants';
    }
    
    if (values.partnerCanadianWorkExperienceYears !== undefined && values.partnerCanadianWorkExperienceYears !== null) {
      errors.partnerCanadianWorkExperienceYears = 'Partner work experience should not be provided for single applicants';
    }
  }
  
  // Partner test scores validation
  if (values.partnerTestSpeakingScore !== undefined && values.partnerTestSpeakingScore !== null) {
    if (values.partnerTestSpeakingScore < 0 || values.partnerTestSpeakingScore > 10) {
      errors.partnerTestSpeakingScore = 'Partner speaking score must be between 0 and 10';
    }
  }
  
  if (values.partnerTestListeningScore !== undefined && values.partnerTestListeningScore !== null) {
    if (values.partnerTestListeningScore < 0 || values.partnerTestListeningScore > 10) {
      errors.partnerTestListeningScore = 'Partner listening score must be between 0 and 10';
    }
  }
  
  if (values.partnerTestReadingScore !== undefined && values.partnerTestReadingScore !== null) {
    if (values.partnerTestReadingScore < 0 || values.partnerTestReadingScore > 10) {
      errors.partnerTestReadingScore = 'Partner reading score must be between 0 and 10';
    }
  }
  
  if (values.partnerTestWritingScore !== undefined && values.partnerTestWritingScore !== null) {
    if (values.partnerTestWritingScore < 0 || values.partnerTestWritingScore > 10) {
      errors.partnerTestWritingScore = 'Partner writing score must be between 0 and 10';
    }
  }
  
  // Partner work experience validation
  if (values.partnerCanadianWorkExperienceYears !== undefined && values.partnerCanadianWorkExperienceYears !== null) {
    if (values.partnerCanadianWorkExperienceYears < 0) {
      errors.partnerCanadianWorkExperienceYears = 'Partner Canadian work experience years cannot be negative';
    }
  }
};

/**
 * Example usage with React Hook Form:
 * 
 * import { useForm } from 'react-hook-form';
 * import { validateProfileForm } from './validation/ProfileValidation';
 * 
 * function ProfileForm() {
 *   const { register, handleSubmit, formState: { errors } } = useForm({
 *     validate: validateProfileForm
 *   });
 *   
 *   const onSubmit = (data) => {
 *     // Submit data to backend
 *   };
 *   
 *   return (
 *     <form onSubmit={handleSubmit(onSubmit)}>
 *       <input {...register('userEmail')} />
 *       {errors.userEmail && <span>{errors.userEmail}</span>}
 *       
 *      
 *       
 *       <button type="submit">Submit</button>
 *     </form>
 *   );
 * }
 */ 