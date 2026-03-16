package com.plywood.payroll.enums;

import lombok.Getter;

@Getter
public enum FilmCoatingType {
    NONE("Không phủ phim"),
    SIDE_1("Phủ phim 1 mặt"),
    SIDE_2("Phủ phim 2 mặt");

    private final String description;

    FilmCoatingType(String description) {
        this.description = description;
    }
}
