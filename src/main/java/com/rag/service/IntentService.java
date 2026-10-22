package com.rag.service;

import com.rag.dto.IntentResult;

public interface IntentService {
    IntentResult classify(String message);
}