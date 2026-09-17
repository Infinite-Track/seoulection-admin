package com.seoulection.admin.product.functional.application;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FunctionalScreeningListSortTest {
    @Test
    void sortsByRegistrationDateRatherThanScreeningState() {
        MongoTemplate mongo=mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(Document.class), eq("products"))).thenReturn(List.of());
        new FunctionalScreeningList(mongo, mock(JdbcTemplate.class)).find("", "QUEUED", 0, 25);
        ArgumentCaptor<Query> query=ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("products"));
        assertEquals(new Document("created_at", 1).append("_id", 1), query.getValue().getSortObject());
    }
    @Test
    void newestButtonUsesDescendingRegistrationOrder() {
        MongoTemplate mongo=mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(Document.class), eq("products"))).thenReturn(List.of());
        new FunctionalScreeningList(mongo, mock(JdbcTemplate.class)).find("", "QUEUED", 0, 25, "newest");
        ArgumentCaptor<Query> query=ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(Document.class), eq("products"));
        assertEquals(new Document("created_at", -1).append("_id", -1), query.getValue().getSortObject());
    }
}
