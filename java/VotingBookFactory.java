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
    private static final int LINES_PER_PAGE = 12;
    private final NamespacedKey votingKey;

    public VotingBookFactory(JavaPlugin plugin) {
        this.votingKey = new NamespacedKey(plugin, "voting_book");
    }

    public ItemStack createVotingBook(Collection<Player> voteTargets) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text("Voting", NamedTextColor.AQUA));
        meta.author(Component.text("WatchBox"));

        List<Component> pages = new ArrayList<>();
        BookPageBuilder builder = new BookPageBuilder(LINES_PER_PAGE);
        builder.addLine(Component.text("Select one player to vote out. You may only vote once.", NamedTextColor.DARK_AQUA));
        builder.addBlankLine();
        builder.addHeading("Candidates", NamedTextColor.GREEN);

        for (Player target : voteTargets) {
            Component line = Component.text(target.getName() + " - ", NamedTextColor.GRAY)
                    .append(Component.text("[VOTE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/vote " + target.getName())));
            builder.addLine(line);
        }
        pages.addAll(builder.build());
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
}
