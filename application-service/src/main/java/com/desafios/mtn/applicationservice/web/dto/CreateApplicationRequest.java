package com.desafios.mtn.applicationservice.web.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating new applications
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateApplicationRequest {
    
    // Student Information
    @NotBlank(message = "Student name is required")
    @Size(min = 2, max = 100, message = "Student name must be between 2 and 100 characters")
    private String studentName;
    
    @NotBlank(message = "Student RUT is required")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9Kk]$", message = "Invalid RUT format")
    private String studentRut;
    
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @NotBlank(message = "Grade level is required")
    @Pattern(regexp = "^(PRE_K|KINDER|1ST|2ND|3RD|4TH|5TH|6TH|7TH|8TH|9TH|10TH|11TH|12TH)$", 
             message = "Invalid grade level")
    private String gradeLevel;
    
    @NotBlank(message = "Target school is required")
    @Pattern(regexp = "^(MONTE_TABOR|NAZARET)$", message = "Target school must be MONTE_TABOR or NAZARET")
    private String targetSchool;
    
    // Previous school information
    private String previousSchool;
    private String previousSchoolAddress;
    
    // Special Categories
    private boolean isEmployeeChild;
    private String employeeParentName;
    
    private boolean isAlumniChild;
    private Integer alumniParentGraduationYear;
    
    private boolean isInclusionStudent;
    private String inclusionType;
    private String inclusionNotes;
    
    // Medical Information
    private boolean hasSpecialNeeds;
    private String specialNeedsDescription;
    private boolean hasMedicalConditions;
    private String medicalConditionsDescription;
    
    // Family Information
    @Valid
    @NotNull(message = "Father information is required")
    private ParentInfoDto father;
    
    @Valid
    @NotNull(message = "Mother information is required")
    private ParentInfoDto mother;
    
    @Valid
    private GuardianInfoDto guardian; // Optional if different from parents
    
    @Valid
    private EmergencyContactDto emergencyContact;
    
    // Contact Information
    @NotBlank(message = "Primary phone is required")
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Invalid Chilean phone format")
    private String primaryPhone;
    
    private String secondaryPhone;
    
    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    private String address;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotBlank(message = "Region is required")
    private String region;
    
    // Academic Information
    private Double lastYearGpa;
    private String academicStrengths;
    private String academicChallenges;
    private String extracurricularActivities;
    
    // Application Preferences
    private LocalDate preferredStartDate;
    private String additionalComments;
    
    // Required documents list
    @NotNull
    @Size(min = 1, message = "At least one required document must be specified")
    private List<@NotBlank String> requiredDocuments;
    
    // Privacy and consent
    @NotNull
    @AssertTrue(message = "Terms and conditions must be accepted")
    private Boolean termsAccepted;
    
    @NotNull
    @AssertTrue(message = "Privacy policy must be accepted") 
    private Boolean privacyPolicyAccepted;
    
    @NotNull
    @AssertTrue(message = "Data processing consent must be given")
    private Boolean dataProcessingConsent;
}

/**
 * Parent information DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ParentInfoDto {
    
    @NotBlank(message = "Parent name is required")
    @Size(min = 2, max = 100, message = "Parent name must be between 2 and 100 characters")
    private String fullName;
    
    @NotBlank(message = "Parent RUT is required")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9Kk]$", message = "Invalid RUT format")
    private String rut;
    
    @NotBlank(message = "Parent email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Parent phone is required")
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Invalid Chilean phone format")
    private String phone;
    
    private String workPhone;
    
    @NotBlank(message = "Occupation is required")
    private String occupation;
    
    private String workPlace;
    private String workAddress;
    
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @NotBlank(message = "Education level is required")
    private String educationLevel;
    
    // Relationship to student
    @NotBlank(message = "Relationship is required")
    @Pattern(regexp = "^(FATHER|MOTHER)$", message = "Relationship must be FATHER or MOTHER")
    private String relationship;
}

/**
 * Guardian information DTO (when different from parents)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class GuardianInfoDto {
    
    @NotBlank(message = "Guardian name is required")
    private String fullName;
    
    @NotBlank(message = "Guardian RUT is required")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9Kk]$", message = "Invalid RUT format")
    private String rut;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Guardian phone is required")
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Invalid Chilean phone format")
    private String phone;
    
    @NotBlank(message = "Relationship is required")
    private String relationshipToStudent;
    
    @NotBlank(message = "Legal authorization is required")
    private String legalAuthorization;
}

/**
 * Emergency contact DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class EmergencyContactDto {
    
    @NotBlank(message = "Emergency contact name is required")
    private String fullName;
    
    @NotBlank(message = "Emergency contact phone is required")
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Invalid Chilean phone format")
    private String phone;
    
    @NotBlank(message = "Relationship is required")
    private String relationshipToStudent;
    
    private String alternativePhone;
    private String address;
}