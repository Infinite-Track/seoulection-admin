package com.seoulection.admin.ingredient.infrastructure.config;

import com.seoulection.admin.ingredient.infrastructure.document.IngredientDocument;
import com.seoulection.admin.ingredient.infrastructure.repository.IngredientMongoRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class IngredientSeedConfiguration {
    @Bean
    ApplicationRunner seedIngredients(IngredientMongoRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            repository.saveAll(List.of(
                    i("101","Hyaluronic Acid","Hyaluronic Acid","히알루론산","HYALURONIC_ACID_FAMILY",List.of("HA","히알루론산"),List.of("HYALURONIC_ACID_SEARCH"),Map.of("WATER_SCORE","CORE","ROUGH_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("102","Sodium DNA","Sodium DNA","PDRN","NUCLEOTIDE_DERIVATIVE",List.of("PDRN","소듐 DNA"),List.of("PDRN_SEARCH"),Map.of("WATER_SCORE","SUPPORT","WRINKLE_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("103","Panthenol","Panthenol","판테놀","SOOTHING",List.of("D-Panthenol","판테놀"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","CORE","WATER_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("104","Centella Asiatica","Centella Asiatica","병풀","BOTANICAL_CICA",List.of("Cica","병풀"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT","RED_SPOT_SCORE","SUPPORT"),Map.of()),
                    i("105","Retinol","Retinol","레티놀","RETINOID",List.of("Vitamin A","레티놀"),List.of("RETINOID_SEARCH"),Map.of("WRINKLE_SCORE","CORE","ROUGH_SCORE","CORE"),Map.of("SOLUBILITY","OIL_SOLUBLE")),
                    i("106","Retinal","Retinal","레티날","RETINOID",List.of("Retinaldehyde","레티날"),List.of("RETINOID_SEARCH"),Map.of("WRINKLE_SCORE","CORE","ROUGH_SCORE","CORE"),Map.of()),
                    i("107","Niacinamide","Niacinamide","나이아신아마이드","ACTIVE",List.of("Nicotinamide","나이아신아마이드"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE","OILY_INTENSITY_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("108","Ascorbic Acid","Ascorbic Acid","비타민 C","ANTIOXIDANT",List.of("Vitamin C","L-Ascorbic Acid"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE","ROUGH_SCORE","SUPPORT"),Map.of("STABILITY","OXIDATION_SENSITIVE")),
                    i("109","Collagen","Collagen","콜라겐","CONDITIONING",List.of("Hydrolyzed Collagen","콜라겐"),List.of("BARRIER_SEARCH"),Map.of("ROUGH_SCORE","SUPPORT"),Map.of()),
                    i("110","Madecassoside","Madecassoside","마데카소사이드","BOTANICAL_CICA",List.of("마데카소사이드"),List.of("SOOTHING_SEARCH","BARRIER_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT"),Map.of()),
                    i("111","Ceramide NP","Ceramide NP","세라마이드 NP","CERAMIDE_LIPID",List.of("Ceramide","세라마이드"),List.of("BARRIER_SEARCH"),Map.of("BARRIER_SCORE","CORE","WATER_SCORE","CORE","SENSITIVITY_SCORE","SUPPORT"),Map.of("SOLUBILITY","OIL_DISPERSIBLE")),
                    i("112","Salicylic Acid","Salicylic Acid","살리실산","EXFOLIANT",List.of("BHA","살리실산"),List.of("EXFOLIATION_SEARCH"),Map.of("BLACKHEAD_SCORE","CORE","ACNE_SCORE","CORE"),Map.of()),
                    i("113","Kojic Acid","Kojic Acid","코직산","BRIGHTENING",List.of("Kojic","코직산"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE"),Map.of("STABILITY","OXIDATION_SENSITIVE")),
                    i("114","Guaiazulene","Guaiazulene","구아이아줄렌","AZULENE",List.of("Azulene","아줄렌"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT","RED_SPOT_SCORE","SUPPORT"),Map.of("SOLUBILITY","OIL_SOLUBLE")),
                    i("115","Human Oligopeptide-1","Human Oligopeptide-1","EGF","GROWTH_FACTOR",List.of("EGF","상피세포성장인자"),List.of("EGF_SEARCH"),Map.of("ROUGH_SCORE","SUPPORT","WRINKLE_SCORE","SUPPORT"),Map.of("STABILITY","PROTEIN_STABILITY_SENSITIVE"))
            ));
        };
    }

    private IngredientDocument i(String id, String name, String inci, String ko, String family,
                                 List<String> aliases, List<String> groups, Map<String,String> effects, Map<String,String> properties) {
        return new IngredientDocument(id, name, inci, ko, family, aliases, groups, effects, properties);
    }
}
