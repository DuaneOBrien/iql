package com.indeed.iql2.language.query.fieldresolution;

import com.google.common.collect.ImmutableMap;
import com.indeed.iql.exceptions.IqlKnownException;
import com.indeed.iql.metadata.DatasetsMetadata;
import com.indeed.iql.metadata.ImhotepMetadataCache;
import com.indeed.iql.web.FieldFrequencyCache;
import com.indeed.iql2.language.AggregateMetric;
import com.indeed.iql2.language.DocFilter;
import com.indeed.iql2.language.DocMetric;
import com.indeed.iql2.language.JQLParser;
import com.indeed.iql2.language.query.Queries;
import com.indeed.iql2.language.query.fieldresolution.ScopedFieldResolver.MetricResolverCallback;
import com.indeed.iql2.server.web.servlets.DimensionUtils;
import com.indeed.iql2.server.web.servlets.dataset.AllData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.indeed.iql2.language.query.fieldresolution.FieldResolver.FAILED_TO_RESOLVE_DATASET;
import static com.indeed.iql2.language.query.fieldresolution.FieldResolver.FAILED_TO_RESOLVE_FIELD;
import static com.indeed.iql2.language.query.fieldresolution.ScopedFieldResolver.PLAIN_DOC_METRIC_CALLBACK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author jwolfe
 */
public class FieldResolverTest {
    public static FieldResolver fromQuery(final String query) {
        final JQLParser.QueryContext parseResult = Queries.parseQueryContext(query, false);
        final FieldResolver resolver = FieldResolver.build(parseResult, parseResult.fromContents(), AllData.DATASET.getDatasetsMetadata());
        resolver.setErrorMode(FieldResolver.ErrorMode.IMMEDIATE);
        return resolver;
    }

    private static JQLParser.IdentifierContext parseIdentifier(final String input) {
        return Queries.runParser(input, JQLParser::identifierTerminal).identifier();
    }

    private static JQLParser.SinglyScopedFieldContext parseSinglyScopedField(final String input) {
        return Queries.runParser(input, JQLParser::singlyScopedFieldTerminal).singlyScopedField();
    }

    @Test
    public void testSimpleCase() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of("organic", "oji"), scopedResolver.resolve(parseIdentifier("oji")));
        assertEquals(FieldSet.of("organic", "tk"), scopedResolver.resolve(parseIdentifier("tk")));
        assertEquals(FieldSet.of("organic", "ojc"), scopedResolver.resolve(parseIdentifier("ojc")));
    }

    @Test
    public void testCaseInsensitiveDataset() {
        final FieldResolver fieldResolver = fromQuery("from OrGaNiC 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of("organic", "oji"), scopedResolver.resolve(parseIdentifier("oji")));
        assertEquals(FieldSet.of("organic", "tk"), scopedResolver.resolve(parseIdentifier("tk")));
        assertEquals(FieldSet.of("organic", "ojc"), scopedResolver.resolve(parseIdentifier("ojc")));
    }

    @Test
    public void testCaseInsensitiveField() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of("organic", "oji"), scopedResolver.resolve(parseIdentifier("oJi")));
        assertEquals(FieldSet.of("organic", "tk"), scopedResolver.resolve(parseIdentifier("TK")));
        assertEquals(FieldSet.of("organic", "ojc"), scopedResolver.resolve(parseIdentifier("OjC")));
    }

    @Test
    public void testNonExistentDataset() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d");
        fieldResolver.setErrorMode(FieldResolver.ErrorMode.DEFERRED);
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of(FAILED_TO_RESOLVE_DATASET, FAILED_TO_RESOLVE_FIELD), scopedResolver.resolve(parseSinglyScopedField("MyFakeDataset.oji")));
        final IqlKnownException error = fieldResolver.errors();
        assertNotNull(error);
        assertTrue(error instanceof IqlKnownException.UnknownDatasetException);
        final Throwable[] suppressed = error.getSuppressed();
        assertEquals(1, suppressed.length);
        assertTrue(suppressed[0] instanceof IqlKnownException.UnknownFieldException);
    }

    @Test
    public void testNonExistentField() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        try {
            scopedResolver.resolve(parseIdentifier("MyFakeField"));
            Assert.fail("Expected UnknownFieldException");
        } catch (final IqlKnownException.UnknownFieldException e) {
        }
    }

    @Test
    public void testAlias() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d aliasing (oji as imps)");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of("organic", "oji"), scopedResolver.resolve(parseIdentifier("imps")));
        assertEquals(FieldSet.of("organic", "oji"), scopedResolver.resolve(parseIdentifier("iMPs")));
    }

    @Test
    public void testNamedMetric() {
        final FieldResolver fieldResolver = fromQuery("from organic 2d 1d select oji+3 as oji3, oji3");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(new AggregateMetric.NeedsSubstitution("oji3"), scopedResolver.resolveMetricAlias(parseIdentifier("oji3")));
        assertEquals(new AggregateMetric.NeedsSubstitution("oji3"), scopedResolver.resolveMetricAlias(parseIdentifier("oJI3")));

        try {
            scopedResolver.resolve(parseIdentifier("oji3"));
            Assert.fail("Expected UnknownFieldException");
        } catch (final IqlKnownException.UnknownFieldException e) {
        }
    }

    @Test
    public void testMultipleDatasets() {
        final FieldResolver fieldResolver = fromQuery("from dataset1 2d 1d, dataset2 aliasing (intField3 as intField2)");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();
        assertEquals(FieldSet.of("dataset1", "intField1", "dataset2", "intField1"), scopedResolver.resolve(parseIdentifier("intField1")));
        assertEquals(FieldSet.of("dataset1", "intField2", "dataset2", "intField3"), scopedResolver.resolve(parseIdentifier("intField2")));
        assertEquals(FieldSet.of("dataset2", "intField3"), scopedResolver.resolve(parseSinglyScopedField("dataset2.intField2")));
        final ScopedFieldResolver smallerScopedResolver = scopedResolver.forScope(Collections.singleton("dataset2"));
        assertEquals(FieldSet.of("dataset2", "intField3"), smallerScopedResolver.resolve(parseIdentifier("intField3")));
        assertEquals(FieldSet.of("dataset2", "intField3"), smallerScopedResolver.resolve(parseIdentifier("intField2")));

        try {
            scopedResolver.resolve(parseIdentifier("intField3"));
            Assert.fail("Expected UnknownFieldException");
        } catch (final IqlKnownException.UnknownFieldException e) {
        }
    }

    private static final ImhotepMetadataCache DIMENSIONS_METADATA = new ImhotepMetadataCache(new DimensionUtils.ImsClient(), AllData.DATASET.getNormalClient(), "", new FieldFrequencyCache(null), true);
    static {
        DIMENSIONS_METADATA.updateDatasets();
    }

    // same as fromQuery, but uses the dimensions data
    private static FieldResolver fromQueryDimensions(final String query) {
        final JQLParser.QueryContext parseResult = Queries.parseQueryContext(query, false);
        final DatasetsMetadata datasetsMetadata = DIMENSIONS_METADATA.get();
        final FieldResolver resolver = FieldResolver.build(parseResult, parseResult.fromContents(), datasetsMetadata);
        resolver.setErrorMode(FieldResolver.ErrorMode.IMMEDIATE);
        return resolver;
    }

    @Test
    public void testDimensionsDocMetric() {
        final DocMetric failure = new DocMetric.Field(FieldSet.of("Failure", "Failure"));

        final FieldResolver fieldResolver = fromQueryDimensions("from dimension 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();

        final DocMetric.Field i1 = new DocMetric.Field(FieldSet.of("DIMension", "i1"));
        assertEquals(i1, scopedResolver.resolveDocMetric(parseIdentifier("i1"), PLAIN_DOC_METRIC_CALLBACK));

        final DocMetric.Field i2 = new DocMetric.Field(FieldSet.of("DIMension", "i2"));
        assertEquals(new DocMetric.Add(i1, i2), scopedResolver.resolveDocMetric(parseIdentifier("plus"), PLAIN_DOC_METRIC_CALLBACK));

        // Ensure it's not wrapped in qualified because {DIMension}={DIMension}
        assertEquals(new DocMetric.Add(i1, i2), scopedResolver.resolveDocMetric(parseSinglyScopedField("dimension.plus"), PLAIN_DOC_METRIC_CALLBACK));
    }

    @Test
    public void testDimensionsDocFilter() {
        final DocFilter.Never failure = new DocFilter.Never();
        final DocFilter.Always success = new DocFilter.Always();

        final FieldResolver fieldResolver = fromQueryDimensions("from dimension 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();

        assertEquals(success, scopedResolver.resolveDocFilter(parseIdentifier("i1"), new MetricResolverCallback<DocFilter>() {
            @Override
            public DocFilter plainFields(final FieldSet fieldSet) {
                Assert.assertEquals(FieldSet.of("DIMension", "i1"), fieldSet);
                return success;
            }

            @Override
            public DocFilter metric(final DocMetric metric) {
                Assert.fail("Should not find metric");
                return failure;
            }
        }));

        final DocMetric.Field i1 = new DocMetric.Field(FieldSet.of("DIMension", "i1"));
        final DocMetric.Field i2 = new DocMetric.Field(FieldSet.of("DIMension", "i2"));
        assertEquals(success, scopedResolver.resolveDocFilter(parseIdentifier("plus"), new MetricResolverCallback<DocFilter>() {
            @Override
            public DocFilter plainFields(final FieldSet fieldSet) {
                Assert.fail("Should find metric");
                return failure;
            }

            @Override
            public DocFilter metric(final DocMetric metric) {
                Assert.assertEquals(new DocMetric.Add(i1, i2), metric);
                return success;
            }
        }));

        // Ensure it's not wrapped in qualified because {DIMension}={DIMension}
        assertEquals(success, scopedResolver.resolveDocFilter(parseSinglyScopedField("dimension.plus"), new MetricResolverCallback<DocFilter>() {
            @Override
            public DocFilter plainFields(final FieldSet fieldSet) {
                Assert.fail("Should find metric");
                return failure;
            }

            @Override
            public DocFilter metric(final DocMetric metric) {
                Assert.assertEquals(new DocMetric.Add(i1, i2), metric);
                return success;
            }
        }));
    }

    @Test
    public void testDimensionsAggregateMetric() {
        final FieldResolver fieldResolver = fromQueryDimensions("from dimension 2d 1d");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();

        final DocMetric.Field i1 = new DocMetric.Field(FieldSet.of("DIMension", "i1"));
        final DocMetric.Field i2 = new DocMetric.Field(FieldSet.of("DIMension", "i2"));
        assertEquals(new AggregateMetric.DocStats(i1), scopedResolver.resolveAggregateMetric(parseIdentifier("i1")));
        assertEquals(new AggregateMetric.DocStats(new DocMetric.Add(i1, i2)), scopedResolver.resolveAggregateMetric(parseIdentifier("plus")));
        assertEquals(new AggregateMetric.DocStats(new DocMetric.Add(i1, i2)), scopedResolver.resolveAggregateMetric(parseSinglyScopedField("dimension.plus")));
    }

    @Test
    public void testMultiDatasetDimensions() {
        final FieldResolver fieldResolver = fromQueryDimensions("from dimension 2d 1d, dimension2");
        final ScopedFieldResolver scopedResolver = fieldResolver.universalScope();

        assertEquals(new DocMetric.Field(FieldSet.of("DIMension", "i1", "dimension2", "i1")), scopedResolver.resolveDocMetric(parseIdentifier("i1"), PLAIN_DOC_METRIC_CALLBACK));

        assertEquals(new DocMetric.Field(FieldSet.of("DIMension", "i2", "dimension2", "i1")), scopedResolver.resolveDocMetric(parseIdentifier("i2"), PLAIN_DOC_METRIC_CALLBACK));

        final DocMetric.Qualified qualifiedExpected = new DocMetric.Qualified("DIMension", new DocMetric.Field(FieldSet.of("DIMension", "i1")));
        assertEquals(qualifiedExpected, scopedResolver.resolveDocMetric(parseSinglyScopedField("dimension.aliasi1"), PLAIN_DOC_METRIC_CALLBACK));

        final DocMetric.PerDatasetDocMetric calcExpected = new DocMetric.PerDatasetDocMetric(
                ImmutableMap.of(
                        "DIMension",
                        new DocMetric.Multiply( // calc = (i1 + i2) * 10
                                new DocMetric.Add(
                                        new DocMetric.Field(FieldSet.of("DIMension", "i1")),
                                        new DocMetric.Field(FieldSet.of("DIMension", "i2"))
                                ),
                                new DocMetric.Constant(10)
                        ),
                        "dimension2",
                        new DocMetric.Multiply( // calc = (i1 + i2) * 10
                                new DocMetric.Add(
                                        new DocMetric.Field(FieldSet.of("dimension2", "i1")),
                                        new DocMetric.Field(FieldSet.of("dimension2", "i1")) // i2 is aliased to i1
                                ),
                                new DocMetric.Constant(10)
                        )
                )
        );
        assertEquals(calcExpected, scopedResolver.resolveDocMetric(parseIdentifier("calc"), PLAIN_DOC_METRIC_CALLBACK));
    }
}