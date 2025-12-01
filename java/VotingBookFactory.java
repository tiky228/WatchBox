import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * Builds voting books populated with alive players.
 */
public class VotingBookFactory {
    private final NamespacedKey key;

    public VotingBookFactory(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "voting_book");
    }

    public ItemStack createVotingBook(Collection<Player> voteTargets) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Voting Book", NamedTextColor.GOLD));
        meta.author(Component.text("Maniac"));
        meta.addPages(buildIntroPage(), buildTargetsPage(voteTargets));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isVotingBook(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private Component buildIntroPage() {
        return Component.text("Select one player to vote out. You can vote only once.", NamedTextColor.DARK_AQUA);
    }

    private Component buildTargetsPage(Collection<Player> voteTargets) {
        Component.Builder builder = Component.text();
        for (Player player : voteTargets) {
            builder.append(Component.text(player.getName() + " - ", NamedTextColor.WHITE));
            builder.append(Component.text("[VOTE]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/vote " + player.getName())));
            builder.append(Component.newline());
        }
        return builder.build();
    }
}
