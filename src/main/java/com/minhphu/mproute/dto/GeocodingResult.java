package com.minhphu.mproute.dto;

/**
 * Mot ket qua tim toa do tra ve cho man hinh (CN-01-05).
 *
 * Dung record vi day chi la du lieu chi doc, khong co hanh vi gi.
 *
 * @param displayName dia chi day du Nominatim doc duoc, do vao danh sach chon (BR01-06)
 * @param latitude    vi do
 * @param longitude   kinh do
 */
public record GeocodingResult(String displayName, double latitude, double longitude) {
}
