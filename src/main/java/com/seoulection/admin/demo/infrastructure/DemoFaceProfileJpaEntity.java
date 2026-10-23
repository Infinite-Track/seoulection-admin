package com.seoulection.admin.demo.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** {@code demo_face_profile}(api-server V37). 스키마 주인은 api-server, 여기는 {@code ddl-auto=none}. */
@Entity
@Table(name = "demo_face_profile")
public class DemoFaceProfileJpaEntity {
    @Id
    @Column(length = 32)
    private String code;
    @Column(nullable = false, length = 64)
    private String label;
    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;
    @Column(name = "sort_order", nullable = false, columnDefinition = "integer default 0")
    private int sortOrder;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active;

    protected DemoFaceProfileJpaEntity() {
    }

    public DemoFaceProfileJpaEntity(String code, String label, String imageUrl, int sortOrder, boolean active) {
        this.code = code;
        this.label = label;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
}
