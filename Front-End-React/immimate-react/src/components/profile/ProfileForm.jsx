import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import axios from 'axios'; // Add axios import
import useFormDraft from '../../hooks/useFormDraft';
import { useAuth } from '../../contexts/AuthContext';
import apiService from '../../services/ApiService';
import Logger from '../../utils/LoggingService';
import Select from 'react-select';

// Import validation
// eslint-disable-next-line no-unused-vars
import { validateProfileForm } from '../../validation/ProfileValidation';

// Import styles
import '../../styles/ProfileForm.css';
import '../../styles/FormPage.css';

// Import utils
import useLanguageTestConverter, { getScoreRangeDescription } from '../../hooks/useLanguageTestConverter';

// Import components
import FormSummaryModal from './FormSummaryModal';

// Replace the ValidationSummary component to use existing CSS classes
const ValidationSummary = ({ isOpen, onClose, errors, scrollToSection }) => {
  if (!isOpen) return null;
  
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header error-display">
          <h3>Validation Errors</h3>
          <button className="close-button" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <p>Please fix the following issues before submitting your profile:</p>
          <ul className="error-list">
            {errors.map((error, index) => (
              <li 
                key={index} 
                className="error-item"
                onClick={() => {
                  onClose();
                  scrollToSection(error.sectionId);
                }}
              >
                <span>{error.message}</span>
                <small>(Click to navigate to this section)</small>
              </li>
            ))}
          </ul>
        </div>
        <div className="modal-footer">
          <button className="btn-primary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

// Map section IDs to readable names (moved outside component to avoid duplication)
const sectionNames = {
  'personal-info': 'Personal Information',
  'education': 'Education',
  'language': 'Language Proficiency',
  'secondary-language': 'Secondary Language',
  'work-experience': 'Work Experience',
  'spouse': 'Spouse / Partner',
  'job-offer': 'Job Offer',
  'provincial': 'Provincial Nomination',
  'additional': 'Additional Information'
};

const ProfileForm = () => {
  // Configuration
  // const API_BASE_URL = 'http://localhost:8080/api'; // No longer needed - using apiService
  
  // Use the formDraft hook for automatic saving and loading
  const {
    formData,
    // eslint-disable-next-line no-unused-vars
    loadDraft, // Keeping for future use but currently handled by the hook internally
    saveDraft,
    discardDraft,
    isLoading: isDraftLoading,
    isSaving,
    saveStatus,
    // eslint-disable-next-line no-unused-vars
    draftError, // Keeping for potential error handling improvements
    hasUnsavedChanges,
    updateFormData,
  } = useFormDraft({
    formId: 'profile-form',
    initialData: {},
    autoSaveInterval: 30000, // 30 seconds
    enabled: true,
    expirationHours: 24 // Keep draft data for 24 hours (1 day)
  });
  
  // Add the language test converter hook at the top level
  const { 
    convertToCLB, 
    areSameLanguage, 
    getSecondaryTestOptions,
    getScoreOptions
  } = useLanguageTestConverter();

  // State for the form sections
  const [openSections, setOpenSections] = useState({
    'personal-info': true,
    'language': false,
    'secondary-language': false,
    'education': false,
    'work': false,
    'spouse': false,
    'canada-connection': false,
    'additional-info': false
  });
  
  // State for info blocks to track which ones are open
  const [openInfoBlocks, setOpenInfoBlocks] = useState({
    'age-info': true,
    'education-level-info': true,
    'study-canada-info': true,
    'language-requirements-info': true,
    'secondary-language-info': true,
    'partner-language-info': true,
    'canadian-work-info': true,
    'foreign-work-info': true,
    'trades-certification-info': true,
    'partner-education-info': true,
    'partner-work-info': true,
    'job-offer-info': true,
    'noc-teer-info': true,
    'provincial-nomination-info': true,
    'canadian-relatives-info': true,
    'ita-info': true,
    'preferred-destination-info': true,
    'settlement-funds-info': true
  });
  
  // Wrapper function for score options to maintain backward compatibility
  const getTestScoreOptions = useCallback((testType, skill) => {
    // Simply delegate to the hook's function
    return getScoreOptions(testType, skill);
  }, [getScoreOptions]);
  
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  // Add formRef definition
  const formRef = useRef(null);

  // Debug Auth Token
  useEffect(() => {
    // Using cookies now, so we don't need to check localStorage for tokens
    // Just check if we have a currentUser instead
    if (!currentUser) {
      Logger.warn('DEBUG: No authenticated user found');
    } else {
      Logger.debug('DEBUG: User authenticated:', currentUser.email);
    }
    
    // Also log user email and ID
    Logger.debug('DEBUG: user_email in localStorage:', localStorage.getItem('user_email'));
    Logger.debug('DEBUG: user_id in localStorage:', localStorage.getItem('user_id'));
    Logger.debug('DEBUG: currentUser object:', currentUser);
  }, [currentUser]);

  // Check authentication state
  useEffect(() => {
    if (!currentUser) {
      // If not authenticated, redirect to login
      Logger.warn('ProfileForm: No authenticated user found, redirecting to login');
      navigate('/login');
    }
  }, [currentUser, navigate]);

  // Fetch CSRF token on component mount
  useEffect(() => {
    // Fetch CSRF token when component mounts
    // Add a flag to ensure we only fetch once
    let hasFetchedToken = false;
    
    const fetchCsrfToken = async () => {
      // Prevent multiple fetches
      if (hasFetchedToken) return;
      
      try {
        hasFetchedToken = true;
        
        // Only fetch CSRF token if user is authenticated
        if (currentUser) {
          await apiService.fetchCsrfToken();
          Logger.debug('CSRF token fetched during component initialization');
        } else {
          Logger.debug('Skipping CSRF token fetch - user not authenticated');
        }
      } catch (error) {
        // Don't set hasFetchedToken back to false on errors
        // to prevent infinite retry loops
        Logger.error('Failed to fetch CSRF token during initialization:', error);
      }
    };
    
    fetchCsrfToken();
    
    // Cleanup function to cancel any pending operations
    return () => {
      hasFetchedToken = true; // Prevent further fetches during unmount
    };
  }, [currentUser]); // Only re-run if currentUser changes

  // --------------------- STATE MANAGEMENT --------------------- //
  const [formErrors, setFormErrors] = useState([]);
  const [validationSummary, setValidationSummary] = useState({
    showSummary: false,
    errors: []
  });
  const [apiResponse, setApiResponse] = useState(null);
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [hasSubmittedProfile, setHasSubmittedProfile] = useState(false);
  const [showSummaryModal, setShowSummaryModal] = useState(false);
  const [tempFormData, setTempFormData] = useState(null);
  // eslint-disable-next-line no-unused-vars
  const [progressValue, setProgressValue] = useState(0);

  // Calculate section completion status
  const [sectionStatus, setSectionStatus] = useState({
    'personal-info': { complete: false, active: true, hasErrors: false, touched: false },
    'education': { complete: false, active: false, hasErrors: false, touched: false },
    'language': { complete: false, active: false, hasErrors: false, touched: false },
    'secondary-language': { complete: false, active: false, hasErrors: false, touched: false },
    'work-experience': { complete: false, active: false, hasErrors: false, touched: false },
    'spouse': { complete: false, active: false, hasErrors: false, touched: false },
    'job-offer': { complete: false, active: false, hasErrors: false, touched: false },
    'provincial': { complete: false, active: false, hasErrors: false, touched: false },
    'additional': { complete: false, active: false, hasErrors: false, touched: false }
  });

  // Replace localStorage functions with useFormDraft methods
  const loadSavedData = useCallback(() => {
    // Form data is already loaded by the useFormDraft hook
    try {
      // Fetch CSRF token separately
      apiService.fetchCsrfToken()
        .then(() => Logger.debug('CSRF token fetched during form initialization'))
        .catch(err => Logger.error('Error fetching CSRF token:', err));
      
      // Return form data from the hook - must return synchronously
      return formData || {};
    } catch (err) {
      Logger.error('Error loading saved form data:', err);
      return {};
    }
  }, [formData]);

  // eslint-disable-next-line no-unused-vars
  const saveData = useCallback((data) => {
    // Ensure data is what we expect
    Logger.debug('Saving form data, fullName value:', data.fullName);
    
    // Only update if the data has actually changed
    const dataStr = JSON.stringify(data);
    const formDataStr = JSON.stringify(formData);
    
    if (dataStr !== formDataStr) {
      updateFormData(data);
      saveDraft(); // Trigger manual save for immediate persistence
      Logger.debug('Form data updated and saved via draft service');
    }
  }, [updateFormData, saveDraft, formData]);

  // --------------------- REACT HOOK FORM --------------------- //
  const {
    register,
    handleSubmit,
    watch,
    // eslint-disable-next-line no-unused-vars
    setValue, // Used for programmatically setting form values
    formState: { errors },
    // eslint-disable-next-line no-unused-vars
    getValues,
    reset,
    clearErrors,
    control
  } = useForm({
    defaultValues: loadSavedData()
    // If using a Yup schema, you can do: resolver: yupResolver(validationSchema)
  });

  // Watch all fields for localStorage + progress bar
  const watchAllFields = watch();

  // Keep track of whether we're in the middle of a reset operation
  const justResetRef = useRef(false);
  // Keep track of last saved data to prevent unnecessary updates
  const lastSavedDataRef = useRef({});

  // Reset form with draft data when it's loaded
  useEffect(() => {
    if (!isDraftLoading && formData) {
      // Check if the form data is actually different from what's already in the form
      const currentFormData = getValues();
      const formDataStr = JSON.stringify(formData);
      const currentFormDataStr = JSON.stringify(currentFormData);
      
      if (formDataStr !== currentFormDataStr) {
        Logger.debug('Resetting form with loaded draft data', {
          formId: 'profile-form',
          fieldsCount: Object.keys(formData).length,
          hasJobOffer: formData.jobOffer ? 'Yes' : 'No',
          timestamp: new Date().toISOString()
        });
        
        // Set the flag to indicate we're resetting
        justResetRef.current = true;
        reset(formData);
        
        // Clear the flag after a short delay
        setTimeout(() => {
          justResetRef.current = false;
        }, 300);
      }
    }
  }, [isDraftLoading, formData, reset, getValues]);

  // Save form data when fields change
  useEffect(() => {
    // Skip initial render and only save if form has been loaded
    if (isDraftLoading) return;
    
    // Avoid saving if we just reset the form
    if (justResetRef.current) return;
    
    // Check if watchAllFields has actually changed since last save
    const watchAllFieldsStr = JSON.stringify(watchAllFields);
    const lastSavedStr = JSON.stringify(lastSavedDataRef.current);
    
    if (watchAllFieldsStr === lastSavedStr) return;
    
    // Add a small delay to avoid too frequent saves
    const timer = setTimeout(() => {
      // Use the saveData function to update formData in the draft hook
      Logger.debug('Form fields changed, saving draft data');
      saveData(watchAllFields);
      // Update our reference to the last saved data
      lastSavedDataRef.current = {...watchAllFields};
    }, 1000); // 1000ms (1 second) debounce
    
    return () => clearTimeout(timer);
  }, [watchAllFields, saveData, isDraftLoading]);

  // Watch specific fields to automatically clean others when they change
  const watchMaritalStatus = watch('applicantMaritalStatus');
  const watchJobOffer = watch('jobOffer');
  const watchTookSecondaryLanguage = watch('tookSecondaryLanguageTest');
  const watchEducationInCanada = watch('eduInCanada');
  const watchHasProvincialNomination = watch('hasProvincialNomination');
  const watchHasCanadianRelatives = watch('hasCanadianRelatives');
  const watchForeignWorkExperience = watch('foreignExp');
  const watchPrimaryLanguageTest = watch('primaryLanguageTest');
  const watchSpouseIsPR = watch('spouseIsPR');
  const watchSpouseIsAccompanying = watch('spouseIsAccompanying');
  
  const watchApplicantAge = watch('applicantAge');
  const watchSettlementFunds = watch('settlementFunds');
  // We now use watchAllFields.jobWage directly in the validation effect
  // const watchJobOfferWage = watch('jobOfferWage');
  // Use useMemo for watchPrimaryLanguageScores to prevent recreation on every render
  const watchPrimaryLanguageScores = useMemo(() => [
    watch('primarySpeakingScore'),
    watch('primaryListeningScore'),
    watch('primaryReadingScore'),
    watch('primaryWritingScore')
  ], [watch]);

  // Custom error state management for immediate field validation feedback
  // eslint-disable-next-line no-unused-vars
  const [customErrors, setCustomErrors] = useState({});
  
  // Add state for section error highlights
  // eslint-disable-next-line no-unused-vars
  const [sectionErrorHighlights, setSectionErrorHighlights] = useState({});
  
  const setCustomError = useCallback((field, message) => {
    setCustomErrors(prev => ({
      ...prev,
      [field]: message
    }));
  }, []);
  
  const clearCustomError = useCallback((field) => {
    setCustomErrors(prev => {
      const newErrors = { ...prev };
      delete newErrors[field];
      return newErrors;
    });
  }, []);

  // Reset secondary language scores when not taking a secondary test
  useEffect(() => {
    if (watchTookSecondaryLanguage === 'no') {
      // Reset all secondary language fields
      setValue('secondaryLanguageTest', null);
      setValue('secSpeaking', null);
      setValue('secListening', null);
      setValue('secReading', null);
      setValue('secWriting', null);
      
      Logger.info('Cleared secondary language fields because tookSecondaryLanguageTest is no');
    }
  }, [watchTookSecondaryLanguage, setValue]);
  
  // Reset secondary language test when primary language test changes
  useEffect(() => {
    if (watchPrimaryLanguageTest) {
      const currentSecondaryTest = watch('secondaryLanguageTest');
      
      // If secondary test is already selected, check if they're the same language
      if (currentSecondaryTest && areSameLanguage(watchPrimaryLanguageTest, currentSecondaryTest)) {
        // Clear secondary language test if it's the same language as primary
        setValue('secondaryLanguageTest', '');
        clearErrors('secondaryLanguageTest');
        
        // Also clear secondary scores
        setValue('secSpeaking', '');
        setValue('secListening', '');
        setValue('secReading', '');
        setValue('secWriting', '');
      }
    }
  }, [watchPrimaryLanguageTest, watch, setValue, clearErrors, areSameLanguage]);
  
  // Validate applicant age
  useEffect(() => {
    if (watchApplicantAge !== undefined && watchApplicantAge !== null && watchApplicantAge !== '') {
      const age = Number(watchApplicantAge);
      if (isNaN(age)) {
        setCustomError('applicantAge', 'Age must be a number');
      } else if (age <= 0) {
        setCustomError('applicantAge', 'Age must be positive');
      } else if (age > 120) {
        setCustomError('applicantAge', 'Age must be reasonable (under 120)');
      } else {
        clearCustomError('applicantAge');
      }
    }
  }, [watchApplicantAge, setCustomError, clearCustomError]);

  // Validate settlement funds
  useEffect(() => {
    if (watchSettlementFunds !== undefined && watchSettlementFunds !== null && watchSettlementFunds !== '') {
      const funds = Number(watchSettlementFunds);
      if (isNaN(funds)) {
        setCustomError('settlementFunds', 'Settlement funds must be a number');
      } else if (funds < 0) {
        setCustomError('settlementFunds', 'Settlement funds must be non-negative');
      } else {
        clearCustomError('settlementFunds');
      }
    }
  }, [watchSettlementFunds, setCustomError, clearCustomError]);

  // Validate job offer wage
  useEffect(() => {
    if (watchJobOffer === 'yes' && 
        watchAllFields.jobWage !== undefined && 
        watchAllFields.jobWage !== null && 
        watchAllFields.jobWage !== '') {
      const wage = Number(watchAllFields.jobWage);
      if (isNaN(wage)) {
        setCustomError('jobWage', 'Job offer wage must be a number');
      } else if (wage <= 0) {
        setCustomError('jobWage', 'Job offer wage must be positive');
      } else {
        clearCustomError('jobWage');
      }
    }
  }, [watchJobOffer, watchAllFields.jobWage, setCustomError, clearCustomError]);

  // Validate primary language scores are all provided
  useEffect(() => {
    // Check if at least one score is provided
    const hasAnyScore = watchPrimaryLanguageScores.some(score => 
      score !== undefined && score !== null && score !== '');
    
    // Check if all scores are provided
    const hasAllScores = watchPrimaryLanguageScores.every(score => 
      score !== undefined && score !== null && score !== '');
    
    // If some scores are provided but not all, show an error
    if (hasAnyScore && !hasAllScores) {
      setCustomError('primaryLanguageScores', 'All language test scores (speaking, listening, reading, writing) must be provided');
    } else {
      clearCustomError('primaryLanguageScores');
    }
  }, [watchPrimaryLanguageScores, setCustomError, clearCustomError]);

  // Clear partner fields when marital status changes to Single
  // or when spouse is PR/Citizen or not accompanying (treated as Single for CRS)
  useEffect(() => {
    // Check if user is actually single or should be treated as single for CRS
    const isTreatedAsSingle = 
      watchMaritalStatus === 'Single' || 
      watchSpouseIsPR === 'yes' || 
      (watchSpouseIsPR === 'no' && watchSpouseIsAccompanying === 'no');
    
    if (isTreatedAsSingle) {
      setValue('partnerEducationLevel', null);
      setValue('partnerLanguageTest', null);
      setValue('partnerSpeakingScore', null);
      setValue('partnerListeningScore', null);
      setValue('partnerReadingScore', null);
      setValue('partnerWritingScore', null);
      setValue('partnerCanadianWorkExperience', null);
      
      if (watchMaritalStatus === 'Single') {
        Logger.info('Cleared partner fields because marital status is Single');
      } else if (watchSpouseIsPR === 'yes') {
        Logger.info('Cleared partner fields because spouse is a Canadian citizen/PR (treated as Single for CRS)');
      } else if (watchSpouseIsPR === 'no' && watchSpouseIsAccompanying === 'no') {
        Logger.info('Cleared partner fields because spouse is not accompanying (treated as Single for CRS)');
      }
    }
  }, [watchMaritalStatus, watchSpouseIsPR, watchSpouseIsAccompanying, setValue]);
  
  // Job offer field clearing is now handled directly in the radio button onChange handlers
  // This provides immediate feedback to the user when they change their selection
  
  // Clear secondary language fields when tookSecondaryLanguageTest changes to "no"
  useEffect(() => {
    if (watchTookSecondaryLanguage === 'no') {
      setValue('secondaryLanguageTest', null);
      setValue('secondarySpeakingScore', null);
      setValue('secondaryListeningScore', null);
      setValue('secondaryReadingScore', null);
      setValue('secondaryWritingScore', null);
      Logger.info('Cleared secondary language fields because tookSecondaryLanguageTest is no');
    }
  }, [watchTookSecondaryLanguage, setValue]);
  
  // Clear Canadian education level field when eduInCanada changes to "no"
  useEffect(() => {
    if (watchEducationInCanada === 'no') {
      setValue('canadianEducationLevel', null);
      Logger.info('Cleared Canadian education level because eduInCanada is no');
    }
  }, [watchEducationInCanada, setValue]);
  
  // Clear province of interest when hasProvincialNomination changes to "no"
  useEffect(() => {
    if (watchHasProvincialNomination === 'no') {
      setValue('provinceInterest', 'Not Specified');
      Logger.info('Set province of interest to "Not Specified" because hasProvincialNomination is no');
    }
  }, [watchHasProvincialNomination, setValue]);
  
  // Clear relationship when hasCanadianRelatives changes to "no"
  useEffect(() => {
    if (watchHasCanadianRelatives === 'no') {
      setValue('relationshipToCanadian', null);
      Logger.info('Cleared relationship to Canadian because hasCanadianRelatives is no');
    }
  }, [watchHasCanadianRelatives, setValue]);
  
  // Validate foreign work experience and NOC code
  useEffect(() => {
    if (watchForeignWorkExperience > 0) {
      if (!watchAllFields.nocCodeForeign) {
        setCustomError('nocCodeForeign', 'NOC code is required for foreign work experience');
      } else {
        clearCustomError('nocCodeForeign');
      }
    } else {
      clearCustomError('nocCodeForeign');
    }
  }, [watchForeignWorkExperience, watchAllFields.nocCodeForeign, setCustomError, clearCustomError]);

  // --------------------- PROGRESS BAR LOGIC --------------------- //
  const updateProgressBar = useCallback(() => {
    if (!watchAllFields) return;
    const data = watchAllFields;

    let totalFields = 0;
    let filledFields = 0;

    Object.keys(data).forEach((field) => {
      // Example: skip spouse fields if single, etc. – up to you
      totalFields++;
      if (data[field]) {
        filledFields++;
      }
    });

    const pct = totalFields > 0 ? Math.round((filledFields / totalFields) * 100) : 0;
    const progressBar = document.getElementById('progressBar');
    if (progressBar) {
      progressBar.style.width = `${pct}%`;
    }
    return pct; // Return the percentage for ARIA attributes
  }, [watchAllFields]);

  // Function to check section completion
  const checkSectionCompletion = useCallback(() => {
    const data = watchAllFields;
    if (!data) return;
    
    // Get fields with errors
    const errorFields = Object.keys(errors);
    
    // Map fields to sections
    const fieldToSection = {
      // Personal Info
      fullName: 'personal-info',
      age: 'personal-info',
      citizenship: 'personal-info',
      residence: 'personal-info',
      maritalStatus: 'personal-info',
      
      // Education
      educationLevel: 'education',
      eduInCanada: 'education',
      canadianEducationLevel: 'education',
      hasECA: 'education',
      tradesCertification: 'work-experience',
      
      // Language
      primaryLanguageTest: 'language',
      speaking: 'language',
      listening: 'language',
      reading: 'language',
      writing: 'language',
      
      // Secondary Language
      secondaryLangTest: 'secondary-language',
      secondaryLanguageTest: 'secondary-language',
      secSpeaking: 'secondary-language',
      secListening: 'secondary-language',
      secReading: 'secondary-language',
      secWriting: 'secondary-language',
      
      // Work Experience
      canadianExp: 'work-experience',
      nocCodeCanadian: 'work-experience',
      foreignExp: 'work-experience',
      nocCodeForeign: 'work-experience',
      workInsideCanada: 'work-experience',
      
      // Spouse
      partnerEducation: 'spouse',
      partnerLanguageTest: 'spouse',
      partnerSpeaking: 'spouse',
      partnerListening: 'spouse',
      partnerReading: 'spouse',
      partnerWriting: 'spouse',
      partnerCanadianExp: 'spouse',
      
      // Job Offer
      jobOffer: 'job-offer',
      lmiaStatus: 'job-offer',
      jobWage: 'job-offer',
      jobOfferNocCode: 'job-offer',
      weeklyHours: 'job-offer',
      jobDetails: 'job-offer',
      
      // Provincial
      provNomination: 'provincial',
      provinceInterest: 'provincial',
      canadianRelatives: 'provincial',
      relativeRelationship: 'provincial',
      receivedITA: 'provincial',
      
      // Additional
      settlementFunds: 'additional',
      preferredCity: 'additional',
      preferredDestination: 'additional'
    };
    
    // Track which sections have errors
    const sectionsWithErrors = {};
    errorFields.forEach(field => {
      const section = fieldToSection[field];
      if (section) {
        sectionsWithErrors[section] = true;
      }
    });
    
    // Check each section
    const newSectionStatus = { ...sectionStatus };
    
    // Personal Info
    newSectionStatus['personal-info'].touched = 
      !!data.fullName || 
      !!data.age || 
      !!data.citizenship || 
      !!data.residence || 
      !!data.maritalStatus;
    
    newSectionStatus['personal-info'].complete = 
      !!data.fullName && 
      !!data.age && 
      !!data.citizenship && 
      !!data.residence && 
      !!data.maritalStatus;
    
    // Spouse - only required for married/common-law
    newSectionStatus['spouse'].touched = 
      (data.maritalStatus?.toLowerCase() === 'married' || data.maritalStatus?.toLowerCase() === 'common-law') &&
      (!!data.partnerEducation || 
      !!data.partnerLanguageTest || 
      !!data.partnerSpeaking || 
      !!data.partnerListening || 
      !!data.partnerReading || 
      !!data.partnerWriting || 
      !!data.partnerCanadianExp);
    
    newSectionStatus['spouse'].complete = 
      !(data.maritalStatus?.toLowerCase() === 'married' || data.maritalStatus?.toLowerCase() === 'common-law') || 
      (!!data.partnerEducation && 
      !!data.partnerLanguageTest &&
      !!data.partnerSpeaking &&
      !!data.partnerListening &&
      !!data.partnerReading &&
      !!data.partnerWriting);
    
    // Education section
    const educationComplete = 
      data.educationLevel && 
      data.eduInCanada && 
      data.hasECA && 
      data.tradesCertification && 
      (data.eduInCanada !== 'yes' || data.canadianEducationLevel);
    
    const educationTouched = 
      !!data.educationLevel || 
      !!data.eduInCanada || 
      !!data.hasECA || 
      !!data.tradesCertification || 
      !!data.canadianEducationLevel;
    
    // Language section
    const languageComplete = 
      data.primaryLanguageTest && 
      data.speaking && 
      data.listening && 
      data.reading && 
      data.writing;
    
    const languageTouched = 
      !!data.primaryLanguageTest || 
      !!data.speaking || 
      !!data.listening || 
      !!data.reading || 
      !!data.writing;
    
    // Secondary Language section
    const secondaryLanguageComplete = 
      data.secondaryLangTest === 'no' || 
      (data.secondaryLangTest === 'yes' && 
       data.secondaryLanguageTest && 
       data.secSpeaking && 
       data.secListening && 
       data.secReading && 
       data.secWriting);
    
    const secondaryLanguageTouched = 
      !!data.secondaryLangTest || 
      !!data.secondaryLanguageTest || 
      !!data.secSpeaking || 
      !!data.secListening || 
      !!data.secReading || 
      !!data.secWriting;
    
    // Work Experience section
    const workExperienceComplete = 
      data.canadianExp !== undefined && 
      data.foreignExp !== undefined && 
      data.workInsideCanada;
    
    const workExperienceTouched = 
      data.canadianExp !== undefined || 
      data.foreignExp !== undefined || 
      !!data.workInsideCanada;
    
    // Provincial section
    const provincialComplete = 
      data.provNomination && 
      data.provinceInterest && 
      data.canadianRelatives && 
      data.receivedITA && 
      (data.canadianRelatives !== 'yes' || data.relativeRelationship);
    
    const provincialTouched = 
      !!data.provNomination || 
      !!data.provinceInterest || 
      !!data.canadianRelatives || 
      !!data.receivedITA || 
      !!data.relativeRelationship;
    
    // Additional section
    const additionalComplete = 
      data.settlementFunds && 
      data.preferredDestination;
    
    const additionalTouched = 
      !!data.settlementFunds || 
      !!data.preferredCity || 
      !!data.preferredDestination;
    
    // Create new status object
    newSectionStatus['personal-info'] = { 
      complete: newSectionStatus['personal-info'].complete, 
        active: openSections['personal-info'],
        hasErrors: !!sectionsWithErrors['personal-info'],
      touched: newSectionStatus['personal-info'].touched
    };
    newSectionStatus['education'] = { 
        complete: educationComplete, 
        active: openSections['education'],
        hasErrors: !!sectionsWithErrors['education'],
        touched: educationTouched
    };
    newSectionStatus['language'] = { 
        complete: languageComplete, 
        active: openSections['language'],
        hasErrors: !!sectionsWithErrors['language'],
        touched: languageTouched 
    };
    newSectionStatus['secondary-language'] = { 
        complete: secondaryLanguageComplete, 
        active: openSections['secondary-language'],
        hasErrors: !!sectionsWithErrors['secondary-language'],
        touched: secondaryLanguageTouched
    };
    newSectionStatus['work-experience'] = { 
        complete: workExperienceComplete, 
        active: openSections['work-experience'],
        hasErrors: !!sectionsWithErrors['work-experience'],
        touched: workExperienceTouched
    };
    newSectionStatus['spouse'] = { 
      complete: newSectionStatus['spouse'].complete, 
        active: openSections['spouse'],
        hasErrors: !!sectionsWithErrors['spouse'],
      touched: newSectionStatus['spouse'].touched
    };
    
    // Job Offer section
    const jobOfferComplete = 
      data.jobOffer === 'no' || 
      (data.jobOffer === 'yes' && 
       data.jobOfferNocCode && 
       data.lmiaStatus && 
       data.jobWage && 
       data.weeklyHours);
    
    const jobOfferTouched = 
      !!data.jobOffer || 
      !!data.jobOfferNocCode || 
      !!data.lmiaStatus || 
      !!data.jobWage || 
      !!data.weeklyHours;
    
    // For job offer section, force hasErrors to false when jobOffer is 'no'
    // This ensures we get a green checkmark when user selects "No"
    const jobOfferHasErrors = data.jobOffer === 'no' ? false : !!sectionsWithErrors['job-offer'];
    
    newSectionStatus['job-offer'] = { 
        complete: jobOfferComplete, 
        active: openSections['job-offer'],
        hasErrors: jobOfferHasErrors,
        touched: jobOfferTouched
    };
    
    newSectionStatus['provincial'] = { 
        complete: provincialComplete, 
        active: openSections['provincial'],
        hasErrors: !!sectionsWithErrors['provincial'],
        touched: provincialTouched
    };
    
    newSectionStatus['additional'] = { 
        complete: additionalComplete, 
        active: openSections['additional'],
        hasErrors: !!sectionsWithErrors['additional'],
        touched: additionalTouched
    };
    
    // Only update if there's a change to prevent infinite loops
    setSectionStatus(prevStatus => {
      // Check if any values have changed
      const hasChanged = Object.keys(newSectionStatus).some(key => 
        newSectionStatus[key].complete !== prevStatus[key].complete || 
        newSectionStatus[key].active !== prevStatus[key].active ||
        newSectionStatus[key].hasErrors !== prevStatus[key].hasErrors ||
        newSectionStatus[key].touched !== prevStatus[key].touched
      );
      
      // Only update if there's a change
      return hasChanged ? newSectionStatus : prevStatus;
    });
  }, [watchAllFields, openSections, errors, sectionStatus]);

  // On every render of the form, save to localStorage + update progress
  useEffect(() => {
    try {
      localStorage.setItem('profileFormData', JSON.stringify(watchAllFields));
    } catch (err) {
      Logger.error('Error saving to localStorage:', err);
    }
    const pct = updateProgressBar();
    setProgressValue(pct || 0);
    
    // Check section completion
    checkSectionCompletion();
  }, [watchAllFields, updateProgressBar, checkSectionCompletion]); // Add checkSectionCompletion to dependencies

  // --------------------- COLLAPSIBLE SECTIONS --------------------- //
  const toggleSection = (sectionId) => {
    setOpenSections(prevState => ({
      ...prevState,
      [sectionId]: !prevState[sectionId]
    }));
  };
  
  // Toggle an info block open/closed
  const toggleInfoBlock = (blockId) => {
    setOpenInfoBlocks(prev => ({
      ...prev,
      [blockId]: !prev[blockId]
    }));
  };

  // --------------------- DROPDOWN DATA (COUNTRIES, CITIES, JOBS) --------------------- //
  const [countries, setCountries] = useState([]);
  const [cities, setCities] = useState({});
  const [jobs, setJobs] = useState([
    { NOC: '21234', 'Job Title': 'Software Developer' },
    { NOC: '31102', 'Job Title': 'Medical Doctor' },
    { NOC: '41200', 'Job Title': 'Teacher' }
  ]);

  // Load reference data from local JSON files
  useEffect(() => {
    const loadReferenceData = async () => {
      Logger.debug('Loading reference data from local files...');
      setIsLoading(true);
      
      try {
        // Load from public/Data files
    const loadCountries = async () => {
      try {
            Logger.debug('Loading countries from local file');
        const response = await fetch('/Data/countries.json');
            
            if (!response.ok) {
              throw new Error(`Failed to fetch countries: ${response.status} ${response.statusText}`);
            }
            
          const data = await response.json();
            Logger.debug('Countries loaded successfully:', data.length);
          setCountries(data);
            return data;
      } catch (error) {
            Logger.error('Error loading countries from file:', error);
            throw error;
      }
    };
        
    const loadCities = async () => {
      try {
            Logger.debug('Loading cities from local file');
        const response = await fetch('/Data/canadacities.json');
            
            if (!response.ok) {
              throw new Error(`Failed to fetch cities: ${response.status} ${response.statusText}`);
            }
            
          const data = await response.json();
            Logger.debug('Cities loaded successfully:', data.length);
          setCities(data);
            return data;
      } catch (error) {
            Logger.error('Error loading cities from file:', error);
            throw error;
      }
    };
        
    const loadJobs = async () => {
      try {
            Logger.debug('Loading jobs from local file');
        const response = await fetch('/Data/jobs.json');
            
            if (!response.ok) {
              throw new Error(`Failed to fetch jobs: ${response.status} ${response.statusText}`);
            }
            
          const data = await response.json();
            Logger.debug('Jobs loaded successfully:', data.length);
          setJobs(data);
            return data;
      } catch (error) {
            Logger.error('Error loading jobs from file:', error);
            throw error;
          }
        };
        
        // Load all data in parallel
        await Promise.all([
          loadCountries(),
          loadCities(),
          loadJobs()
        ]);
        
        Logger.debug('Reference data loaded successfully from local files');
      } catch (error) {
        Logger.error('Error loading reference data:', error);
      } finally {
          setIsLoading(false);
      }
    };

    // Start loading reference data
    loadReferenceData();
  }, []);

  // Check if user already has a profile
  useEffect(() => {
    const checkExistingProfile = async () => {
      try {
        Logger.debug('Checking for existing profile...');
        setIsLoading(true);
        
        const response = await apiService.api.get('/profiles/recent', {
          withCredentials: true // Important for JWT cookie
        });
        
        Logger.debug('Profile check response:', response.status);
        
        if (response.data && response.data.profileExists === true) {
          Logger.debug('Existing profile found with ID:', response.data.profileId);
          // Process existing profile data if needed
          // If you want to load the profile data into the form:
          // reset(response.data);
          // Or just notify the user
            setHasSubmittedProfile(true);
        } else {
          Logger.debug('No existing profile found');
        }
      } catch (error) {
        if (error.response) {
          Logger.error('API error checking profile:', error.message);
          
          if (error.response.status === 401 || error.response.status === 403) {
            Logger.error('Authentication issue - not authenticated or token expired');
          } else if (error.response.status === 404) {
            // 404 is expected if the endpoint doesn't exist
            Logger.error('Endpoint not found: /profiles/recent');
          } else {
            Logger.error('Status:', error.response.status);
            Logger.error('Data:', error.response.data);
          }
        } else if (error.request) {
          Logger.error('No response received from server');
        } else {
          Logger.error('Error checking existing profile:', error);
        }
      } finally {
        setIsLoading(false);
      }
    };

    // Only check for existing profile if user is authenticated
    if (currentUser) {
      checkExistingProfile();
    } else {
      setIsLoading(false);
    }
  }, [currentUser]);

  // --------------------- CLEAR LOCALSTORAGE (IF NEEDED) --------------------- //
  const clearSavedFormData = () => {
    // Instead of just clearing localStorage, also discard the draft
    discardDraft();
    Logger.debug('Form data cleared from drafts');
  };

  // --------------------- FORM SUBMISSION --------------------- //
  const onSubmit = async (formData) => {
    try {
      // Process language test scores first
      const processedData = { ...formData };
      
      // Process primary language test scores
      if (processedData.primaryLanguageTest) {
        try {
          // Convert to CLB levels
          const [speakingCLB, listeningCLB, readingCLB, writingCLB] = await Promise.all([
            convertToCLB(processedData.primaryLanguageTest, 'speaking', processedData.speaking),
            convertToCLB(processedData.primaryLanguageTest, 'listening', processedData.listening),
            convertToCLB(processedData.primaryLanguageTest, 'reading', processedData.reading),
            convertToCLB(processedData.primaryLanguageTest, 'writing', processedData.writing)
          ]);
          
          processedData.speakingCLB = speakingCLB;
          processedData.listeningCLB = listeningCLB;
          processedData.readingCLB = readingCLB;
          processedData.writingCLB = writingCLB;
        } catch (error) {
          console.error('Error converting primary language test scores:', error);
        }
      }
      
      // Process secondary language test scores if provided
      if (processedData.secondaryLanguageTest) {
        try {
          const [secSpeakingCLB, secListeningCLB, secReadingCLB, secWritingCLB] = await Promise.all([
            convertToCLB(processedData.secondaryLanguageTest, 'speaking', processedData.secSpeaking),
            convertToCLB(processedData.secondaryLanguageTest, 'listening', processedData.secListening),
            convertToCLB(processedData.secondaryLanguageTest, 'reading', processedData.secReading),
            convertToCLB(processedData.secondaryLanguageTest, 'writing', processedData.secWriting)
          ]);
          
          processedData.secSpeakingCLB = secSpeakingCLB;
          processedData.secListeningCLB = secListeningCLB;
          processedData.secReadingCLB = secReadingCLB;
          processedData.secWritingCLB = secWritingCLB;
        } catch (error) {
          console.error('Error converting secondary language test scores:', error);
        }
      }
      
      // Process partner language test scores if provided
      if (processedData.partnerLanguageTest) {
        try {
          const [partnerSpeakingCLB, partnerListeningCLB, partnerReadingCLB, partnerWritingCLB] = await Promise.all([
            convertToCLB(processedData.partnerLanguageTest, 'speaking', processedData.partnerSpeaking),
            convertToCLB(processedData.partnerLanguageTest, 'listening', processedData.partnerListening),
            convertToCLB(processedData.partnerLanguageTest, 'reading', processedData.partnerReading),
            convertToCLB(processedData.partnerLanguageTest, 'writing', processedData.partnerWriting)
          ]);
          
          processedData.partnerSpeakingCLB = partnerSpeakingCLB;
          processedData.partnerListeningCLB = partnerListeningCLB;
          processedData.partnerReadingCLB = partnerReadingCLB;
          processedData.partnerWritingCLB = partnerWritingCLB;
        } catch (error) {
          console.error('Error converting partner language test scores:', error);
        }
      }
      
      // Continue with normal form validation
      // Perform final data consistency validation
      const validationErrors = performFinalValidation(processedData);
      
      if (validationErrors.length > 0) {
        // Show validation errors to user
        setValidationSummary({
          showSummary: true,
          errors: validationErrors
        });
        return;
      }
      
      // Clean data to ensure consistency
      const cleanedData = cleanDataBeforeSubmission(processedData);
      
      // Show loading state
      setIsSubmitting(true);
      
      // Clear prior messages
      setFormErrors([]);
      setApiError(null);
      setApiResponse(null);
      
      // Instead of showing a confirm dialog, show the modal with form data
      setTempFormData(cleanedData);
      setShowSummaryModal(true);
    } catch (error) {
      console.error('Error in form submission:', error);
      setApiError('An unexpected error occurred while processing the form.');
      setIsSubmitting(false);
    }
  };
  
  // Performs a comprehensive final validation check
  const performFinalValidation = (formData) => {
    const validationErrors = [];
    
    // Check marital status consistency and spouse status
    const shouldNotHavePartnerInfo = 
      formData.applicantMaritalStatus === 'Single' || 
      formData.spouseIsPR === 'yes' || 
      (formData.spouseIsPR === 'no' && formData.spouseIsAccompanying === 'no');
      
    if (shouldNotHavePartnerInfo) {
      if (formData.partnerEducationLevel || 
          formData.partnerLanguageTest || 
          formData.partnerSpeakingScore || 
          formData.partnerListeningScore || 
          formData.partnerReadingScore || 
          formData.partnerWritingScore || 
          formData.partnerCanadianWorkExperience) {
            
        let message = '';
        if (formData.applicantMaritalStatus === 'Single') {
          message = 'Partner information should not be provided for single applicants';
        } else if (formData.spouseIsPR === 'yes') {
          message = 'Partner information should not be provided when spouse is a Canadian citizen/PR';
        } else {
          message = 'Partner information should not be provided when spouse is not accompanying';
        }
            
        validationErrors.push({
          field: 'partnerEducationLevel',
          message,
          sectionId: 'spouse'
        });
      }
    }
    
    // Check job offer consistency
    if (formData.jobOffer === 'no') {
      if (formData.jobOfferNocCode || 
          formData.lmiaStatus || // Updated from jobOfferLmia
          formData.jobWage) {    // Updated from jobOfferWage
        validationErrors.push({
          field: 'jobOfferNocCode',
          message: 'Job offer fields should be empty when no job offer is selected',
          sectionId: 'job-offer'
        });
      }
    } else if (formData.jobOffer === 'yes') {
      if (!formData.jobOfferNocCode) {
        validationErrors.push({
          field: 'jobOfferNocCode',
          message: 'Job offer NOC code is required when a job offer is indicated',
          sectionId: 'job-offer'
        });
      }
    }
    
    // Check secondary language consistency
    if (formData.tookSecondaryLanguageTest === 'no') {
      if (formData.secondaryLanguageTest || 
          formData.secSpeaking || 
          formData.secListening || 
          formData.secReading || 
          formData.secWriting) {
        validationErrors.push({
          field: 'secondaryLanguageTest',
          message: 'Secondary language fields should be empty when no secondary test is indicated',
          sectionId: 'secondary-language'
        });
      }
    } else if (formData.tookSecondaryLanguageTest === 'yes') {
      // Check if secondary language test is selected
      if (!formData.secondaryLanguageTest) {
        validationErrors.push({
          field: 'secondaryLanguageTest',
          message: 'Secondary language test type is required',
          sectionId: 'secondary-language'
        });
      } else {
        // Check if primary and secondary tests are of the same language
        if (areSameLanguage(formData.primaryLanguageTest, formData.secondaryLanguageTest)) {
          validationErrors.push({
            field: 'secondaryLanguageTest',
            message: 'Secondary language test must be in a different language than primary test',
            sectionId: 'secondary-language'
          });
        }
      }
      
      // Check if all scores are provided
      const hasAllScores = formData.secSpeaking && 
                          formData.secListening && 
                          formData.secReading && 
                          formData.secWriting;
      
      if (!hasAllScores) {
        validationErrors.push({
          field: 'secSpeaking',
          message: 'All secondary language test scores must be provided',
          sectionId: 'secondary-language'
        });
      }
    }
    
    // Check Canadian education consistency
    if (formData.eduInCanada === 'yes' && !formData.canadianEducationLevel) {
      validationErrors.push({
        field: 'canadianEducationLevel',
        message: 'Canadian education level is required when education in Canada is indicated',
        sectionId: 'education'
      });
    }
    
    // Check provincial nomination consistency
    if (formData.hasProvincialNomination === 'yes' && !formData.provinceOfInterest) {
      validationErrors.push({
        field: 'provinceOfInterest',
        message: 'Province is required when provincial nomination is indicated',
        sectionId: 'provincial'
      });
    }
    
    // Check Canadian relatives consistency
    if (formData.hasCanadianRelatives === 'yes' && !formData.relationshipToCanadian) {
      validationErrors.push({
        field: 'relationshipToCanadian',
        message: 'Relationship must be specified when Canadian relatives are indicated',
        sectionId: 'provincial'
      });
    }
    
    // Check foreign work experience consistency
    if (formData.foreignExp > 0 && !formData.nocCodeForeign) {
      validationErrors.push({
        field: 'nocCodeForeign',
        message: 'Foreign work experience NOC code is required',
        sectionId: 'work-experience'
      });
    }
    
    // Check numeric field ranges
    if (formData.settlementFunds && formData.settlementFunds < 0) {
      validationErrors.push({
        field: 'settlementFunds',
        message: 'Settlement funds must be non-negative',
        sectionId: 'additional'
      });
    }
    
    if (formData.applicantAge && (formData.applicantAge <= 0 || formData.applicantAge > 120)) {
      validationErrors.push({
        field: 'applicantAge',
        message: 'Applicant age must be positive and reasonable (under 120)',
        sectionId: 'personal-info'
      });
    }
    
    // Validate Canadian education level against highest education level
    if (formData.eduInCanada === 'yes' && formData.canadianEducationLevel && formData.educationLevel) {
      const validOptions = getCanadianEducationOptions(formData.educationLevel);
      const isValid = validOptions.some(opt => opt.value === formData.canadianEducationLevel);
      
      if (!isValid) {
        validationErrors.push({
          message: 'Canadian education level cannot be higher than your highest education level',
          field: 'canadianEducationLevel',
          sectionId: 'education'
        });
      }
    }
    
    return validationErrors;
  };
  
  // Clean data before submission to ensure consistency
  const cleanDataBeforeSubmission = (formData) => {
    // Create a copy of the form data
    const cleanedData = { ...formData };
    
    // Convert language scores to proper values
    // Primary language test scores - ensure we send just the value, not the full option object
    if (cleanedData.speaking && typeof cleanedData.speaking === 'object' && cleanedData.speaking.value) {
      cleanedData.speaking = cleanedData.speaking.value;
    }
    if (cleanedData.listening && typeof cleanedData.listening === 'object' && cleanedData.listening.value) {
      cleanedData.listening = cleanedData.listening.value;
    }
    if (cleanedData.reading && typeof cleanedData.reading === 'object' && cleanedData.reading.value) {
      cleanedData.reading = cleanedData.reading.value;
    }
    if (cleanedData.writing && typeof cleanedData.writing === 'object' && cleanedData.writing.value) {
      cleanedData.writing = cleanedData.writing.value;
    }
    
    // Secondary language test scores
    if (cleanedData.secSpeaking && typeof cleanedData.secSpeaking === 'object' && cleanedData.secSpeaking.value) {
      cleanedData.secSpeaking = cleanedData.secSpeaking.value;
    }
    if (cleanedData.secListening && typeof cleanedData.secListening === 'object' && cleanedData.secListening.value) {
      cleanedData.secListening = cleanedData.secListening.value;
    }
    if (cleanedData.secReading && typeof cleanedData.secReading === 'object' && cleanedData.secReading.value) {
      cleanedData.secReading = cleanedData.secReading.value;
    }
    if (cleanedData.secWriting && typeof cleanedData.secWriting === 'object' && cleanedData.secWriting.value) {
      cleanedData.secWriting = cleanedData.secWriting.value;
    }
    
    // Partner language test scores
    if (cleanedData.partnerSpeaking && typeof cleanedData.partnerSpeaking === 'object' && cleanedData.partnerSpeaking.value) {
      cleanedData.partnerSpeaking = cleanedData.partnerSpeaking.value;
    }
    if (cleanedData.partnerListening && typeof cleanedData.partnerListening === 'object' && cleanedData.partnerListening.value) {
      cleanedData.partnerListening = cleanedData.partnerListening.value;
    }
    if (cleanedData.partnerReading && typeof cleanedData.partnerReading === 'object' && cleanedData.partnerReading.value) {
      cleanedData.partnerReading = cleanedData.partnerReading.value;
    }
    if (cleanedData.partnerWriting && typeof cleanedData.partnerWriting === 'object' && cleanedData.partnerWriting.value) {
      cleanedData.partnerWriting = cleanedData.partnerWriting.value;
    }
    
    // Clean partner data if single or if spouse is PR/Citizen or not accompanying
    if (cleanedData.applicantMaritalStatus === 'Single' || 
        cleanedData.spouseIsPR === 'yes' || 
        (cleanedData.spouseIsPR === 'no' && cleanedData.spouseIsAccompanying === 'no')) {
      cleanedData.partnerEducationLevel = null;
      cleanedData.partnerLanguageTest = null;
      cleanedData.partnerSpeaking = null;
      cleanedData.partnerListening = null;
      cleanedData.partnerReading = null;
      cleanedData.partnerWriting = null;
      cleanedData.partnerCanadianWorkExperience = null;
    }
    
    // Clean job offer data if no job offer
    if (cleanedData.jobOffer === 'no') {
      cleanedData.jobOfferNocCode = null;
      cleanedData.lmiaStatus = null;
      cleanedData.jobWage = null;
      cleanedData.weeklyHours = null;
    }
    
    // Clean secondary language data if no test
    if (cleanedData.tookSecondaryLanguageTest === 'no') {
      cleanedData.secondaryLanguageTest = null;
      cleanedData.secSpeaking = null;
      cleanedData.secListening = null;
      cleanedData.secReading = null;
      cleanedData.secWriting = null;
    }
    
    // Clean Canadian education level if no education in Canada
    if (cleanedData.eduInCanada === 'no') {
      cleanedData.canadianEducationLevel = null;
    }
    
    // Clean province of interest if no provincial nomination
    if (cleanedData.hasProvincialNomination === 'no') {
      cleanedData.provinceInterest = 'Not Specified';
    }
    
    // Clean relationship if no Canadian relatives
    if (cleanedData.hasCanadianRelatives === 'no') {
      cleanedData.relationshipToCanadian = null;
    }
    
    return cleanedData;
  };

  // Test API connectivity before form submission
  const testApiConnectivity = async () => {
    try {
      Logger.debug('Testing API connectivity before form submission...');
      
      // First, test if we can fetch a CSRF token
      await apiService.fetchCsrfToken();
      Logger.debug('CSRF token fetch test succeeded');
      
      // Next, test if we can access the /profiles/recent endpoint
      const response = await apiService.api.get('/profiles/recent');
      Logger.debug('API connectivity test succeeded:', response.status);
      
      return { success: true };
    } catch (error) {
      Logger.error('API connectivity test failed:', error);
      
      if (error.response) {
        return { 
          success: false, 
          status: error.response.status,
          message: `Server responded with status ${error.response.status}`
        };
      } else if (error.request) {
        return { 
          success: false, 
          message: 'No response received from server. The server might be down or unreachable.'
        };
      } else {
        return { 
          success: false, 
          message: `Error: ${error.message}`
        };
      }
    }
  };

  // Handle confirmation from the modal
  const handleFormSubmitConfirm = async () => {
    // Close the modal
    setShowSummaryModal(false);
    
    if (!tempFormData) {
      Logger.error('No form data available for submission');
      setFormErrors(['No form data available for submission']);
      return; // Safety check
    }
    
    setIsSubmitting(true);

    try {
      // Test API connectivity first
      const connectivityTest = await testApiConnectivity();
      if (!connectivityTest.success) {
        Logger.error('API connectivity test failed. Cannot proceed with form submission.');
        setFormErrors([`Cannot connect to server: ${connectivityTest.message}`]);
        setIsSubmitting(false);
        return;
      }
      
      // Check authentication using currentUser from AuthContext instead of localStorage token
      if (!currentUser) {
        Logger.error('Authentication required. Please log in.');
        setFormErrors(['Authentication required. Please log in.']);
        setIsSubmitting(false);
        return;
      }

      // Ensure we have a fresh CSRF token before submission
      try {
        await apiService.fetchCsrfToken(true); // Force a fresh token
        Logger.debug('Successfully fetched CSRF token before form submission');
        Logger.debug('CSRF token being used:', apiService.csrfToken);
        Logger.debug('CSRF header name:', apiService.csrfHeaderName);
      } catch (csrfError) {
        Logger.error('Failed to fetch CSRF token before form submission', csrfError);
        setFormErrors(['Failed to fetch CSRF token. Please try again.']);
        setIsSubmitting(false);
        return;
      }
      
      // Debug job offer data
      Logger.debug("========= JOB OFFER DEBUG =========");
      Logger.debug("jobOffer:", formData.jobOffer);
      Logger.debug("jobOfferNocCode:", formData.jobOfferNocCode);
      Logger.debug("All form values:", formData);
      
      let jobOfferNocCodeValue = null;
      
      if (formData.jobOffer === 'yes' && formData.jobOfferNocCode) {
        try {
          jobOfferNocCodeValue = parseInt(formData.jobOfferNocCode, 10);
          Logger.debug("JOB OFFER NOC CODE CAPTURED:", jobOfferNocCodeValue, "- Type:", typeof jobOfferNocCodeValue);
          
          if (isNaN(jobOfferNocCodeValue)) {
            Logger.error("Job offer NOC code could not be parsed as a number:", formData.jobOfferNocCode);
          }
        } catch (err) {
          Logger.error("Error parsing job offer NOC code:", err);
        }
      }
      
      // If NOC code is still not valid after first attempt, check localStorage as fallback
      if (formData.jobOffer === 'yes' && (!jobOfferNocCodeValue || isNaN(jobOfferNocCodeValue))) {
        try {
          // Try to get from localStorage
          const storedFormData = JSON.parse(localStorage.getItem('profileFormData') || '{}');
          if (storedFormData.jobOfferNocCode) {
            jobOfferNocCodeValue = parseInt(storedFormData.jobOfferNocCode, 10);
            Logger.debug("FALLBACK: Using localStorage NOC value:", jobOfferNocCodeValue, "- Type:", typeof jobOfferNocCodeValue);
          }
        } catch (err) {
          Logger.error("Error parsing stored NOC code:", err);
        }
      }
      
      // Convert yes/no string values to boolean
      const yesNoToBool = (val) => val === 'yes';

      // Prepare mapped data for your API
      const mappedSubmission = {
        // Basic user info
        userEmail: localStorage.getItem('user_email') || '',
        userId: localStorage.getItem('user_id') || '',

        // Personal
        applicantName: formData.fullName || '',
        applicantAge: parseInt(formData.age, 10) || null,
        applicantCitizenship: formData.citizenship || '',
        applicantResidence: formData.residence || '',
        applicantMaritalStatus: formData.maritalStatus || 'Single',

        // Education
        applicantEducationLevel: formData.educationLevel || '',
        educationCompletedInCanada: formData.eduInCanada === 'yes',
        canadianEducationLevel: formData.canadianEducationLevel || null,
        hasEducationalCredentialAssessment: yesNoToBool(formData.hasECA),
        tradesCertification: yesNoToBool(formData.tradesCertification),

        // Primary Language - We'll set CLB scores later
        primaryLanguageTestType: formData.primaryLanguageTest || '',
        primaryTestSpeakingScore: null, // Will be set to CLB value
        primaryTestListeningScore: null, // Will be set to CLB value
        primaryTestReadingScore: null, // Will be set to CLB value
        primaryTestWritingScore: null, // Will be set to CLB value

        // Secondary Language - Similar approach
        tookSecondaryLanguageTest: yesNoToBool(formData.secondaryLangTest),
        secondaryTestType: formData.secondaryLanguageTest || null,
        secondaryTestSpeakingScore: null, // Will be set to CLB value
        secondaryTestListeningScore: null, // Will be set to CLB value 
        secondaryTestReadingScore: null, // Will be set to CLB value
        secondaryTestWritingScore: null, // Will be set to CLB value

        // Work Experience
        canadianWorkExperienceYears: parseInt(formData.canadianExp, 10) || 0,
        nocCodeCanadian: formData.nocCodeCanadian ? parseInt(formData.nocCodeCanadian, 10) : null,
        foreignWorkExperienceYears: parseInt(formData.foreignExp, 10) || 0,
        nocCodeForeign: formData.nocCodeForeign ? parseInt(formData.nocCodeForeign, 10) : null,
        workingInCanada: yesNoToBool(formData.workInsideCanada),

        // Spouse status
        spouseIsPR: yesNoToBool(formData.spouseIsPR),
        spouseIsAccompanying: yesNoToBool(formData.spouseIsAccompanying),

        // Spouse - Set to null if marital status is Single or spouse is PR or not accompanying
        partnerEducationLevel: formData.maritalStatus?.toLowerCase() === 'single' || 
          formData.spouseIsPR === 'yes' || 
          (formData.spouseIsPR === 'no' && formData.spouseIsAccompanying === 'no') ? 
          null : formData.partnerEducation || null,
        partnerLanguageTestType: formData.maritalStatus?.toLowerCase() === 'single' || 
          formData.spouseIsPR === 'yes' || 
          (formData.spouseIsPR === 'no' && formData.spouseIsAccompanying === 'no') ? 
          null : formData.partnerLanguageTest || null,
        partnerTestSpeakingScore: null, // Will be set to CLB value
        partnerTestListeningScore: null, // Will be set to CLB value
        partnerTestReadingScore: null, // Will be set to CLB value
        partnerTestWritingScore: null, // Will be set to CLB value
        partnerCanadianWorkExperienceYears: formData.maritalStatus?.toLowerCase() === 'single' || 
          formData.spouseIsPR === 'yes' || 
          (formData.spouseIsPR === 'no' && formData.spouseIsAccompanying === 'no') ? 
          null : parseInt(formData.partnerCanadianExp, 10) || null,

        // Job Offer - FORCE the NOC code to use our directly captured value
        hasJobOffer: yesNoToBool(formData.jobOffer),
        isJobOfferLmiaApproved: formData.jobOffer === 'yes' ? yesNoToBool(formData.lmiaStatus) : false,
        jobOfferWageCAD: formData.jobOffer === 'yes' && formData.jobWage ? parseInt(formData.jobWage, 10) : null,
        jobOfferNocCode: formData.jobOffer === 'yes' ? jobOfferNocCodeValue : null, // Use our directly captured value as a NUMBER
        weeklyHours: parseInt(formData.weeklyHours, 10) || null,
        jobOfferDescription: formData.jobDetails || null,

        // Provincial Information
        hasProvincialNomination: yesNoToBool(formData.provNomination),
        provinceOfInterest: formData.provinceInterest || 'Not Specified',
        hasCanadianRelatives: yesNoToBool(formData.canadianRelatives),
        relationshipWithCanadianRelative: formData.relativeRelationship || null,
        receivedInvitationToApply: yesNoToBool(formData.receivedITA),

        // Additional Information
        settlementFundsCAD: parseInt(formData.settlementFunds, 10) || 0,
        preferredCity: formData.preferredCity || '',
        preferredDestinationProvince: formData.preferredDestination || '',

        // Store the full data as JSON for future reference/backup
        jsonPayload: JSON.stringify(formData)
      };

      // Add the CLB scores based on the extracted values
      // Primary language
      if (formData.primaryLanguageTest) {
        try {
          const speakingValue = typeof formData.speaking === 'object' && formData.speaking !== null ? 
            formData.speaking.value : formData.speaking;
          const listeningValue = typeof formData.listening === 'object' && formData.listening !== null ? 
            formData.listening.value : formData.listening;
          const readingValue = typeof formData.reading === 'object' && formData.reading !== null ? 
            formData.reading.value : formData.reading;
          const writingValue = typeof formData.writing === 'object' && formData.writing !== null ? 
            formData.writing.value : formData.writing;
          
          console.log('DEBUG extractedValues:', {
            testType: formData.primaryLanguageTest,
            speaking: {
              original: formData.speaking,
              extracted: speakingValue,
              type: typeof speakingValue
            },
            listening: {
              original: formData.listening,
              extracted: listeningValue,
              type: typeof listeningValue
            },
            reading: {
              original: formData.reading,
              extracted: readingValue,
              type: typeof readingValue
            },
            writing: {
              original: formData.writing,
              extracted: writingValue,
              type: typeof writingValue
            }
          });
          
          let primaryClbSpeaking, primaryClbListening, primaryClbReading, primaryClbWriting;
          
          if (speakingValue) {
            primaryClbSpeaking = await convertToCLB(formData.primaryLanguageTest, 'speaking', speakingValue);
            mappedSubmission.primaryClbSpeaking = primaryClbSpeaking;
            // Set the actual field that the backend expects
            mappedSubmission.primaryTestSpeakingScore = primaryClbSpeaking;
          }
          if (listeningValue) {
            primaryClbListening = await convertToCLB(formData.primaryLanguageTest, 'listening', listeningValue);
            mappedSubmission.primaryClbListening = primaryClbListening;
            // Set the actual field that the backend expects
            mappedSubmission.primaryTestListeningScore = primaryClbListening;
          }
          if (readingValue) {
            primaryClbReading = await convertToCLB(formData.primaryLanguageTest, 'reading', readingValue);
            mappedSubmission.primaryClbReading = primaryClbReading;
            // Set the actual field that the backend expects
            mappedSubmission.primaryTestReadingScore = primaryClbReading;
          }
          if (writingValue) {
            primaryClbWriting = await convertToCLB(formData.primaryLanguageTest, 'writing', writingValue);
            mappedSubmission.primaryClbWriting = primaryClbWriting;
            // Set the actual field that the backend expects
            mappedSubmission.primaryTestWritingScore = primaryClbWriting;
          }
          
          console.log('DEBUG CLB Scores (Primary):', {
            speaking: primaryClbSpeaking,
            listening: primaryClbListening,
            reading: primaryClbReading,
            writing: primaryClbWriting
          });
        } catch (error) {
          console.error('ERROR converting primary scores to CLB:', error);
          Logger.error('Error calculating primary language CLB scores:', error);
        }
      }

      // Secondary language
      if (formData.secondaryLanguageTest && yesNoToBool(formData.secondaryLangTest)) {
        try {
          const speakingValue = typeof formData.secSpeaking === 'object' && formData.secSpeaking !== null ? 
            formData.secSpeaking.value : formData.secSpeaking;
          const listeningValue = typeof formData.secListening === 'object' && formData.secListening !== null ? 
            formData.secListening.value : formData.secListening;
          const readingValue = typeof formData.secReading === 'object' && formData.secReading !== null ? 
            formData.secReading.value : formData.secReading;
          const writingValue = typeof formData.secWriting === 'object' && formData.secWriting !== null ? 
            formData.secWriting.value : formData.secWriting;
          
          if (speakingValue) {
            const clbScore = await convertToCLB(formData.secondaryLanguageTest, 'speaking', speakingValue);
            mappedSubmission.secondaryClbSpeaking = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.secondaryTestSpeakingScore = clbScore;
          }
          if (listeningValue) {
            const clbScore = await convertToCLB(formData.secondaryLanguageTest, 'listening', listeningValue);
            mappedSubmission.secondaryClbListening = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.secondaryTestListeningScore = clbScore;
          }
          if (readingValue) {
            const clbScore = await convertToCLB(formData.secondaryLanguageTest, 'reading', readingValue);
            mappedSubmission.secondaryClbReading = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.secondaryTestReadingScore = clbScore;
          }
          if (writingValue) {
            const clbScore = await convertToCLB(formData.secondaryLanguageTest, 'writing', writingValue);
            mappedSubmission.secondaryClbWriting = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.secondaryTestWritingScore = clbScore;
          }
        } catch (error) {
          Logger.error('Error calculating secondary language CLB scores:', error);
        }
      }

      // Partner language
      if (formData.maritalStatus?.toLowerCase() !== 'single' && 
          formData.spouseIsPR !== 'yes' && 
          !(formData.spouseIsPR === 'no' && formData.spouseIsAccompanying === 'no') && 
          formData.partnerLanguageTest) {
        try {
          const speakingValue = typeof formData.partnerSpeaking === 'object' && formData.partnerSpeaking !== null ? 
            formData.partnerSpeaking.value : formData.partnerSpeaking;
          const listeningValue = typeof formData.partnerListening === 'object' && formData.partnerListening !== null ? 
            formData.partnerListening.value : formData.partnerListening;
          const readingValue = typeof formData.partnerReading === 'object' && formData.partnerReading !== null ? 
            formData.partnerReading.value : formData.partnerReading;
          const writingValue = typeof formData.partnerWriting === 'object' && formData.partnerWriting !== null ? 
            formData.partnerWriting.value : formData.partnerWriting;
          
          if (speakingValue) {
            const clbScore = await convertToCLB(formData.partnerLanguageTest, 'speaking', speakingValue);
            mappedSubmission.partnerClbSpeaking = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.partnerTestSpeakingScore = clbScore;
          }
          if (listeningValue) {
            const clbScore = await convertToCLB(formData.partnerLanguageTest, 'listening', listeningValue);
            mappedSubmission.partnerClbListening = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.partnerTestListeningScore = clbScore;
          }
          if (readingValue) {
            const clbScore = await convertToCLB(formData.partnerLanguageTest, 'reading', readingValue);
            mappedSubmission.partnerClbReading = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.partnerTestReadingScore = clbScore;
          }
          if (writingValue) {
            const clbScore = await convertToCLB(formData.partnerLanguageTest, 'writing', writingValue);
            mappedSubmission.partnerClbWriting = clbScore;
            // Set the actual field that the backend expects
            mappedSubmission.partnerTestWritingScore = clbScore;
          }
        } catch (error) {
          Logger.error('Error calculating partner language CLB scores:', error);
        }
      }

      // Update the final submission debugging logs
      // ENHANCED DEBUGGING FOR FINAL SUBMISSION
      Logger.debug("========== FINAL SUBMISSION DEBUGGING ==========");
      Logger.debug("Final hasJobOffer:", mappedSubmission.hasJobOffer);
      Logger.debug("Final jobOfferNocCode:", mappedSubmission.jobOfferNocCode);
      Logger.debug("Final jobOfferNocCode type:", typeof mappedSubmission.jobOfferNocCode);
      
      // Log the exact JSON payload being sent to the backend
      Logger.debug("EXACT JSON PAYLOAD BEING SENT:", JSON.stringify(mappedSubmission, null, 2));
      
      // ADDITIONAL LANGUAGE SCORE DEBUGGING
      Logger.debug("========== LANGUAGE SCORE VALIDATION ==========");
      Logger.debug("PRIMARY LANGUAGE TEST SCORES:");
      Logger.debug("- Original speaking:", formData.speaking, "Type:", typeof formData.speaking);
      Logger.debug("- Original listening:", formData.listening, "Type:", typeof formData.listening);
      Logger.debug("- Original reading:", formData.reading, "Type:", typeof formData.reading);
      Logger.debug("- Original writing:", formData.writing, "Type:", typeof formData.writing);
      
      Logger.debug("PRIMARY LANGUAGE MAPPED SCORES:");
      Logger.debug("- Mapped speaking:", mappedSubmission.primaryTestSpeakingScore, "Type:", typeof mappedSubmission.primaryTestSpeakingScore);
      Logger.debug("- Mapped listening:", mappedSubmission.primaryTestListeningScore, "Type:", typeof mappedSubmission.primaryTestListeningScore);
      Logger.debug("- Mapped reading:", mappedSubmission.primaryTestReadingScore, "Type:", typeof mappedSubmission.primaryTestReadingScore);
      Logger.debug("- Mapped writing:", mappedSubmission.primaryTestWritingScore, "Type:", typeof mappedSubmission.primaryTestWritingScore);
      
      // Check for problematic fields that would cause 400 errors
      Logger.debug("========== FIELD TYPE VALIDATION ==========");
      Object.entries(mappedSubmission).forEach(([key, value]) => {
        if (value !== null && typeof value === 'object' && !Array.isArray(value) && !(value instanceof Date)) {
          Logger.error(`PROBLEMATIC FIELD: ${key} is still an object: ${JSON.stringify(value)}`);
        }
      });
      
      // Ensure this field is properly set in the payload
      Logger.debug("FINAL JOB OFFER NOC CODE VALUE:", mappedSubmission.jobOfferNocCode);
      
      // Ensure province field is properly set
      Logger.debug("FINAL PROVINCE OF INTEREST VALUE:", mappedSubmission.provinceOfInterest);

      // Log the request for debugging
      Logger.debug('Submission payload:', mappedSubmission);
      
      // Final check for job offer data
      if (mappedSubmission.hasJobOffer && mappedSubmission.jobOfferNocCode === null) {
        Logger.error("ERROR: Job offer is marked as Yes but NOC code is null");
        setFormErrors(["Job offer NOC code is required when job offer is Yes. Please select a NOC code for your job offer."]);
        return;
      }
      
      // Debug logging for language scores
      Logger.debug('Primary language scores (CLB):', {
        type: mappedSubmission.primaryLanguageTestType,
        speaking: mappedSubmission.primaryTestSpeakingScore,
        listening: mappedSubmission.primaryTestListeningScore,
        reading: mappedSubmission.primaryTestReadingScore,
        writing: mappedSubmission.primaryTestWritingScore
      });
      
      if (mappedSubmission.tookSecondaryLanguageTest) {
        Logger.debug('Secondary language scores (CLB):', {
          type: mappedSubmission.secondaryTestType,
          speaking: mappedSubmission.secondaryTestSpeakingScore,
          listening: mappedSubmission.secondaryTestListeningScore,
          reading: mappedSubmission.secondaryTestReadingScore,
          writing: mappedSubmission.secondaryTestWritingScore
        });
      }
      
      // Ensure we have a payload
      let jsonPayload = JSON.stringify(mappedSubmission);
      if (!jsonPayload) {
        Logger.warn('Warning: jsonPayload is empty, setting default value');
        jsonPayload = "{}";
      }
      
      // Log job offer details before submission
      Logger.debug('Job offer details before submission:');
      Logger.debug('jobOffer radio value:', formData.jobOffer);
      Logger.debug('jobOfferNocCode value:', formData.jobOfferNocCode);
      Logger.debug('jobOfferNocCode parsed:', formData.jobOfferNocCode ? parseInt(formData.jobOfferNocCode, 10) : null);

      // Log the HTTP request details for debugging
      Logger.debug('About to send POST request to /profiles...');
      Logger.debug('Request headers:', JSON.stringify(apiService.api.defaults.headers));
      Logger.debug('CSRF token being used:', apiService.csrfToken);
      Logger.debug('CSRF header name:', apiService.csrfHeaderName);
      
      try {
        // Make the URL explicit for debugging - using a direct variable to avoid issues with accessing apiService properties
        const API_BASE_URL = 'http://localhost:8080/api';
        const apiUrl = API_BASE_URL + '/profiles';
        Logger.debug(`Making explicit API call to URL: ${apiUrl}`);
        
        // Add debug headers to trace the request
        const requestConfig = {
        headers: {
            'X-Debug-Info': 'ProfileForm-Submission',
            'Content-Type': 'application/json'
          },
          withCredentials: true
        };
        
        // Debug the final submission object before sending
        console.log('DEBUG FINAL mappedSubmission:', {
          primary: {
            test: mappedSubmission.primaryLanguageTest,
            speaking: {
              rawScore: mappedSubmission.primaryTestSpeakingScore,
              clbScore: mappedSubmission.primaryClbSpeaking,
              scoreType: typeof mappedSubmission.primaryTestSpeakingScore
            },
            listening: {
              rawScore: mappedSubmission.primaryTestListeningScore,
              clbScore: mappedSubmission.primaryClbListening,
              scoreType: typeof mappedSubmission.primaryTestListeningScore
            },
            reading: {
              rawScore: mappedSubmission.primaryTestReadingScore,
              clbScore: mappedSubmission.primaryClbReading,
              scoreType: typeof mappedSubmission.primaryTestReadingScore
            },
            writing: {
              rawScore: mappedSubmission.primaryTestWritingScore,
              clbScore: mappedSubmission.primaryClbWriting,
              scoreType: typeof mappedSubmission.primaryTestWritingScore
            }
          }
        });
        
        // Log the full original object for debugging
        console.log('DEBUG SUBMISSION FULL OBJECT:', mappedSubmission);
        
        if (apiService.csrfToken) {
          requestConfig.headers[apiService.csrfHeaderName] = apiService.csrfToken;
        }
        
        Logger.debug('Final submission request config:', requestConfig);
        
        // Variable to store response
        let response;
        
        // Try both approach: apiService and direct axios
        Logger.debug('Trying API call with direct axios first...');
        try {
          // Use axios directly with explicit URL
          response = await axios.post(apiUrl, mappedSubmission, requestConfig);
          Logger.debug('Direct axios call successful! Response:', response);
        } catch (directAxiosError) {
          Logger.error('Direct axios call failed:', directAxiosError);
          // Log detailed error information
          if (directAxiosError.response) {
            Logger.error('Error response status:', directAxiosError.response.status);
            Logger.error('Error response data:', directAxiosError.response.data);
          } else if (directAxiosError.request) {
            Logger.error('No response received from server:', directAxiosError.request);
          } else {
            Logger.error('Error setting up request:', directAxiosError.message);
          }
          
          Logger.debug('Falling back to apiService...');
          
          // Debug the final submission object before sending
          console.log('DEBUG FINAL mappedSubmission:', {
            primary: {
              test: mappedSubmission.primaryLanguageTest,
              speaking: {
                rawScore: mappedSubmission.primaryTestSpeakingScore,
                clbScore: mappedSubmission.primaryClbSpeaking,
                scoreType: typeof mappedSubmission.primaryTestSpeakingScore
              },
              listening: {
                rawScore: mappedSubmission.primaryTestListeningScore,
                clbScore: mappedSubmission.primaryClbListening,
                scoreType: typeof mappedSubmission.primaryTestListeningScore
              },
              reading: {
                rawScore: mappedSubmission.primaryTestReadingScore,
                clbScore: mappedSubmission.primaryClbReading,
                scoreType: typeof mappedSubmission.primaryTestReadingScore
              },
              writing: {
                rawScore: mappedSubmission.primaryTestWritingScore,
                clbScore: mappedSubmission.primaryClbWriting,
                scoreType: typeof mappedSubmission.primaryTestWritingScore
              }
            },
            secondary: mappedSubmission.secondaryLanguageTest ? {
              test: mappedSubmission.secondaryLanguageTest,
              speaking: {
                rawScore: mappedSubmission.secondaryTestSpeakingScore,
                clbScore: mappedSubmission.secondaryClbSpeaking,
                scoreType: typeof mappedSubmission.secondaryTestSpeakingScore
              },
              listening: {
                rawScore: mappedSubmission.secondaryTestListeningScore,
                clbScore: mappedSubmission.secondaryClbListening,
                scoreType: typeof mappedSubmission.secondaryTestListeningScore
              },
              reading: {
                rawScore: mappedSubmission.secondaryTestReadingScore,
                clbScore: mappedSubmission.secondaryClbReading,
                scoreType: typeof mappedSubmission.secondaryTestReadingScore
              },
              writing: {
                rawScore: mappedSubmission.secondaryTestWritingScore,
                clbScore: mappedSubmission.secondaryClbWriting,
                scoreType: typeof mappedSubmission.secondaryTestWritingScore
              }
            } : 'Not provided',
            partner: mappedSubmission.partnerLanguageTest ? {
              test: mappedSubmission.partnerLanguageTest,
              speaking: {
                rawScore: mappedSubmission.partnerTestSpeakingScore,
                clbScore: mappedSubmission.partnerClbSpeaking,
                scoreType: typeof mappedSubmission.partnerTestSpeakingScore
              },
              listening: {
                rawScore: mappedSubmission.partnerTestListeningScore,
                clbScore: mappedSubmission.partnerClbListening,
                scoreType: typeof mappedSubmission.partnerTestListeningScore
              },
              reading: {
                rawScore: mappedSubmission.partnerTestReadingScore,
                clbScore: mappedSubmission.partnerClbReading,
                scoreType: typeof mappedSubmission.partnerTestReadingScore
              },
              writing: {
                rawScore: mappedSubmission.partnerTestWritingScore,
                clbScore: mappedSubmission.partnerClbWriting,
                scoreType: typeof mappedSubmission.partnerTestWritingScore
              }
            } : 'Not provided'
          });
          
          try {
            // Fall back to apiService
            response = await apiService.api.post('/profiles', mappedSubmission);
            Logger.debug('apiService call successful! Response:', response);
          } catch (apiServiceError) {
            Logger.error('apiService call also failed:', apiServiceError);
            throw apiServiceError; // Re-throw to be caught by the main try/catch
          }
        }
        
        Logger.debug('Form submission successful! Response data:', response.data);
        
        // Clear both localStorage and draft storage on success
      clearSavedFormData();
      localStorage.setItem('has_submitted_profile', 'true');
      localStorage.setItem('profile_created_at', new Date().toISOString());
      localStorage.setItem('profile_id', response.data.profileId || 'Unknown');
      localStorage.setItem('applicant_name', mappedSubmission.applicantName);

      setApiResponse({
        success: true,
        message: 'Profile submitted successfully!',
        data: response.data
      });

        // Navigate to dashboard page on success
        navigate('/dashboard', { state: { profileData: response.data } });
        
      } catch (apiError) {
        Logger.error('API request failed:', apiError);
        
        // Log more detailed error information
        if (apiError.response) {
          // The request was made and the server responded with a status code
          // that falls out of the range of 2xx
          Logger.error('Server responded with error:', {
            status: apiError.response.status,
            data: apiError.response.data,
            headers: apiError.response.headers
          });
          
          setApiError({
            status: apiError.response.status,
            message: `Server error (${apiError.response.status}): ${apiError.response.data.message || 'Unknown error'}`
          });
          
          if (apiError.response.status === 401 || apiError.response.status === 403) {
            setFormErrors(['Your session has expired. Please log in again.']);
        } else {
            setFormErrors([`Submission failed: ${apiError.response.data.message || 'Server error'}`]);
          }
        } else if (apiError.request) {
          // The request was made but no response was received
          Logger.error('No response received from server:', apiError.request);
          setApiError({
            status: 0,
            message: 'No response received from server. Please check your internet connection and try again.'
          });
          setFormErrors(['No response received from server. Please check your internet connection and try again.']);
      } else {
          // Something happened in setting up the request that triggered an Error
          Logger.error('Error setting up request:', apiError.message);
          setApiError({
            status: 0,
            message: `Error: ${apiError.message}`
          });
          setFormErrors([`Error: ${apiError.message}`]);
        }
      }
    } catch (error) {
      // Catch any other errors in the overall try block
      Logger.error('Unexpected error during form submission:', error);
      setApiError({
        status: 0,
        message: `Unexpected error: ${error.message}`
      });
      setFormErrors([`Unexpected error: ${error.message}`]);
    } finally {
      setIsSubmitting(false);
    }
  };

  // --------------------- VALIDATION SUMMARY --------------------- //
  // Generate validation summary from errors
  // eslint-disable-next-line no-unused-vars
  const generateValidationSummary = (errors) => {
    if (!errors || Object.keys(errors).length === 0) {
      setValidationSummary(null);
      return;
    }
    
    // Map validation field names to form field names
    const validationToFormField = {
      // Personal Info
      userEmail: 'userEmail', // This doesn't exist in the form, will be handled specially
      applicantName: 'fullName',
      applicantAge: 'age',
      applicantCitizenship: 'citizenship',
      applicantResidence: 'residence',
      applicantMaritalStatus: 'maritalStatus',
      
      // Education
      applicantEducationLevel: 'educationLevel',
      educationCompletedInCanada: 'eduInCanada',
      canadianEducationLevel: 'canadianEducationLevel',
      
      // Language
      primaryLanguageTestType: 'primaryLanguageTest',
      primaryTestSpeakingScore: 'speaking',
      primaryTestListeningScore: 'listening',
      primaryTestReadingScore: 'reading',
      primaryTestWritingScore: 'writing',
      
      // Secondary Language
      tookSecondaryLanguageTest: 'secondaryLangTest',
      secondaryTestType: 'secondaryLanguageTest',
      secondaryTestSpeakingScore: 'secSpeaking',
      secondaryTestListeningScore: 'secListening',
      secondaryTestReadingScore: 'secReading',
      secondaryTestWritingScore: 'secWriting',
      
      // Work Experience
      canadianWorkExperienceYears: 'canadianExp',
      nocCodeCanadian: 'nocCodeCanadian',
      foreignWorkExperienceYears: 'foreignExp',
      nocCodeForeign: 'nocCodeForeign',
      
      // Provincial
      provinceOfInterest: 'provinceInterest',
      hasProvincialNomination: 'provNomination',
      hasCanadianRelatives: 'canadianRelatives',
      relationshipWithCanadianRelative: 'relativeRelationship',
      
      // Financial
      settlementFundsCAD: 'settlementFunds',
      preferredCity: 'preferredCity',
      preferredDestinationProvince: 'preferredDestination',
      
      // Job Offer
      hasJobOffer: 'jobOffer',
      isJobOfferLmiaApproved: 'lmiaStatus',
      jobOfferWageCAD: 'jobWage',
      jobOfferNocCode: 'jobOfferNocCode',
      
      // Partner
      partnerEducationLevel: 'partnerEducation',
      partnerLanguageTestType: 'partnerLanguageTest',
      partnerTestSpeakingScore: 'partnerSpeaking',
      partnerTestListeningScore: 'partnerListening',
      partnerTestReadingScore: 'partnerReading',
      partnerTestWritingScore: 'partnerWriting',
      partnerCanadianWorkExperienceYears: 'partnerCanadianExp'
    };
    
    // Map field names to section names
    const fieldToSection = {
      // Personal Info
      fullName: 'personal-info',
      age: 'personal-info',
      citizenship: 'personal-info',
      residence: 'personal-info',
      maritalStatus: 'personal-info',
      
      // Education
      educationLevel: 'education',
      eduInCanada: 'education',
      canadianEducationLevel: 'education',
      hasECA: 'education',
      tradesCertification: 'work-experience',
      
      // Language
      primaryLanguageTest: 'language',
      speaking: 'language',
      listening: 'language',
      reading: 'language',
      writing: 'language',
      
      // Secondary Language
      secondaryLangTest: 'secondary-language',
      secondaryLanguageTest: 'secondary-language',
      secSpeaking: 'secondary-language',
      secListening: 'secondary-language',
      secReading: 'secondary-language',
      secWriting: 'secondary-language',
      
      // Work Experience
      canadianExp: 'work-experience',
      nocCodeCanadian: 'work-experience',
      foreignExp: 'work-experience',
      nocCodeForeign: 'work-experience',
      workInsideCanada: 'work-experience',
      
      // Spouse
      partnerEducation: 'spouse',
      partnerLanguageTest: 'spouse',
      partnerSpeaking: 'spouse',
      partnerListening: 'spouse',
      partnerReading: 'spouse',
      partnerWriting: 'spouse',
      partnerCanadianExp: 'spouse',
      
      // Job Offer
      jobOffer: 'job-offer',
      lmiaStatus: 'job-offer',
      jobWage: 'job-offer',
      jobOfferNocCode: 'job-offer',
      weeklyHours: 'job-offer',
      jobDetails: 'job-offer',
      
      // Provincial
      provNomination: 'provincial',
      provinceInterest: 'provincial',
      canadianRelatives: 'provincial',
      relativeRelationship: 'provincial',
      receivedITA: 'provincial',
      
      // Additional
      settlementFunds: 'additional',
      preferredCity: 'additional',
      preferredDestination: 'additional'
    };

    // Group errors by section
    const errorsBySection = {};
    
    Object.entries(errors).forEach(([validationField, fieldErrors]) => {
      // Map validation field to form field
      const formField = validationToFormField[validationField] || validationField;
      
      // Determine section based on form field
      const section = fieldToSection[formField] || 'other';
      
      if (!errorsBySection[section]) {
        errorsBySection[section] = [];
      }
      
      // Add each error with its field
      if (Array.isArray(fieldErrors)) {
        fieldErrors.forEach(error => {
          errorsBySection[section].push({ field: formField, error });
        });
      } else {
        errorsBySection[section].push({ field: formField, error: fieldErrors });
      }
    });
    
    setValidationSummary(errorsBySection);
  };

  // Handle clicking on a validation error link
  // eslint-disable-next-line no-unused-vars
  const handleErrorLinkClick = (sectionId, fieldId) => {
    // Open the section
    setOpenSections(prev => ({
      ...prev,
      [sectionId]: true
    }));
    
    // Scroll to the field
    setTimeout(() => {
      const fieldElement = document.getElementById(fieldId);
      if (fieldElement) {
        fieldElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        fieldElement.focus();
      }
    }, 100);
  };

  // --------------------- CONDITIONAL RENDERS (AUTH, SUBMITTED) --------------------- //
  // Show loading state
  if (isLoading) {
    return (
      <div className="profile-form-container">
        <h1>Loading...</h1>
      </div>
    );
  }

  // Show notification if user has already submitted a profile
  if (hasSubmittedProfile) {
    return (
      <div className="profile-form-container">
        <div id="profile-notification" className="notification success-notification">
          <h3>Profile Already Submitted</h3>
          <p>You have already submitted your immigration profile. You can view your results in the dashboard.</p>
          <button 
            onClick={() => navigate('/dashboard')} 
            className="btn-primary"
            style={{ marginTop: '20px' }}
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  // Show notification if user is not authenticated
  if (!currentUser) {
    return (
      <div className="profile-form-container">
        <div id="auth-notification" className="notification error-notification">
          <h3>Authentication Required</h3>
          <p>Please log in to access the immigration profile form.</p>
          <div id="countdown">5</div>
        </div>
      </div>
    );
  }

  // --------------------- RENDER HELPERS --------------------- //



  // Format jobs data for react-select component
  const formatJobsForSelect = () => {
    if (!jobs || !Array.isArray(jobs)) {
      return [];
    }
    return jobs.map((job) => ({
      value: job.NOC,
      label: `${job.NOC} - ${job['Job Title']}`
    }));
  };

  // Format cities data for react-select component
  const formatCitiesForSelect = () => {
    if (!cities || !Array.isArray(cities)) {
      return [];
    }
    return cities.map((city) => ({
      value: city.City,
      label: `${city.City}${city.Provinces ? `, ${city.Provinces}` : ''}`
    }));
  };

  // Format countries data for react-select component
  const formatCountriesForSelect = () => {
    if (!countries || !Array.isArray(countries)) {
      return [];
    }
    return countries.map((country) => ({
      value: country.Countries,
      label: country.Countries
    }));
  };

  // Render validation summary
  const renderValidationSummary = (isBottom = false) => {
    // Don't render anything if validationSummary is null or empty
    if (!validationSummary) return null;
    
    // Check if we're using the new format with showSummary and errors array
    if (validationSummary.errors && Array.isArray(validationSummary.errors)) {
      // Don't show anything if there are no errors
      if (validationSummary.errors.length === 0) return null;
      
      // Using new format
      return (
        <div className={`validation-summary ${isBottom ? 'bottom' : 'top'}`}>
          <div className="validation-section">
            <h4>
              <span className="section-icon">⚠️</span>
              <span>Validation Errors</span>
            </h4>
            <ul>
              {validationSummary.errors.map((error, index) => (
                <li key={index} className="validation-error">
                  <span className="error-message">{error.message}</span>
                  {/* Fix href accessibility warning */}
                  <button 
                    type="button"
                    className="goto-field-button"
                    onClick={() => {
                      if (error.sectionId) {
                        scrollToSection(error.sectionId);
                      }
                    }}
                  >
                    Go to section
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      );
    }
    
    // Original format - skip this part if validationSummary is not an object with entries
    if (typeof validationSummary !== 'object' || !Object.entries(validationSummary).length) {
      return null;
    }
    
    // Field names to readable names mapping
    const fieldToReadableName = {
      // Personal Info
      fullName: 'Full Name',
      age: 'Age',
      citizenship: 'Country of Citizenship',
      residence: 'Country of Residence',
      maritalStatus: 'Marital Status',
      userEmail: 'Email',
      
      // Education
      educationLevel: 'Education Level',
      eduInCanada: 'Canadian Education',
      canadianEducationLevel: 'Canadian Education Level',
      hasECA: 'Educational Credential Assessment',
      tradesCertification: 'Trades Certification',
      
      // Language
      primaryLanguageTest: 'Language Test Type',
      speaking: 'Speaking Score',
      listening: 'Listening Score',
      reading: 'Reading Score',
      writing: 'Writing Score',
      
      // Secondary Language
      secondaryLangTest: 'Secondary Language Test',
      secondaryLanguageTest: 'Secondary Language Test Type',
      secSpeaking: 'Secondary Speaking Score',
      secListening: 'Secondary Listening Score',
      secReading: 'Secondary Reading Score',
      secWriting: 'Secondary Writing Score',
      
      // Work Experience
      canadianExp: 'Canadian Work Experience',
      nocCodeCanadian: 'Canadian NOC Code',
      foreignExp: 'Foreign Work Experience',
      nocCodeForeign: 'Foreign NOC Code',
      workInsideCanada: 'Currently Working in Canada',
      
      // Spouse
      partnerEducation: 'Partner Education Level',
      partnerLanguageTest: 'Partner Language Test',
      partnerSpeaking: 'Partner Speaking Score',
      partnerListening: 'Partner Listening Score',
      partnerReading: 'Partner Reading Score',
      partnerWriting: 'Partner Writing Score',
      partnerCanadianExp: 'Partner Canadian Experience',
      
      // Job Offer
      jobOffer: 'Job Offer',
      lmiaStatus: 'LMIA Status',
      jobWage: 'Job Wage',
      jobOfferNocCode: 'Job NOC Code',
      weeklyHours: 'Weekly Hours',
      jobDetails: 'Job Details',
      
      // Provincial
      provNomination: 'Provincial Nomination',
      provinceInterest: 'Province of Interest',
      hasCanadianRelatives: 'Canadian Relatives',
      relativeRelationship: 'Relationship with Canadian',
      receivedITA: 'Received ITA',
      
      // Additional
      settlementFunds: 'Settlement Funds',
      preferredCity: 'Preferred City',
      preferredDestination: 'Preferred Province'
    };
    
    // Add fieldToSection mapping here to make it accessible within this function
    const fieldToSection = {
      // Personal Info
      fullName: 'personal-info',
      age: 'personal-info',
      citizenship: 'personal-info',
      residence: 'personal-info',
      maritalStatus: 'personal-info',
      
      // Education
      educationLevel: 'education',
      eduInCanada: 'education',
      canadianEducationLevel: 'education',
      hasECA: 'education',
      tradesCertification: 'work-experience',
      
      // Language
      primaryLanguageTest: 'language',
      speaking: 'language',
      listening: 'language',
      reading: 'language',
      writing: 'language',
      
      // Secondary Language
      secondaryLangTest: 'secondary-language',
      secondaryLanguageTest: 'secondary-language',
      secSpeaking: 'secondary-language',
      secListening: 'secondary-language',
      secReading: 'secondary-language',
      secWriting: 'secondary-language',
      
      // Work Experience
      canadianExp: 'work-experience',
      nocCodeCanadian: 'work-experience',
      foreignExp: 'work-experience',
      nocCodeForeign: 'work-experience',
      workInsideCanada: 'work-experience',
      
      // Spouse
      partnerEducation: 'spouse',
      partnerLanguageTest: 'spouse',
      partnerSpeaking: 'spouse',
      partnerListening: 'spouse',
      partnerReading: 'spouse',
      partnerWriting: 'spouse',
      partnerCanadianExp: 'spouse',
      
      // Job Offer
      jobOffer: 'job-offer',
      lmiaStatus: 'job-offer',
      jobWage: 'job-offer',
      jobOfferNocCode: 'job-offer',
      weeklyHours: 'job-offer',
      jobDetails: 'job-offer',
      
      // Provincial
      provNomination: 'provincial',
      provinceInterest: 'provincial',
      canadianRelatives: 'provincial',
      relativeRelationship: 'provincial',
      receivedITA: 'provincial',
      
      // Additional
      settlementFunds: 'additional',
      preferredCity: 'additional',
      preferredDestination: 'additional'
    };

    // Check if there are any sections with errors
    const sectionsWithErrors = Object.entries(validationSummary)
      .filter(([key]) => key !== 'showSummary' && key !== 'errors')
      .filter(([, errors]) => errors && Array.isArray(errors) && errors.length > 0);

    // Don't show anything if there are no errors
    if (sectionsWithErrors.length === 0) return null;

    return (
      <div className="validation-summary">
        <h3>Please fix the following issues before submitting:</h3>
        {isBottom && (
          <button 
            type="button" 
            className="scroll-to-top-btn"
            onClick={() => {
              const topSummary = document.getElementById('top-validation-summary');
              if (topSummary) {
                topSummary.scrollIntoView({ behavior: 'smooth', block: 'start' });
              } else {
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }
            }}
          >
            Scroll to Top ↑
          </button>
        )}
        {sectionsWithErrors.map(([section, errors]) => (
          <div key={section} className="validation-summary-section">
            <h4 onClick={() => toggleSection(section)} className="validation-section-header">
              {sectionNames[section] || section}
              <span className="validation-section-count">({errors.length})</span>
            </h4>
            <ul>
              {errors.map((error, index) => (
                <li key={index} className="validation-error">
                  <span className="field-name">{fieldToReadableName[error.field] || error.field}: </span>
                  <span className="error-message">{error.error}</span>
                  {/* Replace anchor with button for better accessibility */}
                  <button 
                    type="button"
                    className="goto-field-button"
                    onClick={() => {
                      const element = document.getElementById(error.field);
                      if (element) {
                        // Make sure the section is open
                        const sectionId = fieldToSection[error.field];
                        if (sectionId && !openSections[sectionId]) {
                          toggleSection(sectionId);
                        }
                        // Scroll to the element
                        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        element.focus();
                      }
                    }}
                  >
                    Go to field
                  </button>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  };

  // Render custom error messages in form
  const renderCustomError = (fieldName) => {
    // Use the errors object from react-hook-form instead of customErrors
    return errors[fieldName] && <span className="error">{errors[fieldName].message}</span>;
  };

  // Helper function to scroll to an element by ID
  const scrollToElementById = (id) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  // Function to scroll to a specific section
  const scrollToSection = (sectionId) => {
    // First, ensure the section is expanded
    if (!openSections[sectionId]) {
      toggleSection(sectionId);
    }
    
    // Set error highlights for the section
    setSectionErrorHighlights(prev => ({
      ...prev,
      [sectionId]: true
    }));
    
    // Then scroll to it
    scrollToElementById(`${sectionId}-content`);
  };

  // Filter Canadian education options based on highest education level
  const getCanadianEducationOptions = (highestEducationLevel) => {
    // Default - show all options
    const allOptions = [
      { value: "", label: "Select..." },
      { value: "secondary-or-less", label: "Secondary (high school) or less" },
      { value: "one-or-two-year-diploma", label: "1-2 year diploma or certificate" },
      { value: "degree-3-plus-years", label: "Degree, diploma or certificate of three years or longer OR a Master's, professional or doctoral degree of at least one academic year" }
    ];
    
    // If no highest education selected, return all options
    if (!highestEducationLevel) {
      return allOptions;
    }
    
    // Map highest education to maximum allowed Canadian education
    switch (highestEducationLevel) {
      case "none-or-less-than-secondary":
        // Only allow secondary or less
        return allOptions.filter(opt => opt.value === "" || opt.value === "secondary-or-less");
        
      case "secondary-diploma":
        // Allow secondary or less
        return allOptions.filter(opt => opt.value === "" || opt.value === "secondary-or-less");
        
      case "one-year-program":
        // Allow up to 1-2 year diploma
        return allOptions.filter(opt => opt.value === "" || opt.value === "secondary-or-less" || opt.value === "one-or-two-year-diploma");
        
      case "two-year-program":
        // Allow up to 1-2 year diploma
        return allOptions.filter(opt => opt.value === "" || opt.value === "secondary-or-less" || opt.value === "one-or-two-year-diploma");
        
      // All options below can have any Canadian education level
      case "bachelors-degree":
      case "two-or-more-certificates":
      case "masters":
      case "doctoral":
        return allOptions;
        
      default:
        return allOptions;
    }
  };

  // Language test conversion hooks and helper functions are defined at the top of the component


  // --------------------- MAIN RETURN --------------------- //
  return (
    <div className="profile-form-container" style={{ maxWidth: '1200px', width: '100%' }}>
      {/* Form Summary Modal */}
      <FormSummaryModal 
        formData={tempFormData || {}}
        isOpen={showSummaryModal}
        onClose={() => setShowSummaryModal(false)}
        onConfirm={handleFormSubmitConfirm}
        onEdit={() => setShowSummaryModal(false)}
      />

      {/* Loading state */}
      {(isLoading || isDraftLoading) && (
        <div className="loading-overlay">
          <div className="loading-spinner"></div>
          <p>Loading your profile data...</p>
        </div>
      )}
      
      {/* Auto-save status indicator */}
      <div className="auto-save-status">
        {isSaving && <span className="saving">Saving draft...</span>}
        {saveStatus === 'saved' && <span className="saved">Draft saved</span>}
        {saveStatus === 'error' && <span className="error">Error saving draft</span>}
        {hasUnsavedChanges && <span className="unsaved">Unsaved changes</span>}
      </div>

      {/* Top validation summary */}
      {renderValidationSummary(false)}

      <form onSubmit={handleSubmit(onSubmit)} id="profileForm" className="profile-form" ref={formRef}>
        <div className="form-container">
          <h1>Immigration Profile Form</h1>
          <div className="form-intro">
            <p>Please fill in your information to determine your eligibility for Canadian immigration programs.</p>
            
            <div className="important-message">
              <div className="important-icon">ⓘ</div>
              <div className="important-content">
                <h3>For the Best Immigration Pathway Recommendation</h3>
                <p>To receive an accurate evaluation and find the best immigration pathway for your unique situation:</p>
                <ul>
                  <li>Answer <strong>all questions truthfully</strong> with your actual qualifications and circumstances</li>
                  <li>Take time to <strong>read all information blocks</strong> <span className="info-icon-inline">ⓘ</span> which contain essential eligibility criteria</li>
                  <li>Follow the specific instructions for each section to ensure your profile is assessed correctly</li>
                </ul>
                <p>The quality and accuracy of your answers directly impact our ability to identify your optimal immigration options.</p>
              </div>
            </div>
          </div>
          
          <div className="required-explanation">
            Fields marked with <span>*</span> are required.
          </div>
        </div>
        
        {/* Form Steps Container */}
        <div className="form-steps-container" style={{ display: 'none' }}>
          {/* This is now hidden, we'll use the section status indicators instead */}
          {Object.entries(sectionStatus).map(([sectionId, status]) => (
            <div 
              key={sectionId}
              className={`form-step ${status.complete ? 'complete' : ''} ${status.active ? 'active' : ''}`}
              onClick={() => toggleSection(sectionId)}
              onKeyDown={(e) => e.key === 'Enter' && toggleSection(sectionId)}
              tabIndex={0}
            >
              <span className="step-indicator">
                {status.complete ? '✓' : '○'}
              </span>
              <span className="step-name">
                {sectionId === 'personal-info' ? 'Personal Info' : 
                  sectionId === 'secondary-language' ? 'Second Language' : 
                  sectionId.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ')}
              </span>
            </div>
          ))}
        </div>

        {/* Server success/error notifications */}
        {apiResponse && apiResponse.success && (
          <div className="notification success-notification" role="alert" aria-live="polite">
            <h3>Success!</h3>
            <p>{apiResponse.message}</p>
            {apiResponse.data?.profileId && (
              <p>Your profile ID: {apiResponse.data.profileId}</p>
            )}
          </div>
        )}
        {apiError && (
          <div className="notification error-notification" role="alert" aria-live="assertive">
            <h3>Error</h3>
            <p>{apiError.message}</p>
            {apiError.details && apiError.details.length > 0 && (
              <ul className="error-list">
                {apiError.details.map((err, index) => (
                  <li key={index}>{err}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        {/* If we have custom form errors (from validateProfileForm) */}
        {formErrors.length > 0 && (
          <div id="form-error-display" className="error-display" role="alert" aria-live="assertive">
            <h3>Please correct the following errors:</h3>
            <ul>
              {formErrors.map((error, idx) => (
                <li key={idx}>{error}</li>
              ))}
            </ul>
          </div>
        )}

        {/* ---------------- PERSONAL INFO ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['personal-info'] ? 'active' : ''}`}
            onClick={() => toggleSection('personal-info')}
            aria-expanded={openSections['personal-info']}
            aria-controls="personal-info-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['personal-info'].hasErrors ? 'status-error' : 
                sectionStatus['personal-info'].complete ? 'status-complete' : 
                sectionStatus['personal-info'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['personal-info'].hasErrors ? '✕' : 
                 sectionStatus['personal-info'].complete ? '✓' : 
                 sectionStatus['personal-info'].touched ? '⭘' : 
                 '-'}
              </span>
              Personal Information
            </div>
            <span className="chevron" aria-hidden="true">{openSections['personal-info'] ? '▲' : '▼'}</span>
          </button>

          {openSections['personal-info'] && (
            <div className="section-content" id="personal-info-content">
              {/* Full Name */}
              <div className="form-group">
                <label htmlFor="fullName" className="required-field">
                  Full Name:
                  <span
                    className="tooltip"
                    data-tooltip="Enter your full legal name (first + last)."
                  >
                    ?
                  </span>
                </label>
                <input
                  type="text"
                  id="fullName"
                  {...register('fullName', { required: 'Full name is required' })}
                />
                {errors.fullName && <span className="error">{errors.fullName.message}</span>}
                {renderCustomError('fullName')}
              </div>

              {/* Age */}
              <div className="form-group">
                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('age-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">About Age Entry</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['age-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['age-info'] && (
                    <div className="info-content">
                      <p>Choose the best answer:</p>
                      <ul>
                        <li>If you've been invited to apply, enter your age on the date you were invited.</li>
                        <li className="emphasis">OR</li>
                        <li>If you plan to complete an Express Entry profile, enter your current age.</li>
                      </ul>
                    </div>
                  )}
                </div>
                
                <label htmlFor="age" className="required-field">
                  Age:
                  <span className="tooltip" data-tooltip="Must be 16 or older.">?</span>
                </label>
                <input
                  type="number"
                  id="age"
                  min="16"
                  {...register('age', { required: 'Age is required' })}
                />
                {errors.age && <span className="error">{errors.age.message}</span>}
                {renderCustomError('age')}
              </div>

              {/* Citizenship */}
              <div className="form-group">
                <label htmlFor="citizenship" className="required-field">
                  Country of Citizenship
                </label>
                <Controller
                  name="citizenship"
                  control={control}
                  rules={{ required: 'Citizenship is required' }}
                  render={({ field }) => (
                    <Select
                      inputId="citizenship"
                      options={formatCountriesForSelect()}
                      className="react-select-container"
                      classNamePrefix="react-select"
                      placeholder="Search and select a country..."
                      isSearchable={true}
                      isClearable={true}
                      value={formatCountriesForSelect().find(option => option.value === field.value) || null}
                      onChange={(option) => field.onChange(option ? option.value : '')}
                      onBlur={field.onBlur}
                    />
                  )}
                />
                {errors.citizenship && <span className="error">{errors.citizenship.message}</span>}
                {renderCustomError('citizenship')}
              </div>

              {/* Residence */}
              <div className="form-group">
                <label htmlFor="residence" className="required-field">
                  Country of Residence
                </label>
                <Controller
                  name="residence"
                  control={control}
                  rules={{ required: 'Residence is required' }}
                  render={({ field }) => (
                    <Select
                      inputId="residence"
                      options={formatCountriesForSelect()}
                      className="react-select-container"
                      classNamePrefix="react-select"
                      placeholder="Search and select a country..."
                      isSearchable={true}
                      isClearable={true}
                      value={formatCountriesForSelect().find(option => option.value === field.value) || null}
                      onChange={(option) => field.onChange(option ? option.value : '')}
                      onBlur={field.onBlur}
                    />
                  )}
                />
                {errors.residence && <span className="error">{errors.residence.message}</span>}
                {renderCustomError('residence')}
              </div>

              {/* Marital Status */}
              <div className="form-group">
                <label className="required-field">Marital Status</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="maritalSingle"
                    value="Single"
                    {...register('maritalStatus', { required: 'Marital status is required' })}
                  />
                  <label htmlFor="maritalSingle">Single</label>

                  <input
                    type="radio"
                    id="maritalMarried"
                    value="Married"
                    {...register('maritalStatus')}
                  />
                  <label htmlFor="maritalMarried">Married</label>

                  <input
                    type="radio"
                    id="maritalCommon"
                    value="Common-Law"
                    {...register('maritalStatus')}
                  />
                  <label htmlFor="maritalCommon">Common-Law</label>
                </div>
                {errors.maritalStatus && <span className="error">{errors.maritalStatus.message}</span>}
                {renderCustomError('maritalStatus')}
              </div>

              {/* Show these questions only if user is Married or Common-Law */}
              {(watch('maritalStatus') === 'Married' || watch('maritalStatus') === 'Common-Law') && (
                <>
                  {/* Spouse is Canadian Citizen or PR */}
                  <div className="form-group">
                    <label htmlFor="spouseIsPR" className="required-field">
                      Is your spouse a Canadian citizen or permanent resident?
                      <span 
                        className="tooltip" 
                        data-tooltip="If your spouse is a Canadian citizen or permanent resident, you'll be assessed as a single applicant for CRS score purposes."
                      >?</span>
                    </label>
                    <div className="pill-radio-group">
                      <input
                        type="radio"
                        id="spouseIsPRYes"
                        value="yes"
                        {...register('spouseIsPR', { required: 'This field is required' })}
                      />
                      <label htmlFor="spouseIsPRYes">Yes</label>

                      <input
                        type="radio"
                        id="spouseIsPRNo"
                        value="no"
                        {...register('spouseIsPR')}
                      />
                      <label htmlFor="spouseIsPRNo">No</label>
                    </div>
                    {errors.spouseIsPR && <span className="error">{errors.spouseIsPR.message}</span>}
                    {renderCustomError('spouseIsPR')}
                  </div>

                  {/* Only show the accompanying question if spouse is not a Canadian citizen/PR */}
                  {watch('spouseIsPR') === 'no' && (
                    <div className="form-group">
                      <label htmlFor="spouseIsAccompanying" className="required-field">
                        Will your spouse/partner accompany you to Canada?
                        <span 
                          className="tooltip" 
                          data-tooltip="If your spouse will not accompany you to Canada, you'll be assessed as a single applicant for CRS score purposes."
                        >?</span>
                      </label>
                      <div className="pill-radio-group">
                        <input
                          type="radio"
                          id="spouseIsAccompanyingYes"
                          value="yes"
                          {...register('spouseIsAccompanying', { required: 'This field is required' })}
                        />
                        <label htmlFor="spouseIsAccompanyingYes">Yes</label>

                        <input
                          type="radio"
                          id="spouseIsAccompanyingNo"
                          value="no"
                          {...register('spouseIsAccompanying')}
                        />
                        <label htmlFor="spouseIsAccompanyingNo">No</label>
                      </div>
                      {errors.spouseIsAccompanying && <span className="error">{errors.spouseIsAccompanying.message}</span>}
                      {renderCustomError('spouseIsAccompanying')}
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>

        {/* ---------------- EDUCATION SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['education'] ? 'active' : ''}`}
            onClick={() => toggleSection('education')}
            aria-expanded={openSections['education']}
            aria-controls="education-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['education'].hasErrors ? 'status-error' : 
                sectionStatus['education'].complete ? 'status-complete' : 
                sectionStatus['education'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['education'].hasErrors ? '✕' : 
                 sectionStatus['education'].complete ? '✓' : 
                 sectionStatus['education'].touched ? '⭘' : 
                 '-'}
              </span>
              Education
            </div>
            <span className="chevron" aria-hidden="true">{openSections['education'] ? '▲' : '▼'}</span>
          </button>

          {openSections['education'] && (
            <div className="section-content" id="education-content">
              {/* Highest level of education */}
              <div className="form-group">
                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('education-level-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">Education Level Guidelines</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['education-level-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['education-level-info'] && (
                    <div className="info-content">
                      <p>Enter the highest level of education for which you:</p>
                      <ul>
                        <li>earned a Canadian degree, diploma or certificate <strong className="emphasis">or</strong></li>
                        <li>had an Educational Credential Assessment (ECA) if you did your study outside Canada. (ECAs must be from an approved agency, in the last five years)</li>
                      </ul>
                      <p className="note"><strong>Note:</strong> a Canadian degree, diploma or certificate must either have been earned at an accredited Canadian university, college, trade or technical school, or other institute in Canada. Distance learning counts for education points, but not for bonus points in your profile or application.</p>
                    </div>
                  )}
                </div>
                
                <label htmlFor="educationLevel" className="required-field">Highest Level of Education:</label>
                <select
                  id="educationLevel"
                  {...register('educationLevel', { required: 'Education level is required' })}
                >
                  <option value="">Select...</option>
                  <option value="none-or-less-than-secondary">None, or less than secondary (high school)</option>
                  <option value="secondary-diploma">Secondary diploma (high school graduation)</option>
                  <option value="one-year-program">One-year program at a university, college, trade or technical school, or other institute</option>
                  <option value="two-year-program">Two-year program at a university, college, trade or technical school, or other institute</option>
                  <option value="bachelors-degree">Bachelor's degree (three or more year program at a university, college, trade or technical school, or other institute)</option>
                  <option value="two-or-more-certificates">Two or more certificates, diplomas or degrees. One must be for a program of three or more years</option>
                  <option value="masters">Master's degree, or professional degree needed to practice in a licensed profession (see Help)</option>
                  <option value="doctoral">Doctoral level university degree (PhD)</option>
                </select>
                {errors.educationLevel && <span className="error">{errors.educationLevel.message}</span>}
                {renderCustomError('educationLevel')}
              </div>

              {/* Education in Canada? */}
              <div className="form-group">
                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('study-canada-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">Study in Canada Requirements</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['study-canada-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['study-canada-info'] && (
                    <div className="info-content">
                      <p className="note"><strong>Note:</strong> To answer yes, you must meet ALL of these conditions:</p>
                      <ul>
                        <li>English or French as a Second Language must not have made up more than half your study</li>
                        <li>You must not have studied under an award that required you to return to your home country after graduation to apply your skills and knowledge</li>
                        <li>You must have studied at a school within Canada (foreign campuses don't count)</li>
                        <li>You had to be enrolled full time for at least eight months, unless you completed the study or training program (in whole or in part) between March 2020 and August 2022</li>
                        <li>You had to have been physically present in Canada for at least eight months, unless you completed the study or training program (in whole or in part) between March 2020 and August 2022</li>
                      </ul>
                    </div>
                  )}
                </div>
                
                <label className="required-field">Do you have any education completed in Canada?</label>
                <div className="pill-radio-group">
                  <input 
                    type="radio" 
                    id="eduYes"
                    value="yes" 
                    {...register('eduInCanada', { required: true })}
                  />
                  <label htmlFor="eduYes">Yes</label>

                  <input 
                    type="radio" 
                    id="eduNo"
                    value="no" 
                    {...register('eduInCanada')}
                  />
                  <label htmlFor="eduNo">No</label>
                </div>
                {errors.eduInCanada && <span className="error">This field is required.</span>}
                {renderCustomError('eduInCanada')}
              </div>

              {watchAllFields.eduInCanada === 'yes' && (
                <div className="form-group">
                  <label htmlFor="canadianEducationLevel" className="required-field">
                    Canadian Education Level
                  </label>
                  <select
                    id="canadianEducationLevel"
                    {...register('canadianEducationLevel', { required: 'Specify your Canadian education level' })}
                  >
                    {getCanadianEducationOptions(watchAllFields.educationLevel).map(option => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  {errors.canadianEducationLevel && <span className="error">{errors.canadianEducationLevel.message}</span>}
                  {renderCustomError('canadianEducationLevel')}
                </div>
              )}

              {/* ECA */}
              <div className="form-group">
                <label className="required-field">Do you have an Educational Credential Assessment (ECA)?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="hasECAYes"
                    value="yes"
                    {...register('hasECA', { required: true })}
                  />
                  <label htmlFor="hasECAYes">Yes</label>

                  <input
                    type="radio"
                    id="hasECANo"
                    value="no"
                    {...register('hasECA')}
                  />
                  <label htmlFor="hasECANo">No</label>
                </div>
                {errors.hasECA && <span className="error">This field is required.</span>}
                {renderCustomError('hasECA')}
              </div>
            </div>
          )}
        </div>

        {/* ---------------- LANGUAGE SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['language'] ? 'active' : ''}`}
            onClick={() => toggleSection('language')}
            aria-expanded={openSections['language']}
            aria-controls="language-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['language'].hasErrors ? 'status-error' : 
                sectionStatus['language'].complete ? 'status-complete' : 
                sectionStatus['language'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['language'].hasErrors ? '✕' : 
                 sectionStatus['language'].complete ? '✓' : 
                 sectionStatus['language'].touched ? '⭘' : 
                 '-'}
              </span>
              Language Proficiency
            </div>
            <span className="chevron" aria-hidden="true">{openSections['language'] ? '▲' : '▼'}</span>
          </button>

          {openSections['language'] && (
            <div className="section-content" id="language-content">
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('language-requirements-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Language Test Requirements</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['language-requirements-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['language-requirements-info'] && (
                  <div className="info-content">
                    <p><strong className="emphasis">Official languages:</strong> Canada's official languages are English and French.</p>
                    <p>You need to submit language test results that are less than two years old for all programs under Express Entry, <strong>even if English or French is your first language</strong>.</p>
                    <p className="note"><strong>Note:</strong> Valid tests must be a maximum of 2 years old from the date of application.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="primaryLanguageTest" className="required-field">
                  Primary Language Test:
                  <span
                    className="tooltip"
                    data-tooltip="Official English/French test (e.g. IELTS, CELPIP, TEF, TCF)"
                  >
                    ?
                  </span>
                </label>
                <select
                  id="primaryLanguageTest"
                  {...register('primaryLanguageTest', { required: 'Primary test is required' })}
                >
                  <option value="">Select...</option>
                  <option value="CELPIP">CELPIP-G</option>
                  <option value="IELTS">IELTS</option>
                  <option value="PTE">PTE Core</option>
                  <option value="TEF">TEF Canada</option>
                  <option value="TCF">TCF Canada</option>
                </select>
                {errors.primaryLanguageTest && <span className="error">{errors.primaryLanguageTest.message}</span>}
                {watch('primaryLanguageTest') && (
                  <div className="score-range-info">
                    {getScoreRangeDescription(watch('primaryLanguageTest'))}
                  </div>
                )}
                {renderCustomError('primaryLanguageTest')}
              </div>

              {/* Speaking */}
              <div className="form-group">
                <label htmlFor="speaking" className="required-field">
                  Speaking {watch('primaryLanguageTest') ? '' : '(CLB)'}
                </label>
                <select
                  id="speaking"
                  {...register('speaking', { required: 'Speaking score is required' })}
                >
                  <option value="">Select...</option>
                  {getTestScoreOptions(watch('primaryLanguageTest'), 'speaking').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.speaking && <span className="error">{errors.speaking.message}</span>}
                {renderCustomError('speaking')}
              </div>

              {/* Listening */}
              <div className="form-group">
                <label htmlFor="listening" className="required-field">
                  Listening {watch('primaryLanguageTest') ? '' : '(CLB)'}
                </label>
                <select
                  id="listening"
                  {...register('listening', { required: 'Listening score is required' })}
                >
                  <option value="">Select...</option>
                  {getTestScoreOptions(watch('primaryLanguageTest'), 'listening').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.listening && <span className="error">{errors.listening.message}</span>}
                {renderCustomError('listening')}
              </div>

              {/* Reading */}
              <div className="form-group">
                <label htmlFor="reading" className="required-field">
                  Reading {watch('primaryLanguageTest') ? '' : '(CLB)'}
                </label>
                <select
                  id="reading"
                  {...register('reading', { required: 'Reading score is required' })}
                >
                  <option value="">Select...</option>
                  {getTestScoreOptions(watch('primaryLanguageTest'), 'reading').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.reading && <span className="error">{errors.reading.message}</span>}
                {renderCustomError('reading')}
              </div>

              {/* Writing */}
              <div className="form-group">
                <label htmlFor="writing" className="required-field">
                  Writing {watch('primaryLanguageTest') ? '' : '(CLB)'}
                </label>
                <select
                  id="writing"
                  {...register('writing', { required: 'Writing score is required' })}
                >
                  <option value="">Select...</option>
                  {getTestScoreOptions(watch('primaryLanguageTest'), 'writing').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.writing && <span className="error">{errors.writing.message}</span>}
                {renderCustomError('writing')}
              </div>
            </div>
          )}
        </div>

        {/* ---------------- SECONDARY LANGUAGE SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['secondary-language'] ? 'active' : ''}`}
            onClick={() => toggleSection('secondary-language')}
            aria-expanded={openSections['secondary-language']}
            aria-controls="secondary-language-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['secondary-language'].hasErrors ? 'status-error' : 
                sectionStatus['secondary-language'].complete ? 'status-complete' : 
                sectionStatus['secondary-language'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['secondary-language'].hasErrors ? '✕' : 
                 sectionStatus['secondary-language'].complete ? '✓' : 
                 sectionStatus['secondary-language'].touched ? '⭘' : 
                 '-'}
              </span>
              Secondary Language
            </div>
            <span className="chevron" aria-hidden="true">{openSections['secondary-language'] ? '▲' : '▼'}</span>
          </button>

          {openSections['secondary-language'] && (
            <div className="section-content" id="secondary-language-content">
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('secondary-language-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Secondary Language Benefits</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['secondary-language-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['secondary-language-info'] && (
                  <div className="info-content">
                    <p>Providing test results for your second official language can earn you additional points in the Comprehensive Ranking System (CRS).</p>
                    <p>If your first language is English, you can submit French test results as your second language, and vice versa.</p>
                    <p className="note"><strong>Note:</strong> The same 2-year validity period applies to secondary language tests.</p>
                    <p className="warning"><strong>Important:</strong> You cannot claim points for two tests in the same language. For example, if you took both IELTS and CELPIP (both English tests), you must choose only one.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="secondaryLangTest">Did you take a second language test?</label>
                <select id="secondaryLangTest" {...register('secondaryLangTest')}>
                  <option value="no">No</option>
                  <option value="yes">Yes</option>
                </select>
              </div>

              {watchAllFields.secondaryLangTest === 'yes' && (
                <>
                  <div className="form-group">
                    <label htmlFor="secondaryLanguageTest" className="required-field">Secondary Language Test:</label>
                    <select
                      id="secondaryLanguageTest"
                      {...register('secondaryLanguageTest', { required: 'Select your secondary language test' })}
                    >
                      <option value="">Select...</option>
                      {watch('primaryLanguageTest') ? (
                        // Display only tests in a different language than the primary test
                        getSecondaryTestOptions(watch('primaryLanguageTest')).map(option => (
                          <option key={option.value} value={option.value}>{option.label}</option>
                        ))
                      ) : (
                        // If no primary test is selected yet, show a disabled message
                        <option value="" disabled>Select primary test first</option>
                      )}
                    </select>
                    {errors.secondaryLanguageTest && <span className="error">{errors.secondaryLanguageTest.message}</span>}
                    {watch('secondaryLanguageTest') && (
                      <div className="score-range-info">
                        {getScoreRangeDescription(watch('secondaryLanguageTest'))}
                      </div>
                    )}
                    {renderCustomError('secondaryLanguageTest')}
                  </div>

                  {/* Scores */}
                  <div className="form-group">
                    <label htmlFor="secSpeaking" className="required-field">
                      Speaking {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secSpeaking" {...register('secSpeaking', { required: true })}>
                      <option value="">Select...</option>
                      {getTestScoreOptions(watch('secondaryLanguageTest'), 'speaking').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secSpeaking && <span className="error">This field is required</span>}
                    {renderCustomError('secSpeaking')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secListening" className="required-field">
                      Listening {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secListening" {...register('secListening', { required: true })}>
                      <option value="">Select...</option>
                      {getTestScoreOptions(watch('secondaryLanguageTest'), 'listening').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secListening && <span className="error">This field is required</span>}
                    {renderCustomError('secListening')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secReading" className="required-field">
                      Reading {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secReading" {...register('secReading', { required: true })}>
                      <option value="">Select...</option>
                      {getTestScoreOptions(watch('secondaryLanguageTest'), 'reading').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secReading && <span className="error">This field is required</span>}
                    {renderCustomError('secReading')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secWriting" className="required-field">
                      Writing {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secWriting" {...register('secWriting', { required: true })}>
                      <option value="">Select...</option>
                      {getTestScoreOptions(watch('secondaryLanguageTest'), 'writing').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secWriting && <span className="error">This field is required</span>}
                    {renderCustomError('secWriting')}
                  </div>
                </>
              )}
            </div>
          )}
        </div>

        {/* ---------------- WORK EXPERIENCE SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['work-experience'] ? 'active' : ''}`}
            onClick={() => toggleSection('work-experience')}
            aria-expanded={openSections['work-experience']}
            aria-controls="work-experience-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['work-experience'].hasErrors ? 'status-error' : 
                sectionStatus['work-experience'].complete ? 'status-complete' : 
                sectionStatus['work-experience'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['work-experience'].hasErrors ? '✕' : 
                 sectionStatus['work-experience'].complete ? '✓' : 
                 sectionStatus['work-experience'].touched ? '⭘' : 
                 '-'}
              </span>
              Work Experience
            </div>
            <span className="chevron" aria-hidden="true">{openSections['work-experience'] ? '▲' : '▼'}</span>
          </button>

          {openSections['work-experience'] && (
            <div className="section-content" id="work-experience-content">
              {/* Canadian Work Experience */}
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('canadian-work-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Canadian Work Experience Requirements</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['canadian-work-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['canadian-work-info'] && (
                  <div className="info-content">
                    <p>In the last 10 years, how many years of skilled work experience in Canada do you have?</p>
                    <p>It must have been <strong className="emphasis">paid and full-time</strong> (or an equal amount in part-time).</p>
                    <p>You must have been physically in Canada and working for a Canadian employer. This includes remote work.</p>
                    <p className="note"><strong>Note:</strong> "Skilled work" in the NOC is TEER 0, 1, 2 or 3 category jobs.</p>
                    <p>If you aren't sure of the NOC TEER category for your job, you can find your NOC by searching the <a href="https://noc.esdc.gc.ca/?GoCTemplateCulture=en-CA" target="_blank" rel="noopener noreferrer" className="emphasis-link">official government classification</a>.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="canadianExp" className="required-field">Canadian Work Experience (years) in the last 10 years</label>
                <select id="canadianExp" {...register('canadianExp', { required: true })}>
                  <option value="">Select...</option>
                  <option value="0">None or &lt;1 year</option>
                  <option value="1">1 year</option>
                  <option value="2">2 years</option>
                  <option value="3">3 years</option>
                  <option value="4">4 years</option>
                  <option value="5">5+ years</option>
                </select>
                {errors.canadianExp && <span className="error">This field is required.</span>}
                {renderCustomError('canadianExp')}
              </div>

              {watchAllFields.canadianExp > 0 && (
                <div className="form-group">
                  <label htmlFor="nocCodeCanadian" className="required-field">Canadian NOC Code</label>
                  <Controller
                    name="nocCodeCanadian"
                    control={control}
                    rules={{ required: true }}
                    render={({ field }) => (
                      <Select
                        inputId="nocCodeCanadian"
                        options={formatJobsForSelect()}
                        className="react-select-container"
                        classNamePrefix="react-select"
                        placeholder="Search and select a NOC code..."
                        isSearchable={true}
                        isClearable={true}
                        value={formatJobsForSelect().find(option => option.value === parseInt(field.value)) || null}
                        onChange={(option) => field.onChange(option ? option.value : '')}
                        onBlur={field.onBlur}
                      />
                    )}
                  />
                  {errors.nocCodeCanadian && <span className="error">This field is required.</span>}
                  {renderCustomError('nocCodeCanadian')}
                </div>
              )}

              {/* Foreign Work Experience */}
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('foreign-work-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Foreign Work Experience Requirements</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['foreign-work-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['foreign-work-info'] && (
                  <div className="info-content">
                    <p>In the last 10 years, how many total years of foreign skilled work experience do you have?</p>
                    <p>It must have been <strong className="emphasis">paid, full-time</strong> (or an equal amount in part-time), and in only one occupation (NOC TEER category 0, 1, 2 or 3).</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="foreignExp" className="required-field">Foreign Work Experience (years) in the last 10 years</label>
                <select id="foreignExp" {...register('foreignExp', { required: true })}>
                  <option value="">Select...</option>
                  <option value="0">None or &lt;1 year</option>
                  <option value="1">1 year</option>
                  <option value="2">2 years</option>
                  <option value="3">3+ years</option>
                </select>
                {errors.foreignExp && <span className="error">This field is required.</span>}
                {renderCustomError('foreignExp')}
              </div>

              {watchAllFields.foreignExp > 0 && (
                <div className="form-group">
                  <label htmlFor="nocCodeForeign" className="required-field">Foreign NOC Code</label>
                  <Controller
                    name="nocCodeForeign"
                    control={control}
                    rules={{ required: true }}
                    render={({ field }) => (
                      <Select
                        inputId="nocCodeForeign"
                        options={formatJobsForSelect()}
                        className="react-select-container"
                        classNamePrefix="react-select"
                        placeholder="Search and select a NOC code..."
                        isSearchable={true}
                        isClearable={true}
                        value={formatJobsForSelect().find(option => option.value === parseInt(field.value)) || null}
                        onChange={(option) => field.onChange(option ? option.value : '')}
                        onBlur={field.onBlur}
                      />
                    )}
                  />
                  {errors.nocCodeForeign && <span className="error">This field is required.</span>}
                  {renderCustomError('nocCodeForeign')}
                </div>
              )}

              {/* Trades Certification - Moved from Education section */}
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('trades-certification-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Certificate of Qualification Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['trades-certification-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['trades-certification-info'] && (
                  <div className="info-content">
                    <p>Do you have a certificate of qualification from a Canadian province, territory or federal body?</p>
                    <p className="note"><strong>Note:</strong> A certificate of qualification lets people work in some skilled trades in Canada. Only the provinces, territories and a federal body can issue these certificates. To get one, a person must have them assess their training, trade experience and skills to and then pass a certification exam.</p>
                    <p>People usually have to go to the province or territory to be assessed. They may also need experience and training from an employer in Canada.</p>
                    <p className="warning"><strong>Important:</strong> This isn't the same as a nomination from a province or territory.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label className="required-field">Do you have a trades certification?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="tradesYes"
                    value="yes"
                    {...register('tradesCertification', { required: true })}
                  />
                  <label htmlFor="tradesYes">Yes</label>

                  <input
                    type="radio"
                    id="tradesNo"
                    value="no"
                    {...register('tradesCertification')}
                  />
                  <label htmlFor="tradesNo">No</label>
                </div>
                {errors.tradesCertification && <span className="error">This field is required.</span>}
                {renderCustomError('tradesCertification')}
              </div>
            </div>
          )}
        </div>

        {/* ---------------- SPOUSE / COMMON-LAW SECTION ---------------- */}
        {(watchAllFields.maritalStatus?.toLowerCase() === 'married' ||
          watchAllFields.maritalStatus?.toLowerCase() === 'common-law') && 
          watchAllFields.spouseIsPR === 'no' && 
          watchAllFields.spouseIsAccompanying === 'yes' && (
          <div className="collapsible-section" id="spouseSection">
            <button
              type="button"
              className={`section-header ${openSections['spouse'] ? 'active' : ''}`}
              onClick={() => toggleSection('spouse')}
              aria-expanded={openSections['spouse']}
              aria-controls="spouse-content"
            >
              <div className="section-header-content">
                <span className={`section-status ${
                  sectionStatus['spouse'].hasErrors ? 'status-error' : 
                  sectionStatus['spouse'].complete ? 'status-complete' : 
                  sectionStatus['spouse'].touched ? 'status-incomplete' : 
                  'status-untouched'
                }`} aria-hidden="true">
                  {sectionStatus['spouse'].hasErrors ? '✕' : 
                   sectionStatus['spouse'].complete ? '✓' : 
                   sectionStatus['spouse'].touched ? '⭘' : 
                   '-'}
                </span>
                Spouse / Common-Law Partner
              </div>
              <span className="chevron" aria-hidden="true">{openSections['spouse'] ? '▲' : '▼'}</span>
            </button>

            {openSections['spouse'] && (
              <div className="section-content" id="spouse-content">
                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('partner-education-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">Spouse's Education Requirements</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['partner-education-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['partner-education-info'] && (
                    <div className="info-content">
                      <p>What is the highest level of education for which your spouse or common-law partner has:</p>
                      <ul>
                        <li>earned a Canadian degree, diploma or certificate <strong className="emphasis">or</strong></li>
                        <li>had an Educational Credential Assessment (ECA)? (ECAs must be from an approved agency, in the last five years)</li>
                      </ul>
                      <p className="note"><strong>Note:</strong> To get the correct number of points, make sure you choose the answer that best reflects your case. For example:</p>
                      <p>If your spouse has TWO Bachelor's degrees, or one Bachelor's AND a two year college diploma, choose – "Two or more certificates, diplomas, or degrees. One must be for a program of three or more years."</p>
                    </div>
                  )}
                </div>
                
                <div className="form-group">
                  <label htmlFor="partnerEducation" className="required-field">Spouse's Highest Education</label>
                  <select id="partnerEducation" {...register('partnerEducation', { required: true })}>
                    <option value="">Select...</option>
                    <option value="none-or-less-than-secondary">None or &lt; high school</option>
                    <option value="secondary-diploma">High school diploma</option>
                    <option value="one-year-program">One-year program</option>
                    <option value="two-year-program">Two-year program</option>
                    <option value="bachelors-degree">Bachelor's degree</option>
                    <option value="two-or-more-certificates">Two or more credentials</option>
                    <option value="masters">Master's or professional degree</option>
                    <option value="doctoral">PhD</option>
                  </select>
                </div>

                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('partner-language-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">Partner Language Requirements</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['partner-language-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['partner-language-info'] && (
                    <div className="info-content">
                      <p>Your spouse or common-law partner's language proficiency can earn you additional points in the Comprehensive Ranking System (CRS).</p>
                      <p>The same language tests and scoring criteria apply to your spouse or partner as they do to you.</p>
                      <p className="note"><strong>Note:</strong> For your spouse's language ability to be counted, they must take an approved language test and meet at least CLB 4 level in all abilities (reading, writing, speaking, listening).</p>
                      <p>If your spouse hasn't taken a language test, or doesn't meet CLB 4 in all abilities, select "No test taken" from the dropdown - you won't lose points, but won't gain the additional points either.</p>
                    </div>
                  )}
                </div>
                
                <div className="form-group">
                  <label htmlFor="partnerLanguageTest" className="required-field">Spouse's Language Test</label>
                  <select
                    id="partnerLanguageTest"
                    {...register('partnerLanguageTest', { required: true })}
                  >
                    <option value="">Select...</option>
                    <option value="CELPIP">CELPIP-G</option>
                    <option value="IELTS">IELTS</option>
                    <option value="PTE">PTE Core</option>
                    <option value="TEF">TEF Canada</option>
                    <option value="TCF">TCF Canada</option>
                    <option value="none">No test taken</option>
                  </select>
                  {watch('partnerLanguageTest') && watch('partnerLanguageTest') !== 'none' && (
                    <div className="score-range-info">
                      {getScoreRangeDescription(watch('partnerLanguageTest'))}
                    </div>
                  )}
                </div>

                {/* Spouse language scores - only show if a test is selected and it's not "none" */}
                {watch('partnerLanguageTest') && watch('partnerLanguageTest') !== 'none' && (
                  <>
                    <div className="form-group">
                      <label htmlFor="partnerSpeaking" className="required-field">
                        Speaking {watch('partnerLanguageTest') ? '' : '(CLB)'}
                      </label>
                      <select id="partnerSpeaking" {...register('partnerSpeaking', { required: true })}>
                        <option value="">Select...</option>
                        {getTestScoreOptions(watch('partnerLanguageTest'), 'speaking').map(option => (
                          <option key={option.value} value={option.value}>
                            {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label htmlFor="partnerListening" className="required-field">
                        Listening {watch('partnerLanguageTest') ? '' : '(CLB)'}
                      </label>
                      <select id="partnerListening" {...register('partnerListening', { required: true })}>
                        <option value="">Select...</option>
                        {getTestScoreOptions(watch('partnerLanguageTest'), 'listening').map(option => (
                          <option key={option.value} value={option.value}>
                            {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label htmlFor="partnerReading" className="required-field">
                        Reading {watch('partnerLanguageTest') ? '' : '(CLB)'}
                      </label>
                      <select id="partnerReading" {...register('partnerReading', { required: true })}>
                        <option value="">Select...</option>
                        {getTestScoreOptions(watch('partnerLanguageTest'), 'reading').map(option => (
                          <option key={option.value} value={option.value}>
                            {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group">
                      <label htmlFor="partnerWriting" className="required-field">
                        Writing {watch('partnerLanguageTest') ? '' : '(CLB)'}
                      </label>
                      <select id="partnerWriting" {...register('partnerWriting', { required: true })}>
                        <option value="">Select...</option>
                        {getTestScoreOptions(watch('partnerLanguageTest'), 'writing').map(option => (
                          <option key={option.value} value={option.value}>
                            {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                  </>
                )}
                <div className="info-block">
                  <div 
                    className="info-header"
                    onClick={() => toggleInfoBlock('partner-work-info')}
                  >
                    <div className="info-title-container">
                      <span className="info-icon">ⓘ</span>
                      <h4 className="info-title">Spouse Canadian Work Experience</h4>
                    </div>
                    <span className="info-chevron">
                      {openInfoBlocks['partner-work-info'] ? '▲' : '▼'}
                    </span>
                  </div>
                  
                  {openInfoBlocks['partner-work-info'] && (
                    <div className="info-content">
                      <p>In the last 10 years, how many years of skilled work experience in Canada does your spouse/common-law partner have?</p>
                      <p>It must have been <strong className="emphasis">paid, full-time</strong> (or an equal amount in part-time), and in one or more NOC TEER category 0, 1, 2, or 3 jobs.</p>
                      <p className="note"><strong>Note:</strong> Only count work experience from after your spouse completed their education. Work as part of a study program, such as a co-op term, doesn't count.</p>
                    </div>
                  )}
                </div>
                
                <div className="form-group">
                  <label htmlFor="partnerCanadianExp" className="required-field">Spouse Canadian Work Experience (years)</label>
                  <select id="partnerCanadianExp" {...register('partnerCanadianExp', { required: true })}>
                    <option value="">Select...</option>
                    <option value="0">None or &lt;1 year</option>
                    <option value="1">1 year</option>
                    <option value="2">2 years</option>
                    <option value="3">3 years</option>
                    <option value="4">4 years</option>
                    <option value="5">5+ years</option>
                  </select>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ---------------- JOB OFFER SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['job-offer'] ? 'active' : ''}`}
            onClick={() => toggleSection('job-offer')}
            aria-expanded={openSections['job-offer']}
            aria-controls="job-offer-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['job-offer'].hasErrors ? 'status-error' : 
                sectionStatus['job-offer'].complete ? 'status-complete' : 
                sectionStatus['job-offer'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['job-offer'].hasErrors ? '✕' : 
                 sectionStatus['job-offer'].complete ? '✓' : 
                 sectionStatus['job-offer'].touched ? '⭘' : 
                 '-'}
              </span>
              Job Offer & Arranged Employment
            </div>
            <span className="chevron" aria-hidden="true">{openSections['job-offer'] ? '▲' : '▼'}</span>
          </button>

          {openSections['job-offer'] && (
            <div className="section-content" id="job-offer-content">
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('job-offer-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Valid Job Offer Requirements</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['job-offer-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['job-offer-info'] && (
                  <div className="info-content">
                    <p>Do you have a valid job offer supported by a Labour Market Impact Assessment (if needed)?</p>
                    <p>A valid job offer must be:</p>
                    <ul>
                      <li>full-time</li>
                      <li>in a skilled job listed as TEER 0, 1, 2 or 3 in the 2021 National Occupational Classification</li>
                      <li>supported by a Labour Market Impact Assessment (LMIA) or exempt from needing one</li>
                      <li>for one year from the time you become a permanent resident</li>
                    </ul>
                    <p className="warning"><strong>Important:</strong> A job offer isn't valid if your employer is:</p>
                    <ul>
                      <li>an embassy, high commission or consulate in Canada or</li>
                      <li>on the list of ineligible employers.</li>
                    </ul>
                    <p>Whether an offer is valid or not also depends on different factors, depending on your case.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="jobOffer" className="required-field">Do you have a valid job offer?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="jobOfferYes"
                    value="yes"
                    {...register('jobOffer', { required: true })}
                  />
                  <label htmlFor="jobOfferYes">Yes</label>

                  <input
                    type="radio"
                    id="jobOfferNo"
                    value="no"
                    {...register('jobOffer')}
                  />
                  <label htmlFor="jobOfferNo">No</label>
                </div>
                {errors.jobOffer && <span className="error">This field is required.</span>}
                {renderCustomError('jobOffer')}
              </div>

              {watchAllFields.jobOffer === 'yes' && (
                <>
                  <div className="info-block">
                    <div 
                      className="info-header"
                      onClick={() => toggleInfoBlock('noc-teer-info')}
                    >
                      <div className="info-title-container">
                        <span className="info-icon">ⓘ</span>
                        <h4 className="info-title">NOC TEER Categories</h4>
                      </div>
                      <span className="info-chevron">
                        {openInfoBlocks['noc-teer-info'] ? '▲' : '▼'}
                      </span>
                    </div>
                    
                    {openInfoBlocks['noc-teer-info'] && (
                      <div className="info-content">
                        <p>Which NOC TEER is the job offer?</p>
                        <p>If you don't know your job's TEER classification, you can find it by searching the <a href="https://noc.esdc.gc.ca/?GoCTemplateCulture=en-CA" target="_blank" rel="noopener noreferrer" className="emphasis-link">official NOC classification</a>.</p>
                      </div>
                    )}
                  </div>

                  <div className="form-group">
                    <label htmlFor="jobOfferNocCode" className="required-field">NOC Code</label>
                    <Controller
                      name="jobOfferNocCode"
                      control={control}
                      rules={{ required: true }}
                      render={({ field }) => (
                        <Select
                          inputId="jobOfferNocCode"
                          options={formatJobsForSelect()}
                          className="react-select-container"
                          classNamePrefix="react-select"
                          placeholder="Search and select a NOC code..."
                          isSearchable={true}
                          isClearable={true}
                          value={formatJobsForSelect().find(option => option.value === parseInt(field.value)) || null}
                          onChange={(option) => field.onChange(option ? option.value : '')}
                          onBlur={field.onBlur}
                        />
                      )}
                    />
                    {errors.jobOfferNocCode && <span className="error">This field is required.</span>}
                    {renderCustomError('jobOfferNocCode')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="lmiaStatus" className="required-field">LMIA Status</label>
                    <select id="lmiaStatus" {...register('lmiaStatus', { required: true })}>
                      <option value="">Select...</option>
                      <option value="approved">LMIA Approved</option>
                      <option value="exempt">LMIA Exempt</option>
                      <option value="none">No LMIA</option>
                    </select>
                    {errors.lmiaStatus && <span className="error">This field is required.</span>}
                    {renderCustomError('lmiaStatus')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="jobWage" className="required-field">Wage (CAD $ / hour)</label>
                    <input
                      type="number"
                      id="jobWage"
                      min="0"
                      step="0.01"
                      {...register('jobWage', { required: true })}
                    />
                    {errors.jobWage && <span className="error">This field is required.</span>}
                    {renderCustomError('jobWage')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="weeklyHours" className="required-field">Hours per week</label>
                    <input
                      type="number"
                      id="weeklyHours"
                      min="0"
                      step="0.5"
                      {...register('weeklyHours', { required: true })}
                    />
                    {errors.weeklyHours && <span className="error">This field is required.</span>}
                    {renderCustomError('weeklyHours')}
                  </div>

                  <div className="form-group">
                    <label htmlFor="jobDetails">Job Title & Description</label>
                    <textarea
                      id="jobDetails"
                      rows="3"
                      placeholder="Enter your job title and brief description"
                      {...register('jobDetails')}
                    ></textarea>
                  </div>
                </>
              )}
            </div>
          )}
        </div>

        {/* ---------------- PROVINCIAL NOMINATION SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['provincial'] ? 'active' : ''}`}
            onClick={() => toggleSection('provincial')}
            aria-expanded={openSections['provincial']}
            aria-controls="provincial-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['provincial'].hasErrors ? 'status-error' : 
                sectionStatus['provincial'].complete ? 'status-complete' : 
                sectionStatus['provincial'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['provincial'].hasErrors ? '✕' : 
                 sectionStatus['provincial'].complete ? '✓' : 
                 sectionStatus['provincial'].touched ? '⭘' : 
                 '-'}
              </span>
              Provincial Nomination & Connections
            </div>
            <span className="chevron" aria-hidden="true">{openSections['provincial'] ? '▲' : '▼'}</span>
          </button>

          {openSections['provincial'] && (
            <div className="section-content" id="provincial-content">
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('provincial-nomination-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Provincial Nomination Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['provincial-nomination-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['provincial-nomination-info'] && (
                  <div className="info-content">
                    <p>A Provincial Nomination can significantly increase your chances of receiving an invitation to apply for permanent residence through Express Entry.</p>
                    <p>To answer "Yes" to the provincial nomination question, you must have received a nomination certificate from a Canadian province or territory through one of their Provincial Nominee Programs (PNPs).</p>
                    <p className="note"><strong>Note:</strong> A provincial nomination is different from an expression of interest in a specific province. You must have an actual nomination certificate from a provincial or territorial government.</p>
                    <p>If you have a provincial nomination, you'll automatically receive 600 additional points in the Comprehensive Ranking System (CRS).</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="provNomination" className="required-field">
                  Do you have a Provincial Nomination?
                  <span
                    className="tooltip"
                    data-tooltip="A provincial nomination is an official certificate from a Canadian province/territory under their Provincial Nominee Program (PNP). Having one adds 600 points to your CRS score."
                  >
                    ?
                  </span>
                </label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="provNominationYes"
                    value="yes"
                    {...register('provNomination', { required: true })}
                  />
                  <label htmlFor="provNominationYes">Yes</label>

                  <input
                    type="radio"
                    id="provNominationNo"
                    value="no"
                    {...register('provNomination')}
                  />
                  <label htmlFor="provNominationNo">No</label>
                </div>
                {errors.provNomination && <span className="error">This field is required.</span>}
                {renderCustomError('provNomination')}
              </div>

              <div className="form-group">
                <label htmlFor="provinceInterest" className="required-field">Province of Interest</label>
                <select id="provinceInterest" {...register('provinceInterest', { required: true })}>
                  <option value="">Select...</option>
                  <option value="Alberta">Alberta</option>
                  <option value="British Columbia">British Columbia</option>
                  <option value="Manitoba">Manitoba</option>
                  <option value="New Brunswick">New Brunswick</option>
                  <option value="Newfoundland and Labrador">Newfoundland and Labrador</option>
                  <option value="Nova Scotia">Nova Scotia</option>
                  <option value="Ontario">Ontario</option>
                  <option value="Prince Edward Island">Prince Edward Island</option>
                  <option value="Quebec">Quebec</option>
                  <option value="Saskatchewan">Saskatchewan</option>
                  <option value="Yukon">Yukon</option>
                  <option value="Northwest Territories">Northwest Territories</option>
                  <option value="Nunavut">Nunavut</option>
                  <option value="Undecided">Undecided</option>
                </select>
                {errors.provinceInterest && <span className="error">This field is required.</span>}
                {renderCustomError('provinceInterest')}
              </div>

              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('ita-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Invitation to Apply (ITA) Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['ita-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['ita-info'] && (
                  <div className="info-content">
                    <p>An Invitation to Apply (ITA) is a formal notification from Immigration, Refugees and Citizenship Canada (IRCC) inviting you to submit an application for permanent residence through Express Entry.</p>
                    <p>ITAs are issued to candidates in the Express Entry pool who meet the minimum points threshold in a specific draw.</p>
                    <p className="note"><strong>Note:</strong> If you have received an ITA, you typically have 60 days to submit a complete application for permanent residence.</p>
                    <p className="warning"><strong>Important:</strong> An ITA is different from a provincial nomination or other expressions of interest. It is the official invitation from IRCC to apply for permanent residence.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="receivedITA" className="required-field">
                  Have you received an ITA (Invitation to Apply)?
                  <span
                    className="tooltip"
                    data-tooltip="An Invitation to Apply (ITA) is the official invitation from Immigration, Refugees and Citizenship Canada to submit your permanent residence application after being selected from the Express Entry pool."
                  >
                    ?
                  </span>
                </label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="receivedITAYes"
                    value="yes"
                    {...register('receivedITA', { required: true })}
                  />
                  <label htmlFor="receivedITAYes">Yes</label>

                  <input
                    type="radio"
                    id="receivedITANo"
                    value="no"
                    {...register('receivedITA')}
                  />
                  <label htmlFor="receivedITANo">No</label>
                </div>
                {errors.receivedITA && <span className="error">This field is required.</span>}
                {renderCustomError('receivedITA')}
              </div>

              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('canadian-relatives-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Canadian Relatives Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['canadian-relatives-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['canadian-relatives-info'] && (
                  <div className="info-content">
                    <p>Having close relatives in Canada who are citizens or permanent residents may give you additional points in your Express Entry profile.</p>
                    <p>To answer "Yes" to this question, you or your spouse/common-law partner must have at least one brother or sister living in Canada who is a citizen or permanent resident.</p>
                    <p className="note"><strong>Note:</strong> To qualify, the brother or sister must be:</p>
                    <ul>
                      <li>18 years old or older</li>
                      <li>related to you or your partner by blood, marriage, common-law partnership or adoption</li>
                      <li>have a parent in common with you or your partner</li>
                    </ul>
                    <p>A brother or sister is related to you by:</p>
                    <ul>
                      <li>blood (biological)</li>
                      <li>adoption</li>
                      <li>marriage (step-brother or step-sister)</li>
                    </ul>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="canadianRelatives" className="required-field">Do you have Canadian Relatives?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="relativesYes"
                    value="yes"
                    {...register('canadianRelatives', { required: true })}
                  />
                  <label htmlFor="relativesYes">Yes</label>

                  <input
                    type="radio"
                    id="relativesNo"
                    value="no"
                    {...register('canadianRelatives')}
                  />
                  <label htmlFor="relativesNo">No</label>
                </div>
                {errors.canadianRelatives && <span className="error">This field is required.</span>}
                {renderCustomError('canadianRelatives')}
              </div>

              {watchAllFields.canadianRelatives === 'yes' && (
                <div className="form-group">
                  <label htmlFor="relativeRelationship" className="required-field">
                    Relationship with Canadian Relative
                  </label>
                  <select id="relativeRelationship" {...register('relativeRelationship', { required: true })}>
                    <option value="">Select...</option>
                    <option value="brother-sister">Brother/Sister</option>
                    <option value="parent">Parent</option>
                    <option value="grandparent">Grandparent</option>
                    <option value="child">Child (18+ years old)</option>
                    <option value="aunt-uncle">Aunt/Uncle</option>
                    <option value="cousin">Cousin</option>
                    <option value="niece-nephew">Niece/Nephew</option>
                  </select>
                  {errors.relativeRelationship && <span className="error">This field is required.</span>}
                  {renderCustomError('relativeRelationship')}
                </div>
              )}
            </div>
          )}
        </div>

        {/* ---------------- ADDITIONAL INFORMATION SECTION ---------------- */}
        <div className="collapsible-section">
          <button
            type="button"
            className={`section-header ${openSections['additional'] ? 'active' : ''}`}
            onClick={() => toggleSection('additional')}
            aria-expanded={openSections['additional']}
            aria-controls="additional-content"
          >
            <div className="section-header-content">
              <span className={`section-status ${
                sectionStatus['additional'].hasErrors ? 'status-error' : 
                sectionStatus['additional'].complete ? 'status-complete' : 
                sectionStatus['additional'].touched ? 'status-incomplete' : 
                'status-untouched'
              }`} aria-hidden="true">
                {sectionStatus['additional'].hasErrors ? '✕' : 
                 sectionStatus['additional'].complete ? '✓' : 
                 sectionStatus['additional'].touched ? '⭘' : 
                 '-'}
              </span>
              Additional Information
            </div>
            <span className="chevron" aria-hidden="true">{openSections['additional'] ? '▲' : '▼'}</span>
          </button>

          {openSections['additional'] && (
            <div className="section-content" id="additional-content">
              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('preferred-destination-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Preferred Destination Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['preferred-destination-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['preferred-destination-info'] && (
                  <div className="info-content">
                    <p>Telling us your preferred destination in Canada helps us recommend the best immigration programs for your specific interests.</p>
                    <p>While this information doesn't affect your CRS score, different provinces and cities have unique immigration streams that may benefit you based on your profile and preferences.</p>
                    <p className="note"><strong>Note:</strong> This information is optional but highly recommended to help us provide tailored immigration pathway recommendations.</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="prefDestProvince">
                  Preferred Destination (Province)
                  <span
                    className="tooltip"
                    data-tooltip="Select the province you're planning to live in so we can recommend the best programs for your needs"
                  >
                    ?
                  </span>
                </label>
                <select id="prefDestProvince" {...register('prefDestProvince')}>
                  <option value="">Select...</option>
                  <option value="Alberta">Alberta</option>
                  <option value="British Columbia">British Columbia</option>
                  <option value="Manitoba">Manitoba</option>
                  <option value="New Brunswick">New Brunswick</option>
                  <option value="Newfoundland and Labrador">Newfoundland and Labrador</option>
                  <option value="Nova Scotia">Nova Scotia</option>
                  <option value="Ontario">Ontario</option>
                  <option value="Prince Edward Island">Prince Edward Island</option>
                  <option value="Quebec">Quebec</option>
                  <option value="Saskatchewan">Saskatchewan</option>
                  <option value="Yukon">Yukon</option>
                  <option value="Northwest Territories">Northwest Territories</option>
                  <option value="Nunavut">Nunavut</option>
                  <option value="Undecided">Undecided</option>
                </select>
                {renderCustomError('prefDestProvince')}
              </div>

              <div className="form-group">
                <label htmlFor="prefDestCity">
                  Preferred Destination (City)
                  <span
                    className="tooltip"
                    data-tooltip="Indicate your preferred city to help us identify local immigration programs and opportunities"
                  >
                    ?
                  </span>
                </label>
                <Controller
                  name="prefDestCity"
                  control={control}
                  render={({ field }) => (
                    <Select
                      inputId="prefDestCity"
                      options={formatCitiesForSelect()}
                      className="react-select-container"
                      classNamePrefix="react-select"
                      placeholder="Search and select a city..."
                      isSearchable={true}
                      isClearable={true}
                      value={formatCitiesForSelect().find(option => option.value === field.value) || null}
                      onChange={(option) => field.onChange(option ? option.value : '')}
                      onBlur={field.onBlur}
                    />
                  )}
                />
                {renderCustomError('prefDestCity')}
              </div>

              <div className="info-block">
                <div 
                  className="info-header"
                  onClick={() => toggleInfoBlock('settlement-funds-info')}
                >
                  <div className="info-title-container">
                    <span className="info-icon">ⓘ</span>
                    <h4 className="info-title">Settlement Funds Information</h4>
                  </div>
                  <span className="info-chevron">
                    {openInfoBlocks['settlement-funds-info'] ? '▲' : '▼'}
                  </span>
                </div>
                
                {openInfoBlocks['settlement-funds-info'] && (
                  <div className="info-content">
                    <p>Settlement funds are financial resources you'll need to establish yourself in Canada. While not a CRS requirement for Express Entry candidates with valid job offers or those already working in Canada, many immigration programs require proof of sufficient funds.</p>
                    <p>Different provinces have varying minimum settlement fund requirements for their Provincial Nominee Programs (PNPs).</p>
                    <p>Providing this information helps us identify the best immigration pathways based on your financial readiness.</p>
                    <p className="note"><strong>Note:</strong> For Express Entry without a job offer, the required settlement funds are based on family size and updated annually. For a single applicant, the amount is approximately $13,310 CAD (as of 2023).</p>
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="settlementFunds">
                  Settlement Funds (CAD)
                  <span
                    className="tooltip"
                    data-tooltip="The amount of money available to support your settlement in Canada"
                  >
                    ?
                  </span>
                </label>
                <select id="settlementFunds" {...register('settlementFunds')}>
                  <option value="">Select...</option>
                  <option value="Less than $10,000">Less than $10,000</option>
                  <option value="$10,000 - $15,000">$10,000 - $15,000</option>
                  <option value="$15,001 - $20,000">$15,001 - $20,000</option>
                  <option value="$20,001 - $25,000">$20,001 - $25,000</option>
                  <option value="$25,001 - $50,000">$25,001 - $50,000</option>
                  <option value="$50,001 - $100,000">$50,001 - $100,000</option>
                  <option value="More than $100,000">More than $100,000</option>
                </select>
                {renderCustomError('settlementFunds')}
              </div>
            </div>
          )}
        </div>

        {/* Bottom Validation Summary */}
        {validationSummary && (
          <div className="form-section" id="bottom-validation-summary">
            {renderValidationSummary(true)}
          </div>
        )}

        {/* SUBMIT BUTTON */}
        <div className="submit-container">
          <button type="submit" className="btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Submitting...' : 'Calculate Eligibility'}
          </button>
        </div>
      </form>

      {/* Add ValidationSummary modal */}
      <ValidationSummary 
        isOpen={validationSummary.showSummary}
        onClose={() => setValidationSummary({ ...validationSummary, showSummary: false })}
        errors={validationSummary.errors}
        scrollToSection={scrollToSection}
      />
    </div>
  );
};

export default ProfileForm;