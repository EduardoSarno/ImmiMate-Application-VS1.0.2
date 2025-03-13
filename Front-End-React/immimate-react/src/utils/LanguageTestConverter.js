/**
 * Utility functions for converting language test scores to Canadian Language Benchmark (CLB) levels
 * Based on the official CLB conversion table
 */

// Conversion ranges for CELPIP-G
const CELPIP_TO_CLB = {
  speaking: {
    1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10, 11: 11, 12: 12
  },
  listening: {
    1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10, 11: 11, 12: 12
  },
  reading: {
    1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10, 11: 11, 12: 12
  },
  writing: {
    1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9, 10: 10, 11: 11, 12: 12
  }
};

// Conversion ranges for IELTS
const IELTS_TO_CLB = {
  speaking: {
    '1.0': 1, '2.0': 2, '3.0': 3, '4.0': 4, '5.0': 5, '5.5': 6, '6.0': 7, '6.5': 8, '7.0': 9, '8.0': 10, '8.5': 11, '9.0': 12
  },
  listening: {
    '1.0': 1, '2.0': 2, '3.0': 3, '4.0': 4, '5.0': 5, '5.5': 6, '6.0': 7, '6.5': 8, '7.0': 9, '8.0': 10, '8.5': 11, '9.0': 12
  },
  reading: {
    '1.0': 1, '2.0': 2, '3.0': 3, '4.0': 4, '5.0': 5, '5.5': 6, '6.0': 7, '6.5': 8, '7.0': 9, '8.0': 10, '8.5': 11, '9.0': 12
  },
  writing: {
    '1.0': 1, '2.0': 2, '3.0': 3, '4.0': 4, '5.0': 5, '5.5': 6, '6.0': 7, '6.5': 8, '7.0': 9, '8.0': 10, '8.5': 11, '9.0': 12
  }
};

// Conversion ranges for PTE Core
const PTE_TO_CLB = {
  speaking: {
    '10-19': 1, '20-29': 2, '30-39': 3, '40-49': 4, '50-57': 5, '58-66': 6, '67-75': 7, '76-84': 8, '85-92': 9, '93-100': 10, '101-108': 11, '109-115': 12
  },
  listening: {
    '10-19': 1, '20-29': 2, '30-39': 3, '40-49': 4, '50-57': 5, '58-66': 6, '67-75': 7, '76-84': 8, '85-92': 9, '93-100': 10, '101-108': 11, '109-115': 12
  },
  reading: {
    '10-19': 1, '20-29': 2, '30-39': 3, '40-49': 4, '50-57': 5, '58-66': 6, '67-75': 7, '76-84': 8, '85-92': 9, '93-100': 10, '101-108': 11, '109-115': 12
  },
  writing: {
    '10-19': 1, '20-29': 2, '30-39': 3, '40-49': 4, '50-57': 5, '58-66': 6, '67-75': 7, '76-84': 8, '85-92': 9, '93-100': 10, '101-108': 11, '109-115': 12
  }
};

// Conversion ranges for TEF Canada
const TEF_TO_CLB = {
  speaking: {
    '121-140': 1, '141-150': 2, '151-180': 3, '181-216': 4, '217-248': 5, '249-279': 6, '280-297': 7, '298-315': 8, '316-333': 9, '334-360': 10, '361-388': 11, '389-450': 12
  },
  listening: {
    '121-140': 1, '141-150': 2, '151-180': 3, '181-216': 4, '217-248': 5, '249-279': 6, '280-297': 7, '298-315': 8, '316-333': 9, '334-360': 10, '361-388': 11, '389-450': 12
  },
  reading: {
    '121-140': 1, '141-150': 2, '151-180': 3, '181-216': 4, '217-248': 5, '249-279': 6, '280-297': 7, '298-315': 8, '316-333': 9, '334-360': 10, '361-388': 11, '389-450': 12
  },
  writing: {
    '121-140': 1, '141-150': 2, '151-180': 3, '181-216': 4, '217-248': 5, '249-279': 6, '280-297': 7, '298-315': 8, '316-333': 9, '334-360': 10, '361-388': 11, '389-450': 12
  }
};

// Conversion ranges for TCF Canada
const TCF_TO_CLB = {
  speaking: {
    '101-199': 1, '200-299': 2, '300-399': 3, '400-499': 4, '500-599': 5, '600-699': 6, '700-799': 7, '800-899': 8, '900-999': 9, '1000-1099': 10, '1100-1199': 11, '1200': 12
  },
  listening: {
    '101-199': 1, '200-299': 2, '300-399': 3, '400-499': 4, '500-599': 5, '600-699': 6, '700-799': 7, '800-899': 8, '900-999': 9, '1000-1099': 10, '1100-1199': 11, '1200': 12
  },
  reading: {
    '101-199': 1, '200-299': 2, '300-399': 3, '400-499': 4, '500-599': 5, '600-699': 6, '700-799': 7, '800-899': 8, '900-999': 9, '1000-1099': 10, '1100-1199': 11, '1200': 12
  },
  writing: {
    '101-199': 1, '200-299': 2, '300-399': 3, '400-499': 4, '500-599': 5, '600-699': 6, '700-799': 7, '800-899': 8, '900-999': 9, '1000-1099': 10, '1100-1199': 11, '1200': 12
  }
};

/**
 * Get the score options for a specific test type and skill
 * @param {string} testType - The language test type (CELPIP, IELTS, PTE, TEF, TCF)
 * @param {string} skill - The language skill (speaking, listening, reading, writing)
 * @returns {Array} - Array of objects with value and label for the select options
 */
export const getScoreOptions = (testType, skill) => {
  let options = [];
  
  switch (testType) {
    case 'CELPIP':
      options = Object.keys(CELPIP_TO_CLB[skill]).map(score => ({
        value: score,
        label: `${score}`,
        clb: CELPIP_TO_CLB[skill][score]
      }));
      break;
    case 'IELTS':
      options = Object.keys(IELTS_TO_CLB[skill]).map(score => ({
        value: score,
        label: `${score}`,
        clb: IELTS_TO_CLB[skill][score]
      }));
      break;
    case 'PTE':
      options = Object.keys(PTE_TO_CLB[skill]).map(score => ({
        value: score,
        label: score,
        clb: PTE_TO_CLB[skill][score]
      }));
      break;
    case 'TEF':
      options = Object.keys(TEF_TO_CLB[skill]).map(score => ({
        value: score,
        label: score,
        clb: TEF_TO_CLB[skill][score]
      }));
      break;
    case 'TCF':
      options = Object.keys(TCF_TO_CLB[skill]).map(score => ({
        value: score,
        label: score,
        clb: TCF_TO_CLB[skill][score]
      }));
      break;
    default:
      // Default CLB options
      options = Array.from({ length: 12 }, (_, i) => i + 1).map(clb => ({
        value: clb.toString(),
        label: `CLB ${clb}`,
        clb: clb
      }));
  }
  
  return options;
};

/**
 * Convert a test score to CLB level
 * @param {string} testType - The language test type (CELPIP, IELTS, PTE, TEF, TCF)
 * @param {string} skill - The language skill (speaking, listening, reading, writing)
 * @param {string} score - The score value
 * @returns {number} - The CLB level
 */
export const convertToCLB = (testType, skill, score) => {
  if (!testType || !skill || !score) return null;
  
  switch (testType) {
    case 'CELPIP':
      return CELPIP_TO_CLB[skill][score] || null;
    case 'IELTS':
      return IELTS_TO_CLB[skill][score] || null;
    case 'PTE':
      // For range scores, find the matching range
      return findRangeMatch(PTE_TO_CLB[skill], score) || null;
    case 'TEF':
      return findRangeMatch(TEF_TO_CLB[skill], score) || null;
    case 'TCF':
      return findRangeMatch(TCF_TO_CLB[skill], score) || null;
    default:
      // If it's already a CLB score
      return parseInt(score, 10) || null;
  }
};

/**
 * Helper function to find a matching range for a score
 * @param {Object} rangeMap - Map of score ranges to CLB levels
 * @param {number|string} score - The score to find a match for
 * @returns {number|null} - The matching CLB level or null if no match
 */
const findRangeMatch = (rangeMap, score) => {
  const numScore = parseInt(score, 10);
  if (isNaN(numScore)) return null;
  
  for (const [range, clb] of Object.entries(rangeMap)) {
    if (range.includes('-')) {
      const [min, max] = range.split('-').map(n => parseInt(n, 10));
      if (numScore >= min && numScore <= max) {
        return clb;
      }
    } else if (parseInt(range, 10) === numScore) {
      return clb;
    }
  }
  
  return null;
};

/**
 * Get the valid score range for a test type and skill
 * @param {string} testType - The language test type
 * @param {string} skill - The language skill
 * @returns {string} - Description of the valid score range
 */
export const getScoreRangeDescription = (testType) => {
  switch (testType) {
    case 'CELPIP':
      return 'Score range: 1-12';
    case 'IELTS':
      return 'Score range: 1.0-9.0';
    case 'PTE':
      return 'Score range: 10-115';
    case 'TEF':
      return 'Score range: 121-450';
    case 'TCF':
      return 'Score range: 101-1200';
    default:
      return 'Select a test type';
  }
}; 