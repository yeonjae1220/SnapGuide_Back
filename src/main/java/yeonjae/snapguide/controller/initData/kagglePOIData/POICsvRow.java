package yeonjae.snapguide.controller.initData.kagglePOIData;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class POICsvRow {
    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "latitude_radian")
    private double latitudeRadian;

    @CsvBindByName(column = "longitude_radian")
    private double longitudeRadian;

    @CsvBindByName(column = "num_links")
    private int numLinks;

    @CsvBindByName(column = "links")
    private String links;

    @CsvBindByName(column = "num_categories")
    private int numCategories;

    @CsvBindByName(column = "categories")
    private String categories;
}
