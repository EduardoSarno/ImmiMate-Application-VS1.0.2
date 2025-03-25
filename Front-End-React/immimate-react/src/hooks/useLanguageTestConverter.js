import { useState, useEffect, useCallback } from 'react';
import * as LanguageTestService from '../services/LanguageTestService';
import Logger from '../utils/LoggingService';

// Static data for language test descriptions
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
      return 'Score range: 101-1200 (Listening/Reading), 1-20 (Writing/Speaking)';
    default:
      return 'Select a test type';
  }
};

/**
 * Hook for using language test conversion functionality
 */
const useLanguageTestConverter = () => {
    // State for conversion tables
    const [conversionTables, setConversionTables] = useState(null);
    // State for language test options
    const [languageTestOptions, setLanguageTestOptions] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    // Fetch conversion tables from API
    useEffect(() => {
        const fetchConversionTables = async () => {
            try {
                // Set loading state
                setLoading(true);
                setError(null);
                
                // Fetch conversion tables
                const tables = await LanguageTestService.getConversionTables();
                setConversionTables(tables);
                
                // Fetch test options
                const options = await LanguageTestService.getLanguageTestOptions();
                setLanguageTestOptions(options);
            } catch (err) {
                Logger.error('Error in useLanguageTestConverter:', err);
                setError(err);
            } finally {
                setLoading(false);
            }
        };

        fetchConversionTables();
    }, []);
    
    // Function to convert test score to CLB level
    const convertToCLB = useCallback(async (testType, skill, score) => {
        if (!testType || !skill || !score) return null;
        
        try {
            return await LanguageTestService.convertToCLB(testType, skill, score);
        } catch (error) {
            Logger.warn('API conversion failed, falling back to client-side conversion');
            
            // Fallback to client-side conversion using the tables we've fetched
            if (conversionTables) {
                const testMap = conversionTables[testType.toLowerCase()];
                if (!testMap) return null;
                
                const skillMap = testMap[skill.toLowerCase()];
                if (!skillMap) return null;
                
                return skillMap[score] || null;
            }
            
            return null;
        }
    }, [conversionTables]);
    
    // Function to check if two languages are the same
    const areSameLanguage = useCallback((testType1, testType2) => {
        const englishTests = ['CELPIP', 'IELTS', 'PTE'];
        const frenchTests = ['TEF', 'TCF'];
        
        if (englishTests.includes(testType1) && englishTests.includes(testType2)) {
            return true;
        }
        
        if (frenchTests.includes(testType1) && frenchTests.includes(testType2)) {
            return true;
        }
        
        return false;
    }, []);
    
    // Function to get secondary test options based on primary test
    const getSecondaryTestOptions = useCallback((primaryTest) => {
        const englishTests = ['CELPIP', 'IELTS', 'PTE'];
        const frenchTests = ['TEF', 'TCF'];
        
        if (englishTests.includes(primaryTest)) {
            return frenchTests.map(test => ({ value: test, label: test === 'TEF' ? 'TEF Canada' : 'TCF Canada' }));
        }
        
        if (frenchTests.includes(primaryTest)) {
            return englishTests.map(test => ({ 
                value: test, 
                label: test === 'CELPIP' ? 'CELPIP-G' : test === 'IELTS' ? 'IELTS' : 'PTE Core' 
            }));
        }
        
        return [];
    }, []);
    
    // Function to get score options
    const getScoreOptions = useCallback((testType, skill) => {
        if (!testType || !skill || !languageTestOptions) return [];
        
        // Return the options from the API if available
        if (languageTestOptions[testType] && languageTestOptions[testType][skill]) {
            return languageTestOptions[testType][skill];
        }
        
        // Return empty array if options not found
        return [];
    }, [languageTestOptions]);
    
    return {
        convertToCLB,
        areSameLanguage,
        getSecondaryTestOptions,
        getScoreOptions,
        loading,
        error,
        conversionTables,
        languageTestOptions
    };
};

export default useLanguageTestConverter; 