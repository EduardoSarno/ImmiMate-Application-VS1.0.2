import { useState, useEffect, useCallback, useRef } from 'react';
import apiService from '../services/ApiService';
import Logger from '../utils/LoggingService';
import { useAuth } from '../contexts/AuthContext';
import { debounce } from 'lodash';
import { isEqual } from 'lodash';

// Create a module-level variable to track network errors across hook instances
const networkErrorTracker = {
    hasNetworkError: false,
    lastErrorTime: 0
};

// Track which forms have already been loaded to prevent duplicate loading
const loadedForms = new Set();

/**
 * Custom hook for managing form drafts with auto-save functionality.
 * 
 * @param {Object} options
 * @param {string} options.formId - Unique identifier for the form
 * @param {Object} options.initialData - Initial form data (if no draft exists)
 * @param {number} options.autoSaveInterval - Interval in ms for auto-save (default: 30000)
 * @param {boolean} options.enabled - Whether draft functionality is enabled (default: true)
 * @param {number} options.expirationHours - Hours before localStorage draft expires (default: 24 - 1 day)
 * @returns {Object} Form draft management utilities
 */
const useFormDraft = ({
    formId = 'default',
    initialData = {},
    autoSaveInterval = 30000,
    enabled = true,
    expirationHours = 24 // 1 day expiration by default
}) => {
    const [formData, setFormData] = useState(initialData);
    const [lastSavedData, setLastSavedData] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [saveStatus, setSaveStatus] = useState(null);
    const [error, setError] = useState(null);
    const [hasChanges, setHasChanges] = useState(false);
    
    // Get currentUser from AuthContext instead of checking localStorage
    const { currentUser } = useAuth();
    
    // Create the ref at the top level of the hook
    const hasAttemptedLoadRef = useRef(false);
    const requestCountRef = useRef(0);
    const isMountedRef = useRef(true);
    const lastSaveTimeRef = useRef(0);
    
    // Generate a unique instance ID to prevent duplicate hook instances from causing conflicts
    const instanceIdRef = useRef(`form-instance-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`);
    
    // Track the last time we tried to load data
    const lastLoadAttemptRef = useRef(0);
    
    // Flag that is set to true when a save is triggered by auto-save to prevent loops
    const autoSaveTriggeredRef = useRef(false);

    // Create a debounced save function to prevent excessive saves
    const debouncedSave = useCallback(
        debounce(async (data, forceUpdate = false) => {
            if (!enabled || !data || isSaving || !isMountedRef.current) return;
            
            // Check if we've saved too recently (under 2 seconds ago and not forced)
            const now = Date.now();
            if (!forceUpdate && now - lastSaveTimeRef.current < 2000) {
                Logger.debug('Skipping save - too soon since last save');
                return;
            }
            
            // Skip saving if there are no actual changes and not forced
            if (!forceUpdate && lastSavedData && isEqual(lastSavedData, data)) {
                Logger.debug('Skipping save - no changes detected');
                return;
            }
            
            try {
                setIsSaving(true);
                setSaveStatus('saving');
                setError(null);
                lastSaveTimeRef.current = now;
                
                // Add form identifier to the data
                const draftData = {
                    ...data,
                    _formId: formId,
                    _lastSaved: new Date().toISOString()
                };
                
                // Always save to localStorage first, regardless of authentication status
                saveToLocalStorage(draftData);
                
                // Try to save to server if authenticated and no network errors
                if (currentUser && !networkErrorTracker.hasNetworkError) {
                    try {
                        await apiService.saveFormDraft(draftData);
                        if (isMountedRef.current) {
                            Logger.debug('Form draft saved successfully to server', { formId });
                        }
                    } catch (serverError) {
                        // Check if there was a network error
                        if (serverError.code === 'ERR_NETWORK' || serverError.code === 'ECONNREFUSED') {
                            networkErrorTracker.hasNetworkError = true;
                            networkErrorTracker.lastErrorTime = Date.now();
                            Logger.debug('Network error when saving draft, will use localStorage only for 30 seconds');
                        }
                        
                        if (isMountedRef.current) {
                            Logger.error('Error saving form draft to server', serverError);
                        }
                        // Don't throw - we'll still consider it a success if local save worked
                    }
                }
                
                if (isMountedRef.current) {
                    setLastSavedData(data);
                    setSaveStatus('saved');
                    setHasChanges(false);
                }
            } catch (err) {
                if (isMountedRef.current) {
                    Logger.error('Error saving form draft', err);
                    setSaveStatus('error');
                    setError(err.message || 'Failed to save draft');
                }
            } finally {
                if (isMountedRef.current) {
                    setIsSaving(false);
                }
                autoSaveTriggeredRef.current = false;
            }
        }, 500),
        [enabled, formId, isSaving, currentUser]
    );

    // Helper function to save to localStorage with expiration
    const saveToLocalStorage = useCallback((data) => {
        try {
            // Add expiration timestamp (current time + expiration hours)
            const expiration = new Date();
            expiration.setHours(expiration.getHours() + expirationHours);
            
            const storageData = {
                data: data,
                expiration: expiration.getTime(),
                _formId: formId
            };
            
            localStorage.setItem(`form_draft_${formId}`, JSON.stringify(storageData));
            Logger.debug('Form draft backed up to localStorage with expiration', { 
                formId, 
                expiration: expiration.toISOString() 
            });
            return true;
        } catch (storageError) {
            Logger.warn('Could not back up form draft to localStorage', storageError);
            return false;
        }
    }, [formId, expirationHours]);

    // Helper function to load from localStorage with expiration check
    const loadFromLocalStorage = useCallback(() => {
        try {
            const storageItem = localStorage.getItem(`form_draft_${formId}`);
            if (!storageItem) {
                Logger.debug('No localStorage draft found', { formId });
                return null;
            }
            
            const storageData = JSON.parse(storageItem);
            
            // Check if data has expired
            if (storageData.expiration) {
                const now = new Date().getTime();
                if (now > storageData.expiration) {
                    Logger.debug('localStorage draft has expired, removing it', { 
                        formId,
                        expiration: new Date(storageData.expiration).toISOString()
                    });
                    localStorage.removeItem(`form_draft_${formId}`);
                    return null;
                }
            }
            
            // Check if it's for the right form
            if (storageData._formId !== formId) {
                Logger.debug('localStorage draft is for a different form', { 
                    formId, 
                    storedFormId: storageData._formId 
                });
                return null;
            }
            
            Logger.debug('Form draft loaded from localStorage', { 
                formId,
                expiresIn: `${Math.round((storageData.expiration - new Date().getTime()) / (1000 * 60 * 60))} hours`
            });
            
            return storageData.data;
        } catch (storageError) {
            Logger.warn('Error loading form draft from localStorage', storageError);
            return null;
        }
    }, [formId]);

    // Function to save the current form data as a draft
    const saveDraft = useCallback(async (data = formData) => {
        if (!enabled || !data || isSaving) return;
        
        // Don't save if the data hasn't changed and this isn't a manual save
        if (lastSavedData && isEqual(lastSavedData, data)) {
            return;
        }
        
        await debouncedSave(data, true);
    }, [enabled, formData, isSaving, lastSavedData, debouncedSave]);

    // Set up auto-save for localStorage even without authentication
    useEffect(() => {
        if (!enabled || isLoading) return;
        
        // Skip auto-save if there are no changes
        if (!hasChanges) return;
        
        const timer = setTimeout(() => {
            // Prevent auto-save if we're in a saving/loading state or recently auto-saved
            if (isSaving || autoSaveTriggeredRef.current) return;
            
            // Flag that this save was triggered by auto-save
            autoSaveTriggeredRef.current = true;
            
            // Always try to save to localStorage, regardless of auth status
            if (formData) {
                Logger.debug('Auto-save triggered for localStorage', { formId });
                const draftData = {
                    ...formData,
                    _formId: formId,
                    _lastSaved: new Date().toISOString()
                };
                saveToLocalStorage(draftData);
                setLastSavedData(formData);
                setHasChanges(false);
            }
        }, autoSaveInterval);
        
        return () => clearTimeout(timer);
    }, [enabled, formData, isLoading, formId, autoSaveInterval, saveToLocalStorage, hasChanges, isSaving]);

    // Separate auto-save effect for server if authenticated
    useEffect(() => {
        if (!enabled || isLoading || !currentUser || !hasChanges) return;
        
        const timer = setTimeout(() => {
            // Prevent auto-save if we're in a saving/loading state or recently auto-saved
            if (isSaving || autoSaveTriggeredRef.current) return;
            
            // Reset network error tracker after 30 seconds
            if (networkErrorTracker.hasNetworkError && Date.now() - networkErrorTracker.lastErrorTime > 30000) {
                networkErrorTracker.hasNetworkError = false;
                Logger.debug('Network error status reset after timeout');
            }
            
            // Flag that this save was triggered by auto-save
            autoSaveTriggeredRef.current = true;
            Logger.debug('Auto-save triggered for server', { formId });
            debouncedSave(formData);
        }, autoSaveInterval * 2); // Less frequent server saves
        
        return () => clearTimeout(timer);
    }, [enabled, formData, debouncedSave, autoSaveInterval, isLoading, currentUser, hasChanges, isSaving]);

    // Check if it's safe to attempt a server request
    const shouldAttemptServerRequest = useCallback(() => {
        // First check if this form was already loaded by another instance
        if (loadedForms.has(formId)) {
            Logger.debug(`Form ${formId} already loaded by another instance`);
            return false;
        }
        
        // If we've already seen a network error, don't attempt
        if (networkErrorTracker.hasNetworkError) {
            return false;
        }
        
        // If we've already made too many requests in this component instance, don't attempt
        if (requestCountRef.current >= 3) {
            return false;
        }
        
        // Don't make requests too frequently (at least 2 seconds apart)
        const now = Date.now();
        if (now - lastLoadAttemptRef.current < 2000) {
            return false;
        }
        
        // Update the last attempt time
        lastLoadAttemptRef.current = now;
        requestCountRef.current++;
        
        return true;
    }, [formId]);

    // Load initial draft
    useEffect(() => {
        // Set mounted flag
        isMountedRef.current = true;
        
        if (!enabled) {
            setIsLoading(false);
            return;
        }
        
        // Skip loading if this hook already attempted to load
        if (hasAttemptedLoadRef.current) {
            return;
        }
        
        // Skip if another instance already loaded this form
        if (loadedForms.has(formId)) {
            Logger.debug(`Form ${formId} already loaded by another instance, skipping load`);
            setIsLoading(false);
            return;
        }
        
        // Mark this hook instance as having attempted to load
        hasAttemptedLoadRef.current = true;
        
        const loadDraft = async () => {
            // Skip if we're deliberately disabled
            if (!enabled) {
                setIsLoading(false);
                return;
            }
            
            try {
                setIsLoading(true);
                Logger.debug('Loading form draft', { formId, instance: instanceIdRef.current });
                
                // Always check localStorage first for immediate data
                let draftData = loadFromLocalStorage();
                let dataSource = 'localStorage';
                
                // If authenticated and we should attempt a server request, try getting the data
                if (currentUser && shouldAttemptServerRequest()) {
                    try {
                        // Add a timeout to prevent hanging
                        const draftPromise = apiService.getLatestFormDraft();
                        const timeoutPromise = new Promise((_, reject) => 
                            setTimeout(() => reject(new Error('Draft load timeout')), 5000) // Shorter timeout
                        );
                        
                        // Race between the draft load and the timeout
                        const serverData = await Promise.race([draftPromise, timeoutPromise]);
                        
                        if (serverData && serverData._formId === formId) {
                            // Compare timestamps to use the most recent data
                            const serverTime = new Date(serverData._lastSaved || 0).getTime();
                            const localTime = draftData ? new Date(draftData._lastSaved || 0).getTime() : 0;
                            
                            if (serverTime >= localTime) {
                                draftData = serverData;
                                dataSource = 'server';
                                Logger.debug('Using server draft data (more recent)', { formId });
                            } else {
                                Logger.debug('Using localStorage draft data (more recent)', { formId });
                            }
                        }
                    } catch (serverError) {
                        // Check if there was a network error
                        if (serverError.code === 'ERR_NETWORK' || 
                            serverError.message === 'Draft load timeout' ||
                            serverError.code === 'ECONNREFUSED') {
                            networkErrorTracker.hasNetworkError = true;
                            networkErrorTracker.lastErrorTime = Date.now();
                            Logger.debug('Network error detected, will use localStorage only for 30 seconds');
                        }
                        
                        Logger.error('Error loading form draft from server', serverError);
                        // Continue with localStorage data
                    }
                }
                
                // Process the draft data (from either source)
                if (draftData && draftData._formId === formId) {
                    // Remove metadata before setting form data
                    const { _formId, _lastSaved, ...cleanData } = draftData;
                    
                    Logger.debug('Form draft loaded', { 
                        formId, 
                        lastSaved: _lastSaved,
                        source: dataSource
                    });
                    
                    if (isMountedRef.current) {
                        setLastSavedData(cleanData);
                        setFormData(cleanData);
                    }
                } else {
                    Logger.debug('No matching draft found, using initial data', { formId });
                    if (isMountedRef.current) {
                        setFormData(initialData);
                    }
                }
                
                // Mark this form as loaded to prevent other instances from reloading
                loadedForms.add(formId);
            } catch (err) {
                Logger.error('Error loading form draft', err);
                if (isMountedRef.current) {
                    setError(err.message || 'Failed to load draft');
                    setFormData(initialData);
                }
            } finally {
                if (isMountedRef.current) {
                    setIsLoading(false);
                }
            }
        };
        
        loadDraft();
        
        // Cleanup function
        return () => {
            // Mark this component as unmounted
            isMountedRef.current = false;
            
            // Remove this form from the loaded set when component is fully unmounted
            setTimeout(() => {
                loadedForms.delete(formId);
            }, 500);
            
            // Clear network error status on unmount after a delay to allow other instances to benefit
            setTimeout(() => {
                if (Date.now() - networkErrorTracker.lastErrorTime > 30000) {
                    networkErrorTracker.hasNetworkError = false;
                }
            }, 1000);
        };
    }, [enabled, formId, currentUser, initialData, loadFromLocalStorage, shouldAttemptServerRequest]);

    // Update form data
    const updateFormData = useCallback((newData) => {
        if (typeof newData === 'function') {
            setFormData(prevData => {
                const updatedData = newData(prevData);
                
                // Check if the data has actually changed
                if (isEqual(updatedData, prevData)) {
                    return prevData; // Return the existing reference to prevent re-renders
                }
                
                // Mark that we have unsaved changes
                setHasChanges(true);
                return updatedData;
            });
        } else {
            // Check if the data has actually changed
            if (isEqual(newData, formData)) {
                return; // Skip the update entirely
            }
            
            // Mark that we have unsaved changes
            setHasChanges(true);
            setFormData(newData);
        }
    }, [formData]);

    // Discard draft and reset to initial data
    const discardDraft = useCallback(async () => {
        if (!isMountedRef.current) return;
        
        try {
            // Clear localStorage
            localStorage.removeItem(`form_draft_${formId}`);
            Logger.debug('Form draft removed from localStorage', { formId });
            
            // Clear server draft if authenticated
            if (currentUser && !networkErrorTracker.hasNetworkError) {
                try {
                    await apiService.saveFormDraft({ _formId: formId, _discarded: true });
                    Logger.debug('Form draft discarded on server', { formId });
                } catch (serverError) {
                    Logger.error('Error discarding server form draft', serverError);
                    // Continue anyway since we cleared localStorage
                }
            }
            
            // Reset local state
            setFormData(initialData);
            setLastSavedData(null);
            setSaveStatus(null);
            setHasChanges(false);
            Logger.debug('Form draft discarded completely', { formId });
        } catch (err) {
            Logger.error('Error discarding form draft', err);
            setError(err.message || 'Failed to discard draft');
        }
    }, [formId, initialData, currentUser]);

    return {
        formData,
        updateFormData,
        saveDraft,
        discardDraft,
        isSaving,
        isLoading,
        saveStatus,
        error,
        hasUnsavedChanges: hasChanges
    };
};

export default useFormDraft; 