package ti4.draft;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemDraftPhase extends DraftPhase {

    @JsonManagedReference("phase")
    public Map<String, DraftBag> Bags = new HashMap<>();
    public Map<String, List<DraftItem>> Queues = new HashMap<>();
    public Map<String, Boolean> ReadyFlags = new HashMap<>();

    public Map<DraftItem.Category, Integer> CategoryLimits = new HashMap<>();
    public int PassCount;

    public int getLimitForCategory(DraftItem.Category category) {
        return CategoryLimits.getOrDefault(category, 0);
    }
}
