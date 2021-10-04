package org.teacon.chahoutan.lucene;

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class SmartCNAnalysisConfigurer implements LuceneAnalysisConfigurer
{
    private final SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();

    @Override
    public void configure(LuceneAnalysisConfigurationContext context)
    {
        context.analyzer("smartcn").instance(this.analyzer);
    }
}
