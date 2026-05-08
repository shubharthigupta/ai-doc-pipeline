package com.aidocpipeline.queryservice.service;

import com.aidocpipeline.queryservice.dto.CitationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LlmService {

    @Value("${aws.bedrock.mock-enabled:true}")
    private boolean mockEnabled;

    /**
     * Generates an answer given the user's question and retrieved context chunks.
     *
     * This is the "Augmented Generation" part of RAG:
     * R = Retrieval (done in QueryService — finding similar chunks)
     * A = Augmented (we augment the prompt with retrieved context)
     * G = Generation (this method — LLM generates the answer)
     */
    public String generateAnswer(String question, List<CitationDto> citations) {
        if (mockEnabled) {
            return generateMockAnswer(question, citations);
        }
        return callBedrockClaude(question, citations);
    }

    /**
     * Builds the prompt that gets sent to the LLM.
     * This prompt engineering is critical — it tells Claude:
     * 1. Its role (document analysis assistant)
     * 2. The constraint (only use provided context)
     * 3. How to cite sources
     * 4. The actual question
     */
    public String buildPrompt(String question, List<CitationDto> citations) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a helpful document analysis assistant. ");
        prompt.append("Answer the question using ONLY the information provided in the context below. ");
        prompt.append("If the answer cannot be found in the context, say: ");
        prompt.append("'I could not find relevant information in the uploaded documents.' ");
        prompt.append("Always mention which document your answer comes from.\n\n");

        prompt.append("CONTEXT FROM DOCUMENTS:\n");
        prompt.append("=".repeat(50)).append("\n");

        for (int i = 0; i < citations.size(); i++) {
            CitationDto citation = citations.get(i);
            prompt.append(String.format("[Source %d] Document: %s, Chunk: %d\n",
                    i + 1, citation.getDocumentName(), citation.getChunkIndex()));
            prompt.append(citation.getRelevantText());
            prompt.append("\n").append("-".repeat(30)).append("\n");
        }

        prompt.append("=".repeat(50)).append("\n\n");
        prompt.append("QUESTION: ").append(question).append("\n\n");
        prompt.append("ANSWER (cite sources as [Source N]):");

        return prompt.toString();
    }

    /**
     * LOCAL MOCK — generates a realistic-looking answer without calling AWS.
     * Summarises the top citation so the response feels meaningful.
     */
    private String generateMockAnswer(String question, List<CitationDto> citations) {
        log.info("Generating mock answer for question: {}", question);

        if (citations.isEmpty()) {
            return "I could not find relevant information in the uploaded documents " +
                    "to answer your question: \"" + question + "\". " +
                    "Please ensure relevant documents have been uploaded and processed.";
        }

        CitationDto topCitation = citations.get(0);

        // Truncate the chunk text for the answer preview
        String preview = topCitation.getRelevantText().length() > 200
                ? topCitation.getRelevantText().substring(0, 200) + "..."
                : topCitation.getRelevantText();

        StringBuilder answer = new StringBuilder();
        answer.append(String.format(
                "Based on the uploaded documents, here is what I found regarding \"%s\":\n\n",
                question));
        answer.append(String.format(
                "[Source 1] From \"%s\" (chunk %d):\n%s\n\n",
                topCitation.getDocumentName(),
                topCitation.getChunkIndex(),
                preview));

        if (citations.size() > 1) {
            answer.append(String.format(
                    "Additional context was found in %d other document section(s). ",
                    citations.size() - 1));
            answer.append("See citations below for full details.\n\n");
        }

        answer.append("Note: This is a mock response for local development. ");
        answer.append("Real AI-generated answers will be available after AWS Bedrock ");
        answer.append("is configured in Phase 7.");

        return answer.toString();
    }

    /**
     * PRODUCTION — calls AWS Bedrock Claude for real AI answer generation.
     * Implemented in Phase 7.
     */
    private String callBedrockClaude(String question, List<CitationDto> citations) {
        throw new UnsupportedOperationException(
                "Real Bedrock not configured. Set aws.bedrock.mock-enabled=true for local dev."
        );
    }
}