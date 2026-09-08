package com.seoulection.admin.ingredient.infrastructure.config;

import com.seoulection.admin.ingredient.infrastructure.document.Ingredient;
import com.seoulection.admin.ingredient.infrastructure.repository.IngredientPostgresRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class IngredientSeedConfiguration {
    @Bean
    ApplicationRunner seedIngredients(IngredientPostgresRepository repository) {
        return args -> {
            // ⚠️ "성분이 하나라도 있으면 통째로 건너뛴다"로 두지 말 것. 그러면 아래 목록에
            //    성분을 추가해도 영영 들어가지 않는다 — 코드에는 있는데 DB 에는 없어서
            //    "왜 없지"를 찾는 데 시간을 쓴다. 대신 성분마다 없을 때만 넣는다.
            //    이미 있는 것은 건드리지 않으므로 어드민이 화면에서 고친 값도 살아남는다.
            List.of(
                    i("00000000-0000-0000-0000-000000000101","Hyaluronic Acid","Hyaluronic Acid","히알루론산","HYALURONAN",List.of("HA","히알루론산"),List.of("HYALURONIC_ACID_SEARCH"),Map.of("WATER_SCORE","CORE","WRINKLE_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000102","Sodium DNA","Sodium DNA","PDRN","NUCLEOTIDE_DERIVATIVE",List.of("PDRN","소듐 DNA"),List.of("PDRN_SEARCH"),Map.of("WATER_SCORE","SUPPORT","WRINKLE_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000103","Panthenol","Panthenol","판테놀","VITAMIN_B5_DERIVATIVE",List.of("D-Panthenol","판테놀"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","CORE","WATER_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000104","Centella Asiatica","Centella Asiatica","병풀","BOTANICAL_EXTRACT",List.of("Cica","병풀"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT","RED_SPOT_SCORE","SUPPORT"),Map.of()),
                    i("00000000-0000-0000-0000-000000000105","Retinol","Retinol","레티놀","RETINOID",List.of("Vitamin A","레티놀"),List.of("RETINOID_SEARCH"),Map.of("WRINKLE_SCORE","CORE","ROUGH_SCORE","CORE"),Map.of("SOLUBILITY","OIL_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000106","Retinal","Retinal","레티날","RETINOID",List.of("Retinaldehyde","레티날"),List.of("RETINOID_SEARCH"),Map.of("WRINKLE_SCORE","CORE","ROUGH_SCORE","CORE"),Map.of()),
                    i("00000000-0000-0000-0000-000000000107","Niacinamide","Niacinamide","나이아신아마이드","VITAMIN_B3_DERIVATIVE",List.of("Nicotinamide","나이아신아마이드"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE","OILY_INTENSITY_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000108","Ascorbic Acid","Ascorbic Acid","비타민 C","VITAMIN_C_DERIVATIVE",List.of("Vitamin C","L-Ascorbic Acid"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE","ROUGH_SCORE","SUPPORT"),Map.of("STABILITY","OXIDATION_SENSITIVE")),
                    i("00000000-0000-0000-0000-000000000109","Collagen","Collagen","콜라겐","PROTEIN",List.of("Hydrolyzed Collagen","콜라겐"),List.of("BARRIER_SEARCH"),Map.of("ROUGH_SCORE","SUPPORT"),Map.of()),
                    i("00000000-0000-0000-0000-000000000110","Madecassoside","Madecassoside","마데카소사이드","TRITERPENOID_DERIVATIVE",List.of("마데카소사이드"),List.of("SOOTHING_SEARCH","BARRIER_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT"),Map.of()),
                    i("00000000-0000-0000-0000-000000000111","Ceramide NP","Ceramide NP","세라마이드 NP","CERAMIDE",List.of("Ceramide","세라마이드"),List.of("BARRIER_SEARCH"),Map.of("BARRIER_SCORE","CORE","WATER_SCORE","CORE","SENSITIVITY_SCORE","SUPPORT"),Map.of("SOLUBILITY","OIL_DISPERSIBLE")),
                    i("00000000-0000-0000-0000-000000000112","Salicylic Acid","Salicylic Acid","살리실산","BETA_HYDROXY_ACID",List.of("BHA","살리실산"),List.of("EXFOLIATION_SEARCH"),Map.of("BLACKHEAD_SCORE","CORE","ACNE_SCORE","CORE"),Map.of()),
                    i("00000000-0000-0000-0000-000000000113","Kojic Acid","Kojic Acid","코직산","PHENOLIC_COMPOUND",List.of("Kojic","코직산"),List.of("BRIGHTENING_SEARCH"),Map.of("MELANIN_SCORE","CORE"),Map.of("STABILITY","OXIDATION_SENSITIVE")),
                    i("00000000-0000-0000-0000-000000000114","Guaiazulene","Guaiazulene","구아이아줄렌","AZULENE_DERIVATIVE",List.of("Azulene","아줄렌"),List.of("SOOTHING_SEARCH"),Map.of("SENSITIVITY_SCORE","SUPPORT","RED_SPOT_SCORE","SUPPORT"),Map.of("SOLUBILITY","OIL_SOLUBLE")),
                    i("00000000-0000-0000-0000-000000000115","Human Oligopeptide-1","Human Oligopeptide-1","EGF","PEPTIDE_GROWTH_FACTOR",List.of("EGF","상피세포성장인자"),List.of("EGF_SEARCH"),Map.of("ROUGH_SCORE","SUPPORT","WRINKLE_SCORE","SUPPORT"),Map.of("STABILITY","PROTEIN_STABILITY_SENSITIVE"))
                    ,i("00000000-0000-0000-0000-000000000117","Sodium Hyaluronate","Sodium Hyaluronate","히알루론산 나트륨","HYALURONAN",List.of("히알루론산 나트륨"),List.of(),Map.of("WATER_SCORE","CORE","WRINKLE_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE"))
                    ,i("00000000-0000-0000-0000-000000000118","Hydrolyzed Hyaluronic Acid","Hydrolyzed Hyaluronic Acid","가수분해 히알루론산","HYALURONAN",List.of("Hydrolyzed HA","가수분해 히알루론산"),List.of(),Map.of("WATER_SCORE","CORE","WRINKLE_SCORE","SUPPORT"),Map.of("SOLUBILITY","WATER_SOLUBLE"))
            ).forEach(seed -> repository.saveIfAbsent(seed.getId(), seed.getInciName(), seed.getDisplayNameKo(),
                    seed.getFamily(), seed.getAliases(), seed.getEffects(), seed.getProperties()));
        };
    }

    private Ingredient i(String id, String name, String inci, String ko, String family,
                                 List<String> aliases, List<String> groups, Map<String,String> effects, Map<String,String> properties) {
        return new Ingredient(id, inci, ko, family, aliases, effects, properties);
    }
}
