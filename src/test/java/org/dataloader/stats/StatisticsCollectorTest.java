package org.dataloader.stats;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class StatisticsCollectorTest {

    @Test
    public void basic_collection() throws Exception {
        StatisticsCollector collector = new SimpleStatisticsCollector();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheMissCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadExceptionCount(), equalTo(0L));
        assertThat(collector.getStatistics().getLoadErrorCount(), equalTo(0L));


        collector.incrementLoadCount();
        collector.incrementBatchLoadCountBy(1);
        collector.incrementCacheHitCount();
        collector.incrementBatchLoadExceptionCount();
        collector.incrementLoadErrorCount();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(1L));
        assertThat(collector.getStatistics().getCacheMissCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadExceptionCount(), equalTo(1L));
        assertThat(collector.getStatistics().getLoadErrorCount(), equalTo(1L));
    }

    @Test
    public void ratios_work() throws Exception {

        StatisticsCollector collector = new SimpleStatisticsCollector();

        collector.incrementLoadCount();

        Statistics stats = collector.getStatistics();
        assertThat(stats.getBatchLoadRatio(), equalTo(0d));
        assertThat(stats.getCacheHitRatio(), equalTo(0d));


        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementBatchLoadCountBy(1);

        stats = collector.getStatistics();
        assertThat(stats.getBatchLoadRatio(), equalTo(1d / 4d));


        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementCacheHitCount();
        collector.incrementCacheHitCount();

        stats = collector.getStatistics();
        assertThat(stats.getCacheHitRatio(), equalTo(2d / 7d));

        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementBatchLoadExceptionCount();
        collector.incrementBatchLoadExceptionCount();

        stats = collector.getStatistics();
        assertThat(stats.getBatchLoadExceptionRatio(), equalTo(2d / 10d));

        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementLoadCount();
        collector.incrementLoadErrorCount();
        collector.incrementLoadErrorCount();
        collector.incrementLoadErrorCount();

        stats = collector.getStatistics();
        assertThat(stats.getLoadErrorRatio(), equalTo(3d / 13d));
    }

    @Test
    public void thread_local_collection() throws Exception {

        final ThreadLocalStatisticsCollector collector = new ThreadLocalStatisticsCollector();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(0L));


        collector.incrementLoadCount();
        collector.incrementBatchLoadCountBy(1);
        collector.incrementCacheHitCount();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(1L));

        assertThat(collector.getOverallStatistics().getLoadCount(), equalTo(1L));
        assertThat(collector.getOverallStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(collector.getOverallStatistics().getCacheHitCount(), equalTo(1L));

        CompletableFuture.supplyAsync(() -> {

            collector.incrementLoadCount();
            collector.incrementBatchLoadCountBy(1);
            collector.incrementCacheHitCount();

            // per thread stats here
            assertThat(collector.getStatistics().getLoadCount(), equalTo(1L));
            assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(1L));
            assertThat(collector.getStatistics().getCacheHitCount(), equalTo(1L));

            // overall stats
            assertThat(collector.getOverallStatistics().getLoadCount(), equalTo(2L));
            assertThat(collector.getOverallStatistics().getBatchLoadCount(), equalTo(2L));
            assertThat(collector.getOverallStatistics().getCacheHitCount(), equalTo(2L));

            return null;
        }).join();

        // back on this main thread

        collector.incrementLoadCount();
        collector.incrementBatchLoadCountBy(1);
        collector.incrementCacheHitCount();

        // per thread stats here
        assertThat(collector.getStatistics().getLoadCount(), equalTo(2L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(2L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(2L));

        // overall stats
        assertThat(collector.getOverallStatistics().getLoadCount(), equalTo(3L));
        assertThat(collector.getOverallStatistics().getBatchLoadCount(), equalTo(3L));
        assertThat(collector.getOverallStatistics().getCacheHitCount(), equalTo(3L));


        // stats can be reset per thread
        collector.resetThread();

        // thread is reset
        assertThat(collector.getStatistics().getLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(0L));

        // but not overall stats
        assertThat(collector.getOverallStatistics().getLoadCount(), equalTo(3L));
        assertThat(collector.getOverallStatistics().getBatchLoadCount(), equalTo(3L));
        assertThat(collector.getOverallStatistics().getCacheHitCount(), equalTo(3L));
    }

    @Test
    public void delegating_collector_works() throws Exception {
        SimpleStatisticsCollector delegate = new SimpleStatisticsCollector();
        DelegatingStatisticsCollector collector = new DelegatingStatisticsCollector(delegate);

        assertThat(collector.getStatistics().getLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheMissCount(), equalTo(0L));


        collector.incrementLoadCount();
        collector.incrementBatchLoadCountBy(1);
        collector.incrementCacheHitCount();
        collector.incrementBatchLoadExceptionCount();
        collector.incrementLoadErrorCount();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(1L));
        assertThat(collector.getStatistics().getCacheMissCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadExceptionCount(), equalTo(1L));
        assertThat(collector.getStatistics().getLoadErrorCount(), equalTo(1L));

        assertThat(collector.getDelegateStatistics().getLoadCount(), equalTo(1L));
        assertThat(collector.getDelegateStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(collector.getDelegateStatistics().getCacheHitCount(), equalTo(1L));
        assertThat(collector.getDelegateStatistics().getCacheMissCount(), equalTo(0L));
        assertThat(collector.getDelegateStatistics().getBatchLoadExceptionCount(), equalTo(1L));
        assertThat(collector.getDelegateStatistics().getLoadErrorCount(), equalTo(1L));

        assertThat(delegate.getStatistics().getLoadCount(), equalTo(1L));
        assertThat(delegate.getStatistics().getBatchLoadCount(), equalTo(1L));
        assertThat(delegate.getStatistics().getCacheHitCount(), equalTo(1L));
        assertThat(delegate.getStatistics().getCacheMissCount(), equalTo(0L));
        assertThat(delegate.getStatistics().getBatchLoadExceptionCount(), equalTo(1L));
        assertThat(delegate.getStatistics().getLoadErrorCount(), equalTo(1L));
    }

    @Test
    public void noop_is_just_that() throws Exception {
        StatisticsCollector collector = new NoOpStatisticsCollector();
        collector.incrementLoadErrorCount();
        collector.incrementBatchLoadExceptionCount();
        collector.incrementBatchLoadCountBy(1);
        collector.incrementCacheHitCount();

        assertThat(collector.getStatistics().getLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getBatchLoadCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheHitCount(), equalTo(0L));
        assertThat(collector.getStatistics().getCacheMissCount(), equalTo(0L));

    }
}