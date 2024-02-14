package ti4.draft;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ti4.generator.Mapper;
import ti4.generator.PositionMapper;
import ti4.generator.TileHelper;
import ti4.helpers.AliasHandler;
import ti4.helpers.Storage;
import ti4.model.FactionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class FrankenItemTest {

    @BeforeAll
    public static void init() {
        TileHelper.init();
        PositionMapper.init();
        Mapper.init();
        AliasHandler.init();
        Storage.init();
    }


    @Test
    public void testAllCardsGenerateSuccessfully() {
        var factions = FrankenUtils.getAllFrankenLegalFactions();
        assertDoesNotThrow(() -> getAllCards(factions));
    }

    @Test
    public void testAllCardsHaveValidShortNames() {
        var factions = FrankenUtils.getAllFrankenLegalFactions();
        var cards = getAllCards(factions);
        for (var card: cards) {
            assert !card.getShortDescription().isEmpty() : card.getAlias();
        };
    }

    @Test
    public void testAllCardsHaveValidLongNames() {
        var factions = FrankenUtils.getAllFrankenLegalFactions();
        var cards = getAllCards(factions);
        for (var card: cards) {
            try {
                assert !card.getLongDescription().isEmpty() : card.getAlias();
            }
            catch (Exception e)
            {
                Assertions.fail(card.getAlias() + " threw an exception: " + e);
            }
        };
    }

    @Test
    public void testAllCardsHaveValidEmoji() {
        var factions = FrankenUtils.getAllFrankenLegalFactions();
        var cards = getAllCards(factions);
        for (var card: cards) {
            assert !card.getItemEmoji().isEmpty() : card.getAlias();
        };
    }

    @Test
    public void errataFileSanityTest() {
        var factions = FrankenUtils.getAllFrankenLegalFactions();
        var cards = getAllCards(factions);
        for (var card: cards) {
            // PoK
            assert(!card.getAlias().equals("ABILITY:mitosis"));
            assert(!card.getAlias().equals("ABILITY:hubris"));
            assert(!card.getAlias().equals("ABILITY:fragile"));
            assert(!card.getAlias().equals("STARTINGTECH:sardakk"));
            assert(!card.getAlias().equals("AGENT:mentakagent"));
            assert(!card.getAlias().equals("ABILITY:creuss_gate"));

            // DS
            assert(!card.getAlias().equals("ABILITY:probability_algorithms"));
            assert(!card.getAlias().equals("MECH:kjalengard_mech"));
            assert(!card.getAlias().equals("ABILITY:singularity_point"));
            assert(!card.getAlias().equals("HERO:vadenhero"));
            assert(!card.getAlias().equals("AGENT:mykomentoriagent"));
            assert(!card.getAlias().equals("ABILITY:stealth_insertion"));
        }
    }

    private List<DraftItem> getAllCards(List<FactionModel> factions) {
        List<DraftItem> allCards = new ArrayList<>();
        Map<DraftItem.Category, List<DraftItem>> cardsByCategory = FrankenUtils.generateAllCards(factions);
        for (Map.Entry<DraftItem.Category, List<DraftItem>> entry : cardsByCategory.entrySet()) {
            allCards.addAll(entry.getValue());
        }

        return allCards;
    }
}
