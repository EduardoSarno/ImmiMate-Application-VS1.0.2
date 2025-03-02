/********************************************
  formJS.js - Enhanced Client-Side Validation
********************************************/

// Global array to hold jobs.json data so we can look up TEER & Job Title
let jobsData = [];

document.addEventListener("DOMContentLoaded", () => {
  // Log the user's email
  const userEmail = localStorage.getItem("user_email");
  console.log("Retrieved userEmail from localStorage in formJS:", userEmail);
  if (userEmail) {
    console.log(`User logged in: ${userEmail}`);
  } else {
    console.log("No user is currently logged in.");
  }

  /********************************************
    Collapsible Sections: Unchanged
  ********************************************/
  document.querySelectorAll(".collapsible-section .section-header").forEach((header) => {
    header.addEventListener("click", function () {
      const content = this.nextElementSibling;
      if (content.style.display === "none" || content.style.display === "") {
        content.style.display = "block";
        this.querySelector(".chevron").innerHTML = "▲"; // Up chevron
      } else {
        content.style.display = "none";
        this.querySelector(".chevron").innerHTML = "▼"; // Down chevron
      }
    });
  });

  // Show/hide spouse section
  const maritalStatusRadios = document.getElementsByName("maritalStatus");
  const spouseSection = document.getElementById("spouseSection");
  maritalStatusRadios.forEach((radio) => {
    radio.addEventListener("change", function () {
      spouseSection.style.display = (this.value === "married" || this.value === "common-law") ? "block" : "none";
    });
  });

  // Show/hide job offer details
  const jobOfferRadios = document.getElementsByName("jobOffer");
  const jobOfferDetails = document.getElementById("jobOfferDetails");
  jobOfferRadios.forEach((radio) => {
    radio.addEventListener("change", function () {
      jobOfferDetails.style.display = this.value === "yes" ? "block" : "none";
    });
  });

  // Show/hide secondary language details
  const secondaryLangYes = document.getElementById("secLangYes");
  const secondaryLangNo = document.getElementById("secLangNo");
  const secondaryLangDetails = document.getElementById("secondaryLangDetails");
  secondaryLangYes.addEventListener("change", () => { secondaryLangDetails.style.display = "block"; });
  secondaryLangNo.addEventListener("change", () => { secondaryLangDetails.style.display = "none"; });

  // Show/hide relative details
  const canadianRelativesRadios = document.getElementsByName("canadianRelatives");
  const relativeDetails = document.getElementById("relativeDetails");
  canadianRelativesRadios.forEach((radio) => {
    radio.addEventListener("change", function () {
      relativeDetails.style.display = this.value === "yes" ? "block" : "none";
    });
  });

  // Show/hide Canadian Education Level
  const eduInCanadaRadios = document.getElementsByName("eduInCanada");
  const canadianEducationLevelGroup = document.getElementById("canadianEducationLevelGroup");
  eduInCanadaRadios.forEach((radio) => {
    radio.addEventListener("change", function () {
      canadianEducationLevelGroup.style.display = this.value === "yes" ? "block" : "none";
    });
  });

  // Add event listeners for trades certification
  const tradesCertificationRadios = document.getElementsByName("tradesCertification");
  tradesCertificationRadios.forEach((radio) => {
    radio.addEventListener("change", function () {
      sessionStorage.setItem("tradesCertification", this.value);
    });
  });

  // Populate dropdown fields
  populatePreferredCityDropdown();
  populateCountryDropdowns();
  populateJobsDropdowns(); // loads jobsData globally

  // Initialize Select2 fields and load saved values
  const select2Fields = ["#countrySelect", "#countryOfResidence", "#nocCodeCanadian", "#nocCodeForeign", "#jobOfferNocCode", "#preferredCity"];
  select2Fields.forEach(selector => {
    const element = $(selector);
    element.select2({
      placeholder: "Search and select",
      allowClear: true
    });

    // Save the element ID and its saved value for later use
    const elementId = element.attr('id');
    const savedValue = sessionStorage.getItem(elementId);
    
    // Attach the change event to save future changes
    element.on('change', function() {
      console.log(`Saving value for ${$(this).attr('id')}: ${$(this).val()}`);
      sessionStorage.setItem($(this).attr('id'), $(this).val());
    });

    // Set the saved value after a short delay to ensure Select2 is fully initialized
    // This helps overcome any potential conflicts with the second initialization in HTML
    if (savedValue) {
      console.log(`Loading saved value for ${elementId}: ${savedValue}`);
      setTimeout(() => {
        element.val(savedValue).trigger('change');
        console.log(`Applied saved value for ${elementId}`);
      }, 500); // 500ms delay
    }
  });

  // Load saved form data from sessionStorage
  const formFields = document.querySelectorAll("#eligibilityForm input, #eligibilityForm select, #eligibilityForm textarea");
  formFields.forEach(field => {
    const savedValue = sessionStorage.getItem(field.id);
    if (savedValue) {
      if (field.type === "checkbox" || field.type === "radio") {
        field.checked = savedValue === "true";
      } else {
        field.value = savedValue;
      }
    }

    // Save form data to sessionStorage on change
    field.addEventListener("change", () => {
      if (field.type === "checkbox" || field.type === "radio") {
        sessionStorage.setItem(field.id, field.checked);
      } else {
        sessionStorage.setItem(field.id, field.value);
      }
    });
  });

  /********************************************
    Form Submission + Client-Side Validation
  ********************************************/
  const form = document.getElementById("eligibilityForm");
  form.addEventListener("submit", async (e) => {
    e.preventDefault(); // Prevent default submission
    console.log("Form submission started"); // Log when the form submission begins

    // Clear any leftover highlights from a previous attempt
    document.querySelectorAll(".collapsible-section").forEach((sec) => {
      sec.classList.remove("error-highlight");
    });
    document.querySelectorAll(".form-group input, .form-group select, .form-group textarea").forEach((field) => {
      field.classList.remove("field-error");
    });

    const validationErrors = {}; // Object to store validation errors

    // Quick references
    const userId = localStorage.getItem("user_id");
    const userMail = localStorage.getItem("user_email");
    
    console.log("Current localStorage:", {
      userId: userId,
      userEmail: userMail,
      token: localStorage.getItem("access_token")
    });

    // If user_id doesn't exist but email does, we'll use email as a fallback
    // This helps with legacy data where only email was stored
    if (!userId && userMail) {
      console.log("No user_id found, but email exists. Using email as identifier.");
      // Don't treat missing user_id as an error if we have email
    } else if (!userId) {
      validationErrors["user_id"] = "User ID is missing. Please log in again.";
      console.log("Error: User ID is missing");
    }
    
    if (!userMail) {
      validationErrors["user_email"] = "User Email is missing. Please log in again.";
      console.log("Error: User Email is missing");
    }

    // 2. Collect form fields
    const applicantName = document.getElementById("fullName").value.trim();
    const applicantAge = document.getElementById("age").value.trim();
    const applicantCitizenship = document.getElementById("countrySelect").value;
    const applicantResidence = document.getElementById("countryOfResidence").value;
    const applicantMaritalStatus = document.querySelector('input[name="maritalStatus"]:checked')?.value;
    const applicantEducationLevel = document.getElementById("educationLevel").value;
    const educationCompletedInCanada = document.querySelector('input[name="eduInCanada"]:checked')?.value;
    const hasEducationalCredentialAssessment = document.querySelector('input[name="hasECA"]:checked')?.value;
    const primaryLanguageTestType = document.getElementById("primaryLanguageTest").value;
    const primaryTestSpeakingScore = document.getElementById("speaking").value.trim();
    const primaryTestListeningScore = document.getElementById("listening").value.trim();
    const primaryTestReadingScore = document.getElementById("reading").value.trim();
    const primaryTestWritingScore = document.getElementById("writing").value.trim();
    const tookSecondaryLanguageTest = document.querySelector('input[name="secondaryLangTest"]:checked')?.value;
    const canadianWorkExperienceYears = document.getElementById("canadianExp").value;
    const nocCodeCanadian = document.getElementById("nocCodeCanadian").value;
    const foreignWorkExperienceYears = document.getElementById("foreignExp").value;
    const nocCodeForeign = document.getElementById("nocCodeForeign").value;
    const workingInCanada = document.querySelector('input[name="workInsideCanada"]:checked')?.value;
    const hasProvincialNomination = document.querySelector('input[name="provNomination"]:checked')?.value;
    const provinceOfInterest = document.getElementById("provinceInterest").value;
    const hasCanadianRelatives = document.querySelector('input[name="canadianRelatives"]:checked')?.value;
    const receivedInvitationToApply = document.querySelector('input[name="receivedITA"]:checked')?.value;
    const settlementFundsCAD = document.getElementById("settlementFunds").value.trim();
    const preferredCity = document.getElementById("preferredCity").value;
    const preferredDestinationProvince = document.getElementById("preferredDestination").value;
    const hasJobOffer = document.querySelector('input[name="jobOffer"]:checked')?.value;
    const tradesCertification = document.querySelector('input[name="tradesCertification"]:checked')?.value;

    // **NOT NULL Validation** (with descriptive messages)
    if (!applicantName) {
      validationErrors["applicant_name"] = "Applicant Name is required.";
      document.getElementById("fullName").classList.add("field-error");
      console.log("Validation error: Applicant Name missing");
    }
    if (!applicantAge) {
      validationErrors["applicant_age"] = "Applicant Age is required.";
      document.getElementById("age").classList.add("field-error");
      console.log("Validation error: Applicant Age missing");
    } else if (Number(applicantAge) > 100) {
      validationErrors["applicant_age"] = "Age cannot exceed 100.";
      document.getElementById("age").classList.add("field-error");
      console.log("Validation error: Age exceeds 100");
    }
    if (!applicantCitizenship) {
      validationErrors["applicant_citizenship"] = "Citizenship is required.";
      document.getElementById("countrySelect").classList.add("field-error");
      console.log("Validation error: Citizenship missing");
    }
    if (!applicantResidence) {
      validationErrors["applicant_residence"] = "Country of Residence is required.";
      document.getElementById("countryOfResidence").classList.add("field-error");
    }
    if (!applicantMaritalStatus) {
      validationErrors["applicant_marital_status"] = "Marital Status is required.";
      document.querySelector('input[name="maritalStatus"]').parentElement.classList.add("field-error");
    }
    if (!applicantEducationLevel) {
      validationErrors["applicant_education_level"] = "Education Level is required.";
      document.getElementById("educationLevel").classList.add("field-error");
    }
    if (educationCompletedInCanada === undefined) {
      validationErrors["education_completed_in_canada"] = "Answer if education was completed in Canada.";
      document.querySelector('input[name="eduInCanada"]').parentElement.classList.add("field-error");
    }
    if (hasEducationalCredentialAssessment === undefined) {
      validationErrors["has_educational_credential_assessment"] = "ECA question is required.";
      document.querySelector('input[name="hasECA"]').parentElement.classList.add("field-error");
    }
    if (!primaryLanguageTestType) {
      validationErrors["primary_language_test_type"] = "Primary Language Test Type is required.";
      document.getElementById("primaryLanguageTest").classList.add("field-error");
    }
    if (!primaryTestSpeakingScore) {
      validationErrors["primary_test_speaking_score"] = "Speaking Score is required.";
      document.getElementById("speaking").classList.add("field-error");
    }
    if (!primaryTestListeningScore) {
      validationErrors["primary_test_listening_score"] = "Listening Score is required.";
      document.getElementById("listening").classList.add("field-error");
    }
    if (!primaryTestReadingScore) {
      validationErrors["primary_test_reading_score"] = "Reading Score is required.";
      document.getElementById("reading").classList.add("field-error");
    }
    if (!primaryTestWritingScore) {
      validationErrors["primary_test_writing_score"] = "Writing Score is required.";
      document.getElementById("writing").classList.add("field-error");
    }
    if (tookSecondaryLanguageTest === undefined) {
      validationErrors["took_secondary_language_test"] = "Secondary Language Test question is required.";
      document.querySelector('input[name="secondaryLangTest"]').parentElement.classList.add("field-error");
    }
    if (!canadianWorkExperienceYears) {
      validationErrors["canadian_work_experience_years"] = "Canadian Work Experience is required.";
      document.getElementById("canadianExp").classList.add("field-error");
    }
    if (!nocCodeCanadian) {
      validationErrors["noc_code_canadian"] = "Canadian NOC Code is required.";
      document.getElementById("nocCodeCanadian").classList.add("field-error");
    }
    if (!foreignWorkExperienceYears) {
      validationErrors["foreign_work_experience_years"] = "Foreign Work Experience is required.";
      document.getElementById("foreignExp").classList.add("field-error");
    }
    if (!nocCodeForeign) {
      validationErrors["noc_code_foreign"] = "Foreign NOC Code is required.";
      document.getElementById("nocCodeForeign").classList.add("field-error");
    }
    if (workingInCanada === undefined) {
      validationErrors["working_in_canada"] = "Working in Canada question is required.";
      document.querySelector('input[name="workInsideCanada"]').parentElement.classList.add("field-error");
    }
    if (hasProvincialNomination === undefined) {
      validationErrors["has_provincial_nomination"] = "Provincial Nomination question is required.";
      document.querySelector('input[name="provNomination"]').parentElement.classList.add("field-error");
    }
    if (!provinceOfInterest) {
      validationErrors["province_of_interest"] = "Province of Interest is required.";
      document.getElementById("provinceInterest").classList.add("field-error");
    }
    if (hasCanadianRelatives === undefined) {
      validationErrors["has_canadian_relatives"] = "Canadian Relatives question is required.";
      document.querySelector('input[name="canadianRelatives"]').parentElement.classList.add("field-error");
    }
    if (receivedInvitationToApply === undefined) {
      validationErrors["received_invitation_to_apply"] = "Invitation to Apply question is required.";
      document.querySelector('input[name="receivedITA"]').parentElement.classList.add("field-error");
    }
    if (!settlementFundsCAD) {
      validationErrors["settlement_funds_cad"] = "Settlement Funds is required.";
      document.getElementById("settlementFunds").classList.add("field-error");
    }
    if (!preferredCity) {
      validationErrors["preferred_city"] = "Preferred City is required.";
      document.getElementById("preferredCity").classList.add("field-error");
    }
    if (!preferredDestinationProvince) {
      validationErrors["preferred_destination_province"] = "Preferred Destination Province is required.";
      document.getElementById("preferredDestination").classList.add("field-error");
    }
    if (hasJobOffer === undefined) {
      validationErrors["has_job_offer"] = "Job Offer question is required.";
      document.querySelector('input[name="jobOffer"]').parentElement.classList.add("field-error");
    }
    if (tradesCertification === undefined) {
      validationErrors["trades_certification"] = "Trades Certification question is required.";
      document.querySelector('input[name="tradesCertification"]').parentElement.classList.add("field-error");
    }

    // Map each validation field to a specific collapsible section ID
    const fieldToSectionMap = {
      user_id: null, // Not in any collapsible
      user_email: null, // Not in any collapsible
      applicant_name: "section-personal-info",
      applicant_age: "section-personal-info",
      applicant_citizenship: "section-personal-info",
      applicant_residence: "section-personal-info",
      applicant_marital_status: "section-personal-info",
      applicant_education_level: "section-education",
      education_completed_in_canada: "section-education",
      has_educational_credential_assessment: "section-education",
      primary_language_test_type: "section-language",
      primary_test_speaking_score: "section-language",
      primary_test_listening_score: "section-language",
      primary_test_reading_score: "section-language",
      primary_test_writing_score: "section-language",
      took_secondary_language_test: "section-secondary-language",
      canadian_work_experience_years: "section-work-experience",
      noc_code_canadian: "section-work-experience",
      foreign_work_experience_years: "section-work-experience",
      noc_code_foreign: "section-work-experience",
      working_in_canada: "section-work-experience",
      has_provincial_nomination: "section-provincial",
      province_of_interest: "section-provincial",
      has_canadian_relatives: "section-provincial",
      received_invitation_to_apply: "section-provincial",
      settlement_funds_cad: "section-additional",
      preferred_city: "section-additional",
      preferred_destination_province: "section-additional",
      has_job_offer: "section-job-offer",
      trades_certification: "section-education",
    };

    // If any errors, show them + highlight appropriate sections
    if (Object.keys(validationErrors).length > 0) {
      console.log("Validation failed with errors:", validationErrors);
      const errorDisplayDiv = document.getElementById("form-error-display");
      errorDisplayDiv.innerHTML = ""; // Clear any previous errors
      errorDisplayDiv.style.display = "block"; // Make the error div visible

      // Create error header
      const errorHeader = document.createElement("h3");
      errorHeader.textContent = "Please correct the following issues:";
      errorHeader.style.color = "#1ac0ff"; // Match theme color
      errorHeader.style.marginBottom = "10px";
      errorDisplayDiv.appendChild(errorHeader);

      // Create an unordered list to display errors
      const errorList = document.createElement("ul");
      let firstErrorSection = null; // To track the first section with an error
      let sectionErrors = {}; // Group errors by section for better organization

      // First pass - group errors by section
      for (const field in validationErrors) {
        const sectionId = fieldToSectionMap[field];
        if (sectionId) {
          const sectionDiv = document.getElementById(sectionId);
          if (sectionDiv) {
            const sectionName = sectionDiv.dataset.sectionName || "Unknown Section";
            if (!sectionErrors[sectionName]) {
              sectionErrors[sectionName] = [];
            }
            sectionErrors[sectionName].push(validationErrors[field]);
          }
        } else {
          // Non-section errors (like user_id)
          if (!sectionErrors["General"]) {
            sectionErrors["General"] = [];
          }
          sectionErrors["General"].push(validationErrors[field]);
        }
      }

      // Second pass - create error messages grouped by section
      for (const sectionName in sectionErrors) {
        const sectionLi = document.createElement("li");
        sectionLi.innerHTML = `<strong>${sectionName}:</strong>`;
        
        const innerUl = document.createElement("ul");
        innerUl.style.marginLeft = "20px";
        
        sectionErrors[sectionName].forEach(errorMsg => {
          const innerLi = document.createElement("li");
          innerLi.textContent = errorMsg;
          innerUl.appendChild(innerLi);
        });
        
        sectionLi.appendChild(innerUl);
        errorList.appendChild(sectionLi);
      }
      
      errorDisplayDiv.appendChild(errorList);

      // Third pass - highlight sections with errors and open them
      for (const field in validationErrors) {
        const sectionId = fieldToSectionMap[field];
        if (sectionId) {
          const sectionDiv = document.getElementById(sectionId);
          if (sectionDiv) {
            // Apply highlighting class to the section
            sectionDiv.classList.add("error-highlight");
            
            // Track first error section to scroll to
            if (!firstErrorSection) {
              firstErrorSection = sectionDiv;
            }

            // Force it open for improved UX
            const contentDiv = sectionDiv.querySelector(".section-content");
            if (contentDiv) {
              contentDiv.style.display = "block";
            }
            
            // Update chevron to show section is open
            const chevronSpan = sectionDiv.querySelector(".chevron");
            if (chevronSpan) chevronSpan.innerHTML = "▲";
          }
        }
      }

      // Scroll to the first section with an error
      if (firstErrorSection) {
        // Smooth scroll with slight delay to ensure UI is updated
        setTimeout(() => {
          firstErrorSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
      }

      console.log("Form submission stopped due to validation errors");
      return; // Stop form submission
    } else {
      // Hide error display div if no errors
      const errorDisplayDiv = document.getElementById("form-error-display");
      errorDisplayDiv.style.display = "none";
      console.log("Validation successful, proceeding with submission");
    }

    // Check JWT token
    const token = localStorage.getItem("access_token");
    if (!token) {
      console.log("No access token found, redirecting to sign-in");
      alert("Please sign in to submit your application.");
      window.location.href = "sign-in.html";
      return;
    }

    // Build the payload
    const payload = {
      userEmail: userMail,
      userId: userId || userMail, // Use email as fallback if no userId
      applicantName,
      applicantAge: Number(applicantAge),
      applicantCitizenship,
      applicantResidence,
      applicantMaritalStatus,
      applicantEducationLevel,
      educationCompletedInCanada: educationCompletedInCanada === "yes",
      canadianEducationLevel: document.getElementById("canadianEducationLevel")?.value || "",
      hasEducationalCredentialAssessment: hasEducationalCredentialAssessment === "yes",
      primaryLanguageTestType,
      primaryTestSpeakingScore,
      primaryTestListeningScore,
      primaryTestReadingScore,
      primaryTestWritingScore,
      tookSecondaryLanguageTest: tookSecondaryLanguageTest === "yes",
      secondaryTestType: document.getElementById("secondaryLanguageTest").value,
      secondaryTestSpeakingScore: document.getElementById("secSpeaking").value,
      secondaryTestListeningScore: document.getElementById("secListening").value,
      secondaryTestReadingScore: document.getElementById("secReading").value,
      secondaryTestWritingScore: document.getElementById("secWriting").value,
      canadianWorkExperienceYears,
      nocCodeCanadian,
      foreignWorkExperienceYears,
      nocCodeForeign,
      workingInCanada: workingInCanada === "yes",
      hasProvincialNomination: hasProvincialNomination === "yes",
      provinceOfInterest,
      hasCanadianRelatives: hasCanadianRelatives === "yes",
      relationshipWithCanadianRelative: document.getElementById("relativeRelationship")?.value || "",
      receivedInvitationToApply: receivedInvitationToApply === "yes",
      settlementFundsCAD: Number(settlementFundsCAD),
      preferredCity,
      preferredDestinationProvince,
      partnerEducationLevel: document.getElementById("partnerEducation").value,
      partnerLanguageTestType: document.getElementById("partnerLanguageTest").value,
      partnerTestSpeakingScore: document.getElementById("partnerSpeaking").value,
      partnerTestListeningScore: document.getElementById("partnerListening").value,
      partnerTestReadingScore: document.getElementById("partnerReading").value,
      partnerTestWritingScore: document.getElementById("partnerWriting").value,
      partnerCanadianWorkExperienceYears: document.getElementById("partnerCanadianExp").value,
      hasJobOffer: hasJobOffer === "yes",
      isJobOfferLmiaApproved: document.getElementById("lmiaStatus").value === "yes",
      jobOfferWageCAD: Number(document.getElementById("jobWage").value),
      jobOfferNocCode: document.getElementById("jobOfferNocCode").value,
      jobOfferWeeklyHours: document.getElementById("weeklyHours").value,
      tradesCertification: tradesCertification === "yes",
    };

    // Convert string values to integers for numeric fields
    const numericFields = [
      'foreign_work_experience_years',
      'canadian_work_experience_years',
      'partner_canadian_work_experience_years',
      'job_offer_wage_cad',
      'settlement_funds_cad',
      'secondary_test_writing_score',
      'noc_code_canadian'
    ];
    
    numericFields.forEach(field => {
      if (payload[field] && payload[field] !== '') {
        payload[field] = Number(payload[field]);
      }
    });

    // Convert string values to booleans for boolean fields
    const booleanFields = [
      'has_job_offer',
      'working_in_canada',
      'has_provincial_nomination',
      'received_invitation_to_apply'
    ];
    
    booleanFields.forEach(field => {
      if (payload[field]) {
        payload[field] = payload[field] === 'yes' || payload[field] === 'true';
      }
    });

    // First complete the payload object construction
    // Then add the full JSON payload field 
    payload.jsonPayload = JSON.stringify(payload);

    // Only look up NOC details after payload is fully defined
    // Optional: Lookup additional NOC details
    if (payload.nocCodeCanadian) {
      const selectedNocCan = parseInt(payload.nocCodeCanadian, 10);
      const jobEntryCan = jobsData.find((job) => parseInt(job.NOC, 10) === selectedNocCan);
      if (jobEntryCan) {
        payload.canadianNocTeer = jobEntryCan.TEER;
        payload.canadianNocTitle = jobEntryCan["Job Title"];
      }
    }
    if (payload.nocCodeForeign) {
      const selectedNocFor = parseInt(payload.nocCodeForeign, 10);
      const jobEntryFor = jobsData.find((job) => parseInt(job.NOC, 10) === selectedNocFor);
      if (jobEntryFor) {
        payload.foreignNocTeer = jobEntryFor.TEER;
        payload.foreignNocTitle = jobEntryFor["Job Title"];
      }
    }
    if (payload.hasJobOffer === true && payload.jobOfferNocCode) {
      const selectedNocOffer = parseInt(payload.jobOfferNocCode, 10);
      const jobEntryOffer = jobsData.find((job) => parseInt(job.NOC, 10) === selectedNocOffer);
      if (jobEntryOffer) {
        payload.jobOfferTeer = jobEntryOffer.TEER;
        payload.jobOfferTitle = jobEntryOffer["Job Title"];
      }
    }

    console.log("About to send fetch request with payload", payload);

    // Submit to the new backend route
    try {
      // Check if userId exists and is valid, otherwise send only email
      // And let the backend handle the lookup
      const submissionPayload = {...payload};
      
      if (!userId) {
        console.log("No userId in localStorage, removing from payload and letting backend handle lookup");
        // Remove the userId from the payload so backend will use email lookup
        delete submissionPayload.userId;
      } else {
        console.log("Using userId from localStorage:", userId);
      }
      
      console.log("Sending fetch request to /profile/submit-profile");
      const response = await fetch("http://localhost:3000/profile/submit-profile", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(submissionPayload)
      });

      console.log("Fetch response received:", response.status);
      const result = await response.json();
      console.log("Response data:", result);
      
      if (response.ok && result.success) {
        alert("Your application has been submitted successfully!");
        form.reset();
        window.location.href = "dashboard.html"; // Redirect to the dashboard
      } else {
        console.error("Submission error:", result.error || "Unknown error");
        alert("Submission error: " + (result.error || "Unknown error"));
      }
    } catch (error) {
      console.error("Error during fetch:", error);
      alert("There was an error submitting your application. Please try again.");
    }
  });
});

/********************************************
  POPULATE FUNCTIONS
********************************************/
function populatePreferredCityDropdown() {
  const selectElement = document.getElementById("preferredCity");
  fetch("../Data/option_sets/canadacities.json")
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok for canadacities.json");
      }
      return response.json();
    })
    .then((data) => {
      data.forEach((item) => {
        const option = document.createElement("option");
        option.value = item.City || "";
        option.textContent = item.City || "Unnamed City";
        selectElement.appendChild(option);
      });
    })
    .catch((error) => console.error("Error loading preferred cities:", error));
}

function populateCountryDropdowns() {
  const citizenshipSelect = document.getElementById("countrySelect");
  const residenceSelect = document.getElementById("countryOfResidence");

  fetch("../Data/option_sets/countries.json")
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok for countries.json");
      }
      return response.json();
    })
    .then((data) => {
      data.forEach((item) => {
        const option1 = document.createElement("option");
        option1.value = item.Countries || "";
        option1.textContent = item.Countries || "Unnamed Country";
        citizenshipSelect.appendChild(option1);

        const option2 = document.createElement("option");
        option2.value = item.Countries || "";
        option2.textContent = item.Countries || "Unnamed Country";
        residenceSelect.appendChild(option2);
      });
    })
    .catch((error) => console.error("Error loading countries:", error));
}

function populateJobsDropdowns() {
  const canNocSelect = document.getElementById("nocCodeCanadian");
  const foreignNocSelect = document.getElementById("nocCodeForeign");
  const jobOfferNocSelect = document.getElementById("jobOfferNocCode");

  fetch("../Data/option_sets/jobs.json")
    .then((response) => {
      if (!response.ok) {
        throw new Error("Network response was not ok for jobs.json");
      }
      return response.json();
    })
    .then((data) => {
      jobsData = data;
      data.forEach((item) => {
        const option1 = document.createElement("option");
        option1.value = item.NOC || "";
        option1.textContent = item["Job Title"] || "Unnamed Job";
        canNocSelect.appendChild(option1);

        const option2 = document.createElement("option");
        option2.value = item.NOC || "";
        option2.textContent = item["Job Title"] || "Unnamed Job";
        foreignNocSelect.appendChild(option2);

        const option3 = document.createElement("option");
        option3.value = item.NOC || "";
        option3.textContent = item["Job Title"] || "Unnamed Job";
        jobOfferNocSelect.appendChild(option3);
      });
    })
    .catch((error) => console.error("Error loading jobs:", error));
}