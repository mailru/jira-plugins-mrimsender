package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.jira.issue.fields.CustomField;
import com.google.common.primitives.Longs;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class SprintCollector extends Collector {
    private final CustomField sprintField;
    private final IndexSearcher searcher;
    private final Set<String> sprintIds = new HashSet<>();
    private int docBase;

    SprintCollector(CustomField sprintField, IndexSearcher searcher) {
        this.sprintField = sprintField;
        this.searcher = searcher;
    }

    @Override
    public void setScorer(Scorer scorer) {

    }

    @Override
    public void collect(int i) {
        try {
            Document doc = searcher.doc(docBase + i);

            sprintIds.addAll(Arrays.asList(doc.getValues(sprintField.getId())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setNextReader(IndexReader indexReader, int docBase) {
        this.docBase = docBase;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }

    public Set<Long> getSprintIds() {
        return sprintIds
            .stream()
            .map(Longs::tryParse)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
