const express = require('express');
const router = express.Router();
const { Pool } = require('pg');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

// Helper: parse to integer or null (also handles strings like "10-12" by forcing them to 10)
function parseIntOrNull(value) {
  if (!value || value === "") {
    return null; // empty or missing => store null
  }
  if (typeof value === "string" && value.includes('-')) {
    // e.g. "10-12"
    return 10; 
  }
  const parsed = parseInt(value, 10);
  return isNaN(parsed) ? null : parsed;
}

router.post('/submit-profile', async (req, res) => {
  console.log("Received request body:", req.body);

  let {
    userEmail: user_email,
    userId: user_id,
    applicantName: applicant_name,
    applicantAge: applicant_age,
    applicantCitizenship: applicant_citizenship,
    applicantResidence: applicant_residence,
    applicantMaritalStatus: applicant_marital_status,
    applicantEducationLevel: applicant_education_level,
    educationCompletedInCanada: education_completed_in_canada,
    canadianEducationLevel: canadian_education_level,
    hasEducationalCredentialAssessment: has_educational_credential_assessment,
    primaryLanguageTestType: primary_language_test_type,
    primaryTestSpeakingScore: primary_test_speaking_score,
    primaryTestListeningScore: primary_test_listening_score,
    primaryTestReadingScore: primary_test_reading_score,
    primaryTestWritingScore: primary_test_writing_score,
    tookSecondaryLanguageTest: took_secondary_language_test,
    secondaryTestType: secondary_test_type,
    secondaryTestSpeakingScore: secondary_test_speaking_score,
    secondaryTestListeningScore: secondary_test_listening_score,
    secondaryTestReadingScore: secondary_test_reading_score,
    secondaryTestWritingScore: secondary_test_writing_score,
    canadianWorkExperienceYears: canadian_work_experience_years,
    nocCodeCanadian: noc_code_canadian,
    foreignWorkExperienceYears: foreign_work_experience_years,
    nocCodeForeign: noc_code_foreign,
    workingInCanada: working_in_canada,
    hasProvincialNomination: has_provincial_nomination,
    provinceOfInterest: province_of_interest,
    hasCanadianRelatives: has_canadian_relatives,
    relationshipWithCanadianRelative: relationship_with_canadian_relative,
    receivedInvitationToApply: received_invitation_to_apply,
    settlementFundsCAD: settlement_funds_cad,
    preferredCity: preferred_city,
    preferredDestinationProvince: preferred_destination_province,
    partnerEducationLevel: partner_education_level,
    partnerLanguageTestType: partner_language_test_type,
    partnerTestSpeakingScore: partner_test_speaking_score,
    partnerTestListeningScore: partner_test_listening_score,
    partnerTestReadingScore: partner_test_reading_score,
    partnerTestWritingScore: partner_test_writing_score,
    partnerCanadianWorkExperienceYears: partner_canadian_work_experience_years,
    hasJobOffer: has_job_offer,
    isJobOfferLmiaApproved: is_job_offer_lmia_approved,
    jobOfferWageCAD: job_offer_wage_cad,
    jobOfferNocCode: job_offer_noc_code,
    jobOfferWeeklyHours: job_offer_weekly_hours,
    jsonPayload: json_payload,
    tradesCertification: trades_certification,
  } = req.body;

  // Convert booleans
  has_job_offer = (has_job_offer === true || has_job_offer === 'true' || has_job_offer === 'yes');
  working_in_canada = (working_in_canada === true || working_in_canada === 'true' || working_in_canada === 'yes');
  has_provincial_nomination = (has_provincial_nomination === true || has_provincial_nomination === 'true' || has_provincial_nomination === 'yes');
  received_invitation_to_apply = (received_invitation_to_apply === true || received_invitation_to_apply === 'true' || received_invitation_to_apply === 'yes');
  took_secondary_language_test = (took_secondary_language_test === true || took_secondary_language_test === 'true' || took_secondary_language_test === 'yes');
  education_completed_in_canada = (education_completed_in_canada === true || education_completed_in_canada === 'true' || education_completed_in_canada === 'yes');
  trades_certification = (trades_certification === true || trades_certification === 'true' || trades_certification === 'yes');

  // Convert numeric fields with parseIntOrNull
  applicant_age = parseIntOrNull(applicant_age);
  primary_test_speaking_score = parseIntOrNull(primary_test_speaking_score);
  primary_test_listening_score = parseIntOrNull(primary_test_listening_score);
  primary_test_reading_score = parseIntOrNull(primary_test_reading_score);
  primary_test_writing_score = parseIntOrNull(primary_test_writing_score);
  secondary_test_speaking_score = parseIntOrNull(secondary_test_speaking_score);
  secondary_test_listening_score = parseIntOrNull(secondary_test_listening_score);
  secondary_test_reading_score = parseIntOrNull(secondary_test_reading_score);
  secondary_test_writing_score = parseIntOrNull(secondary_test_writing_score);
  canadian_work_experience_years = parseIntOrNull(canadian_work_experience_years);
  noc_code_canadian = parseIntOrNull(noc_code_canadian);
  foreign_work_experience_years = parseIntOrNull(foreign_work_experience_years);
  noc_code_foreign = parseIntOrNull(noc_code_foreign);
  settlement_funds_cad = parseIntOrNull(settlement_funds_cad);
  partner_canadian_work_experience_years = parseIntOrNull(partner_canadian_work_experience_years);
  job_offer_wage_cad = parseIntOrNull(job_offer_wage_cad);
  job_offer_weekly_hours = parseIntOrNull(job_offer_weekly_hours);
  partner_test_speaking_score = parseIntOrNull(partner_test_speaking_score);    // $38
  partner_test_listening_score = parseIntOrNull(partner_test_listening_score);  // $39
  partner_test_reading_score = parseIntOrNull(partner_test_reading_score);      // $40
  partner_test_writing_score = parseIntOrNull(partner_test_writing_score);      // $41

  try {
    let actualUserId = user_id;

    // Fetch user by email if ID not provided
    if (!actualUserId && user_email) {
      const userResult = await pool.query(
        'SELECT id FROM "Users" WHERE email = $1',
        [user_email]
      );
      if (userResult.rows.length > 0) {
        actualUserId = userResult.rows[0].id;
      } else {
        return res.status(400).json({ success: false, error: 'User not found with provided email' });
      }
    }

    if (!actualUserId) {
      return res.status(400).json({ success: false, error: 'User ID is required to submit a profile' });
    }

    console.log(`Submitting profile for user ID: ${actualUserId}`);

    const result = await pool.query(
      `INSERT INTO user_immigration_profile_applications (
        user_email, user_id, applicant_name, applicant_age, applicant_citizenship, applicant_residence,
        applicant_marital_status, applicant_education_level, education_completed_in_canada, canadian_education_level,
        has_educational_credential_assessment, primary_language_test_type, primary_test_speaking_score,
        primary_test_listening_score, primary_test_reading_score, primary_test_writing_score, took_secondary_language_test,
        secondary_test_type, secondary_test_speaking_score, secondary_test_listening_score, secondary_test_reading_score,
        secondary_test_writing_score, canadian_work_experience_years, noc_code_canadian, foreign_work_experience_years,
        noc_code_foreign, working_in_canada, has_provincial_nomination, province_of_interest, has_canadian_relatives,
        relationship_with_canadian_relative, received_invitation_to_apply, settlement_funds_cad, preferred_city,
        preferred_destination_province, partner_education_level, partner_language_test_type, partner_test_speaking_score,
        partner_test_listening_score, partner_test_reading_score, partner_test_writing_score, partner_canadian_work_experience_years,
        has_job_offer, is_job_offer_lmia_approved, job_offer_wage_cad, job_offer_noc_code, job_offer_weekly_hours,
        json_payload,
        trades_certification
      ) VALUES (
        $1, $2, $3, $4, $5, $6,
        $7, $8, $9, $10,
        $11, $12, $13, $14, $15, $16,
        $17, $18, $19, $20,
        $21, $22, $23, $24, $25, $26,
        $27, $28, $29, $30,
        $31, $32, $33, $34, $35, $36,
        $37, $38, $39, $40,
        $41, $42, $43, $44, $45, $46,
        $47, $48, $49
      )
      RETURNING application_id`,
      [
        user_email,                    // $1
        actualUserId,                  // $2
        applicant_name,                // $3
        applicant_age,                 // $4
        applicant_citizenship,         // $5
        applicant_residence,           // $6

        applicant_marital_status,      // $7
        applicant_education_level,     // $8
        education_completed_in_canada, // $9
        canadian_education_level,      // $10

        has_educational_credential_assessment, // $11
        primary_language_test_type,    // $12
        primary_test_speaking_score,   // $13
        primary_test_listening_score,  // $14
        primary_test_reading_score,    // $15
        primary_test_writing_score,    // $16

        took_secondary_language_test,  // $17
        secondary_test_type,           // $18
        secondary_test_speaking_score, // $19
        secondary_test_listening_score,// $20

        secondary_test_reading_score,  // $21
        secondary_test_writing_score,  // $22
        canadian_work_experience_years,// $23
        noc_code_canadian,             // $24
        foreign_work_experience_years, // $25
        noc_code_foreign,              // $26

        working_in_canada,             // $27
        has_provincial_nomination,     // $28
        province_of_interest,          // $29
        has_canadian_relatives,        // $30

        relationship_with_canadian_relative, // $31
        received_invitation_to_apply,  // $32
        settlement_funds_cad,          // $33
        preferred_city,                // $34
        preferred_destination_province,// $35
        partner_education_level,       // $36

        partner_language_test_type,    // $37
        partner_test_speaking_score,   // $38
        partner_test_listening_score,  // $39
        partner_test_reading_score,    // $40

        partner_test_writing_score,    // $41
        partner_canadian_work_experience_years, // $42
        has_job_offer,                 // $43
        is_job_offer_lmia_approved,    // $44
        job_offer_wage_cad,            // $45
        job_offer_noc_code,            // $46

        job_offer_weekly_hours,        // $47
        json_payload,                  // $48
        trades_certification,          // $49
      ]
    );

    res.status(201).json({ success: true, application_id: result.rows[0].application_id });
  } catch (error) {
    console.error('Error inserting profile:', error);
    res.status(500).json({ success: false, error: 'Database error' });
  }
});

module.exports = router;