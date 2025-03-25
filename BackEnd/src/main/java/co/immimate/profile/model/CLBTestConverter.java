package co.immimate.profile.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing language test score conversions to Canadian Language Benchmark (CLB) levels.
 */
@Entity
@Table(name = "clb_test_converter", schema = "static_canadian_immigration_data")
@Data
@NoArgsConstructor
public class CLBTestConverter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "clb_level")
    private Integer clbLevel;
    
    // CELPIP-G scores
    @Column(name = "celpip_listening")
    private String celpipListening;
    
    @Column(name = "celpip_reading")
    private String celpipReading;
    
    @Column(name = "celpip_writing")
    private String celpipWriting;
    
    @Column(name = "celpip_speaking")
    private String celpipSpeaking;
    
    // IELTS scores
    @Column(name = "ielts_listening")
    private String ieltsListening;
    
    @Column(name = "ielts_reading")
    private String ieltsReading;
    
    @Column(name = "ielts_writing")
    private String ieltsWriting;
    
    @Column(name = "ielts_speaking")
    private String ieltsSpeaking;
    
    // PTE Core scores
    @Column(name = "pte_listening")
    private String pteListening;
    
    @Column(name = "pte_reading")
    private String pteReading;
    
    @Column(name = "pte_writing")
    private String pteWriting;
    
    @Column(name = "pte_speaking")
    private String pteSpeaking;
    
    // TEF Canada scores
    @Column(name = "tef_listening")
    private String tefListening;
    
    @Column(name = "tef_reading")
    private String tefReading;
    
    @Column(name = "tef_writing")
    private String tefWriting;
    
    @Column(name = "tef_speaking")
    private String tefSpeaking;
    
    // TCF Canada scores
    @Column(name = "tcf_listening")
    private String tcfListening;
    
    @Column(name = "tcf_reading")
    private String tcfReading;
    
    @Column(name = "tcf_writing")
    private String tcfWriting;
    
    @Column(name = "tcf_speaking")
    private String tcfSpeaking;
    
    // Metadata
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "active")
    private Boolean active;
} 