package com.minhphu.mproute.dto;

import com.minhphu.mproute.entity.SystemParameter;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Du lieu form sua tham so he thong (CN-01-08).
 *
 * Ca chin tham so nam tren MOT form va luu mot lan. Ly do: chung thuong duoc chinh
 * cung nhau (vi du doi tocdo trung binh thi thuong doi luon thoi gian boc do),
 * bat bam Sua tung dong se rat vuong.
 */
@Data
public class SettingsForm {

    private List<Item> items = new ArrayList<>();

    /**
     * Mot dong tham so.
     *
     * Khong dat rang buoc Bean Validation o day: moi ma tham so co kieu va mien
     * gia tri rieng, khong the mo ta bang mot bo annotation chung. Viec kiem nam o
     * SystemParameterService.kiemGiaTri().
     */
    @Data
    public static class Item {
        private String code;
        private String value;

        // Chi de hien ra man hinh. Khong luu, va khong tin gia tri gui len tu trinh duyet
        private String description;
    }

    public static SettingsForm from(List<SystemParameter> parameters) {
        SettingsForm form = new SettingsForm();
        for (SystemParameter parameter : parameters) {
            Item row = new Item();
            row.setCode(parameter.getCode());
            row.setValue(parameter.getValue());
            row.setDescription(parameter.getDescription());
            form.getItems().add(row);
        }
        return form;
    }
}
