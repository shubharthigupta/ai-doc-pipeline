package com.aidocpipeline.queryservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class QueryMetrics {

    private final Counter queriesTotal;
    private final Counter queriesWithNoResults;
    private final Timer queryDuration;
    private final DistributionSummary citationsPerQuery;

    public QueryMetrics(MeterRegistry registry) {
        this.queriesTotal = Counter.builder("docpipeline.queries.total")
                .description("Total number of queries processed")
                .tag("service", "query")
                .register(registry);

        this.queriesWithNoResults = Counter.builder("docpipeline.queries.no_results")
                .description("Queries that returned no matching chunks")
                .tag("service", "query")
                .register(registry);

        this.queryDuration = Timer.builder("docpipeline.query.duration")
                .description("End-to-end query processing time including LLM call")
                .tag("service", "query")
                .publishPercentiles(0.5, 0.95, 0.99)  // P50, P95, P99
                .register(registry);

        this.citationsPerQuery = DistributionSummary.builder("docpipeline.query.citations")
                .description("Number of citations returned per query")
                .tag("service", "query")
                .register(registry);
    }

    public void incrementQueries() { queriesTotal.increment(); }
    public void incrementNoResults() { queriesWithNoResults.increment(); }
    public void recordCitations(int count) { citationsPerQuery.record(count); }
    public Timer.Sample startQueryTimer() { return Timer.start(); }
    public void stopQueryTimer(Timer.Sample sample) { sample.stop(queryDuration); }
}