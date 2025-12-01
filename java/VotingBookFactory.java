package com.watchbox.maniac;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds the voting book distributed during the voting phase.
 */
public class VotingBookFactory {
    private final NamespacedKey votingKey;

    public VotingBookFactory(JavaPlugin plugin) {
        this.votingKey = new NamespacedKey(plugin, "voting_book");
    }

    public ItemStack createVotingBook(Collection<Player> voteTargets) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Voting", NamedTextColor.GOLD));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = new ArrayList<>();
        pages.add(Component.text("Select one player to vote out. You may only vote once.", NamedTextColor.DARK_AQUA));

        List<Component> targetPages = buildTargetPages(voteTargets);
        pages.addAll(targetPages);
        meta.pages(pages);

        meta.getPersistentDataContainer().set(votingKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        return book;
    }

    public boolean isVotingBook(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(votingKey, PersistentDataType.BYTE);
    }

    private List<Component> buildTargetPages(Collection<Player> voteTargets) {
        List<Component> pages = new ArrayList<>();
        Component.Builder current = Component.text();
        int lines = 0;
        for (Player target : voteTargets) {
            current.append(Component.text(target.getName() + " - ", NamedTextColor.WHITE))
                    .append(Component.text("[VOTE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/vote " + target.getName())));
            current.append(Component.newline());
            lines++;
            if (lines >= 10) {
                pages.add(current.build());
                current = Component.text();
                lines = 0;
            }
        }
        if (lines > 0 || pages.isEmpty()) {
            pages.add(current.build());
        }
        return pages;
    }
}
