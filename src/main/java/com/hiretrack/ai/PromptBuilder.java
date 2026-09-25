package com.hiretrack.ai;

import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.interview.Interview;
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

    public String buildSystemPrompt(Long userId, Long applicationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the HireTrack AI Assistant — a focused, practical career & job application coach.\n");
        sb.append("Your goal is to help job seekers stay organized, prepare for interviews, and move their applications forward.\n\n");

        // 1. User Resume Context
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        if (profile != null && profile.getResumeText() != null && !profile.getResumeText().trim().isEmpty()) {
            sb.append("=== USER MASTER RESUME ===\n");
            sb.append(profile.getResumeText().trim());
            sb.append("\n===========================\n\n");
        } else {
            sb.append("=== USER MASTER RESUME ===\n(No resume text uploaded yet)\n===========================\n\n");
        }

        // 2. Application Context (Targeted vs General Summary)
        if (applicationId != null) {
            Application app = applicationRepository.findByIdAndUserId(applicationId, userId).orElse(null);
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
                    if (a.getFollowUpDate() != null) {
                        sb.append(" [Follow-up: ").append(a.getFollowUpDate()).append("]");
                    }
                    sb.append("\n");
                }
            }
            sb.append("===================================\n\n");
        }

        sb.append("Instructions:\n");
        sb.append("- Provide clear, concise, actionable advice.\n");
        sb.append("- Ground your answers in the user's resume, job descriptions, and interview notes above.\n");
        sb.append("- Do not invent facts not present in the user's record.\n");

        return sb.toString();
    }
}
