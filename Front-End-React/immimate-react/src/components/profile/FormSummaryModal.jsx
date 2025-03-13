import React from 'react';
import '../../styles/FormSummaryModal.css';

/**
 * Modal component that displays a summary of the user's form data before submission
 * @param {Object} props - Component props
 * @param {Object} props.formData - The form data to display
 * @param {boolean} props.isOpen - Whether the modal is open
 * @param {Function} props.onClose - Function to call when the modal is closed
 * @param {Function} props.onConfirm - Function to call when the user confirms submission
 * @param {Function} props.onEdit - Function to call when the user wants to edit their application
 */
const FormSummaryModal = ({ formData, isOpen, onClose, onConfirm, onEdit }) => {
  if (!isOpen) return null;

  // Section names for display
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

  // Field mappings to readable names
  const fieldToReadableName = {
    // Personal Info
    fullName: 'Full Name',
    age: 'Age',
    citizenship: 'Country of Citizenship',
    residence: 'Country of Residence',
    maritalStatus: 'Marital Status',
    
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

  // Group fields by section
  const fieldsBySection = {};
  Object.keys(formData).forEach(field => {
    const section = fieldToSection[field];
    if (section) {
      if (!fieldsBySection[section]) {
        fieldsBySection[section] = [];
      }
      fieldsBySection[section].push(field);
    }
  });

  // Format value for display
  const formatValue = (field, value) => {
    if (value === undefined || value === null || value === '') {
      return <span className="empty-value">EMPTY</span>;
    }
    
    // Handle yes/no values
    if (value === 'yes' || value === 'no') {
      return value.charAt(0).toUpperCase() + value.slice(1);
    }
    
    // Handle arrays
    if (Array.isArray(value)) {
      return value.join(', ');
    }
    
    return value.toString();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h2>Review Your Application</h2>
          <button type="button" className="close-button" onClick={onClose}>×</button>
        </div>
        
        <div className="modal-body">
          <p className="modal-instructions">
            Please review your information below before submitting. Empty fields are marked as <span className="empty-value">EMPTY</span>.
          </p>
          
          <div className="summary-container">
            {Object.entries(fieldsBySection).map(([section, fields]) => (
              <div key={section} className="summary-section">
                <h3>{sectionNames[section] || section}</h3>
                <table className="summary-table">
                  <tbody>
                    {fields.map(field => (
                      <tr key={field}>
                        <td className="field-name">{fieldToReadableName[field] || field}</td>
                        <td className="field-value">{formatValue(field, formData[field])}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
          </div>
        </div>
        
        <div className="modal-footer">
          <button type="button" className="btn-secondary" onClick={onEdit}>
            Fix My Application
          </button>
          <button type="button" className="btn-primary" onClick={onConfirm}>
            Confirm and Submit
          </button>
        </div>
      </div>
    </div>
  );
};

export default FormSummaryModal; 