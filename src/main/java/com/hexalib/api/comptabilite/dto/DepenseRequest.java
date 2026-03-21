package com.hexalib.api.comptabilite.dto;
 
import jakarta.validation.constraints.*;
import lombok.Data;
 
import java.math.BigDecimal;
import java.time.LocalDate;
 
@Data
public class DepenseRequest {
 
    @NotBlank(message = "La description est obligatoire")
    @Size(max = 255)
    private String description;
 
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;
 
    @NotNull(message = "La date est obligatoire")
    private LocalDate dateDepense;
 
    @NotBlank(message = "La catégorie est obligatoire")
    private String categorieId;
 
    private String reference;
}
 