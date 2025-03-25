import axios from 'axios';
import { API_BASE_URL } from '../config/config';
import Logger from '../utils/LoggingService';

// Path constants
const LANGUAGE_TESTS_PATH = '/language-tests';
const CONVERSIONS_PATH = '/conversions';
const CONVERT_PATH = '/convert';
const OPTIONS_PATH = '/options';

/**
 * Get all language test conversion tables
 * @returns {Promise<Object>} The conversion tables
 */
export const getConversionTables = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}${LANGUAGE_TESTS_PATH}${CONVERSIONS_PATH}`);
    return response.data;
  } catch (error) {
    Logger.error('Error fetching language test conversion tables:', error);
    throw error;
  }
};

/**
 * Get all available language test score options by test type and skill
 * Used to populate dropdowns in the UI
 * @returns {Promise<Object>} Map of test types to skills to available score options
 */
export const getLanguageTestOptions = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}${LANGUAGE_TESTS_PATH}${OPTIONS_PATH}`);
    
    // Transform the data to match the format expected by the form
    // From: {testType: {skill: [value1, value2]}} 
    // To: {testType: {skill: [{value: value1, label: value1}, ...]}}
    const transformedOptions = {};
    
    Object.entries(response.data).forEach(([testType, skills]) => {
      transformedOptions[testType] = {};
      
      Object.entries(skills).forEach(([skill, values]) => {
        transformedOptions[testType][skill] = values.map(value => ({
          value,
          label: value
        }));
      });
    });
    
    return transformedOptions;
  } catch (error) {
    Logger.error('Error fetching language test options:', error);
    // Fallback to hardcoded options if API fails
    throw error;
  }
};

/**
 * Convert a test score to CLB level
 * @param {string} testType The language test type (CELPIP, IELTS, etc)
 * @param {string} skill The language skill (listening, reading, etc)
 * @param {string} score The score value
 * @returns {Promise<number>} The CLB level
 */
export const convertToCLB = async (testType, skill, score) => {
  console.log(`DEBUG convertToCLB called with:`, {
    testType,
    skill,
    score,
    scoreType: typeof score,
    valueField: score && typeof score === 'object' ? score.value : 'N/A'
  });
  
  // Ensure we have a string value for API call
  let scoreValue = score;
  if (typeof score === 'object' && score !== null && 'value' in score) {
    scoreValue = score.value;
    console.log(`Converting object score to value: ${scoreValue}`);
  }
  
  try {
    const response = await axios.get(
      `${API_BASE_URL}${LANGUAGE_TESTS_PATH}${CONVERT_PATH}?testType=${testType}&skill=${skill}&score=${scoreValue}`
    );
    return response.data;
  } catch (error) {
    Logger.error('Error using API for CLB conversion, falling back to client-side conversion:', error);
    throw error;
  }
};

/**
 * Service for language test related operations
 */
class LanguageTestService {
    // Cache for conversion tables
    #conversionTables = null;
    #lastFetched = null;
    #cacheExpiryTimeMs = 1000 * 60 * 60; // 1 hour
    
    /**
     * Get language test conversion tables from API or cache
     * @param {boolean} forceRefresh - Force refresh from API
     * @returns {Promise<Object>} - The conversion tables
     */
    async getConversionTables(forceRefresh = false) {
        // Check if cache is valid
        const now = Date.now();
        const cacheValid = this.#conversionTables !== null && 
                          this.#lastFetched !== null && 
                          (now - this.#lastFetched) < this.#cacheExpiryTimeMs;
        
        // Return from cache if valid and not forcing refresh
        if (cacheValid && !forceRefresh) {
            console.log('Using cached language test conversion tables');
            return this.#conversionTables;
        }
        
        try {
            // Fetch from API
            console.log('Fetching language test conversion tables from API');
            const response = await axios.get(`${API_BASE_URL}${LANGUAGE_TESTS_PATH}${CONVERSIONS_PATH}`);
            
            // Update cache
            this.#conversionTables = response.data;
            this.#lastFetched = now;
            
            // Also cache in localStorage as backup
            try {
                localStorage.setItem('languageTestConversions', JSON.stringify({
                    data: this.#conversionTables,
                    timestamp: now
                }));
            } catch (e) {
                console.warn('Failed to cache language test conversions in localStorage:', e);
            }
            
            return this.#conversionTables;
        } catch (error) {
            console.error('Error fetching language test conversion tables:', error);
            
            // Try to load from localStorage as fallback
            try {
                const cached = localStorage.getItem('languageTestConversions');
                if (cached) {
                    const parsedCache = JSON.parse(cached);
                    console.log('Using localStorage fallback for language test conversions');
                    this.#conversionTables = parsedCache.data;
                    this.#lastFetched = parsedCache.timestamp;
                    return this.#conversionTables;
                }
            } catch (e) {
                console.error('Failed to load language test conversions from localStorage:', e);
            }
            
            throw error;
        }
    }
    
    /**
     * Convert a test score to CLB level
     * @param {string} testType - The language test type (CELPIP, IELTS, etc.)
     * @param {string} skill - The language skill (listening, reading, etc.)
     * @param {string} score - The score value
     * @returns {Promise<number|null>} - The CLB level or null if not found
     */
    async convertToCLB(testType, skill, score) {
        if (!testType || !skill || !score) {
            return null;
        }
        
        try {
            // Try to use API endpoint
            const response = await axios.get(
                `${API_BASE_URL}${LANGUAGE_TESTS_PATH}${CONVERT_PATH}?testType=${testType}&skill=${skill}&score=${score}`
            );
            return response.data;
        } catch (error) {
            console.warn('Error using API for CLB conversion, falling back to client-side conversion:', error);
            
            // Fall back to client-side conversion using tables
            return this.#convertLocally(testType, skill, score);
        }
    }
    
    /**
     * Client-side conversion fallback
     * @param {string} testType - The language test type
     * @param {string} skill - The language skill
     * @param {string} score - The score value
     * @returns {Promise<number|null>} - The CLB level or null if not found
     */
    async #convertLocally(testType, skill, score) {
        try {
            const tables = await this.getConversionTables();
            
            // Get the map for this test type
            const testMap = this.#getTestMap(tables, testType.toUpperCase());
            if (!testMap) {
                return null;
            }
            
            // Get the map for this skill
            const skillMap = testMap[skill.toLowerCase()];
            if (!skillMap) {
                return null;
            }
            
            // Try exact match first
            if (skillMap[score] !== undefined) {
                return skillMap[score];
            }
            
            // Try range match
            const numScore = parseFloat(score);
            if (isNaN(numScore)) {
                return null;
            }
            
            // Find a range that contains this score
            for (const [range, clbLevel] of Object.entries(skillMap)) {
                if (range.includes('-')) {
                    const [min, max] = range.split('-').map(n => parseFloat(n));
                    if (numScore >= min && numScore <= max) {
                        return clbLevel;
                    }
                }
            }
            
            return null;
        } catch (error) {
            console.error('Error in client-side CLB conversion:', error);
            return null;
        }
    }
    
    /**
     * Get the test map for a specific test type
     * @param {Object} tables - The conversion tables
     * @param {string} testType - The test type
     * @returns {Object|null} - The test map or null if not found
     */
    #getTestMap(tables, testType) {
        switch (testType) {
            case 'CELPIP':
                return tables.celpip;
            case 'IELTS':
                return tables.ielts;
            case 'PTE':
                return tables.pte;
            case 'TEF':
                return tables.tef;
            case 'TCF':
                return tables.tcf;
            default:
                return null;
        }
    }
    
    /**
     * Get available test options for secondary language based on primary test language
     * @param {string} primaryTestType - The primary language test type
     * @returns {Array} - Array of valid secondary language test types
     */
    getSecondaryTestOptions(primaryTestType) {
        const primaryLanguage = this.getTestLanguage(primaryTestType);
        if (!primaryLanguage) return [];
        
        if (primaryLanguage === 'english') {
            return [
                { value: 'TEF', label: 'TEF Canada' },
                { value: 'TCF', label: 'TCF Canada' }
            ];
        } else {
            return [
                { value: 'CELPIP', label: 'CELPIP-G' },
                { value: 'IELTS', label: 'IELTS' },
                { value: 'PTE', label: 'PTE Core' }
            ];
        }
    }
    
    /**
     * Get the language of a test type
     * @param {string} testType - The language test type (CELPIP, IELTS, PTE, TEF, TCF)
     * @returns {string|null} - The language ('english' or 'french')
     */
    getTestLanguage(testType) {
        if (!testType) return null;
        
        switch (testType) {
            case 'CELPIP':
            case 'IELTS':
            case 'PTE':
                return 'english';
            case 'TEF':
            case 'TCF':
                return 'french';
            default:
                return null;
        }
    }
    
    /**
     * Check if two test types are in the same language
     * @param {string} testType1 - The first language test type
     * @param {string} testType2 - The second language test type
     * @returns {boolean} - True if both tests are in the same language, false otherwise
     */
    areSameLanguage(testType1, testType2) {
        if (!testType1 || !testType2) return false;
        return this.getTestLanguage(testType1) === this.getTestLanguage(testType2);
    }
}

// Create a named instance of the service
const languageTestService = new LanguageTestService();

// Export the instance
export default languageTestService; 