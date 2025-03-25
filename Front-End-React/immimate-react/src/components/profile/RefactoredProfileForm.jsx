import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import useFormDraft from '../../hooks/useFormDraft';
import { useAuth } from '../../contexts/AuthContext';
import apiService from '../../services/ApiService';
import Logger from '../../utils/LoggingService';

// Import styles
import '../../styles/ProfileForm.css';
import '../../styles/FormPage.css';

// Import utils
import useLanguageTestConverter, { getScoreRangeDescription } from '../../hooks/useLanguageTestConverter';
import useSectionStatus from './hooks/useSectionStatus';

// Import components
import FormSummaryModal from './FormSummaryModal';
import ValidationSummary from './common/ValidationSummary';

// Import section components
import {
  PersonalInfoSection,
  EducationSection,
  LanguageSection,
  SecondaryLanguageSection,
  WorkExperienceSection,
  PartnerSection,
  JobOfferSection,
  ProvincialNominationSection,
  AdditionalInfoSection
} from './sections';

const RefactoredProfileForm = ({ draftOptions = {} }) => {
  // Merge default options with provided options
  const finalDraftOptions = {
    formId: 'profile-form',
    autoSaveInterval: 30000,
    enabled: false, // Default to disabled
    ...draftOptions
  };
  
  // Destructure the combined options
  const { formId, autoSaveInterval, enabled } = finalDraftOptions;

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Initialize form draft with disabled state from props
  const {
    formData,
    updateFormData,
    saveDraft,
    discardDraft,
    isLoading: isDraftLoading,
    isSaving,
    saveStatus,
    hasUnsavedChanges
  } = useFormDraft({
    formId,
    autoSaveInterval,
    enabled,
    initialData: {} // Start with empty data
  });
  
  // Add the language test converter hook
  const { 
    convertToCLB, 
    getSecondaryTestOptions,
    getScoreOptions
  } = useLanguageTestConverter();
  
  // Wrapper function for score options
  const getTestScoreOptions = useCallback((testType, skill) => {
    return getScoreOptions(testType, skill);
  }, [getScoreOptions]);
  
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const formRef = useRef(null);

  // --------------------- FORM STATE HOOKS --------------------- //
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
    reset,
    getValues
  } = useForm({
    mode: 'onChange',
    criteriaMode: 'all',
    defaultValues: formData || {}
  });

  // Watch all fields for dynamic validation
  const watchAllFields = watch();

  const foreignWorkExperience = watch('foreignExp');

  // --------------------- STATE MANAGEMENT --------------------- //
  const [formErrors, setFormErrors] = useState([]);
  const [validationSummary, setValidationSummary] = useState({
    showSummary: false,
    errors: []
  });
  const [setApiResponse] = useState(null);
  const [setApiError] = useState(null);
  const [hasSubmittedProfile, setHasSubmittedProfile] = useState(false);
  const [showSummaryModal, setShowSummaryModal] = useState(false);
  const [tempFormData, setTempFormData] = useState(null);

  // Use the custom section status hook for managing sections
  const { 
    sectionStatus, 
    openSections, 
    toggleSection, 
    setSectionComplete, 
    setSectionTouched 
  } = useSectionStatus([
    'personal-info',
    'education',
    'language',
    'secondary-language',
    'work-experience',
    'partner-info',
    'job-offer',
    'provincial-nomination',
    'additional-info'
  ]);

  // --------------------- EFFECTS --------------------- //
  
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
    let hasFetchedToken = false;
    
    const fetchCsrfToken = async () => {
      if (hasFetchedToken) return;
      
      try {
        hasFetchedToken = true;
        
        if (currentUser) {
          await apiService.fetchCsrfToken();
          Logger.debug('CSRF token fetched during component initialization');
        } else {
          Logger.debug('Skipping CSRF token fetch - user not authenticated');
        }
      } catch (error) {
        Logger.error('Failed to fetch CSRF token during initialization:', error);
      }
    };
    
    fetchCsrfToken();
    
    return () => {
      hasFetchedToken = true;
    };
  }, [currentUser]);

  // Load saved form data and initial setup
  useEffect(() => {
    const initialize = async () => {
      setIsLoading(true);

      try {
        // Initialize form with data from the useFormDraft hook
        if (formData) {
          reset(formData);
        }

        // Check if the user has already submitted a profile
        const hasProfile = await checkExistingProfile();
        
        // If they have an existing profile, set the flag
        setHasSubmittedProfile(hasProfile);
      } catch (error) {
        Logger.error('Error initializing form:', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (currentUser) {
      initialize();
    }
  }, [currentUser, reset, formData]);

  // Function to check if user has an existing profile
  const checkExistingProfile = async () => {
    try {
      const response = await apiService.api.get('/profiles/recent', {
        withCredentials: true // Important for JWT cookie
      });
      
      Logger.debug('Profile check response:', response.status);
      
      if (response.data && response.data.profileExists === true) {
        Logger.debug('Existing profile found with ID:', response.data.profileId);
        return true;
      } else {
        Logger.debug('No existing profile found');
        return false;
      }
    } catch (error) {
      if (error.response) {
        Logger.error('API error checking profile:', error.message);
        
        if (error.response.status === 401 || error.response.status === 403) {
          Logger.error('Authentication issue - not authenticated or token expired');
        } else if (error.response.status === 404) {
          // 404 is expected if the endpoint doesn't exist
          Logger.error('Endpoint not found: /profiles/recent');
        }
      } else {
        Logger.error('Error checking existing profile:', error);
      }
      return false;
    }
  };

  // Track field changes to update section status - Add dependency array and memoization
  useEffect(() => {
    // Skip if form is not yet initialized or we're still loading
    if (!watchAllFields || isLoading || isDraftLoading) return;
    
    // Use a ref to avoid infinite loops when updating section states
    const updateSectionStates = () => {
      const data = watchAllFields;
      
      // Update personal info section
      const personalInfoTouched = 
        !!data.fullName || 
        !!data.age || 
        !!data.citizenship || 
        !!data.residence || 
        !!data.maritalStatus;
      
      const personalInfoComplete = 
        !!data.fullName && 
        !!data.age && 
        !!data.citizenship && 
        !!data.residence && 
        !!data.maritalStatus;
      
      setSectionTouched('personal-info', personalInfoTouched);
      setSectionComplete('personal-info', personalInfoComplete);
      
      // Check if the user has a spouse/partner based on marital status and spouse conditions
      const hasSpouse = 
        (data.maritalStatus === 'Married' || data.maritalStatus === 'Common-Law') && 
        data.spouseIsPR === 'no' && 
        data.spouseIsAccompanying === 'yes';
      
      // Partner info section
      if (hasSpouse) {
        const partnerInfoTouched = 
          !!data.partnerEducation || 
          !!data.partnerLanguageTest || 
          !!data.partnerSpeaking || 
          !!data.partnerListening || 
          !!data.partnerReading || 
          !!data.partnerWriting || 
          !!data.partnerCanadianExp;
        
        const partnerInfoComplete = 
          !!data.partnerEducation && 
          !!data.partnerLanguageTest &&
          (data.partnerLanguageTest === 'none' || (
            !!data.partnerSpeaking &&
            !!data.partnerListening &&
            !!data.partnerReading &&
            !!data.partnerWriting
          )) &&
          data.partnerCanadianExp !== undefined;
        
        setSectionTouched('partner-info', partnerInfoTouched);
        setSectionComplete('partner-info', partnerInfoComplete);
      } else {
        // If the user doesn't have a spouse, mark section as complete and untouched
        setSectionTouched('partner-info', false);
        setSectionComplete('partner-info', true);
      }
      
      // Education section
      const educationTouched = 
        !!data.educationLevel || 
        data.eduInCanada !== undefined;
      
      const educationComplete = 
        !!data.educationLevel && 
        data.eduInCanada !== undefined;
      
      setSectionTouched('education', educationTouched);
      setSectionComplete('education', educationComplete);
      
      // Primary language section
      const languageTouched = 
        !!data.languageTest || 
        !!data.speaking || 
        !!data.listening || 
        !!data.reading || 
        !!data.writing;
      
      const languageComplete = 
        !!data.languageTest && 
        !!data.speaking && 
        !!data.listening && 
        !!data.reading && 
        !!data.writing;
      
      setSectionTouched('language', languageTouched);
      setSectionComplete('language', languageComplete);
      
      // Secondary language section is only required if they have a second language test
      if (data.secondaryLangTest) {
        const secondaryLanguageTouched = 
          !!data.secondarySpeaking || 
          !!data.secondaryListening || 
          !!data.secondaryReading || 
          !!data.secondaryWriting;
        
        const secondaryLanguageComplete = 
          !!data.secondarySpeaking && 
          !!data.secondaryListening && 
          !!data.secondaryReading && 
          !!data.secondaryWriting;
        
        setSectionTouched('secondary-language', secondaryLanguageTouched);
        setSectionComplete('secondary-language', secondaryLanguageComplete);
      } else {
        // If no secondary language test, mark as complete
        setSectionTouched('secondary-language', false);
        setSectionComplete('secondary-language', true);
      }
      
      // Work experience section
      const workExperienceTouched = 
        data.workInsideCanada !== undefined || 
        data.workOutsideCanada !== undefined ||
        data.foreignExp !== undefined;
      
      const workExperienceComplete = 
        data.workInsideCanada !== undefined && 
        data.workOutsideCanada !== undefined;
      
      setSectionTouched('work-experience', workExperienceTouched);
      setSectionComplete('work-experience', workExperienceComplete);
      
      // Provincial nomination section
      const provincialTouched = 
        data.provNomination !== undefined || 
        !!data.provinceInterest || 
        data.canadianRelatives !== undefined ||
        !!data.relativeRelationship ||
        data.receivedITA !== undefined;
      
      const provincialComplete = 
        data.provNomination !== undefined && 
        !!data.provinceInterest && 
        data.canadianRelatives !== undefined &&
        (data.canadianRelatives === 'no' || 
         (data.canadianRelatives === 'yes' && !!data.relativeRelationship)) &&
        data.receivedITA !== undefined;
      
      setSectionTouched('provincial-nomination', provincialTouched);
      setSectionComplete('provincial-nomination', provincialComplete);
      
      // Job offer section
      const jobOfferTouched = data.jobOffer !== undefined;
      const jobOfferComplete = data.jobOffer !== undefined && 
        (data.jobOffer === 'no' || (data.jobOffer === 'yes' && !!data.jobOfferNocCode));
      
      setSectionTouched('job-offer', jobOfferTouched);
      setSectionComplete('job-offer', jobOfferComplete);
      
      // Additional info section
      const additionalInfoTouched = !!data.settlementFunds || !!data.preferredDestination;
      const additionalInfoComplete = !!data.settlementFunds;
      
      setSectionTouched('additional-info', additionalInfoTouched);
      setSectionComplete('additional-info', additionalInfoComplete);
    };
    
    // Use setTimeout to prevent multiple updates in the same render cycle
    const timeoutId = setTimeout(updateSectionStates, 0);
    
    return () => clearTimeout(timeoutId);
  }, [watchAllFields, isLoading, isDraftLoading, setSectionTouched, setSectionComplete]);

  // --------------------- UTILITY FUNCTIONS --------------------- //
  
  // Function to clear saved form data
  const clearSavedFormData = () => {
    discardDraft();
    reset({});
  };

  // Function to handle form submission
  const onSubmit = async (data) => {
    setIsSubmitting(true);
    setApiError(null);
    setApiResponse(null);
    
    try {
      // Perform final validation
      const validationErrors = performFinalValidation(data);
      if (validationErrors.length > 0) {
        setFormErrors(validationErrors);
        setIsSubmitting(false);
        return;
      }
      
      // Prepare data for submission
      const cleanedData = cleanDataBeforeSubmission(data);
      
      // Show the form summary modal
      setTempFormData(cleanedData);
      setShowSummaryModal(true);
      setIsSubmitting(false);
    } catch (error) {
      setApiError({
        message: 'An error occurred while processing your submission.',
        details: [error.message]
      });
      setIsSubmitting(false);
    }
  };

  // Perform final validation before submission
  const performFinalValidation = (formData) => {
    const errors = [];
    
    // Example validation checks - add your actual validation logic here
    if (!formData.fullName) {
      errors.push({ field: 'fullName', message: 'Full name is required' });
    }
    
    if (!formData.age || formData.age < 16) {
      errors.push({ field: 'age', message: 'Age must be 16 or older' });
    }
    
    // Return any found errors
    return errors;
  };

  // Clean data before submission
  const cleanDataBeforeSubmission = (formData) => {
    // Create a copy of the form data
    const cleanedData = { ...formData };
    
    // Convert yes/no values to booleans
    const yesNoFields = [
      'hasPartner', 'partnerComing', 'workInsideCanada', 'eduInCanada', 'jobOffer',
      'provNomination', 'receivedITA', 'canadianRelatives'
    ];
    
    yesNoFields.forEach(field => {
      if (cleanedData[field] === 'yes') {
        cleanedData[field] = true;
      } else if (cleanedData[field] === 'no') {
        cleanedData[field] = false;
      }
    });
    
    // Map fields from education and work sections to studiedInCanada and workedInCanada
    cleanedData.studiedInCanada = cleanedData.eduInCanada;
    cleanedData.workedInCanada = cleanedData.workInsideCanada;
    
    // Convert numeric string values to numbers
    const numericFields = [
      'age', 'canadianExp', 'foreignExp', 'partnerAge'
    ];
    
    numericFields.forEach(field => {
      if (cleanedData[field] && !isNaN(cleanedData[field])) {
        cleanedData[field] = Number(cleanedData[field]);
      }
    });

    // Calculate CLB scores for language tests
    if (cleanedData.primaryLanguageTest && cleanedData.primaryLanguageTest !== 'none') {
      try {
        cleanedData.primaryLanguageCLB = {
          speaking: convertToCLB(cleanedData.primaryLanguageTest, 'speaking', cleanedData.speaking),
          listening: convertToCLB(cleanedData.primaryLanguageTest, 'listening', cleanedData.listening),
          reading: convertToCLB(cleanedData.primaryLanguageTest, 'reading', cleanedData.reading),
          writing: convertToCLB(cleanedData.primaryLanguageTest, 'writing', cleanedData.writing)
        };
      } catch (error) {
        Logger.error('Error calculating primary language CLB:', error);
      }
    }

    if (cleanedData.secondaryLanguageTest && cleanedData.secondaryLanguageTest !== 'none') {
      try {
        cleanedData.secondaryLanguageCLB = {
          speaking: convertToCLB(cleanedData.secondaryLanguageTest, 'speaking', cleanedData.secSpeaking),
          listening: convertToCLB(cleanedData.secondaryLanguageTest, 'listening', cleanedData.secListening),
          reading: convertToCLB(cleanedData.secondaryLanguageTest, 'reading', cleanedData.secReading),
          writing: convertToCLB(cleanedData.secondaryLanguageTest, 'writing', cleanedData.secWriting)
        };
      } catch (error) {
        Logger.error('Error calculating secondary language CLB:', error);
      }
    }

    if (cleanedData.partnerTestType && cleanedData.partnerTestType !== 'none') {
      try {
        cleanedData.partnerLanguageCLB = {
          speaking: convertToCLB(cleanedData.partnerTestType, 'speaking', cleanedData.partnerSpeaking),
          listening: convertToCLB(cleanedData.partnerTestType, 'listening', cleanedData.partnerListening),
          reading: convertToCLB(cleanedData.partnerTestType, 'reading', cleanedData.partnerReading),
          writing: convertToCLB(cleanedData.partnerTestType, 'writing', cleanedData.partnerWriting)
        };
      } catch (error) {
        Logger.error('Error calculating partner language CLB:', error);
      }
    }
    
    // Check if the user has a spouse/partner
    const hasSpouse = 
      (cleanedData.maritalStatus === 'Married' || cleanedData.maritalStatus === 'Common-Law') && 
      cleanedData.spouseIsPR === 'no' && 
      cleanedData.spouseIsAccompanying === 'yes';
    
    // Partner info section
    if (hasSpouse) {
      const partnerInfoTouched = 
        !!cleanedData.partnerEducation || 
        !!cleanedData.partnerLanguageTest || 
        !!cleanedData.partnerSpeaking || 
        !!cleanedData.partnerListening || 
        !!cleanedData.partnerReading || 
        !!cleanedData.partnerWriting || 
        !!cleanedData.partnerCanadianExp;
      
      const partnerInfoComplete = 
        !!cleanedData.partnerEducation && 
        !!cleanedData.partnerLanguageTest &&
        !!cleanedData.partnerSpeaking &&
        !!cleanedData.partnerListening &&
        !!cleanedData.partnerReading &&
        !!cleanedData.partnerWriting &&
        !!cleanedData.partnerCanadianExp;
      
      cleanedData.partnerInfoTouched = partnerInfoTouched;
      cleanedData.partnerInfoComplete = partnerInfoComplete;
    } else {
      // If the user doesn't have a spouse, mark section as complete
      cleanedData.partnerInfoComplete = true;
    }
    
    return cleanedData;
  };

  // Handle form submission confirmation
  const handleFormSubmitConfirm = async () => {
    setIsSubmitting(true);
    setShowSummaryModal(false);
    
    try {
      // Submit the data to the backend
      const response = await apiService.api.post('/profiles/submit', tempFormData, {
        withCredentials: true // Important for JWT cookie
      });
      
      setApiResponse({
        success: true,
        message: 'Your profile has been submitted successfully!',
        data: response.data
      });
      
      // Clear the form data after successful submission
      clearSavedFormData();
      setHasSubmittedProfile(true);
    } catch (error) {
      setApiError({
        message: 'Failed to submit your profile. Please try again.',
        details: [error.response?.data?.message || error.message]
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle save draft button click
  const handleSaveDraft = () => {
    const currentFormData = getValues();
    updateFormData(currentFormData);
    saveDraft();
  };

  // Handle discard draft button click
  const handleDiscardDraft = () => {
    if (window.confirm('Are you sure you want to discard all changes?')) {
      clearSavedFormData();
    }
  };

  // Handle opening form summary
  const handleOpenFormSummary = () => {
    const currentFormData = getValues();
    setTempFormData(cleanDataBeforeSubmission(currentFormData));
    setShowSummaryModal(true);
  };

  // --------------------- RENDERING FUNCTIONS --------------------- //
  
  // Function to render country options
  const renderCountryOptions = useCallback(() => {
    const countries = [
      "Afghanistan", "Albania", "Algeria", "Andorra", "Angola", "Antigua & Deps", "Argentina", "Armenia", "Australia", "Austria",
      "Azerbaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bhutan",
      "Bolivia", "Bosnia Herzegovina", "Botswana", "Brazil", "Brunei", "Bulgaria", "Burkina", "Burundi", "Cambodia", "Cameroon",
      "Canada", "Cape Verde", "Central African Rep", "Chad", "Chile", "China", "Colombia", "Comoros", "Congo", "Congo {Democratic Rep}",
      "Costa Rica", "Croatia", "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "East Timor",
      "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia", "Fiji", "Finland", "France",
      "Gabon", "Gambia", "Georgia", "Germany", "Ghana", "Greece", "Grenada", "Guatemala", "Guinea", "Guinea-Bissau",
      "Guyana", "Haiti", "Honduras", "Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland {Republic}",
      "Israel", "Italy", "Ivory Coast", "Jamaica", "Japan", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Korea North",
      "Korea South", "Kosovo", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya",
      "Liechtenstein", "Lithuania", "Luxembourg", "Macedonia", "Madagascar", "Malawi", "Malaysia", "Maldives", "Mali", "Malta",
      "Marshall Islands", "Mauritania", "Mauritius", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montenegro", "Morocco",
      "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "New Zealand", "Nicaragua", "Niger", "Nigeria",
      "Norway", "Oman", "Pakistan", "Palau", "Palestine", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines",
      "Poland", "Portugal", "Qatar", "Romania", "Russian Federation", "Rwanda", "St Kitts & Nevis", "St Lucia", "Saint Vincent & the Grenadines",
      "Samoa", "San Marino", "Sao Tome & Principe", "Saudi Arabia", "Senegal", "Serbia", "Seychelles", "Sierra Leone", "Singapore",
      "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Sudan", "Spain", "Sri Lanka", "Sudan", "Suriname",
      "Swaziland", "Sweden", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Togo", "Tonga",
      "Trinidad & Tobago", "Tunisia", "Turkey", "Turkmenistan", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States",
      "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Yemen", "Zambia", "Zimbabwe"
    ];
    
    return countries.map(country => (
      <option key={country} value={country}>{country}</option>
    ));
  }, []);

  // Function to render job options
  const renderJobOptions = useCallback(() => {
    // Example NOC codes - this would be populated from an API or larger dataset
    const nocCodes = [
      { code: "00011", title: "Legislators" },
      { code: "00012", title: "Senior government managers and officials" },
      { code: "00013", title: "Senior managers - financial, communications and other business services" },
      { code: "00014", title: "Senior managers - health, education, social and community services and membership organizations" },
      { code: "00015", title: "Senior managers - trade, broadcasting and other services, n.e.c." },
      { code: "00016", title: "Senior managers - construction, transportation, production and utilities" },
      { code: "10010", title: "Administrative services managers" },
      { code: "10011", title: "Administrative services managers" },
      { code: "10012", title: "Insurance, real estate and financial brokerage managers" },
      { code: "10019", title: "Other business services managers" },
      { code: "10020", title: "Managers in communication (except broadcasting)" },
      // Add more as needed
    ];
    
    return nocCodes.map(job => (
      <option key={job.code} value={job.code}>
        {job.code} - {job.title}
      </option>
    ));
  }, []);

  // Function to get Canadian education options
  const getCanadianEducationOptions = useCallback((currentEducation) => {
    const options = [
      { value: '', label: 'Select...' },
      { value: 'high-school', label: 'High school' },
      { value: 'one-year', label: 'One-year program' },
      { value: 'two-year', label: 'Two-year program' },
      { value: 'bachelors', label: 'Bachelor\'s degree' },
      { value: 'masters', label: 'Master\'s degree' },
      { value: 'doctoral', label: 'Doctoral degree' }
    ];
    
    return options;
  }, []);

  // Function to render custom error messages
  const renderCustomError = useCallback((fieldName) => {
    if (!formErrors || formErrors.length === 0) return null;
    
    const error = formErrors.find(err => err.field === fieldName);
    if (!error) return null;
    
    return <div className="error custom-error">{error.message}</div>;
  }, [formErrors]);

  // Function to render validation summary
  const renderValidationSummary = useCallback((isModal = false) => {
    // If no errors or not showing summary, don't render
    if (!validationSummary.showSummary || !validationSummary.errors || validationSummary.errors.length === 0) {
      return null;
    }
    
    // Function to scroll to a section
    const scrollToSection = (sectionId) => {
      // Open the section
      toggleSection(sectionId);
      
      // Scroll to it
      setTimeout(() => {
        const sectionElement = document.getElementById(sectionId + '-content');
        if (sectionElement) {
          sectionElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
      }, 100);
    };
    
    return (
      <ValidationSummary 
        isOpen={validationSummary.showSummary}
        onClose={() => setValidationSummary(prev => ({ ...prev, showSummary: false }))}
        errors={validationSummary.errors}
        scrollToSection={scrollToSection}
      />
    );
  }, [validationSummary, toggleSection]);

  // Function to get job offer options
  const getJobOfferOptions = useCallback(() => {
    return renderJobOptions();
  }, [renderJobOptions]);

  // --------------------- RENDER FORM --------------------- //
  if (isLoading) {
    return (
      <div className="profile-form-container">
        <h1>Loading...</h1>
      </div>
    );
  }

  if (hasSubmittedProfile) {
    return (
      <div className="profile-form-container">
        <div className="notification success-notification">
          <h3>Profile Already Submitted</h3>
          <p>You have already submitted a profile. If you need to make changes, please contact support.</p>
        </div>
      </div>
    );
  }

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
          <p>Loading your profile information...</p>
        </div>
      )}
      
      {/* Auto-save status indicator - only show if draft is enabled */}
      {enabled && (
        <div className="auto-save-status">
          {isSaving && <span className="saving">Saving draft...</span>}
          {saveStatus === 'saved' && <span className="saved">Draft saved</span>}
          {saveStatus === 'error' && <span className="error">Error saving draft</span>}
          {hasUnsavedChanges && <span className="unsaved">Unsaved changes</span>}
        </div>
      )}

      {/* Top validation summary */}
      {renderValidationSummary(false)}

      <form onSubmit={handleSubmit(onSubmit)} id="profileForm" className="profile-form" ref={formRef}>
        <div className="form-intro">
          <h2>Express Entry Profile</h2>
          <p>Please fill in your information to determine your eligibility for Canadian immigration programs.</p>
        </div>
        
        {/* Form Sections */}
        <div className="form-sections">
          {/* Personal Information Section */}
          <PersonalInfoSection
            register={register}
            errors={errors}
            watch={watch}
            isOpen={openSections['personal-info']}
            onToggle={() => toggleSection('personal-info')}
            isComplete={sectionStatus['personal-info'].complete}
            hasErrors={sectionStatus['personal-info'].hasErrors}
            touched={sectionStatus['personal-info'].touched}
            renderCountryOptions={renderCountryOptions}
            renderCustomError={renderCustomError}
          />

          {/* Education Section */}
          <EducationSection
            register={register}
            errors={errors}
            watchAllFields={watchAllFields}
            isOpen={openSections['education']}
            onToggle={() => toggleSection('education')}
            isComplete={sectionStatus['education'].complete}
            hasErrors={sectionStatus['education'].hasErrors}
            touched={sectionStatus['education'].touched}
            getCanadianEducationOptions={getCanadianEducationOptions}
            renderCustomError={renderCustomError}
          />

          {/* Language Section */}
          <LanguageSection
            register={register}
            errors={errors}
            watch={watch}
            isOpen={openSections['language']}
            onToggle={() => toggleSection('language')}
            isComplete={sectionStatus['language'].complete}
            hasErrors={sectionStatus['language'].hasErrors}
            touched={sectionStatus['language'].touched}
            getTestScoreOptions={getTestScoreOptions}
            getScoreRangeDescription={getScoreRangeDescription}
            renderCustomError={renderCustomError}
          />

          {/* Secondary Language Section */}
          <SecondaryLanguageSection
            register={register}
            errors={errors}
            watchAllFields={watchAllFields}
            watch={watch}
            isOpen={openSections['secondary-language']}
            onToggle={() => toggleSection('secondary-language')}
            isComplete={sectionStatus['secondary-language'].complete}
            hasErrors={sectionStatus['secondary-language'].hasErrors}
            touched={sectionStatus['secondary-language'].touched}
            getTestScoreOptions={getTestScoreOptions}
            getSecondaryTestOptions={getSecondaryTestOptions}
            getScoreRangeDescription={getScoreRangeDescription}
            renderCustomError={renderCustomError}
          />

          {/* Work Experience Section */}
          <WorkExperienceSection
            register={register}
            errors={errors}
            watchForeignWorkExperience={parseInt(foreignWorkExperience || 0)}
            isOpen={openSections['work-experience']}
            onToggle={() => toggleSection('work-experience')}
            isComplete={sectionStatus['work-experience'].complete}
            hasErrors={sectionStatus['work-experience'].hasErrors}
            touched={sectionStatus['work-experience'].touched}
            renderJobOptions={renderJobOptions}
            renderCustomError={renderCustomError}
          />

          {/* Partner Section - Only render when user is married/common-law with a non-PR spouse who will accompany */}
          {watchAllFields.maritalStatus && 
           (watchAllFields.maritalStatus === 'Married' || watchAllFields.maritalStatus === 'Common-Law') && 
           watchAllFields.spouseIsPR === 'no' && 
           watchAllFields.spouseIsAccompanying === 'yes' && (
            <PartnerSection
              register={register}
              errors={errors}
              watch={watch}
              watchAllFields={watchAllFields}
              isOpen={openSections['partner-info']}
              onToggle={() => toggleSection('partner-info')}
              isComplete={sectionStatus['partner-info'].complete}
              hasErrors={sectionStatus['partner-info'].hasErrors}
              touched={sectionStatus['partner-info'].touched}
              getTestScoreOptions={getTestScoreOptions}
              getScoreRangeDescription={getScoreRangeDescription}
              renderCustomError={renderCustomError}
            />
          )}

          {/* Job Offer Section */}
          <JobOfferSection
            register={register}
            errors={errors}
            watch={watch}
            isOpen={openSections['job-offer']}
            onToggle={() => toggleSection('job-offer')}
            isComplete={sectionStatus['job-offer'].complete}
            hasErrors={sectionStatus['job-offer'].hasErrors}
            touched={sectionStatus['job-offer'].touched}
            getJobOfferOptions={getJobOfferOptions}
            renderCustomError={renderCustomError}
          />

          {/* Provincial Nomination Section */}
          <ProvincialNominationSection
            register={register}
            errors={errors}
            watch={watch}
            watchAllFields={watchAllFields}
            isOpen={openSections['provincial-nomination']}
            onToggle={() => toggleSection('provincial-nomination')}
            isComplete={sectionStatus['provincial-nomination'].complete}
            hasErrors={sectionStatus['provincial-nomination'].hasErrors}
            touched={sectionStatus['provincial-nomination'].touched}
            renderCustomError={renderCustomError}
          />

          {/* Additional Info Section */}
          <AdditionalInfoSection
            register={register}
            errors={errors}
            isOpen={openSections['additional-info']}
            onToggle={() => toggleSection('additional-info')}
            isComplete={sectionStatus['additional-info'].complete}
            hasErrors={sectionStatus['additional-info'].hasErrors}
            touched={sectionStatus['additional-info'].touched}
            renderCustomError={renderCustomError}
          />
        </div>

        {/* Form Buttons */}
        <div className="form-buttons">
          <button 
            type="button" 
            className="btn-secondary"
            onClick={handleDiscardDraft}
            disabled={isSubmitting}
          >
            Discard Changes
          </button>
          <button 
            type="button" 
            className="btn-primary"
            onClick={handleSaveDraft}
            disabled={isSubmitting}
          >
            Save Draft
          </button>
          <button 
            type="button"
            className="btn-primary"
            onClick={handleOpenFormSummary}
            disabled={isSubmitting}
          >
            Review & Submit
          </button>
          <button 
            type="submit" 
            className="btn-submit" 
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="spinner"></span>
                Submitting...
              </>
            ) : (
              'Submit Application'
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default RefactoredProfileForm; 