package com.tailorly.api.job.reader;

import org.jsoup.nodes.Document;

/** Página já baixada e interpretada, com o endereço final depois dos redirecionamentos. */
public record FetchedPage(Document document, String finalUrl) {
}
