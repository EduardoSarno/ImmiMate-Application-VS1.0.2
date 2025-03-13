import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useForm } from 'react-hook-form';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

// Import validation
import { validateProfileForm } from '../../validation/ProfileValidation';

// Import styles
import '../../styles/ProfileForm.css';
import '../../styles/FormPage.css';

// Import utils
import { getScoreOptions, convertToCLB, getScoreRangeDescription } from '../../utils/LanguageTestConverter';

// Import components
import FormSummaryModal from './FormSummaryModal';

const ProfileForm = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();

  // Add formRef definition
  const formRef = useRef(null);

  // Debug Auth Token
  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (!token) {
      console.warn('DEBUG: No access_token found in localStorage');
    } else {
      console.log('DEBUG: Found access_token in localStorage');
      // Log the first 10 characters of the token (safe to log partial token)
      console.log('DEBUG: Token starts with:', token.substring(0, 10) + '...');
      
      try {
        // Check if token is JWT format (has 3 parts separated by dots)
        const parts = token.split('.');
        if (parts.length === 3) {
          console.log('DEBUG: Token appears to be in JWT format');
        } else {
          console.warn('DEBUG: Token does not appear to be in JWT format');
        }
      } catch (err) {
        console.error('DEBUG: Error analyzing token:', err);
      }
    }
    
    // Check other auth-related items in localStorage
    console.log('DEBUG: user_email in localStorage:', localStorage.getItem('user_email'));
    console.log('DEBUG: user_id in localStorage:', localStorage.getItem('user_id'));
    console.log('DEBUG: currentUser object:', currentUser);
  }, [currentUser]);

  // --------------------- STATE MANAGEMENT --------------------- //
  const [progressValue, setProgressValue] = useState(0);
  const [formErrors, setFormErrors] = useState([]);
  const [validationSummary, setValidationSummary] = useState(null);
  const [apiResponse, setApiResponse] = useState(null);
  const [apiError, setApiError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [hasSubmittedProfile, setHasSubmittedProfile] = useState(false);
  const [showSummaryModal, setShowSummaryModal] = useState(false);
  const [tempFormData, setTempFormData] = useState(null);

  // Track which sections are open
  const [openSections, setOpenSections] = useState({
    'personal-info': true,
    'education': false,
    'language': false,
    'secondary-language': false,
    'work-experience': false,
    'spouse': false,
    'job-offer': false,
    'provincial': false,
    'additional': false
  });

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

  // --------------------- LOCALSTORAGE: LOAD DEFAULTS --------------------- //
  const loadSavedData = () => {
    try {
      const saved = localStorage.getItem('immigrationFormData');
      return saved ? JSON.parse(saved) : {};
    } catch (err) {
      console.error('Error parsing saved form data:', err);
      return {};
    }
  };

  // --------------------- REACT HOOK FORM --------------------- //
  const {
    register,
    handleSubmit,
    watch,
    // eslint-disable-next-line no-unused-vars
    setValue, // Used for programmatically setting form values
    formState: { errors }
  } = useForm({
    defaultValues: loadSavedData()
    // If using a Yup schema, you can do: resolver: yupResolver(validationSchema)
  });

  // Watch all fields for localStorage + progress bar
  const watchAllFields = watch();

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
      tradesCertification: 'education',
      
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
    
    // Personal Info section
    const personalInfoComplete = 
      data.fullName && 
      data.age && 
      data.citizenship && 
      data.residence && 
      data.maritalStatus;
    
    // Track which sections have been touched
    const personalInfoTouched = 
      !!data.fullName || 
      !!data.age || 
      !!data.citizenship || 
      !!data.residence || 
      !!data.maritalStatus;
    
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
    
    // Spouse section (only if married or common-law)
    const spouseComplete = 
      (data.maritalStatus !== 'married' && data.maritalStatus !== 'common-law') || 
      (data.partnerEducation && 
       data.partnerLanguageTest && 
       data.partnerSpeaking && 
       data.partnerListening && 
       data.partnerReading && 
       data.partnerWriting && 
       data.partnerCanadianExp !== undefined);
    
    const spouseTouched = 
      (data.maritalStatus === 'married' || data.maritalStatus === 'common-law') && 
      (!!data.partnerEducation || 
       !!data.partnerLanguageTest || 
       !!data.partnerSpeaking || 
       !!data.partnerListening || 
       !!data.partnerReading || 
       !!data.partnerWriting || 
       data.partnerCanadianExp !== undefined);
    
    // Job Offer section
    const jobOfferComplete = 
      data.jobOffer && 
      (data.jobOffer === 'no' || 
       (data.lmiaStatus && 
        data.jobWage && 
        data.jobOfferNocCode && 
        data.weeklyHours && 
        data.jobDetails));
    
    const jobOfferTouched = 
      !!data.jobOffer || 
      !!data.lmiaStatus || 
      !!data.jobWage || 
      !!data.jobOfferNocCode || 
      !!data.weeklyHours || 
      !!data.jobDetails;
    
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
    const newSectionStatus = {
      'personal-info': { 
        complete: personalInfoComplete, 
        active: openSections['personal-info'],
        hasErrors: !!sectionsWithErrors['personal-info'],
        touched: personalInfoTouched
      },
      'education': { 
        complete: educationComplete, 
        active: openSections['education'],
        hasErrors: !!sectionsWithErrors['education'],
        touched: educationTouched
      },
      'language': { 
        complete: languageComplete, 
        active: openSections['language'],
        hasErrors: !!sectionsWithErrors['language'],
        touched: languageTouched 
      },
      'secondary-language': { 
        complete: secondaryLanguageComplete, 
        active: openSections['secondary-language'],
        hasErrors: !!sectionsWithErrors['secondary-language'],
        touched: secondaryLanguageTouched
      },
      'work-experience': { 
        complete: workExperienceComplete, 
        active: openSections['work-experience'],
        hasErrors: !!sectionsWithErrors['work-experience'],
        touched: workExperienceTouched
      },
      'spouse': { 
        complete: spouseComplete, 
        active: openSections['spouse'],
        hasErrors: !!sectionsWithErrors['spouse'],
        touched: spouseTouched
      },
      'job-offer': { 
        complete: jobOfferComplete, 
        active: openSections['job-offer'],
        hasErrors: !!sectionsWithErrors['job-offer'],
        touched: jobOfferTouched
      },
      'provincial': { 
        complete: provincialComplete, 
        active: openSections['provincial'],
        hasErrors: !!sectionsWithErrors['provincial'],
        touched: provincialTouched
      },
      'additional': { 
        complete: additionalComplete, 
        active: openSections['additional'],
        hasErrors: !!sectionsWithErrors['additional'],
        touched: additionalTouched
      }
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
  }, [watchAllFields, openSections, errors]);

  // On every render of the form, save to localStorage + update progress
  useEffect(() => {
    try {
      localStorage.setItem('immigrationFormData', JSON.stringify(watchAllFields));
    } catch (err) {
      console.error('Error saving to localStorage:', err);
    }
    const pct = updateProgressBar();
    setProgressValue(pct || 0);
    
    // Check section completion
    checkSectionCompletion();
  }, [watchAllFields, updateProgressBar, checkSectionCompletion]); // Add checkSectionCompletion to dependencies

  // --------------------- COLLAPSIBLE SECTIONS --------------------- //
  const toggleSection = (sectionId) => {
    setOpenSections((prev) => {
      const newState = {
        ...prev,
        [sectionId]: !prev[sectionId]
      };
      
      // Update section status to reflect active state
      setSectionStatus(prevStatus => ({
        ...prevStatus,
        [sectionId]: {
          ...prevStatus[sectionId],
          active: newState[sectionId]
        }
      }));
      
      return newState;
    });
  };

  // --------------------- DROPDOWN DATA (COUNTRIES, CITIES, JOBS) --------------------- //
  const [countries, setCountries] = useState([
    { Countries: 'Canada' },
    { Countries: 'United States' },
    { Countries: 'United Kingdom' },
    { Countries: 'India' },
    { Countries: 'Australia' }
  ]);
  const [cities, setCities] = useState([
    { City: 'Toronto', Provinces: 'Ontario' },
    { City: 'Vancouver', Provinces: 'British Columbia' },
    { City: 'Montreal', Provinces: 'Quebec' }
  ]);
  const [jobs, setJobs] = useState([
    { NOC: '21234', 'Job Title': 'Software Developer' },
    { NOC: '31102', 'Job Title': 'Medical Doctor' },
    { NOC: '41200', 'Job Title': 'Teacher' }
  ]);

  // Example fetch from /Data/. Adjust as needed
  useEffect(() => {
    const loadCountries = async () => {
      try {
        const response = await fetch('/Data/countries.json');
        if (response.ok) {
          const data = await response.json();
          setCountries(data);
        }
      } catch (error) {
        console.warn('Falling back to default countries. Reason:', error);
      }
    };
    const loadCities = async () => {
      try {
        const response = await fetch('/Data/canadacities.json');
        if (response.ok) {
          const data = await response.json();
          setCities(data);
        }
      } catch (error) {
        console.warn('Falling back to default cities. Reason:', error);
      }
    };
    const loadJobs = async () => {
      try {
        const response = await fetch('/Data/jobs.json');
        if (response.ok) {
          const data = await response.json();
          setJobs(data);
        }
      } catch (error) {
        console.warn('Falling back to default jobs. Reason:', error);
      }
    };

    // Check if user already has a profile
    const checkExistingProfile = async () => {
      try {
        const token = localStorage.getItem('access_token');
        
        // More detailed token check
        if (!token) {
          console.log('No access token found in localStorage');
          setIsLoading(false);
          return;
        }
        
        console.log('Token found, attempting to check existing profile...');
        
        // Add a try block to better diagnose the API call
        try {
          const response = await axios.get(`http://localhost:8080/api/profiles/recent`, {
            headers: { 
              Authorization: `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          });
          
          console.log('Profile check response:', response.status, response.data);
          
          const data = response.data;
          if (data.profileExists) {
            setHasSubmittedProfile(true);
            localStorage.setItem('has_submitted_profile', 'true');
            navigate('/dashboard');
            return;
          }
        } catch (apiError) {
          // Detailed API error logging
          console.error('API error checking profile:', apiError.message);
          
          if (apiError.response) {
            // Server responded with a non-2xx status
            console.error('Status:', apiError.response.status);
            console.error('Data:', apiError.response.data);
            console.error('Headers:', apiError.response.headers);
            
            // Check if token might be invalid (401/403)
            if (apiError.response.status === 401 || apiError.response.status === 403) {
              console.error('Authentication issue - token may be invalid or expired');
            }
          } else if (apiError.request) {
            // Request was made but no response received
            console.error('No response received from server');
          }
          
          // Don't rethrow - just log and continue
        }
      } catch (err) {
        console.error('Error checking existing profile:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadCountries();
    loadCities();
    loadJobs();

    if (currentUser) {
      checkExistingProfile();
    } else {
      setIsLoading(false);
    }
  }, [currentUser, navigate]);

  // --------------------- CLEAR LOCALSTORAGE (IF NEEDED) --------------------- //
  const clearSavedFormData = () => {
    localStorage.removeItem('immigrationFormData');
  };

  // --------------------- SUBMISSION HANDLER --------------------- //
  const onSubmit = async (formData) => {
    // Clear prior messages
    setFormErrors([]);
    setApiError(null);
    setApiResponse(null);
    setValidationSummary(null); // Clear validation summary

    // Create a transformed data object that matches the validation field names
    const validationData = {
      // Map form fields to validation expected names
      userEmail: localStorage.getItem('user_email') || '',
      applicantName: formData.fullName || '',
      applicantAge: parseInt(formData.age, 10) || null,
      applicantCitizenship: formData.citizenship || '',
      applicantResidence: formData.residence || '',
      applicantMaritalStatus: formData.maritalStatus ? formData.maritalStatus.charAt(0).toUpperCase() + formData.maritalStatus.slice(1) : '',
      
      // Education
      applicantEducationLevel: formData.educationLevel || '',
      educationCompletedInCanada: formData.eduInCanada === 'yes',
      canadianEducationLevel: formData.canadianEducationLevel || null,
      
      // Language - Convert to CLB if needed
      primaryLanguageTestType: formData.primaryLanguageTest || '',
      primaryTestSpeakingScore: formData.primaryLanguageTest && formData.speaking ? 
        convertToCLB(formData.primaryLanguageTest, 'speaking', formData.speaking) : 
        parseInt(formData.speaking, 10) || null,
      primaryTestListeningScore: formData.primaryLanguageTest && formData.listening ? 
        convertToCLB(formData.primaryLanguageTest, 'listening', formData.listening) : 
        parseInt(formData.listening, 10) || null,
      primaryTestReadingScore: formData.primaryLanguageTest && formData.reading ? 
        convertToCLB(formData.primaryLanguageTest, 'reading', formData.reading) : 
        parseInt(formData.reading, 10) || null,
      primaryTestWritingScore: formData.primaryLanguageTest && formData.writing ? 
        convertToCLB(formData.primaryLanguageTest, 'writing', formData.writing) : 
        parseInt(formData.writing, 10) || null,
      
      // Secondary Language - Convert to CLB if needed
      tookSecondaryLanguageTest: formData.secondaryLangTest === 'yes',
      secondaryTestType: formData.secondaryLanguageTest || '',
      secondaryTestSpeakingScore: formData.secondaryLanguageTest && formData.secSpeaking ? 
        convertToCLB(formData.secondaryLanguageTest, 'speaking', formData.secSpeaking) : 
        parseInt(formData.secSpeaking, 10) || null,
      secondaryTestListeningScore: formData.secondaryLanguageTest && formData.secListening ? 
        convertToCLB(formData.secondaryLanguageTest, 'listening', formData.secListening) : 
        parseInt(formData.secListening, 10) || null,
      secondaryTestReadingScore: formData.secondaryLanguageTest && formData.secReading ? 
        convertToCLB(formData.secondaryLanguageTest, 'reading', formData.secReading) : 
        parseInt(formData.secReading, 10) || null,
      secondaryTestWritingScore: formData.secondaryLanguageTest && formData.secWriting ? 
        convertToCLB(formData.secondaryLanguageTest, 'writing', formData.secWriting) : 
        parseInt(formData.secWriting, 10) || null,
      
      // Work Experience
      canadianWorkExperienceYears: parseInt(formData.canadianExp, 10) || null,
      nocCodeCanadian: formData.nocCodeCanadian || '',
      foreignWorkExperienceYears: parseInt(formData.foreignExp, 10) || null,
      nocCodeForeign: formData.nocCodeForeign || '',
      
      // Provincial
      provinceOfInterest: formData.provinceInterest || '',
      hasProvincialNomination: formData.provNomination === 'yes',
      hasCanadianRelatives: formData.canadianRelatives === 'yes',
      relationshipWithCanadianRelative: formData.relativeRelationship || null,
      
      // Financial
      settlementFundsCAD: parseInt(formData.settlementFunds, 10) || null,
      preferredCity: formData.preferredCity || '',
      preferredDestinationProvince: formData.preferredDestination || '',
      
      // Job Offer
      hasJobOffer: formData.jobOffer === 'yes',
      isJobOfferLmiaApproved: formData.lmiaStatus === 'yes',
      jobOfferWageCAD: formData.jobWage ? parseInt(formData.jobWage, 10) : null,
      jobOfferNocCode: formData.jobOfferNocCode || '',
      
      // Partner - Convert to CLB if needed
      partnerEducationLevel: formData.partnerEducation || null,
      partnerLanguageTestType: formData.partnerLanguageTest || null,
      partnerTestSpeakingScore: formData.partnerLanguageTest && formData.partnerSpeaking ? 
        convertToCLB(formData.partnerLanguageTest, 'speaking', formData.partnerSpeaking) : 
        parseInt(formData.partnerSpeaking, 10) || null,
      partnerTestListeningScore: formData.partnerLanguageTest && formData.partnerListening ? 
        convertToCLB(formData.partnerLanguageTest, 'listening', formData.partnerListening) : 
        parseInt(formData.partnerListening, 10) || null,
      partnerTestReadingScore: formData.partnerLanguageTest && formData.partnerReading ? 
        convertToCLB(formData.partnerLanguageTest, 'reading', formData.partnerReading) : 
        parseInt(formData.partnerReading, 10) || null,
      partnerTestWritingScore: formData.partnerLanguageTest && formData.partnerWriting ? 
        convertToCLB(formData.partnerLanguageTest, 'writing', formData.partnerWriting) : 
        parseInt(formData.partnerWriting, 10) || null,
      partnerCanadianWorkExperienceYears: formData.partnerCanadianExp ? parseInt(formData.partnerCanadianExp, 10) : null
    };

    // 1. Do your custom validation from `ProfileValidation.js`
    //    (In addition to the react-hook-form checks)
    const customErrors = validateProfileForm(validationData);
    if (Object.keys(customErrors).length > 0) {
      // Flatten the error messages and show them
      const allErrorMessages = Object.values(customErrors).flat();
      setFormErrors(allErrorMessages);
      
      // Generate validation summary
      generateValidationSummary(customErrors);
      
      // Scroll to top to show validation summary
      window.scrollTo({ top: 0, behavior: 'smooth' });
      
      return; // Stop here
    }

    // Instead of showing a confirm dialog, show the modal with form data
    setTempFormData(formData);
    setShowSummaryModal(true);
  };

  // Handle confirmation from the modal
  const handleFormSubmitConfirm = async () => {
    // Close the modal
    setShowSummaryModal(false);
    
    if (!tempFormData) {
      return; // Safety check
    }
    
    const formData = tempFormData;
    setIsSubmitting(true);

    try {
      const token = localStorage.getItem('access_token');
      if (!token) {
        setFormErrors(['Authentication required. Please log in.']);
        setIsSubmitting(false);
        return;
      }

      // Example: convert "yes/no" → boolean
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
        educationCompletedInCanada: yesNoToBool(formData.eduInCanada),
        canadianEducationLevel: formData.canadianEducationLevel || null,
        hasEducationalCredentialAssessment: yesNoToBool(formData.hasECA),
        tradesCertification: yesNoToBool(formData.tradesCertification),

        // Primary Language - Store original scores and CLB scores
        primaryLanguageTestType: formData.primaryLanguageTest || '',
        primaryTestSpeakingScore: parseInt(formData.speaking, 10) || null,
        primaryTestListeningScore: parseInt(formData.listening, 10) || null,
        primaryTestReadingScore: parseInt(formData.reading, 10) || null,
        primaryTestWritingScore: parseInt(formData.writing, 10) || null,
        primaryTestSpeakingOriginal: formData.speaking || null,
        primaryTestListeningOriginal: formData.listening || null,
        primaryTestReadingOriginal: formData.reading || null,
        primaryTestWritingOriginal: formData.writing || null,

        // Secondary Language - Store original scores and CLB scores
        tookSecondaryLanguageTest: yesNoToBool(formData.secondaryLangTest),
        secondaryTestType: formData.secondaryLanguageTest || null,
        secondaryTestSpeakingScore: formData.secondaryLanguageTest && formData.secSpeaking ? 
          convertToCLB(formData.secondaryLanguageTest, 'speaking', formData.secSpeaking) : null,
        secondaryTestListeningScore: formData.secondaryLanguageTest && formData.secListening ? 
          convertToCLB(formData.secondaryLanguageTest, 'listening', formData.secListening) : null,
        secondaryTestReadingScore: formData.secondaryLanguageTest && formData.secReading ? 
          convertToCLB(formData.secondaryLanguageTest, 'reading', formData.secReading) : null,
        secondaryTestWritingScore: formData.secondaryLanguageTest && formData.secWriting ? 
          convertToCLB(formData.secondaryLanguageTest, 'writing', formData.secWriting) : null,
        secondaryTestSpeakingOriginal: formData.secSpeaking || null,
        secondaryTestListeningOriginal: formData.secListening || null,
        secondaryTestReadingOriginal: formData.secReading || null,
        secondaryTestWritingOriginal: formData.secWriting || null,

        // Work Experience
        canadianWorkExperienceYears: parseInt(formData.canadianExp, 10) || 0,
        nocCodeCanadian: formData.nocCodeCanadian || null,
        foreignWorkExperienceYears: parseInt(formData.foreignExp, 10) || 0,
        nocCodeForeign: formData.nocCodeForeign || '',
        workingInCanada: yesNoToBool(formData.workInsideCanada),

        // Spouse - Set to null if marital status is Single
        partnerEducationLevel: formData.maritalStatus === 'Single' ? null : formData.partnerEducation || null,
        partnerLanguageTestType: formData.maritalStatus === 'Single' ? null : formData.partnerLanguageTest || null,
        partnerTestSpeakingScore: formData.maritalStatus === 'Single' ? null : 
          (formData.partnerLanguageTest && formData.partnerSpeaking ? 
            convertToCLB(formData.partnerLanguageTest, 'speaking', formData.partnerSpeaking) : null),
        partnerTestListeningScore: formData.maritalStatus === 'Single' ? null : 
          (formData.partnerLanguageTest && formData.partnerListening ? 
            convertToCLB(formData.partnerLanguageTest, 'listening', formData.partnerListening) : null),
        partnerTestReadingScore: formData.maritalStatus === 'Single' ? null : 
          (formData.partnerLanguageTest && formData.partnerReading ? 
            convertToCLB(formData.partnerLanguageTest, 'reading', formData.partnerReading) : null),
        partnerTestWritingScore: formData.maritalStatus === 'Single' ? null : 
          (formData.partnerLanguageTest && formData.partnerWriting ? 
            convertToCLB(formData.partnerLanguageTest, 'writing', formData.partnerWriting) : null),
        partnerCanadianWorkExperienceYears: formData.maritalStatus === 'Single' ? null : parseInt(formData.partnerCanadianExp, 10) || null,

        // Job Offer
        hasJobOffer: yesNoToBool(formData.jobOffer),
        isJobOfferLmiaApproved: yesNoToBool(formData.lmiaStatus),
        jobOfferWageCAD: parseInt(formData.jobWage, 10) || null,
        jobOfferNocCode: formData.jobOfferNocCode || null,
        weeklyHours: parseInt(formData.weeklyHours, 10) || null,
        jobOfferDescription: formData.jobDetails || null,

        // Provincial Information
        hasProvincialNomination: yesNoToBool(formData.provNomination),
        provinceOfInterest: formData.provinceInterest || '',
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

      // Log the request for debugging
      console.log('Submission payload:', mappedSubmission);
      
      // Debug logging for language scores
      console.log('Primary language scores (CLB):', {
        type: mappedSubmission.primaryLanguageTestType,
        speaking: mappedSubmission.primaryTestSpeakingScore,
        listening: mappedSubmission.primaryTestListeningScore,
        reading: mappedSubmission.primaryTestReadingScore,
        writing: mappedSubmission.primaryTestWritingScore
      });
      
      if (mappedSubmission.tookSecondaryLanguageTest) {
        console.log('Secondary language scores (CLB):', {
          type: mappedSubmission.secondaryTestType,
          speaking: mappedSubmission.secondaryTestSpeakingScore,
          listening: mappedSubmission.secondaryTestListeningScore,
          reading: mappedSubmission.secondaryTestReadingScore,
          writing: mappedSubmission.secondaryTestWritingScore
        });
      }
      
      // Check if jsonPayload is empty
      if (!mappedSubmission.jsonPayload || mappedSubmission.jsonPayload === '{}' || mappedSubmission.jsonPayload === 'null') {
        console.warn('Warning: jsonPayload is empty, setting default value');
        mappedSubmission.jsonPayload = JSON.stringify({
          formData: formData,
          timestamp: new Date().toISOString()
        });
      }

      // Submit via Axios
      const response = await axios.post('http://localhost:8080/api/profiles', mappedSubmission, {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        }
      });

      // Clear localStorage on success
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

      alert('Your profile has been successfully submitted! Redirecting to dashboard...');
      setTimeout(() => navigate('/dashboard'), 1500);
      
    } catch (err) {
      console.error('Error submitting profile:', err);
      
      // Enhanced error handling
      if (err.response) {
        console.error('Response status:', err.response.status);
        console.error('Response data:', err.response.data);
        console.error('Response headers:', err.response.headers);

        // Special handling for validation errors from Spring (usually contains errors array)
        if (err.response.data && err.response.data.errors) {
          console.error('Validation errors:', err.response.data.errors);
          
          // Build a more user-friendly error message
          const validationErrors = err.response.data.errors.map(error => 
            `${error.field || ''}: ${error.defaultMessage || error.message || 'Invalid value'}`
          );
          
          setFormErrors(validationErrors);
        } else if (err.response.data && err.response.data.message) {
          // Handle case where there's a single error message
          setFormErrors([err.response.data.message]);
        } else {
          // Generic error based on status
          setFormErrors([`Server error (${err.response.status}): Please check your submission and try again.`]);
        }
      } else {
        // Network error or other non-response error
        setFormErrors(['Connection error: Could not reach the server. Please check your internet connection and try again.']);
      }
      
      const msg = err.response?.data?.message || err.message || 'Submission failed';
      const detailErrors = err.response?.data?.errors || [];

      setApiError({
        message: msg,
        details: detailErrors
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // --------------------- VALIDATION SUMMARY --------------------- //
  // Map section IDs to readable names (used for displaying section names in the validation summary)
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
  
  // Generate validation summary from errors
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
      tradesCertification: 'education',
      
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
  const renderCountryOptions = () => {
    if (!countries || !Array.isArray(countries)) {
      return <option value="">Loading countries...</option>;
    }
    return (
      <>
        <option value="">Select...</option>
        {countries.map((c, i) => {
          const name = c.Countries || 'UnknownCountry';
          return (
            <option key={`country-${i}-${name}`} value={name}>
              {name}
            </option>
          );
        })}
      </>
    );
  };

  const renderCityOptions = () => {
    if (!cities || !Array.isArray(cities)) {
      return <option value="">Loading cities...</option>;
    }
    return (
      <>
        <option value="">Select...</option>
        {cities.map((city, i) => {
          const cityName = city.City || 'UnknownCity';
          return (
            <option key={`city-${i}-${cityName}`} value={cityName}>
              {cityName}
            </option>
          );
        })}
      </>
    );
  };

  const renderJobOptions = () => {
    if (!jobs || !Array.isArray(jobs)) {
      return <option value="">Loading jobs...</option>;
    }
    return (
      <>
        <option value="">Select occupation...</option>
        {jobs.map((job, i) => {
          const noc = job.NOC || 'None';
          const title = job['Job Title'] || 'Unknown Job';
          return (
            <option key={`job-${i}-${noc}`} value={noc}>
              {title} ({noc})
            </option>
          );
        })}
      </>
    );
  };

  // Render validation summary
  const renderValidationSummary = (isBottom = false) => {
    if (!validationSummary) return null;
    
    // Map field IDs to readable names
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
      canadianRelatives: 'Canadian Relatives',
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
      tradesCertification: 'education',
      
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
        {Object.entries(validationSummary).map(([section, errors]) => (
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
                  <a 
                    href={`#${error.field}`} 
                    className="goto-field"
                    onClick={(e) => {
                      e.preventDefault();
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
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  };

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

      <form onSubmit={handleSubmit(onSubmit)} id="profileForm" className="profile-form" ref={formRef}>
        <div className="form-intro">
          <h2>Express Entry Profile</h2>
          <p>Please fill in your information to determine your eligibility for Canadian immigration programs.</p>
        </div>
        
        {/* Top Validation Summary */}
        {validationSummary && (
          <div className="form-section" id="top-validation-summary">
            {renderValidationSummary(false)}
          </div>
        )}
        
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
            {apiError.details.length > 0 && (
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
              </div>

              {/* Age */}
              <div className="form-group">
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
              </div>

              {/* Citizenship */}
              <div className="form-group">
                <label htmlFor="citizenship" className="required-field">
                  Country of Citizenship
                </label>
                <select
                  id="citizenship"
                  {...register('citizenship', { required: 'Citizenship is required' })}
                >
                  {renderCountryOptions()}
                </select>
                {errors.citizenship && <span className="error">{errors.citizenship.message}</span>}
              </div>

              {/* Residence */}
              <div className="form-group">
                <label htmlFor="residence" className="required-field">
                  Country of Residence
                </label>
                <select
                  id="residence"
                  {...register('residence', { required: 'Residence is required' })}
                >
                  {renderCountryOptions()}
                </select>
                {errors.residence && <span className="error">{errors.residence.message}</span>}
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
              </div>
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
              <div className="form-group">
                <label htmlFor="educationLevel" className="required-field">Highest Level of Education:</label>
                <select
                  id="educationLevel"
                  {...register('educationLevel', { required: 'Education level is required' })}
                >
                  <option value="">Select...</option>
                  <option value="none-or-less-than-secondary">None or less than high school</option>
                  <option value="secondary-diploma">High school diploma</option>
                  <option value="one-year-program">One-year program</option>
                  <option value="two-year-program">Two-year program</option>
                  <option value="bachelors-degree">Bachelor's degree (3+ yrs)</option>
                  <option value="two-or-more-certificates">Two+ credentials</option>
                  <option value="masters">Master's degree or professional degree</option>
                  <option value="doctoral">PhD</option>
                </select>
                {errors.educationLevel && <span className="error">{errors.educationLevel.message}</span>}
              </div>

              {/* Education in Canada? */}
              <div className="form-group">
                <label className="required-field">Was your education completed in Canada?</label>
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
                    <option value="">Select...</option>
                    <option value="secondary-or-less">Secondary or less</option>
                    <option value="one-or-two-year-diploma">1-2 year diploma/certificate</option>
                    <option value="degree-3-plus-years">3+ year degree/diploma or higher</option>
                  </select>
                  {errors.canadianEducationLevel && <span className="error">{errors.canadianEducationLevel.message}</span>}
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
              </div>

              {/* Trades Certification */}
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
                  {getScoreOptions(watch('primaryLanguageTest'), 'speaking').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.speaking && <span className="error">{errors.speaking.message}</span>}
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
                  {getScoreOptions(watch('primaryLanguageTest'), 'listening').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.listening && <span className="error">{errors.listening.message}</span>}
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
                  {getScoreOptions(watch('primaryLanguageTest'), 'reading').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.reading && <span className="error">{errors.reading.message}</span>}
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
                  {getScoreOptions(watch('primaryLanguageTest'), 'writing').map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                    </option>
                  ))}
                </select>
                {errors.writing && <span className="error">{errors.writing.message}</span>}
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
                      <option value="CELPIP">CELPIP-G</option>
                      <option value="IELTS">IELTS</option>
                      <option value="PTE">PTE Core</option>
                      <option value="TEF">TEF Canada</option>
                      <option value="TCF">TCF Canada</option>
                    </select>
                    {errors.secondaryLanguageTest && <span className="error">{errors.secondaryLanguageTest.message}</span>}
                    {watch('secondaryLanguageTest') && (
                      <div className="score-range-info">
                        {getScoreRangeDescription(watch('secondaryLanguageTest'))}
                      </div>
                    )}
                  </div>

                  {/* Scores */}
                  <div className="form-group">
                    <label htmlFor="secSpeaking" className="required-field">
                      Speaking {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secSpeaking" {...register('secSpeaking', { required: true })}>
                      <option value="">Select...</option>
                      {getScoreOptions(watch('secondaryLanguageTest'), 'speaking').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secSpeaking && <span className="error">This field is required</span>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secListening" className="required-field">
                      Listening {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secListening" {...register('secListening', { required: true })}>
                      <option value="">Select...</option>
                      {getScoreOptions(watch('secondaryLanguageTest'), 'listening').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secListening && <span className="error">This field is required</span>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secReading" className="required-field">
                      Reading {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secReading" {...register('secReading', { required: true })}>
                      <option value="">Select...</option>
                      {getScoreOptions(watch('secondaryLanguageTest'), 'reading').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secReading && <span className="error">This field is required</span>}
                  </div>

                  <div className="form-group">
                    <label htmlFor="secWriting" className="required-field">
                      Writing {watch('secondaryLanguageTest') ? '' : '(CLB)'}
                    </label>
                    <select id="secWriting" {...register('secWriting', { required: true })}>
                      <option value="">Select...</option>
                      {getScoreOptions(watch('secondaryLanguageTest'), 'writing').map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                        </option>
                      ))}
                    </select>
                    {errors.secWriting && <span className="error">This field is required</span>}
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
              {/* Canadian Experience */}
              <div className="form-group">
                <label htmlFor="canadianExp" className="required-field">Canadian Work Experience (years)</label>
                <select id="canadianExp" {...register('canadianExp', { required: 'This field is required' })}>
                  <option value="">Select...</option>
                  <option value="0">None or &lt;1 year</option>
                  <option value="1">1 year</option>
                  <option value="2">2 years</option>
                  <option value="3">3 years</option>
                  <option value="4">4 years</option>
                  <option value="5">5+ years</option>
                </select>
                {errors.canadianExp && <span className="error">{errors.canadianExp.message}</span>}
              </div>
              <div className="form-group">
                <label htmlFor="nocCodeCanadian">NOC Code for Canadian Work</label>
                <select id="nocCodeCanadian" {...register('nocCodeCanadian')}>
                  <option value="">Select...</option>
                  {renderJobOptions()}
                </select>
              </div>

              {/* Foreign Experience */}
              <div className="form-group">
                <label htmlFor="foreignExp" className="required-field">Foreign Work Experience (years)</label>
                <select id="foreignExp" {...register('foreignExp', { required: 'This field is required' })}>
                  <option value="">Select...</option>
                  <option value="0">None or &lt;1 year</option>
                  <option value="1">1 year</option>
                  <option value="2">2 years</option>
                  <option value="3">3+ years</option>
                </select>
                {errors.foreignExp && <span className="error">{errors.foreignExp.message}</span>}
              </div>
              <div className="form-group">
                <label htmlFor="nocCodeForeign">NOC Code for Foreign Work</label>
                <select id="nocCodeForeign" {...register('nocCodeForeign')}>
                  {renderJobOptions()}
                </select>
              </div>

              {/* Working in Canada */}
              <div className="form-group">
                <label className="required-field">Are you currently working in Canada?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="insideCanadaYes"
                    value="yes"
                    {...register('workInsideCanada', { required: true })}
                  />
                  <label htmlFor="insideCanadaYes">Yes</label>

                  <input
                    type="radio"
                    id="insideCanadaNo"
                    value="no"
                    {...register('workInsideCanada')}
                  />
                  <label htmlFor="insideCanadaNo">No</label>
                </div>
                {errors.workInsideCanada && <span className="error">This field is required.</span>}
              </div>
            </div>
          )}
        </div>

        {/* ---------------- SPOUSE / COMMON-LAW SECTION ---------------- */}
        {(watchAllFields.maritalStatus === 'married' ||
          watchAllFields.maritalStatus === 'common-law') && (
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
                  </select>
                  {watch('partnerLanguageTest') && (
                    <div className="score-range-info">
                      {getScoreRangeDescription(watch('partnerLanguageTest'))}
                    </div>
                  )}
                </div>

                {/* Spouse language scores */}
                <div className="form-group">
                  <label htmlFor="partnerSpeaking" className="required-field">
                    Speaking {watch('partnerLanguageTest') ? '' : '(CLB)'}
                  </label>
                  <select id="partnerSpeaking" {...register('partnerSpeaking', { required: true })}>
                    <option value="">Select...</option>
                    {getScoreOptions(watch('partnerLanguageTest'), 'speaking').map(option => (
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
                    {getScoreOptions(watch('partnerLanguageTest'), 'listening').map(option => (
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
                    {getScoreOptions(watch('partnerLanguageTest'), 'reading').map(option => (
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
                    {getScoreOptions(watch('partnerLanguageTest'), 'writing').map(option => (
                      <option key={option.value} value={option.value}>
                        {option.label} {option.clb !== undefined ? `(CLB ${option.clb})` : ''}
                      </option>
                    ))}
                  </select>
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
              <div className="form-group">
                <label className="required-field">Do you have a valid Canadian job offer?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="jobOfferYes"
                    value="yes"
                    {...register('jobOffer', { required: 'This field is required' })}
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
                {errors.jobOffer && <span className="error">{errors.jobOffer.message}</span>}
              </div>

              {watchAllFields.jobOffer === 'yes' && (
                <>
                  <div className="form-group">
                    <label htmlFor="lmiaStatus" className="required-field">
                      LMIA-approved?
                      <span
                        className="tooltip"
                        data-tooltip="LMIA: Labour Market Impact Assessment"
                      >
                        ?
                      </span>
                    </label>
                    <select id="lmiaStatus" {...register('lmiaStatus', { required: true })}>
                      <option value="">Select...</option>
                      <option value="yes">Yes</option>
                      <option value="no">No</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="jobWage" className="required-field">
                      Job Wage (CAD per hour)
                      <span
                        className="tooltip"
                        data-tooltip="Hourly wage, e.g. 35. Must meet median wage for NOC."
                      >
                        ?
                      </span>
                    </label>
                    <input
                      type="number"
                      id="jobWage"
                      placeholder="e.g., 35"
                      {...register('jobWage', { required: true })}
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="jobOfferNocCode" className="required-field">Job Offer NOC Code</label>
                    <select id="jobOfferNocCode" {...register('jobOfferNocCode', { required: true })}>
                      {renderJobOptions()}
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="weeklyHours" className="required-field">Weekly Work Hours</label>
                    <select id="weeklyHours" {...register('weeklyHours', { required: true })}>
                      <option value="">Select...</option>
                      <option value="casual">Casual (~under 15 hrs/week)</option>
                      <option value="part-time">Part-time (15-30 hrs/week)</option>
                      <option value="full-time">Full-time (30+ hrs/week)</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label htmlFor="jobDetails" className="required-field">Brief Job Description</label>
                    <textarea
                      id="jobDetails"
                      rows={3}
                      {...register('jobDetails', { required: true })}
                    />
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
              <div className="form-group">
                <label className="required-field">Do you have a Provincial Nomination?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="provYes"
                    value="yes"
                    {...register('provNomination', { required: true })}
                  />
                  <label htmlFor="provYes">Yes</label>

                  <input
                    type="radio"
                    id="provNo"
                    value="no"
                    {...register('provNomination')}
                  />
                  <label htmlFor="provNo">No</label>
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="provinceInterest" className="required-field">Province or Territory of Interest</label>
                <select id="provinceInterest" {...register('provinceInterest', { required: true })}>
                  <option value="">Select...</option>
                  <option value="Ontario">Ontario</option>
                  <option value="British Columbia">British Columbia</option>
                  <option value="Alberta">Alberta</option>
                  <option value="Manitoba">Manitoba</option>
                  <option value="Saskatchewan">Saskatchewan</option>
                  <option value="Nova Scotia">Nova Scotia</option>
                  <option value="New Brunswick">New Brunswick</option>
                  <option value="Prince Edward Island">Prince Edward Island</option>
                  <option value="Yukon">Yukon</option>
                  <option value="Northwest Territories">Northwest Territories</option>
                </select>
              </div>

              <div className="form-group">
                <label className="required-field">Do you have Canadian relatives (citizens or PR)?</label>
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
              </div>
              {watchAllFields.canadianRelatives === 'yes' && (
                <div className="form-group">
                  <label htmlFor="relativeRelationship" className="required-field">
                    Relationship
                  </label>
                  <select
                    id="relativeRelationship"
                    {...register('relativeRelationship', { required: 'Please specify relationship' })}
                  >
                    <option value="">Select...</option>
                    <option value="sibling">Sibling</option>
                    <option value="parent">Parent</option>
                    <option value="grandparent">Grandparent</option>
                    <option value="other">Other</option>
                  </select>
                </div>
              )}

              <div className="form-group">
                <label className="required-field">Have you received an ITA (Invitation to Apply)?</label>
                <div className="pill-radio-group">
                  <input
                    type="radio"
                    id="itaYes"
                    value="yes"
                    {...register('receivedITA', { required: true })}
                  />
                  <label htmlFor="itaYes">Yes</label>

                  <input
                    type="radio"
                    id="itaNo"
                    value="no"
                    {...register('receivedITA')}
                  />
                  <label htmlFor="itaNo">No</label>
                </div>
              </div>
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
              <div className="form-group">
                <label htmlFor="settlementFunds" className="required-field">
                  Settlement Funds (CAD)
                  <span
                    className="tooltip"
                    data-tooltip="Total unencumbered funds available to support yourself/family."
                  >
                    ?
                  </span>
                </label>
                <input
                  type="number"
                  id="settlementFunds"
                  {...register('settlementFunds', { required: true })}
                />
              </div>
              <div className="form-group">
                <label htmlFor="preferredCity">Preferred City</label>
                <select id="preferredCity" {...register('preferredCity')}>
                  {renderCityOptions()}
                </select>
              </div>
              <div className="form-group">
                <label htmlFor="preferredDestination" className="required-field">
                  Preferred Destination (Province)
                </label>
                <select
                  id="preferredDestination"
                  {...register('preferredDestination', { required: true })}
                >
                  <option value="">Select...</option>
                  <option value="Ontario">Ontario</option>
                  <option value="British Columbia">British Columbia</option>
                  <option value="Alberta">Alberta</option>
                  <option value="Manitoba">Manitoba</option>
                  <option value="Saskatchewan">Saskatchewan</option>
                  <option value="Nova Scotia">Nova Scotia</option>
                  <option value="New Brunswick">New Brunswick</option>
                  <option value="Prince Edward Island">Prince Edward Island</option>
                  <option value="Yukon">Yukon</option>
                  <option value="Northwest Territories">Northwest Territories</option>
                </select>
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
    </div>
  );
};

export default ProfileForm;