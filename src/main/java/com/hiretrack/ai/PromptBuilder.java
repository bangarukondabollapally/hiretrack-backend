package com.hiretrack.ai;

import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.assistant.ChatMessage;
import com.hiretrack.interview.Interview;
import com.hiretrack.opening.PlacementOpening;
import com.hiretrack.opening.PlacementOpeningRepository;
import com.hiretrack.user.Profile;
import com.hiretrack.user.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final ProfileRepository profileRepository;
    private final ApplicationRepository applicationRepository;
    private final PlacementOpeningRepository placementOpeningRepository;

    public String buildSystemPrompt(Long userId, Long applicationId) {
        return buildSystemPrompt(userId, applicationId, null);
    }

    public String buildSystemPrompt(Long userId, Long applicationId, List<ChatMessage> historyMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the HireTrack AI Assistant — a focused, practical career & job application coach.\n");
        sb.append("Your goal is to help job seekers stay organized, prepare for interviews, and move their applications forward.\n\n");

        // 1. User Profile & Resume Context
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        sb.append("=== USER PROFILE & EXPERIENCE ===\n");
        if (profile != null) {
            if (profile.getName() != null && !profile.getName().trim().isEmpty()) {
                sb.append("User Name: ").append(profile.getName().trim()).append("\n");
            }
            if (profile.getTargetRole() != null && !profile.getTargetRole().trim().isEmpty()) {
                sb.append("Target Role: ").append(profile.getTargetRole().trim()).append("\n");
            }
            if (profile.getYearsOfExperience() != null) {
                sb.append("Years of Experience: ").append(profile.getYearsOfExperience()).append("\n");
            }
            if (profile.getExperienceSummary() != null && !profile.getExperienceSummary().trim().isEmpty()) {
                sb.append("Experience Summary:\n").append(profile.getExperienceSummary().trim()).append("\n");
            }
            if (profile.getResumeText() != null && !profile.getResumeText().trim().isEmpty()) {
                sb.append("Master Resume:\n").append(profile.getResumeText().trim()).append("\n");
            }
            if ((profile.getResumeText() == null || profile.getResumeText().trim().isEmpty()) &&
                (profile.getExperienceSummary() == null || profile.getExperienceSummary().trim().isEmpty())) {
                sb.append("(No resume text or experience summary uploaded yet)\n");
            }
        } else {
            sb.append("(No profile created yet)\n");
        }
        sb.append("===================================\n\n");

        // 2. Application Context (Targeted vs General Summary)
        if (applicationId != null) {
            Application app = applicationRepository.findByIdAndUserId(applicationId, userId)
                    .orElseThrow(() -> new com.hiretrack.common.exception.ResourceNotFoundException("Application not found with id: " + applicationId));
            if (app != null) {
                sb.append("=== TARGET APPLICATION CONTEXT ===\n");
                sb.append("Company: ").append(app.getCompanyName()).append("\n");
                sb.append("Role: ").append(app.getJobRole()).append("\n");
                sb.append("Status: ").append(app.getStatus()).append("\n");
                if (app.getJobType() != null) sb.append("Type: ").append(app.getJobType()).append("\n");
                if (app.getAppliedDate() != null) sb.append("Applied Date: ").append(app.getAppliedDate()).append("\n");
                if (app.getFollowUpDate() != null) sb.append("Follow-up Date: ").append(app.getFollowUpDate()).append("\n");
                if (app.getNotes() != null && !app.getNotes().trim().isEmpty()) {
                    sb.append("Notes: ").append(app.getNotes().trim()).append("\n");
                }
                if (app.getJobDescription() != null && !app.getJobDescription().trim().isEmpty()) {
                    sb.append("Job Description:\n").append(app.getJobDescription().trim()).append("\n");
                }

                List<Interview> interviews = app.getInterviews();
                if (interviews != null && !interviews.isEmpty()) {
                    sb.append("\nScheduled / Past Interview Rounds:\n");
                    for (Interview i : interviews) {
                        sb.append("- ").append(i.getRound())
                                .append(" | Date: ").append(i.getInterviewDate())
                                .append(" | Outcome: ").append(i.getOutcome());
                        if (i.getNotes() != null && !i.getNotes().trim().isEmpty()) {
                            sb.append(" | Notes: ").append(i.getNotes().trim());
                        }
                        sb.append("\n");
                    }
                }

                // TASK 6: Placement Opening details in untrusted block
                if (app.getPlacementOpeningId() != null) {
                    PlacementOpening opening = placementOpeningRepository.findById(app.getPlacementOpeningId()).orElse(null);
                    if (opening != null) {
                        sb.append("\n<PLACEMENT_OPENING_DETAILS>\n");
                        sb.append("This application originated from a placement cell opening:\n");
                        sb.append("Company: ").append(opening.getCompanyName()).append("\n");
                        sb.append("Job Role: ").append(opening.getJobRole()).append("\n");
                        if (opening.getJobType() != null) sb.append("Job Type: ").append(opening.getJobType()).append("\n");
                        if (opening.getLocation() != null) sb.append("Location: ").append(opening.getLocation()).append("\n");
                        if (opening.getWorkMode() != null) sb.append("Work Mode: ").append(opening.getWorkMode()).append("\n");
                        if (opening.getPackageDetails() != null) sb.append("Package/Stipend: ").append(opening.getPackageDetails()).append("\n");
                        if (opening.getEligibility() != null) sb.append("Eligibility: ").append(opening.getEligibility()).append("\n");
                        if (opening.getDegreeTypes() != null && !opening.getDegreeTypes().isBlank()) {
                            sb.append("Degree Types: ").append(opening.getDegreeTypes()).append("\n");
                        }
                        if (opening.getEligibleBranches() != null && !opening.getEligibleBranches().isBlank()) {
                            sb.append("Eligible Branches: ").append(opening.getEligibleBranches()).append("\n");
                        }
                        if (opening.getDeadline() != null) sb.append("Deadline: ").append(opening.getDeadline()).append("\n");
                        sb.append("Status: ").append(opening.getStatus()).append("\n");
                        if (opening.getDescription() != null && !opening.getDescription().trim().isEmpty()) {
                            sb.append("Opening Description:\n").append(opening.getDescription().trim()).append("\n");
                        }
                        sb.append("</PLACEMENT_OPENING_DETAILS>\n");
                    }
                }

                sb.append("===================================\n\n");
            }
        } else {
            // General Applications Overview for user
            List<Application> apps = applicationRepository.findByUserIdOrderByAppliedDateDescIdDesc(userId);
            sb.append("=== USER APPLICATIONS OVERVIEW ===\n");
            if (apps.isEmpty()) {
                sb.append("No active applications tracked yet.\n");
            } else {
                sb.append("Total active applications: ").append(apps.size()).append("\n");
                for (Application a : apps) {
                    sb.append("- [ID: ").append(a.getId()).append("] ")
                            .append(a.getCompanyName()).append(" — ").append(a.getJobRole())
                            .append(" (Status: ").append(a.getStatus()).append(")");
                    if (a.getPlacementOpeningId() != null) {
                        sb.append(" [Source: Placement Cell Opening]");
                    }
                    if (a.getFollowUpDate() != null) {
                        sb.append(" [Follow-up: ").append(a.getFollowUpDate()).append("]");
                    }
                    sb.append("\n");
                }
            }
            sb.append("===================================\n\n");
        }

        // TASK 5: Recent Chat History Context
        if (historyMessages != null && !historyMessages.isEmpty()) {
            sb.append("=== RECENT CONVERSATION HISTORY ===\n");
            for (ChatMessage msg : historyMessages) {
                sb.append("[").append(msg.getRole()).append("]: ").append(msg.getContent()).append("\n");
            }
            sb.append("===================================\n\n");
        }

        sb.append("Instructions:\n");
        sb.append("- Answer like a thoughtful career coach: lead directly with the answer or core recommendation.\n");
        sb.append("- Fit Evaluation: When comparing a job description with the user's profile, compare the JD requirements with the user's master resume and experience (years of experience and experience summary). Answer with matched strengths, gaps, and concrete suggestions for improvement. Never invent experience or accomplishments not present in the record. If the user's resume and experience summary are empty, explicitly state what is missing and advise what to add.\n");
        sb.append("- Use short paragraphs and simple bulleted lists for clear, scannable advice.\n");
        sb.append("- Do NOT use emojis anywhere in your response unless explicitly asked by the user.\n");
        sb.append("- Prefer lists, use a table only when it genuinely helps or the user asks. If you use a table, the separator row must have exactly as many cells as the header row, every row must be on its own line, and never use HTML tags such as <br>; put multiple points in one cell separated by '; '. Example:\n" +
                  "| Stage | What to expect | Prep actions |\n" +
                  "| --- | --- | --- |\n" +
                  "| Screening | 15-minute phone call | Review resume; prepare 30-second elevator pitch. |\n");
        sb.append("- Use plain ASCII hyphens (-) for bullet points, lists, and ranges. Never use non-breaking hyphens (U+2011) or special dashes.\n");
        sb.append("- Ground your answers strictly in the user's provided resume, job descriptions, and interview notes above. Never invent or hallucinate dates, facts, or details not present in the record.\n");
        sb.append("- When asked to draft an email, cover letter, or outreach message, put the entire draft inside a single Markdown code block (e.g. ```text ... ```).\n");

        return sb.toString();
    }
}
