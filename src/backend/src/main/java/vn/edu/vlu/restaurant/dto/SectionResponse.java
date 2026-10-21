package vn.edu.vlu.restaurant.dto;

import vn.edu.vlu.restaurant.domain.TableSection;

public record SectionResponse(Long id, String name) {

    public static SectionResponse from(TableSection section) {
        return new SectionResponse(section.getId(), section.getName());
    }
}
