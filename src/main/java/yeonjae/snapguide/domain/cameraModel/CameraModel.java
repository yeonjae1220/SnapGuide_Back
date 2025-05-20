package yeonjae.snapguide.domain.cameraModel;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class CameraModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // HACK : string or enum, not sure now
    /**
     * "Canon", "Nikon", "Sony"
     */
    private String manufacturer;
    /**
     * "Canon EOS 5D Mark IV", "iPhone 14 Pro"
     */
    private String model;
    /**
     * "EF24-70mm f/2.8L II USM", "24.0-105.0 mm f/4.0"
     */
    private String lens;

}
