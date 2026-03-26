package com.llmproxy.detection.controller;

import com.llmproxy.common.dto.PromptRequest;
import com.llmproxy.common.dto.DetectionResult;
import com.llmproxy.detection.service.DetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/detect")
public class DetectionController {

    @Autowired
    private DetectionService detectionService;

    @PostMapping
    public DetectionResult detect(@RequestBody PromptRequest request) {

        // Directly call service (new logic)
        return detectionService.process(request.getPrompt());
    }
}