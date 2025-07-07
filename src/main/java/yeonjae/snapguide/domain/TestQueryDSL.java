package yeonjae.snapguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class TestQueryDSL {
    @Id
    @GeneratedValue
    private Long id;
}
