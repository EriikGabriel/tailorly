package com.tailorly.api.job;

import java.util.Optional;

import com.tailorly.api.job.dto.JobPostingResponse;
import org.jsoup.nodes.Document;

interface JobPageExtractor {
  /** Vazio quando a página não tem o formato que este extrator reconhece. */
  Optional<JobPostingResponse> extract(Document document, String finalUrl);
}
