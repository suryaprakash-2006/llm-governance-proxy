package com.llmproxy.detection.service;

import com.llmproxy.common.dto.DetectionResult;
import com.llmproxy.detection.util.RegexDetector;
import com.llmproxy.detection.util.HeuristicDetector;
import com.llmproxy.detection.util.MaskingUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DetectionService {

    private final RegexDetector regexDetector = new RegexDetector();
    private final HeuristicDetector heuristicDetector = new HeuristicDetector();

    public DetectionResult process(String prompt) {

        List<String> detected = new ArrayList<>();

        // Step 1: detect
        detected.addAll(regexDetector.detect(prompt));
        detected.addAll(heuristicDetector.detect(prompt));

        DetectionResult result = new DetectionResult();
        result.setDetectedTypes(detected);

        // Step 2: decision logic
        if (detected.contains("SECRET")) {
            result.setAction("BLOCK");
        } else if (detected.contains("EMAIL")) {
            result.setAction("MASK");
        } else {
            result.setAction("ALLOW");
        }

        // Step 3: masking
        result.setMaskedPrompt(
                MaskingUtil.applyMasking(prompt)
        );

        return result;
    }
}