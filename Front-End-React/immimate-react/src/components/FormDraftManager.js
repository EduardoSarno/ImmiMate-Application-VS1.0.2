import React, { useState, useEffect, useCallback } from 'react';
import apiService from '../services/ApiService';
import Logger from '../utils/LoggingService';

/**
 * FormDraftManager component provides auto-save functionality for forms.
 * It handles saving form drafts to the server and loading them when the form is opened.
 * 
 * @param {Object} props
 * @param {Object} props.formData - The current form data
 * @param {Function} props.onFormDataLoaded - Callback when form data is loaded from a draft
 * @param {string} props.formId - Unique identifier for the form (optional)
 * @param {number} props.autoSaveInterval - Interval in milliseconds for auto-save (default: 30000)
 * @param {boolean} props.enabled - Whether draft functionality is enabled (default: true)
 */
const FormDraftManager = ({ 
    formData, 
    onFormDataLoaded, 
    formId = 'default', 
    autoSaveInterval = 30000,
    enabled = true 
}) => {
    const [lastSavedData, setLastSavedData] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const [saveStatus, setSaveStatus] = useState(null);
    const [hasLoadedInitialDraft, setHasLoadedInitialDraft] = useState(false);

    // Function to save the current form data as a draft
    const saveDraft = useCallback(async (data) => {
        if (!enabled || !data || isSaving) return;
        
        // Don't save if the data hasn't changed
        if (lastSavedData && JSON.stringify(lastSavedData) === JSON.stringify(data)) {
            Logger.debug('Form data unchanged, skipping save');
            return;
        }
        
        try {
            setIsSaving(true);
            setSaveStatus('saving');
            
            // Add form identifier to the data
            const draftData = {
                ...data,
                _formId: formId,
                _lastSaved: new Date().toISOString()
            };
            
            await apiService.saveFormDraft(draftData);
            
            setLastSavedData(data);
            setSaveStatus('saved');
            Logger.debug('Form draft saved successfully', { formId });
        } catch (error) {
            Logger.error('Error saving form draft', error);
            setSaveStatus('error');
        } finally {
            setIsSaving(false);
        }
    }, [enabled, formId, isSaving, lastSavedData]);

    // Auto-save effect
    useEffect(() => {
        if (!enabled || !formData) return;
        
        const timer = setTimeout(() => {
            saveDraft(formData);
        }, autoSaveInterval);
        
        return () => clearTimeout(timer);
    }, [enabled, formData, saveDraft, autoSaveInterval]);

    // Load initial draft
    useEffect(() => {
        if (!enabled || hasLoadedInitialDraft) return;
        
        const loadDraft = async () => {
            try {
                Logger.debug('Loading form draft', { formId });
                const draftData = await apiService.getLatestFormDraft();
                
                if (draftData && draftData._formId === formId) {
                    // Remove metadata before passing to the form
                    const { _formId, _lastSaved, ...cleanData } = draftData;
                    
                    Logger.debug('Form draft loaded', { 
                        formId, 
                        lastSaved: _lastSaved 
                    });
                    
                    setLastSavedData(cleanData);
                    onFormDataLoaded(cleanData);
                } else {
                    Logger.debug('No matching draft found for this form', { formId });
                }
            } catch (error) {
                Logger.error('Error loading form draft', error);
            } finally {
                setHasLoadedInitialDraft(true);
            }
        };
        
        loadDraft();
    }, [enabled, formId, hasLoadedInitialDraft, onFormDataLoaded]);

    // Manual save function that can be triggered by parent component
    const manualSave = useCallback(() => {
        if (formData) {
            saveDraft(formData);
        }
    }, [formData, saveDraft]);

    return (
        <div className="form-draft-manager">
            {saveStatus === 'saving' && (
                <div className="draft-status saving">Saving draft...</div>
            )}
            {saveStatus === 'saved' && (
                <div className="draft-status saved">
                    Draft saved at {new Date().toLocaleTimeString()}
                </div>
            )}
            {saveStatus === 'error' && (
                <div className="draft-status error">
                    Error saving draft. 
                    <button onClick={manualSave}>Retry</button>
                </div>
            )}
        </div>
    );
};

export default FormDraftManager; 