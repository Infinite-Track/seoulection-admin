package com.seoulection.admin.product.presentation;

import com.seoulection.admin.product.presentation.controller.ProductEnrichmentController;
import com.seoulection.admin.product.infrastructure.repository.ProductEnrichmentRepository;
import com.seoulection.admin.product.infrastructure.ProductImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ProductEnrichmentController.class)
class ProductEnrichmentViewTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductEnrichmentRepository repository;
    @MockitoBean ProductImageStorage images;
    final ProductEnrichmentRepository.Target target = new ProductEnrichmentRepository.Target("p1", "B001");
    @Test void rendersListAndBlankForm() throws Exception {
        given(repository.find("", 0)).willReturn(new PageImpl<>(List.of(target), PageRequest.of(0,25),1));
        given(repository.get("p1")).willReturn(target);
        mvc.perform(get("/admin/products/enrichment")).andExpect(status().isOk()).andExpect(content().string(containsString("B001")));
        mvc.perform(get("/admin/products/enrichment/p1")).andExpect(status().isOk()).andExpect(content().string(containsString("multipart/form-data")));
    }
    @Test void rendersEmptySearch() throws Exception {
        given(repository.find("none", 0)).willReturn(Page.empty(PageRequest.of(0,25)));
        mvc.perform(get("/admin/products/enrichment").param("q", "none")).andExpect(status().isOk());
    }
    @Test void invalidFormNeverUploads() throws Exception {
        given(repository.get("p1")).willReturn(target);
        mvc.perform(multipart("/admin/products/enrichment/p1").param("name", "kept name"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("kept name")));
        verify(images, never()).upload(any(), eq("B001")); verify(repository, never()).save(any(), any());
    }
    @Test void savesOnlyReturnedS3Url() throws Exception {
        given(repository.get("p1")).willReturn(target);
        given(images.upload(any(), eq("B001"))).willReturn("https://cdn.example.com/product-pictures/photo.png");
        mvc.perform(validRequest()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/products/enrichment"));
        verify(repository).save(eq("p1"), argThat(r -> r.getThumbnailUrl().equals("https://cdn.example.com/product-pictures/photo.png") && r.getNameKo() == null && r.getPrice().toPlainString().equals("19.99")));
    }
    @Test void uploadFailurePreservesTextAndDoesNotSave() throws Exception {
        given(repository.get("p1")).willReturn(target);
        given(images.upload(any(), eq("B001"))).willThrow(new IllegalStateException("S3 unavailable"));
        mvc.perform(validRequest()).andExpect(status().isOk()).andExpect(content().string(containsString("Test name")));
        verify(repository, never()).save(any(),any());
    }
    @Test void rejectsInvalidPriceBeforeUploading() throws Exception {
        given(repository.get("p1")).willReturn(target);
        for (String price : List.of("", "-1", "abc", "1.234")) {
            var request = validRequest();
            request.params(new org.springframework.util.LinkedMultiValueMap<>());
            request.with(r -> { r.setParameter("price", price); return r; });
            mvc.perform(request).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("request", "price"));
        }
        verify(images, never()).upload(any(), any());
        verify(repository, never()).save(any(), any());
    }
    @Test void existingFieldsHaveNoInputsAndExistingImageNeedsNoUpload() throws Exception {
        var existing = new ProductEnrichmentRepository.Target("p1", "B001", java.util.Map.of(
                "name", "Stored name", "brand", "Stored brand", "category", "toners",
                "price", java.math.BigDecimal.ZERO, "thumbnailUrl", "https://cdn.example.com/stored.jpg"));
        given(repository.get("p1")).willReturn(existing);
        mvc.perform(get("/admin/products/enrichment/p1")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"brand\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"category\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("id=\"price\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("type=\"file\""))))
                .andExpect(content().string(containsString("id=\"description\"")))
                .andExpect(content().string(containsString("Stored brand")));
        mvc.perform(multipart("/admin/products/enrichment/p1")
                        .param("description", "New description").param("productUrl", "https://shop.example.com"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(images);
        verify(repository).save(eq("p1"), any());
    }
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder validRequest() {
        return multipart("/admin/products/enrichment/p1")
                .file(new MockMultipartFile("image", "image.png", "image/png", new byte[]{1}))
                .param("name","Test name").param("brand","Brand").param("price","19.99")
                .param("category","toners").param("description","설명")
                .param("productUrl","https://shop.example.com/link").param("thumbnailUrl","https://attacker.example.com/image");
    }
}
